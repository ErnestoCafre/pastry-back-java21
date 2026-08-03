package com.malva_pastry_shop.backend.template;

import com.malva_pastry_shop.backend.controller.admin.CategoryController;
import com.malva_pastry_shop.backend.controller.admin.ProductController;
import com.malva_pastry_shop.backend.controller.admin.StorefrontSectionController;
import com.malva_pastry_shop.backend.controller.admin.TagController;
import com.malva_pastry_shop.backend.controller.admin.UserController;
import com.malva_pastry_shop.backend.domain.auth.Role;
import com.malva_pastry_shop.backend.domain.auth.RoleType;
import com.malva_pastry_shop.backend.domain.auth.User;
import com.malva_pastry_shop.backend.domain.inventory.Category;
import com.malva_pastry_shop.backend.domain.inventory.Product;
import com.malva_pastry_shop.backend.domain.storefront.StorefrontSection;
import com.malva_pastry_shop.backend.domain.storefront.Tag;
import com.malva_pastry_shop.backend.repository.RoleRepository;
import com.malva_pastry_shop.backend.service.UserService;
import com.malva_pastry_shop.backend.service.inventory.CategoryService;
import com.malva_pastry_shop.backend.service.inventory.ProductService;
import com.malva_pastry_shop.backend.service.storefront.StorefrontSectionService;
import com.malva_pastry_shop.backend.service.storefront.TagService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Renderiza las fichas de detalle y los sublistados migrados.
 *
 * <p>Fija además las tres reglas de navegación que se unificaron al migrarlos,
 * y que son fáciles de romper sin que falle nada:
 *
 * <ul>
 *   <li><b>Un solo &lt;h1&gt; por página.</b> El layout ya dibuja uno con
 *       {@code pageTitle}; cuatro plantillas agregaban el suyo, así que un
 *       lector de pantalla anunciaba dos títulos de nivel 1.</li>
 *   <li><b>La miga de pan es el único camino de vuelta.</b> Convivían cuatro
 *       redacciones de lo mismo ("Volver al listado", "Volver", "Volver a
 *       Ventas", "Volver a usuarios"), y users/show tenía dos botones
 *       distintos apuntando al mismo lado.</li>
 *   <li><b>Toda ficha tiene miga.</b> users/show era la única sin ella.</li>
 * </ul>
 */
@WebMvcTest(controllers = { CategoryController.class, TagController.class,
        StorefrontSectionController.class, ProductController.class, UserController.class })
@DisplayName("Renderizado de fichas y sublistados")
class DetailPageRenderingTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private CategoryService categoryService;
    @MockitoBean private TagService tagService;
    @MockitoBean private StorefrontSectionService sectionService;
    @MockitoBean private ProductService productService;
    @MockitoBean private UserService userService;
    @MockitoBean private RoleRepository roleRepository;

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

    /** Las tres reglas de navegación, en un solo lugar. */
    private void assertNavigationRules(String html) {
        assertThat(countOf(html, "<h1")).as("un solo <h1> por página").isEqualTo(1);
        assertThat(html).as("tiene miga de pan").contains("aria-label=\"Migas de pan\"");
        assertThat(html).as("sin botón de volver: la miga ya vuelve").doesNotContain(">Volver");
    }

    private static int countOf(String haystack, String needle) {
        int count = 0;
        for (int i = haystack.indexOf(needle); i >= 0; i = haystack.indexOf(needle, i + needle.length())) {
            count++;
        }
        return count;
    }

    private static Product product() {
        Product p = new Product();
        p.setId(8L);
        p.setName("Cheesecake");
        p.setUpdatedAt(LocalDateTime.of(2026, 3, 14, 10, 30));
        p.setInsertedAt(LocalDateTime.of(2026, 3, 1, 9, 0));
        return p;
    }

    private static Tag tag() {
        Tag t = new Tag();
        t.setId(4L);
        t.setName("Vegano");
        t.setSlug("vegano");
        t.setInsertedAt(LocalDateTime.of(2026, 3, 1, 9, 0));
        t.setUpdatedAt(LocalDateTime.of(2026, 3, 14, 10, 30));
        return t;
    }

    @Nested
    @DisplayName("Fichas")
    class Details {

        @Test
        @DisplayName("categories/show")
        void categoryDetail() throws Exception {
            Category c = new Category();
            c.setId(3L);
            c.setName("Tortas");
            c.setInsertedAt(LocalDateTime.of(2026, 3, 1, 9, 0));
            c.setUpdatedAt(LocalDateTime.of(2026, 3, 14, 10, 30));
            when(categoryService.findById(anyLong())).thenReturn(c);
            when(categoryService.countProducts(anyLong())).thenReturn(5L);

            String html = render("/categories/3");

            assertNavigationRules(html);
            assertThat(html).contains("action=\"/categories/3/delete\"");
            assertThat(html).contains("href=\"/categories/3/edit\"");
            assertThat(html).contains("data-confirm=");
        }

        @Test
        @DisplayName("sections/show")
        void sectionDetail() throws Exception {
            StorefrontSection s = new StorefrontSection();
            s.setId(2L);
            s.setName("Más vendidos");
            s.setSlug("mas-vendidos");
            s.setVisible(true);
            s.setInsertedAt(LocalDateTime.of(2026, 3, 1, 9, 0));
            s.setUpdatedAt(LocalDateTime.of(2026, 3, 14, 10, 30));
            when(sectionService.findById(anyLong())).thenReturn(s);
            when(sectionService.countProducts(anyLong())).thenReturn(3L);

            String html = render("/sections/2");

            assertNavigationRules(html);
            assertThat(html).contains("href=\"/sections/2/products\"");
        }

        /**
         * Era la única ficha sin miga, y tenía dos botones de retorno con textos
         * distintos apuntando los dos a /users.
         */
        @Test
        @DisplayName("users/show: gana miga y pierde los dos botones de volver")
        void userDetail() throws Exception {
            User target = new User();
            target.setId(6L);
            target.setName("Grace");
            target.setLastName("Hopper");
            target.setEmail("grace@malva.com");
            target.setPasswordHash("irrelevante");
            target.setEnabled(true);
            Role employee = new Role(RoleType.EMPLOYEE);
            employee.setId(2L);
            target.setRole(employee);
            when(userService.findById(anyLong())).thenReturn(target);

            String html = render("/users/6");

            assertNavigationRules(html);
            assertThat(html).doesNotContain("Volver a usuarios").doesNotContain("Volver a la lista");
            assertThat(html).contains("Grace Hopper");
            // Desactivar deja a alguien afuera del panel: ahora confirma. Antes
            // era un clic sin aviso.
            assertThat(html).contains("action=\"/users/6/toggle\"");
            assertThat(html).contains("data-confirm=");
        }
    }

    @Nested
    @DisplayName("Sublistados (miga de tres niveles)")
    class SubListings {

        @Test
        @DisplayName("products/tags")
        void productTags() throws Exception {
            when(productService.findById(anyLong())).thenReturn(product());
            when(productService.getProductTags(anyLong())).thenReturn(List.of(tag()));
            when(productService.getAvailableTagsForProduct(anyLong())).thenReturn(List.of());

            String html = render("/products/8/tags");

            assertNavigationRules(html);
            // trailDeep: Productos > Cheesecake > Tags
            assertThat(html).contains("href=\"/products\"");
            assertThat(html).contains("href=\"/products/8\"");
            assertThat(html).contains("action=\"/products/8/tags/4/remove\"");
            assertThat(html).contains("Este producto ya tiene todos los tags");
        }

        @Test
        @DisplayName("tags/products")
        void tagProducts() throws Exception {
            when(tagService.findById(anyLong())).thenReturn(tag());
            when(productService.getProductsByTag(anyLong())).thenReturn(List.of(product()));
            when(productService.getAvailableProductsForTag(anyLong())).thenReturn(List.of());

            String html = render("/tags/4/products");

            assertNavigationRules(html);
            assertThat(html).contains("href=\"/tags\"");
            assertThat(html).contains("href=\"/tags/4\"");
            assertThat(html).contains("action=\"/tags/4/products/8/remove\"");
        }

        @Test
        @DisplayName("sections/products: el orden editable queda etiquetado")
        void sectionProducts() throws Exception {
            StorefrontSection s = new StorefrontSection();
            s.setId(2L);
            s.setName("Más vendidos");
            when(sectionService.findById(anyLong())).thenReturn(s);
            when(sectionService.getSectionProducts(anyLong())).thenReturn(List.of());
            when(sectionService.getAvailableProductsForSection(anyLong())).thenReturn(List.of(product()));

            String html = render("/sections/2/products");

            assertNavigationRules(html);
            assertThat(html).contains("href=\"/sections/2\"");
            assertThat(html).contains("action=\"/sections/2/products/8\"");
            assertThat(html).contains("No hay productos en esta sección");
        }
    }
}
