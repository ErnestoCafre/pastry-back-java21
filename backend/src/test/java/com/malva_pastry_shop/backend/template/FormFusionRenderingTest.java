package com.malva_pastry_shop.backend.template;

import com.malva_pastry_shop.backend.controller.admin.IngredientController;
import com.malva_pastry_shop.backend.controller.admin.ProductController;
import com.malva_pastry_shop.backend.controller.admin.StorefrontSectionController;
import com.malva_pastry_shop.backend.controller.admin.TagController;
import com.malva_pastry_shop.backend.controller.admin.UserController;
import com.malva_pastry_shop.backend.domain.auth.Role;
import com.malva_pastry_shop.backend.domain.auth.RoleType;
import com.malva_pastry_shop.backend.domain.auth.User;
import com.malva_pastry_shop.backend.domain.inventory.Category;
import com.malva_pastry_shop.backend.domain.inventory.Ingredient;
import com.malva_pastry_shop.backend.domain.inventory.Product;
import com.malva_pastry_shop.backend.domain.inventory.UnitOfMeasure;
import com.malva_pastry_shop.backend.domain.storefront.StorefrontSection;
import com.malva_pastry_shop.backend.domain.storefront.Tag;
import com.malva_pastry_shop.backend.repository.RoleRepository;
import com.malva_pastry_shop.backend.service.inventory.CategoryService;
import com.malva_pastry_shop.backend.service.inventory.IngredientService;
import com.malva_pastry_shop.backend.service.inventory.ProductService;
import com.malva_pastry_shop.backend.service.storefront.StorefrontSectionService;
import com.malva_pastry_shop.backend.service.storefront.TagService;
import com.malva_pastry_shop.backend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Renderiza las dos ramas de cada formulario fusionado.
 *
 * <p>Los seis pares create/edit se unificaron en un solo archivo por entidad,
 * que elige el modo según el controller haya publicado o no el id. Un test que
 * solo compare el nombre de la vista no alcanza: si la condición se invirtiera,
 * el formulario de alta postearía contra una URL de edición y nada fallaría
 * hasta que alguien intentara crear algo.
 *
 * <p>Por eso cada entidad se prueba en sus dos modos, afirmando sobre las tres
 * cosas que dependen del modo: el th:action, la etiqueta del botón y el último
 * tramo de la miga.
 */
@WebMvcTest(controllers = { TagController.class, StorefrontSectionController.class,
        IngredientController.class, ProductController.class, UserController.class })
@DisplayName("Formularios fusionados: alta y edición")
class FormFusionRenderingTest {

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

    @MockitoBean
    private IngredientService ingredientService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private RoleRepository roleRepository;

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

    /** Las tres cosas que dependen del modo, en un solo lugar. */
    private void assertCreateMode(String html, String action, String submitLabel, String editLabel) {
        assertThat(html).contains("action=\"" + action + "\"");
        assertThat(html).contains(submitLabel);
        assertThat(html).doesNotContain(editLabel);
    }

    @Nested
    @DisplayName("Tags")
    class Tags {
        @Test
        @DisplayName("alta")
        void create() throws Exception {
            assertCreateMode(render("/tags/new"), "/tags", "Guardar Tag", "Actualizar Tag");
        }

        @Test
        @DisplayName("edición")
        void edit() throws Exception {
            Tag tag = new Tag();
            tag.setId(4L);
            tag.setName("Vegano");
            when(tagService.findById(4L)).thenReturn(tag);

            String html = render("/tags/4/edit");

            assertThat(html).contains("action=\"/tags/4\"");
            assertThat(html).contains("Actualizar Tag").doesNotContain("Guardar Tag");
            assertThat(html).contains("value=\"Vegano\"");
        }
    }

    @Nested
    @DisplayName("Secciones")
    class Sections {
        @Test
        @DisplayName("alta: estrena form :: number y form :: checkbox")
        void create() throws Exception {
            String html = render("/sections/new");

            assertCreateMode(html, "/sections", "Guardar Sección", "Actualizar Sección");
            assertThat(html).contains("id=\"displayOrder\"").contains("type=\"number\"");
            assertThat(html).contains("id=\"visible\"").contains("type=\"checkbox\"");
            // El <label> de "Visible en vitrina" no tenía for y no estaba
            // asociado a su casilla.
            assertThat(html).contains("for=\"visible\"");
        }

        @Test
        @DisplayName("edición")
        void edit() throws Exception {
            StorefrontSection section = new StorefrontSection();
            section.setId(2L);
            section.setName("Más vendidos");
            when(sectionService.findById(2L)).thenReturn(section);

            String html = render("/sections/2/edit");

            assertThat(html).contains("action=\"/sections/2\"");
            assertThat(html).contains("Actualizar Sección").doesNotContain("Guardar Sección");
        }
    }

