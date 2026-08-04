package com.malva_pastry_shop.backend.template;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifica {@code pagination :: bar} con los filtros que los listados sin
 * migrar le van a pasar, y que hasta ahora nadie había ejercitado.
 *
 * <p>Lo único probado era un filtro String. Los 11 listados pendientes traen
 * casos que el fragment nunca vio: {@code sales/list} pasa tres filtros a la
 * vez, {@code products/list} pasa un Long, y en ambos casos lo normal es que
 * algunos vengan en null porque el usuario no los completó.
 *
 * <p>Descubrir esto migrando 11 listados sería el orden equivocado: la
 * librería tiene que aguantar antes de que nadie se apoye en ella.
 */
@WebMvcTest(controllers = FragmentProbeController.class)
@Import(FragmentProbeController.class)
@DisplayName("pagination :: bar con filtros múltiples")
class PaginationFiltersTest {

    @Autowired
    private MockMvc mockMvc;

    /** Dos páginas, para que se dibujen enlaces con querystring. */
    private Page<String> page() {
        return new PageImpl<>(List.of("uno"), PageRequest.of(0, 1), 2);
    }

    private String render(Map<String, Object> params) throws Exception {
        // requestAttr rechaza valores nulos, así que "sin filtros" se expresa
        // omitiendo el atributo: el fragment recibe params == null igual.
        var request = get("/probe/pagination")
                .requestAttr(FragmentProbeController.PAGE, page());
        if (params != null) {
            request = request.requestAttr(FragmentProbeController.PARAMS, params);
        }
        return mockMvc.perform(request)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    @DisplayName("tres filtros salen los tres, y en orden estable")
    void keepsThreeFilters() throws Exception {
        // El caso de sales/list. LinkedHashMap porque el orden de los
        // parámetros en el href debe ser reproducible: si cambiara entre
        // renders, cualquier aserción sobre la URL sería intermitente.
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("search", "torta");
        params.put("startDate", LocalDate.of(2026, 3, 1));
        params.put("endDate", LocalDate.of(2026, 3, 31));

        String html = render(params);

        // Las fechas viajan en ISO-8601, no en el formato local. Con el panel
        // fijado en es-AR, SpEL las convertía a "1/3/26": ambiguo y atado al
        // locale. El fragment fuerza toString() por eso.
        assertThat(html).contains("search=torta");
        assertThat(html).contains("startDate=2026-03-01");
        assertThat(html).contains("endDate=2026-03-31");
        assertThat(html).contains("search=torta&amp;startDate=2026-03-01&amp;endDate=2026-03-31");
    }

    @Test
    @DisplayName("los filtros nulos o vacíos no ensucian la URL")
    void dropsNullAndEmpty() throws Exception {
        // Lo habitual: el usuario filtró por fecha pero no escribió búsqueda.
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("search", null);
        params.put("startDate", LocalDate.of(2026, 3, 1));
        params.put("endDate", "");

        String html = render(params);

        assertThat(html).contains("startDate=2026-03-01");
        assertThat(html).doesNotContain("search=");
        assertThat(html).doesNotContain("endDate=");
    }

    @Test
    @DisplayName("un filtro numérico no rompe el fragment")
    void acceptsNonStringValues() throws Exception {
        // El caso de products/list: categoryId es Long, no String. El fragment
        // llama #strings.isEmpty y #uris.escapeQueryParam sobre el valor.
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("categoryId", 7L);
        params.put("search", null);

        String html = render(params);

        assertThat(html).contains("categoryId=7");
    }

    @Test
    @DisplayName("sin filtros, la URL solo lleva paginación")
    void noFilters() throws Exception {
        // El href igual trae un & separando size y page: eso lo arma @{...},
        // no el sufijo de filtros. Lo que no debe aparecer es ningún filtro.
        assertThat(render(null)).contains("/probe/pagination?size=1&amp;page=1\"");
    }

    @Test
    @DisplayName("los valores se escapan una sola vez, también con varios filtros")
    void escapesOnce() throws Exception {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("search", "torta & miel");
        params.put("categoryId", 3L);

        String html = render(params);

        assertThat(html).contains("search=torta%20%26%20miel");
        // Doble codificación: el sufijo se habría metido dentro de @{...}.
        assertThat(html).doesNotContain("%2520").doesNotContain("%2526");
    }
}
