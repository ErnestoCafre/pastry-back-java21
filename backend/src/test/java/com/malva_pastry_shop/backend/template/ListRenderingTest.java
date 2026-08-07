package com.malva_pastry_shop.backend.template;

import com.malva_pastry_shop.backend.controller.admin.CategoryController;
import com.malva_pastry_shop.backend.controller.admin.IngredientController;
import com.malva_pastry_shop.backend.controller.admin.ProductController;
import com.malva_pastry_shop.backend.controller.admin.SaleController;
import com.malva_pastry_shop.backend.controller.admin.StorefrontSectionController;
import com.malva_pastry_shop.backend.controller.admin.TagController;
import com.malva_pastry_shop.backend.controller.admin.UserController;
import com.malva_pastry_shop.backend.domain.sales.Sale;
import com.malva_pastry_shop.backend.repository.RoleRepository;
import com.malva_pastry_shop.backend.service.UserService;
import com.malva_pastry_shop.backend.service.sales.SaleService;

import java.time.LocalDateTime;
import com.malva_pastry_shop.backend.domain.auth.Role;
import com.malva_pastry_shop.backend.domain.auth.RoleType;
import com.malva_pastry_shop.backend.domain.auth.User;
import com.malva_pastry_shop.backend.domain.inventory.Category;
import com.malva_pastry_shop.backend.domain.inventory.Ingredient;
import com.malva_pastry_shop.backend.domain.inventory.Product;
import com.malva_pastry_shop.backend.domain.inventory.UnitOfMeasure;
import com.malva_pastry_shop.backend.domain.storefront.StorefrontSection;
import com.malva_pastry_shop.backend.domain.storefront.Tag;
import com.malva_pastry_shop.backend.service.inventory.CategoryService;
import com.malva_pastry_shop.backend.service.inventory.IngredientService;
import com.malva_pastry_shop.backend.service.inventory.ProductService;
import com.malva_pastry_shop.backend.service.storefront.StorefrontSectionService;
import com.malva_pastry_shop.backend.service.storefront.TagService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Renderiza los listados migrados a la librería de fragments.
 *
 * <p>Además de comprobar que las plantillas abren, fija el modelo de acciones
 * de fila que se unificó al migrarlas: <b>Ver, Editar y Borrar</b> en todos los
 * listados. Antes cada uno ofrecía un juego distinto —varios ni siquiera daban
 * "Editar", pese a que la ruta existía—, así que la acción disponible dependía
 * de en qué pantalla estuvieras.
 */
@WebMvcTest(controllers = { CategoryController.class, IngredientController.class,
        StorefrontSectionController.class, UserController.class, SaleController.class,
        ProductController.class, TagController.class })