    @Nested
    @DisplayName("Ingredientes")
    class Ingredients {
        @Test
        @DisplayName("alta: conserva los optgroups de unidad de medida")
        void create() throws Exception {
            String html = render("/ingredients/new");

            assertCreateMode(html, "/ingredients", "Guardar Ingrediente", "Actualizar Ingrediente");
            // El select agrupado es el motivo por el que este campo NO usa
            // form :: select, que solo hace listas planas.
            assertThat(html).contains("<optgroup label=\"Peso\"");
            assertThat(html).contains("<optgroup label=\"Volumen\"");
            assertThat(html).contains("<optgroup label=\"Unidades\"");
        }

        @Test
        @DisplayName("edición")
        void edit() throws Exception {
            Ingredient ingredient = new Ingredient();
            ingredient.setId(5L);
            ingredient.setName("Harina 000");
            ingredient.setUnitCost(new BigDecimal("12.50"));
            ingredient.setUnitOfMeasure(UnitOfMeasure.KILOGRAMO);
            when(ingredientService.findById(5L)).thenReturn(ingredient);

            String html = render("/ingredients/5/edit");

            assertThat(html).contains("action=\"/ingredients/5\"");
            assertThat(html).contains("Actualizar Ingrediente").doesNotContain("Guardar Ingrediente");
            assertThat(html).contains("value=\"Harina 000\"");
        }
    }

    @Nested
    @DisplayName("Productos")
    class Products {
        private void stubCategories() {
            Category tortas = new Category();
            tortas.setId(3L);
            tortas.setName("Tortas");
            when(categoryService.findAllActive(any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(tortas), PageRequest.of(0, 50), 1));
        }

        @Test
        @DisplayName("alta: vista previa oculta y sin JS inline")
        void create() throws Exception {
            stubCategories();

            String html = render("/products/new");

            assertCreateMode(html, "/products", "Guardar Producto", "Actualizar Producto");
            assertThat(html).contains("Tortas");
            // Sin imagen cargada, la vista previa arranca oculta. Antes esto
            // era markup distinto en cada archivo del par.
            assertThat(html).contains("id=\"imagePreviewContainer\"").contains("hidden");
            // Los manejadores inline se fueron a /js/product-form.js.
            assertThat(html).doesNotContain("oninput=").doesNotContain("onerror=");
            assertThat(html).contains("/js/product-form.js");
        }

        @Test
        @DisplayName("edición: vista previa visible si el producto tiene imagen")
        void edit() throws Exception {
            stubCategories();

            Product product = new Product();
            product.setId(8L);
            product.setName("Cheesecake");
            product.setBasePrice(new BigDecimal("13132.00"));
            product.setImageUrl("/images/products/cheesecake-new-york.webp");
            when(productService.findById(8L)).thenReturn(product);

            String html = render("/products/8/edit");

            assertThat(html).contains("action=\"/products/8\"");
            assertThat(html).contains("Actualizar Producto").doesNotContain("Guardar Producto");
            assertThat(html).contains("/images/products/cheesecake-new-york.webp");
            // El precio va crudo en el input: <input type="number"> exige punto
            // decimal, no el formato local.
            assertThat(html).contains("value=\"13132.00\"");
        }
    }

    @Nested
    @DisplayName("Usuarios")
    class Users {
        private void stubRoles() {
            Role employee = new Role(RoleType.EMPLOYEE);
            employee.setId(2L);
            when(roleRepository.findAll()).thenReturn(List.of(employee));
        }

        @Test
        @DisplayName("alta: contraseña obligatoria y sin casilla de habilitado")
        void create() throws Exception {
            stubRoles();

            String html = render("/users/new");

            assertCreateMode(html, "/users", "Guardar Usuario", "Actualizar Usuario");
            assertThat(html).contains("Empleado (por defecto)");
            // La casilla "habilitado" solo existe al editar. Es el caso que la
            // regla de precedencia arruina si el th:if se pone junto al
            // th:replace: se dibujaría también acá.
            assertThat(html).doesNotContain("id=\"enabled\"");
            assertThat(html).doesNotContain("dejar vacío para no cambiar");
        }

        @Test
        @DisplayName("edición: contraseña opcional y casilla de habilitado")
        void edit() throws Exception {
            stubRoles();

            User target = new User();
            target.setId(6L);
            target.setName("Grace");
            target.setLastName("Hopper");
            target.setEmail("grace@malva.com");
            target.setEnabled(true);
            Role employee = new Role(RoleType.EMPLOYEE);
            employee.setId(2L);
            target.setRole(employee);
            when(userService.findById(6L)).thenReturn(target);

            String html = render("/users/6/edit");

            assertThat(html).contains("action=\"/users/6\"");
            assertThat(html).contains("Actualizar Usuario").doesNotContain("Guardar Usuario");
            assertThat(html).contains("id=\"enabled\"");
            assertThat(html).contains("dejar vacío para no cambiar");
            // La contraseña nunca vuelve al navegador: el campo no usa th:field.
            assertThat(html).doesNotContain("value=\"irrelevante\"");
        }
    }
}
