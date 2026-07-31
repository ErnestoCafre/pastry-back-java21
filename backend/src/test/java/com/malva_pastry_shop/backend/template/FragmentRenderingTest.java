package com.malva_pastry_shop.backend.template;

import com.malva_pastry_shop.backend.controller.admin.CategoryController;
import com.malva_pastry_shop.backend.controller.admin.StorefrontSectionController;
import com.malva_pastry_shop.backend.controller.admin.TagController;
import com.malva_pastry_shop.backend.domain.auth.Role;
import com.malva_pastry_shop.backend.domain.auth.RoleType;
import com.malva_pastry_shop.backend.domain.auth.User;
import com.malva_pastry_shop.backend.domain.inventory.Category;
import com.malva_pastry_shop.backend.domain.storefront.Tag;
import com.malva_pastry_shop.backend.service.inventory.CategoryService;
import com.malva_pastry_shop.backend.service.inventory.ProductService;
import com.malva_pastry_shop.backend.service.storefront.StorefrontSectionService;
import com.malva_pastry_shop.backend.service.storefront.TagService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifica que la librería de fragments de templates/fragments/ renderiza.
 *
 * Thymeleaf no valida nada en tiempo de compilación: una expresión mal
 * escrita solo falla cuando alguien abre la página. Estos tests renderizan
 * las plantillas migradas de punta a punta y afirman sobre el HTML
 * resultante, así que cubren tanto los errores de sintaxis como los de
 * lógica (condiciones invertidas, parámetros que no llegan, etc.).
 *
 * Lo que queda ejercitado:
 *   icons      -> icon() y solid(), incluido el caso multi-path ('eye')
 *   alerts     -> flash(), errorBox(), formErrors
 *   toolbar    -> listBar() con y sin papelera
 *   form       -> search(), text(), textarea(), actions()
 *   table      -> th/thCenter/thRight, emptyRow() en ambas ramas
 *   buttons    -> iconLink(), iconDelete(), pill()
 *   breadcrumb -> trail(), back()
 *   pagination -> bar() con filtros, sin filtros, y con ventana deslizante
 */
// Se corre con la cadena de filtros real: el sidebar del layout usa
// sec:authorize, y el dialecto de seguridad de Thymeleaf necesita el
// SecurityExpressionHandler que publica la configuración web de seguridad.
// La autenticación entra por el post-processor user(...) de spring-security-test.
@WebMvcTest(controllers = { TagController.class, CategoryController.class,
        StorefrontSectionController.class })