@DisplayName("Renderizado de listados")
class ListRenderingTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryService categoryService;

    @MockitoBean
    private IngredientService ingredientService;

    @MockitoBean
    private StorefrontSectionService sectionService;

    @MockitoBean
    private TagService tagService;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private RoleRepository roleRepository;

    @MockitoBean
    private SaleService saleService;

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

    private String render(String url) throws Exception {
        return mockMvc.perform(get(url).locale(Locale.US).with(user(admin)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private static <T> Page<T> onePage(T item) {
        return new PageImpl<>(List.of(item), PageRequest.of(0, 50), 1);
    }

    /**
     * Los títulos de columna, en orden. Los dibuja fragments/table :: th, así
     * que el markup renderizado es siempre {@code <th scope="col" class="…">Título</th>}.
     */
    private static List<String> headers(String html) {
        return Pattern.compile("<th [^>]*>([^<]*)</th>").matcher(html).results()
                .map(m -> m.group(1))
                .toList();
    }

    /** Las celdas de la primera fila de datos, en orden. */
    private static int bodyCellCount(String html) {
        int body = html.indexOf("<tbody");
        int end = html.indexOf("</tr>", body);
        return (int) Pattern.compile("<td[\\s>]")
                .matcher(html.substring(body, end < 0 ? html.length() : end))
                .results().count();
    }

    /** Las tres acciones que ahora ofrece toda fila del panel. */
    private void assertRowActions(String html, String base, long id) {
        assertThat(html).contains("href=\"" + base + "/" + id + "\"");
        assertThat(html).contains("href=\"" + base + "/" + id + "/edit\"");
        assertThat(html).contains("action=\"" + base + "/" + id + "/delete\"");
        // El borrado confirma por el listener delegado, no por onsubmit inline.
        assertThat(html).contains("data-confirm=");
    }

    /**
     * El encabezado tiene tantas celdas como la fila de datos.
     *
     * <p>Suena tautológico y no lo era. El fragment del encabezado por defecto
     * se llamaba {@code th}, y el selector de Thymeleaf —{@code ~{archivo :: nombre}}—
     * es un <b>markup selector</b>: un nombre suelto matchea un
     * {@code th:fragment} <i>o</i> un elemento con ese nombre de etiqueta.
     * {@code fragments/table.html} define sus tres encabezados sobre elementos
     * {@code <th>}, así que {@code :: th('Nombre')} seleccionaba los tres y
     * dibujaba tres celdas —izquierda, centro y derecha— con la misma etiqueta.
     *
     * <p>Medido sobre {@code /ingredients}: 13 celdas de encabezado para una
     * tabla de 5 columnas. Los títulos no caían sobre su columna en ninguno de
     * los 12 listados del panel.
     *
     * <p>Nada lo detectaba. Los invariantes estáticos leen el archivo, donde
     * hay exactamente un {@code <th>} por columna; el defecto aparece recién al
     * resolver el fragment, o sea al renderizar. De ahí que este test viva acá
     * y no en {@code TemplateInvariantsTest}.
     */
    @Nested
    @DisplayName("Encabezados")
    class Headers {

        /**
         * Los 12 listados del panel: 7 activos y 5 papeleras. Es la misma
         * población que midió el diagnóstico —"13 celdas para una tabla de 5
         * columnas, en los 12 listados"—, así que la lista está completa
         * cuando tiene 12 y {@link #everyListIsCovered()} lo fija.
         */
        private static final List<String> EVERY_LIST = List.of(
                "/categories", "/ingredients", "/products", "/sections", "/tags", "/users", "/sales",
                "/categories/deleted", "/ingredients/deleted", "/products/deleted",
                "/sections/deleted", "/tags/deleted");

        @Test
        @DisplayName("cada listado dibuja tantos encabezados como celdas por fila")
        void headerCountMatchesBodyCellCount() throws Exception {
            seedEveryList();

            for (String url : EVERY_LIST) {
                String html = render(url);
                assertThat(headers(html))
                        .as("encabezados de " + url)
                        // Sin filas, bodyCellCount cuenta la única celda del
                        // estado vacío y la comparación deja de significar algo.
                        // isNotEmpty() descarta además el empate en cero, que es
                        // como un detector roto se disfraza de verde.
                        .isNotEmpty()
                        .hasSize(bodyCellCount(html));
            }
        }

        /**
         * El defecto estaba en {@code fragments/table}, o sea en las 12 tablas a
         * la vez. Un test que recorre 4 URLs y dice "los listados" invita a
         * creer que las otras 8 están miradas.
         */
        @Test
        @DisplayName("la lista cubre los 12 listados del panel")
        void everyListIsCovered() {
            assertThat(EVERY_LIST).hasSize(12).doesNotHaveDuplicates();
        }

        /**
         * Una fila en cada listado. Las papeleras usan instancias propias: si
         * compartieran la del listado activo, marcarles {@code deletedAt} dejaría
         * a la vista de activos devolviendo algo ya borrado.
         */
        private void seedEveryList() {
            LocalDateTime deletedOn = LocalDateTime.of(2026, 3, 14, 10, 30);

            Category category = new Category();
            category.setId(3L);
            category.setName("Tortas");
            // Sirve dos veces: el listado de categorías y, sin paginar, el
            // desplegable de filtro de /products.
            when(categoryService.findAllActive(any(Pageable.class))).thenReturn(onePage(category));

            Ingredient ingredient = new Ingredient();
            ingredient.setId(5L);
            ingredient.setName("Harina 000");
            ingredient.setUnitCost(new BigDecimal("13132.00"));
            ingredient.setUnitOfMeasure(UnitOfMeasure.KILOGRAMO);
            when(ingredientService.findAllActive(any(Pageable.class))).thenReturn(onePage(ingredient));

            Product product = new Product();
            product.setId(8L);
            product.setName("Cheesecake");
            product.setBasePrice(new BigDecimal("12500.00"));
            when(productService.findAllActive(any(Pageable.class))).thenReturn(onePage(product));

            StorefrontSection section = new StorefrontSection();
            section.setId(2L);
            section.setName("Más vendidos");
            section.setDisplayOrder(1);
            section.setVisible(true);
            when(sectionService.findAllActive(any(Pageable.class))).thenReturn(onePage(section));

            Tag tag = new Tag("Sin TACC");
            tag.setId(6L);
            when(tagService.findAllActive(any(Pageable.class))).thenReturn(onePage(tag));

            when(userService.findAll(any(Pageable.class))).thenReturn(onePage(admin));

            Sale sale = new Sale();
            sale.setId(9L);
            sale.setProductName("Torta de chocolate");
            sale.setQuantity(2);
            sale.setUnitPrice(new BigDecimal("30.00"));
            sale.setTotalAmount(new BigDecimal("60.00"));
            sale.setSaleDate(deletedOn);
            when(saleService.findAll(any(Pageable.class))).thenReturn(onePage(sale));
            when(saleService.sumTotalAmount()).thenReturn(new BigDecimal("60.00"));

            Category deletedCategory = new Category();
            deletedCategory.setId(4L);
            deletedCategory.setName("Descontinuadas");
            deletedCategory.setDeletedAt(deletedOn);
            when(categoryService.findDeleted(any(Pageable.class))).thenReturn(onePage(deletedCategory));

            Ingredient deletedIngredient = new Ingredient();
            deletedIngredient.setId(7L);
            deletedIngredient.setName("Colorante rojo");
            deletedIngredient.setUnitCost(new BigDecimal("800.00"));
            deletedIngredient.setUnitOfMeasure(UnitOfMeasure.GRAMO);
            deletedIngredient.setDeletedAt(deletedOn);
            when(ingredientService.findDeleted(any(Pageable.class))).thenReturn(onePage(deletedIngredient));

            Product deletedProduct = new Product();
            deletedProduct.setId(10L);
            deletedProduct.setName("Torta descontinuada");
            deletedProduct.setDeletedAt(deletedOn);
            when(productService.findDeleted(any(Pageable.class))).thenReturn(onePage(deletedProduct));

            StorefrontSection deletedSection = new StorefrontSection();
            deletedSection.setId(11L);
            deletedSection.setName("Temporada pasada");
            deletedSection.setDeletedAt(deletedOn);
            when(sectionService.findDeleted(any(Pageable.class))).thenReturn(onePage(deletedSection));

            Tag deletedTag = new Tag("Promo vieja");
            deletedTag.setId(12L);
            deletedTag.setDeletedAt(deletedOn);
            when(tagService.findDeleted(any(Pageable.class))).thenReturn(onePage(deletedTag));
        }
    }

    @Nested
    @DisplayName("Categorías")
    class Categories {
        private Category category() {
            Category c = new Category();
            c.setId(3L);
            c.setName("Tortas");
            c.setDescription("Tortas y tartas");
            return c;
        }

        @Test
        @DisplayName("fila con Ver, Editar y Borrar, y enlace al sub-recurso")
        void listRenders() throws Exception {
            when(categoryService.findAllActive(any(Pageable.class))).thenReturn(onePage(category()));
            when(categoryService.countProductsByCategories(any())).thenReturn(Map.of(3L, 7L));

            String html = render("/categories");

            assertThat(html).contains("Tortas");
            assertRowActions(html, "/categories", 3L);
            // El sub-recurso es una columna de datos, no una acción de fila.
            assertThat(html).contains("href=\"/categories/3/products\"");
            assertThat(html).contains("scope=\"col\"");

            // La columna "Productos" muestra el contador, no la etiqueta "Ver
            // productos": las tres vistas que la tienen decían cosas distintas.
            // Se afirma sobre el texto visible y no sobre "Ver productos" a
            // secas, porque el title del enlace sigue diciéndolo —y debe—.
            assertThat(html).contains(">7<").doesNotContain("<span>Ver productos</span>");
            // Y el enlace se anuncia con algo más que el número.
            assertThat(html).contains("productos en Tortas");
        }

        /**
         * Una categoría sin productos no aparece en el resultado del GROUP BY,
         * así que el mapa no la trae. La celda tiene que decir 0, no quedar
         * vacía ni romper.
         */
        @Test
        @DisplayName("una categoría sin productos muestra 0, no una celda vacía")
        void countMissingFromTheMapRendersZero() throws Exception {
            when(categoryService.findAllActive(any(Pageable.class))).thenReturn(onePage(category()));
            when(categoryService.countProductsByCategories(any())).thenReturn(Map.of());

            assertThat(render("/categories")).contains(">0<");
        }

        @Test
        @DisplayName("con búsqueda sin resultados ofrece limpiar, no crear")
        void emptyWithSearch() throws Exception {
            when(categoryService.search(anyString(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 50), 0));

            String html = render("/categories?search=zzz");

            assertThat(html).contains("Limpiar filtros");
            assertThat(html).doesNotContain("Crear categoría");
        }
    }

    @Nested
    @DisplayName("Ingredientes")
    class Ingredients {
        @Test
        @DisplayName("fila con las tres acciones y el costo formateado")
        void listRenders() throws Exception {
            Ingredient harina = new Ingredient();
            harina.setId(5L);
            harina.setName("Harina 000");
            harina.setUnitCost(new BigDecimal("13132.00"));
            harina.setUnitOfMeasure(UnitOfMeasure.KILOGRAMO);
            when(ingredientService.findAllActive(any(Pageable.class))).thenReturn(onePage(harina));

            String html = render("/ingredients");

            assertThat(html).contains("Harina 000");
            assertRowActions(html, "/ingredients", 5L);
            // El importe sigue pasando por el formateador tras la migración.
            assertThat(html).contains("$13.132,00");
        }

        /**
         * La unidad se leía distinto según la vista: el listado pedía
         * {@code displayName} ("Kilogramo") y la papelera {@code toString()},
         * que le agrega la abreviatura ("Kilogramo (kg)"). Misma insignia azul,
         * misma columna, dos textos.
         *
         * <p>Nada podía detectarlo: las dos expresiones son válidas y las dos
         * páginas abrían. Por eso el test renderiza las dos y las compara, en
         * vez de afirmar un literal en cada una por separado.
         */
        @Test
        @DisplayName("la unidad se lee igual en el listado y en la papelera")
        void unitReadsTheSameInListAndTrash() throws Exception {
            Ingredient harina = new Ingredient();
            harina.setId(5L);
            harina.setName("Harina 000");
            harina.setUnitCost(new BigDecimal("13132.00"));
            harina.setUnitOfMeasure(UnitOfMeasure.KILOGRAMO);
            when(ingredientService.findAllActive(any(Pageable.class))).thenReturn(onePage(harina));

            assertThat(render("/ingredients"))
                    .contains(UnitOfMeasure.KILOGRAMO.getDisplayName())
                    .doesNotContain(UnitOfMeasure.KILOGRAMO.toString());

            harina.setDeletedAt(LocalDateTime.of(2026, 3, 14, 10, 30));
            when(ingredientService.findDeleted(any(Pageable.class))).thenReturn(onePage(harina));

            assertThat(render("/ingredients/deleted"))
                    .contains(UnitOfMeasure.KILOGRAMO.getDisplayName())
                    .doesNotContain(UnitOfMeasure.KILOGRAMO.toString());
        }

        /**
         * La papelera conserva el prefijo de columnas del listado activo —mismo
         * orden y mismos títulos— y recién después agrega los metadatos del
         * borrado. Es la regla que cumplían cuatro de las cinco papeleras sin
         * estar escrita en ningún lado.
         *
         * <p>Esta arrancaba Nombre, Unidad, Costo, con la descripción metida
         * debajo del nombre: las mismas columnas en otro lugar según desde qué
         * pantalla llegaras. Nada podía detectarlo, porque las dos tablas son
         * HTML válido y las dos abren.
         */
        @Test
        @DisplayName("la papelera conserva el orden de columnas del listado")
        void trashKeepsTheColumnOrderOfTheList() throws Exception {
            Ingredient harina = new Ingredient();
            harina.setId(5L);
            harina.setName("Harina 000");
            harina.setUnitCost(new BigDecimal("13132.00"));
            harina.setUnitOfMeasure(UnitOfMeasure.KILOGRAMO);
            when(ingredientService.findAllActive(any(Pageable.class))).thenReturn(onePage(harina));

            List<String> list = headers(render("/ingredients"));

            harina.setDeletedAt(LocalDateTime.of(2026, 3, 14, 10, 30));
            when(ingredientService.findDeleted(any(Pageable.class))).thenReturn(onePage(harina));

            List<String> trash = headers(render("/ingredients/deleted"));

            assertThat(list).containsExactly(
                    "Nombre", "Descripción", "Costo Unitario", "Unidad", "Acciones");
            assertThat(trash).containsExactly(
                    "Nombre", "Descripción", "Costo Unitario", "Unidad",
                    "Eliminado", "Eliminado por", "Acciones");

            // La regla, dicha una vez: la papelera empieza donde empieza el listado.
            assertThat(trash.subList(0, list.size() - 1))
                    .as("el prefijo de identidad tiene que ser el mismo en las dos vistas")
                    .isEqualTo(list.subList(0, list.size() - 1));
        }
    }

    @Nested
    @DisplayName("Secciones")
    class Sections {
        @Test
        @DisplayName("fila con las tres acciones, orden y visibilidad")
        void listRenders() throws Exception {
            StorefrontSection section = new StorefrontSection();
            section.setId(2L);
            section.setName("Más vendidos");
            section.setDisplayOrder(1);
            section.setVisible(true);
            when(sectionService.findAllActive(any(Pageable.class))).thenReturn(onePage(section));
            when(sectionService.countProductsBySections(any())).thenReturn(Map.of(2L, 4L));

            String html = render("/sections");

            assertThat(html).contains("Más vendidos");
            assertRowActions(html, "/sections", 2L);
            assertThat(html).contains("href=\"/sections/2/products\"");
            // Misma columna "Productos" que categorías y tags: el contador.
            // Acá la etiqueta decía "Ver" a secas.
            assertThat(html).contains(">4<").contains("productos en Más vendidos");
            // El encabezado decía "Descripcion" y la insignia "Si", sin tilde.
            assertThat(html).contains("Descripción").doesNotContain("Descripcion");
            assertThat(html).contains(">Sí<");
        }
    }

    @Nested
    @DisplayName("Usuarios")
    class Users {
        private User employee(boolean enabled) {
            Role role = new Role(RoleType.EMPLOYEE);
            role.setId(2L);

            User u = new User();
            u.setId(7L);
            u.setName("Grace");
            u.setLastName("Hopper");
            u.setEmail("grace@malva.com");
            u.setPasswordHash("irrelevante");
            u.setEnabled(enabled);
            u.setRole(role);
            return u;
        }

        @Test
        @DisplayName("no muestra un buscador que el controller ignora")
        void hasNoDeadSearchBox() throws Exception {
            when(userService.findAll(any(Pageable.class))).thenReturn(onePage(employee(true)));

            String html = render("/users");

            // La caja que había no hacía nada: UserController.list ni siquiera
            // acepta el parámetro `search`. Se saca hasta que exista búsqueda
            // de verdad.
            assertThat(html).doesNotContain("name=\"search\"");
            assertThat(html).doesNotContain("Buscar usuarios");
        }

        @Test
        @DisplayName("usuario activo: la acción ofrecida es desactivar, y confirma")
        void enabledUserOffersDisable() throws Exception {
            when(userService.findAll(any(Pageable.class))).thenReturn(onePage(employee(true)));

            String html = render("/users");

            assertThat(html).contains("Grace Hopper");
            assertThat(html).contains("href=\"/users/7\"");
            assertThat(html).contains("href=\"/users/7/edit\"");
            assertThat(html).contains("action=\"/users/7/toggle\"");
            assertThat(html).contains("Desactivar usuario").doesNotContain("Activar usuario");
            // Desactivar deja a alguien afuera del panel: confirma.
            assertThat(html).contains("data-confirm=");
            // Los usuarios no se borran, se desactivan: no hay ruta de borrado.
            assertThat(html).doesNotContain("/users/7/delete");
        }

        @Test
        @DisplayName("usuario inactivo: la acción es activar, y no confirma")
        void disabledUserOffersEnable() throws Exception {
            when(userService.findAll(any(Pageable.class))).thenReturn(onePage(employee(false)));

            String html = render("/users");

            assertThat(html).contains("Activar usuario").doesNotContain("Desactivar usuario");
            // Reactivar no destruye nada: pedir confirmación sería ruido.
            assertThat(html).doesNotContain("data-confirm=");
        }
    }

    @Nested
    @DisplayName("Ventas")
    class Sales {
        private Sale sale() {
            Sale s = new Sale();
            s.setId(9L);
            s.setProductName("Torta de chocolate");
            s.setQuantity(2);
            s.setUnitPrice(new BigDecimal("30.00"));
            s.setTotalAmount(new BigDecimal("60.00"));
            s.setSaleDate(LocalDateTime.of(2026, 3, 14, 10, 30));
            return s;
        }

        /**
         * El pago concreto de haber partido form :: search: tres filtros en un
         * mismo formulario. Con la versión anterior del fragment esta página
         * era imposible de armar, porque cada campo traía su propio &lt;form&gt;.
         */
        @Test
        @DisplayName("los tres filtros viven en un solo formulario, con labels asociados")
        void threeFiltersOneForm() throws Exception {
            when(saleService.findAll(any(Pageable.class))).thenReturn(onePage(sale()));
            when(saleService.sumTotalAmount()).thenReturn(new BigDecimal("60.00"));

            String html = render("/sales");

            // Un solo formulario de filtros (el otro <form> del documento es el
            // de logout, que aporta el layout).
            assertThat(html).contains("action=\"/sales\" method=\"get\"");
            assertThat(html).contains("name=\"search\"");
            assertThat(html).contains("name=\"startDate\"");
            assertThat(html).contains("name=\"endDate\"");

            // Los tres <label> no tenían `for` y no estaban asociados a su campo.
            assertThat(html).contains("for=\"search\"");
            assertThat(html).contains("for=\"startDate\"");
            assertThat(html).contains("for=\"endDate\"");
        }

        @Test
        @DisplayName("una venta solo se puede ver: no se edita ni se borra")
        void salesAreReadOnly() throws Exception {
            when(saleService.findAll(any(Pageable.class))).thenReturn(onePage(sale()));
            when(saleService.sumTotalAmount()).thenReturn(new BigDecimal("60.00"));

            String html = render("/sales");

            assertThat(html).contains("href=\"/sales/9\"");
            // La fila guarda el precio y el nombre tal como estaban al vender;
            // reescribirlos falsearía el histórico.
            assertThat(html).doesNotContain("/sales/9/edit");
            assertThat(html).doesNotContain("/sales/9/delete");
        }

        @Test
        @DisplayName("las fechas del filtro viajan en ISO por los enlaces de página")
        void paginationKeepsDatesInIso() throws Exception {
            when(saleService.findByDateRange(any(), any(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(sale()), PageRequest.of(0, 1), 2));
            when(saleService.sumTotalAmountByDateRange(any(), any())).thenReturn(new BigDecimal("60.00"));

            String html = render("/sales?startDate=2026-03-01&endDate=2026-03-31&size=1");

            // Con el locale del panel en es-AR, SpEL las convertía a "1/3/26".
            assertThat(html).contains("startDate=2026-03-01");
            assertThat(html).doesNotContain("1/3/26");
        }
    }

    /**
     * Las cinco papeleras comparten patrón: volver, restaurar, eliminar
     * definitivo y estado vacío sin CTA. Antes cada una redactaba su propio
     * texto de confirmación —había cinco variantes del mismo aviso, algunas
     * sin tildes—, y ninguna asociaba el aviso al hecho de ser irreversible.
     */
    @Nested
    @DisplayName("Papeleras")
    class Trash {
        private static final String PERMANENT = "PERMANENTEMENTE";

        @Test
        @DisplayName("categorías: restaurar sin confirmar, borrado definitivo con aviso")
        void categoriesTrash() throws Exception {
            Category deleted = new Category();
            deleted.setId(4L);
            deleted.setName("Descontinuadas");
            deleted.setDeletedAt(LocalDateTime.of(2026, 3, 14, 10, 30));
            when(categoryService.findDeleted(any(Pageable.class))).thenReturn(onePage(deleted));

            String html = render("/categories/deleted");

            assertThat(html).contains("Descontinuadas");
            assertThat(html).contains("action=\"/categories/4/restore\"");
            assertThat(html).contains("action=\"/categories/4/hard-delete\"");
            assertThat(html).contains(PERMANENT);
            // Sin dato de quién borró, la celda no queda vacía.
            assertThat(html).contains("Desconocido");
            assertThat(html).contains("Volver a Categorías");
        }

        @Test
        @DisplayName("productos: muestra la categoría o el marcador de vacío")
        void productsTrash() throws Exception {
            Product deleted = new Product();
            deleted.setId(8L);
            deleted.setName("Torta descontinuada");
            deleted.setDeletedAt(LocalDateTime.of(2026, 3, 14, 10, 30));
            when(productService.findDeleted(any(Pageable.class))).thenReturn(onePage(deleted));

            String html = render("/products/deleted");

            assertThat(html).contains("Torta descontinuada");
            assertThat(html).contains("action=\"/products/8/restore\"");
            assertThat(html).contains("action=\"/products/8/hard-delete\"");
            // El producto no tiene categoría: antes salía un guion suelto.
            assertThat(html).contains("Sin categoría");
            // La columna del nombre se titulaba "Nombre" acá y "Producto" en el
            // listado activo: la misma columna con dos nombres según la vista.
            // Se afirma sobre la celda cerrada porque "Producto" suelto aparece
            // igual en el sidebar y el test pasaría sin el arreglo.
            assertThat(html).contains(">Producto</th>").doesNotContain(">Nombre</th>");
        }

        @Test
        @DisplayName("secciones: encabezado acentuado y estado vacío sin CTA")
        void sectionsTrash() throws Exception {
            when(sectionService.findDeleted(any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 50), 0));

            String html = render("/sections/deleted");

            assertThat(html).contains("La papelera está vacía");
            // En una papelera no hay nada que crear ni filtros que limpiar.
            assertThat(html).doesNotContain("Crear sección");
            assertThat(html).doesNotContain("Limpiar filtros");
            assertThat(html).contains("Descripción").doesNotContain("Descripcion");
        }

        /**
         * "Desconocido" estaba escrito como literal en las cinco papeleras, que
         * es justo lo que el bloque de ausencias de messages.properties existe
         * para evitar: cinco literales son cinco redacciones que se separan.
         *
         * <p>El {@code doesNotContain("??")} no es decorativo. Una clave que no
         * existe <b>no falla</b>: Thymeleaf dibuja {@code ??empty.user_es_AR??}
         * y la página se sirve con un 200. Sin esa aserción, este test seguiría
         * verde con la clave mal escrita.
         */
        @Test
        @DisplayName("quién borró sale de messages.properties, no de un literal")
        void whoDeletedComesFromMessages() throws Exception {
            LocalDateTime deletedOn = LocalDateTime.of(2026, 3, 14, 10, 30);

            Category category = new Category();
            category.setId(4L);
            category.setName("Descontinuadas");
            category.setDeletedAt(deletedOn);
            when(categoryService.findDeleted(any(Pageable.class))).thenReturn(onePage(category));

            Ingredient ingredient = new Ingredient();
            ingredient.setId(5L);
            ingredient.setName("Harina 000");
            ingredient.setUnitCost(new BigDecimal("13132.00"));
            ingredient.setUnitOfMeasure(UnitOfMeasure.KILOGRAMO);
            ingredient.setDeletedAt(deletedOn);
            when(ingredientService.findDeleted(any(Pageable.class))).thenReturn(onePage(ingredient));

            StorefrontSection section = new StorefrontSection();
            section.setId(2L);
            section.setName("Más vendidos");
            section.setDeletedAt(deletedOn);
            when(sectionService.findDeleted(any(Pageable.class))).thenReturn(onePage(section));

            Product product = new Product();
            product.setId(8L);
            product.setName("Torta descontinuada");
            product.setDeletedAt(deletedOn);
            when(productService.findDeleted(any(Pageable.class))).thenReturn(onePage(product));

            Tag tag = new Tag("Promo vieja");
            tag.setId(12L);
            tag.setDeletedAt(deletedOn);
            when(tagService.findDeleted(any(Pageable.class))).thenReturn(onePage(tag));

            // Las cinco papeleras, tags incluida desde que TagController entró al
            // slice para el conteo de encabezados.
            for (String url : List.of("/categories/deleted", "/ingredients/deleted",
                    "/sections/deleted", "/products/deleted", "/tags/deleted")) {
                assertThat(render(url)).as(url)
                        .contains("Desconocido")
                        .doesNotContain("??");
            }
        }
    }
}
