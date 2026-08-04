package com.malva_pastry_shop.backend.template;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cubre lo que quedó desbloqueado al partir {@code form :: search} y al
 * distinguir los dos estados vacíos.
 *
 * <p>Son las dos piezas que impedían migrar los 11 listados que siguen con la
 * paginación vieja: {@code sales/list} necesita tres filtros en un mismo
 * formulario, y tanto {@code products/list} como {@code sales/list} distinguen
 * "todavía no hay nada" de "nada coincide con estos filtros".
 */
@WebMvcTest(controllers = FragmentProbeController.class)
@Import(FragmentProbeController.class)
@DisplayName("Campos de filtro y estados vacíos")
class FilterFieldsTest {

    @Autowired
    private MockMvc mockMvc;

    private String render(String url) throws Exception {
        return mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    // ---------- Campos de filtro ----------

    @Test
    @DisplayName("tres campos conviven dentro de un mismo formulario")
    void threeFieldsInOneForm() throws Exception {
        String html = render("/probe/filters?search=torta&startDate=2026-03-01&endDate=2026-03-31");

        // Un solo <form>: la versión anterior emitía uno por campo y los
        // formularios no se pueden anidar, así que esta página era imposible.
        assertThat(html.split("<form", -1).length - 1).isEqualTo(1);

        assertThat(html).contains("name=\"search\"").contains("value=\"torta\"");
        assertThat(html).contains("name=\"startDate\"").contains("value=\"2026-03-01\"");
        assertThat(html).contains("name=\"endDate\"").contains("value=\"2026-03-31\"");
    }

    @Test
    @DisplayName("cada campo tiene id propio y su label lo referencia")
    void everyFieldIsLabelled() throws Exception {
        String html = render("/probe/filters");

        // El id ya no es fijo ("search" hardcodeado con su label a mano), así
        // que varios campos conviven sin pisarse ni robarse la etiqueta.
        assertThat(html).contains("for=\"q\"").contains("id=\"q\"");
        assertThat(html).contains("for=\"startDate\"").contains("id=\"startDate\"");
        assertThat(html).contains("for=\"endDate\"").contains("id=\"endDate\"");

        // Los tres filtros de sales/list tienen hoy un <label> sin for: ningún
        // lector de pantalla los asocia a su campo.
        assertThat(html).doesNotContain("<label class=");
    }

    @Test
    @DisplayName("las fechas llegan al input en ISO, que es lo que exige el navegador")
    void dateValuesAreIso() throws Exception {
        String html = render("/probe/filters?startDate=2026-03-01");

        // Un <input type="date"> con un valor en formato local se muestra
        // VACÍO: el navegador lo descarta sin avisar.
        assertThat(html).contains("value=\"2026-03-01\"");
        assertThat(html).doesNotContain("1/3/26");
    }

    // ---------- Estados vacíos ----------

    @Test
    @DisplayName("sin filtros: invita a crear el primero")
    void emptyWithoutFilters() throws Exception {
        String html = render("/probe/empty");

        assertThat(html).contains("No hay tags registrados");
        assertThat(html).contains("Los tags permiten clasificar productos.");
        assertThat(html).contains("Crear tag");
        assertThat(html).doesNotContain("Limpiar filtros");
    }

    @Test
    @DisplayName("con filtros: ofrece limpiarlos, no crear")
    void emptyWithFilters() throws Exception {
        String html = render("/probe/empty?clearUrl=/tags");

        // "Crear el primero" es la acción equivocada acá: puede haber cien
        // tags y ninguno coincidir con el filtro.
        assertThat(html).contains("Limpiar filtros");
        assertThat(html).contains("href=\"/tags\"");
        assertThat(html).doesNotContain("Crear tag");
        assertThat(html).contains("Ningún resultado coincide con los filtros aplicados.");
        assertThat(html).doesNotContain("Los tags permiten clasificar productos.");
    }
}