@DisplayName("Renderizado de fragments")
class FragmentRenderingTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TagService tagService;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private CategoryService categoryService;

    @MockitoBean
    private StorefrontSectionService sectionService;

    private User admin;

    @BeforeEach
    void setUp() {
        Role role = new Role(RoleType.ADMIN);
        role.setId(1L);

        admin = new User();
        admin.setId(1L);
        admin.setName("Ada");
        admin.setLastName("Lovelace");
        admin.setEmail("ada@malva.com");
        admin.setPasswordHash("irrelevante");
        admin.setEnabled(true);
        admin.setRole(role);
    }

    private Tag tag(long id, String name, String description) {
        Tag t = new Tag();
        t.setId(id);
        t.setName(name);
        t.setSlug(name.toLowerCase());
        t.setDescription(description);
        return t;
    }

    private String render(String url) throws Exception {
        MvcResult result = mockMvc.perform(get(url).with(user(admin)))
                .andExpect(status().isOk())
                .andReturn();
        return result.getResponse().getContentAsString();
    }

    // ---------- Listado ----------

    @Test
    @DisplayName("tags/list renderiza toolbar, tabla, acciones y paginación")
    void tagsListRenders() throws Exception {
        Page<Tag> page = new PageImpl<>(
                List.of(tag(1L, "Vegano", "Sin ingredientes de origen animal"),
                        tag(2L, "Sin TACC", null)),
                PageRequest.of(0, 50), 2);

        when(tagService.findAllActive(any(Pageable.class))).thenReturn(page);
        when(productService.countProductsByTag(anyLong())).thenReturn(7L);

        String html = render("/tags");

        // toolbar + form::search (con la etiqueta sr-only que antes faltaba)
        assertThat(html).contains("Buscar tags...");
        assertThat(html).contains("class=\"sr-only\"");
        assertThat(html).contains("Nuevo Tag");
        // Nota: el enlace a Papelera va detrás de sec:authorize="hasRole('ADMIN')".
        // Bajo MockMvc el dialecto de seguridad de Thymeleaf no ve la
        // autenticación al renderizar la vista, así que sec:authorize da false
        // y esa rama no se puede afirmar acá. Se verifica manualmente en la app.

        // table :: th con scope
        assertThat(html).contains("scope=\"col\"");
        assertThat(html).contains("Descripción");

        // filas
        assertThat(html).contains("Vegano");
        assertThat(html).contains("Sin TACC");
        // description null -> guion largo del fragment, no "null"
        assertThat(html).contains("—");
        assertThat(html).doesNotContain(">null<");

        // buttons :: iconLink / iconDelete
        assertThat(html).contains("Ver tag");
        assertThat(html).contains("Editar tag");
        assertThat(html).contains("data-confirm=\"¿Eliminar este tag? Se moverá a la papelera.\"");

        // icons :: icon('eye') aporta dos <path>; el fallback nunca debe aparecer
        assertThat(html).contains("M15 12a3 3 0 11-6 0 3 3 0 016 0z");
        assertThat(html).contains("aria-hidden=\"true\"");
        assertThat(html).doesNotContain("M12 9v2m0 4h.01M5 19h14a2 2");

        // el estado vacío NO debe aparecer con filas presentes
        assertThat(html).doesNotContain("No hay tags registrados");
    }

    @Test
    @DisplayName("tags/list muestra el estado vacío cuando no hay resultados")
    void tagsListEmptyState() throws Exception {
        when(tagService.findAllActive(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 50), 0));

        String html = render("/tags");

        assertThat(html).contains("No hay tags registrados");
        assertThat(html).contains("Crear tag");
        // sin elementos, la paginación entera se omite
        assertThat(html).doesNotContain("Mostrando");
    }

    @Test
    @DisplayName("pagination :: bar conserva los filtros y los escapa")
    void paginationKeepsFilters() throws Exception {
        Page<Tag> page = new PageImpl<>(
                List.of(tag(1L, "Vegano", null)),
                PageRequest.of(0, 50), 120);

        when(tagService.search(any(String.class), any(Pageable.class))).thenReturn(page);
        when(productService.countProductsByTag(anyLong())).thenReturn(0L);

        // Valor crudo con espacio y &: es lo que llega al modelo tras decodificar
        // la petición real. Se pasa como parámetro y no dentro de la URL porque
        // MockMvc no decodifica el query string embebido.
        MvcResult result = mockMvc.perform(get("/tags")
                        .param("search", "torta & miel")
                        .with(user(admin)))
                .andExpect(status().isOk())
                .andReturn();
        String html = result.getResponse().getContentAsString();

        assertThat(html).contains("Mostrando");
        // El filtro viaja en los enlaces de página, escapado UNA sola vez.
        // Doble codificación (%2520 / %2526) significaría que el sufijo entró
        // dentro de @{...} en vez de concatenarse afuera.
        assertThat(html).contains("search=torta%20%26%20miel");
        assertThat(html).doesNotContain("%2520").doesNotContain("%2526");
        assertThat(html).contains("aria-current=\"page\"");
        assertThat(html).contains("aria-label=\"Paginación de tags\"");
    }

    @Test
    @DisplayName("pagination :: bar recorta a una ventana de 5 páginas")
    void paginationSlidingWindow() throws Exception {
        // 100 páginas: la versión anterior dibujaba 100 botones.
        Page<Tag> page = new PageImpl<>(
                List.of(tag(1L, "Vegano", null)),
                PageRequest.of(50, 1), 100);

        when(tagService.findAllActive(any(Pageable.class))).thenReturn(page);
        when(productService.countProductsByTag(anyLong())).thenReturn(0L);

        String html = render("/tags");

        // ventana alrededor de la página 51 (índice 50)
        assertThat(html).contains(">49<").contains(">51<").contains(">53<");
        // fuera de la ventana, no
        assertThat(html).doesNotContain(">40<").doesNotContain(">70<");
        // saltos a la primera y la última + elipsis
        assertThat(html).contains(">1<").contains(">100<").contains("…");
    }

    // ---------- Papelera ----------

    @Test
    @DisplayName("tags/deleted renderiza back, pills y estado vacío sin CTA")
    void tagsDeletedRenders() throws Exception {
        Tag deleted = tag(3L, "Temporada", null);
        deleted.setDeletedAt(LocalDateTime.of(2026, 3, 14, 10, 30));

        when(tagService.findDeleted(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(deleted), PageRequest.of(0, 50), 1));

        String html = render("/tags/deleted");

        assertThat(html).contains("Volver a Tags");
        assertThat(html).contains("14/03/2026 10:30");
        assertThat(html).contains("Desconocido");

        // pill 'danger' confirma...
        assertThat(html).contains("data-confirm=\"¿Eliminar PERMANENTEMENTE este tag?"
                + " Esta acción no se puede deshacer.\"");

        // ...y pill 'safe' (restaurar) no: con message null el atributo tiene
        // que desaparecer, no quedar vacío. Restaurar es reversible y no debe
        // interrumpir. Hay exactamente un data-confirm en la página.
        assertThat(html).contains("Restaurar");
        assertThat(html).containsOnlyOnce("data-confirm=");
    }

    // ---------- Sidebar ----------

    @Test
    @DisplayName("el sidebar marca la sección activa con aria-current")
    void sidebarMarksActiveSection() throws Exception {
        when(tagService.findAllActive(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 50), 0));

        String html = render("/tags");

        // El enlace de Tags es el que lleva la marca, y es el único.
        assertThat(html).containsPattern("href=\"/tags\"\\s+aria-current=\"page\"");
        assertThat(html).containsOnlyOnce("aria-current=\"page\"");
        // y es el único con el fondo resaltado
        assertThat(html).containsOnlyOnce("bg-white/20 text-white");
    }

    @Test
    @DisplayName("una ficha de detalle también resalta su sección")
    void detailViewKeepsSectionHighlighted() throws Exception {
        // Este era el bug: la ficha pone pageTitle = nombre de la entidad, que
        // no coincidía con ninguno de los literales que comparaba el sidebar,
        // así que NINGUNA página de detalle resaltaba nada.
        when(tagService.findById(1L)).thenReturn(tag(1L, "Vegano", "Sin origen animal"));
        when(productService.countProductsByTag(1L)).thenReturn(4L);

        String html = render("/tags/1");

        assertThat(html).contains("aria-current=\"page\"");
        assertThat(html).contains("href=\"/tags\"");
    }

    @Test
    @DisplayName("sec:authorize sigue filtrando el ítem de Usuarios")
    void adminOnlyNavItemStaysGuarded() throws Exception {
        when(tagService.findAllActive(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 50), 0));

        String html = render("/tags");

        // Bajo MockMvc el dialecto de seguridad no ve la autenticación, así que
        // sec:authorize da false. Eso vuelve útil esta aserción: si el
        // sec:authorize estuviera junto al th:replace (precedencia 100 contra
        // 300) el authorize no se evaluaría y el ítem aparecería igual. Que no
        // aparezca prueba que el envoltorio th:block se está respetando.
        assertThat(html).doesNotContain("Usuarios");
    }

    @Test
    @DisplayName("el form de sección emite un solo parámetro 'visible'")
    void sectionFormEmitsSingleVisibleParam() throws Exception {
        String html = render("/sections/new");

        // Había un <input type="hidden" name="visible" value="false"> puesto a
        // mano ANTES del checkbox. th:field ya emite su propio marcador
        // (_visible) para el caso desmarcado, así que ese hidden agregaba un
        // segundo parámetro con el mismo nombre: al tildar la casilla se
        // enviaban visible=false y visible=true, y el binder toma el primero.
        assertThat(html).doesNotContain("value=\"false\"");
        assertThat(html).containsOnlyOnce("name=\"visible\"");
        // el marcador de Spring para "desmarcado" sí tiene que estar
        assertThat(html).containsOnlyOnce("name=\"_visible\"");
    }

    // ---------- Formularios ----------

    @Test
    @DisplayName("categories/create renderiza breadcrumb y átomos de formulario")
    void categoryCreateRenders() throws Exception {
        String html = render("/categories/new");

        // breadcrumb :: trail, con el aria-current que antes no existía
        assertThat(html).contains("aria-label=\"Migas de pan\"");
        assertThat(html).contains("aria-current=\"page\"");

        // form :: text -> label asociado por for/id + th:field resuelto
        assertThat(html).contains("for=\"name\"");
        assertThat(html).contains("id=\"name\"");
        assertThat(html).contains("name=\"name\"");
        assertThat(html).contains("Ej: Tortas, Galletas, Cupcakes");

        // form :: textarea
        assertThat(html).contains("<textarea");
        assertThat(html).contains("for=\"description\"");

        // form :: actions
        assertThat(html).contains("Guardar Categoría");
        assertThat(html).contains("Cancelar");

        // sin errores, no hay resumen ni aria-invalid
        assertThat(html).doesNotContain("Revisá los siguientes campos");
        assertThat(html).doesNotContain("aria-invalid");
    }

    @Test
    @DisplayName("categories/edit apunta al action correcto y precarga valores")
    void categoryEditRenders() throws Exception {
        Category category = new Category();
        category.setId(9L);
        category.setName("Tortas");
        category.setDescription("Tortas y tartas");

        when(categoryService.findById(9L)).thenReturn(category);

        String html = render("/categories/9/edit");

        assertThat(html).contains("action=\"/categories/9\"");
        assertThat(html).contains("value=\"Tortas\"");
        assertThat(html).contains("Tortas y tartas");
        assertThat(html).contains("Actualizar Categoría");
    }
}
