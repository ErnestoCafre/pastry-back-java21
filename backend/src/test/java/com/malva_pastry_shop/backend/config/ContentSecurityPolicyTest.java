package com.malva_pastry_shop.backend.config;

import com.malva_pastry_shop.backend.controller.admin.DashboardController;
import com.malva_pastry_shop.backend.domain.auth.Role;
import com.malva_pastry_shop.backend.domain.auth.RoleType;
import com.malva_pastry_shop.backend.domain.auth.User;
import com.malva_pastry_shop.backend.service.UserService;
import com.malva_pastry_shop.backend.service.inventory.CategoryService;
import com.malva_pastry_shop.backend.service.inventory.IngredientService;
import com.malva_pastry_shop.backend.service.inventory.ProductService;
import com.malva_pastry_shop.backend.service.sales.SaleService;
import com.malva_pastry_shop.backend.service.storefront.StorefrontSectionService;
import com.malva_pastry_shop.backend.service.storefront.TagService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * El panel sirve una Content-Security-Policy sin {@code 'unsafe-inline'}.
 *
 * <p>Era el objetivo de todo el trabajo de sacar el JavaScript embebido: ocho
 * manejadores {@code onclick} movidos a {@code data-confirm} y un bloque de
 * {@code sales/create} movido a {@code /js/sale-form.js}. Mientras la cabecera
 * no existiera, eso era una precondición cumplida y nada más — el navegador
 * seguía ejecutando cualquier script inline que llegara a la página.
 *
 * <p>Este test es lo que evita que la precondición y la política se separen:
 * {@code inlineJavaScriptDoesNotSpread} garantiza que no vuelva el inline, y
 * este garantiza que la cabecera que lo aprovecha siga estando.
 */
// SecurityConfig es un @Configuration corriente: el slice de @WebMvcTest no lo
// toma solo, y sin él la cadena que se arma es la que autoconfigura Spring
// Boot, que no lleva ninguna CSP. Sin este @Import el test pasa a medias por el
// motivo equivocado —la cabecera falta en todos lados, así que la excepción de
// Swagger se ve idéntica a que la política no exista—.
@WebMvcTest(controllers = DashboardController.class)
@Import(SecurityConfig.class)
@DisplayName("Content-Security-Policy")
class ContentSecurityPolicyTest {

    private static final String HEADER = "Content-Security-Policy";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private CategoryService categoryService;
    @MockitoBean private ProductService productService;
    @MockitoBean private IngredientService ingredientService;
    @MockitoBean private TagService tagService;
    @MockitoBean private StorefrontSectionService sectionService;
    @MockitoBean private SaleService saleService;
    @MockitoBean private UserService userService;

    private static User admin() {
        Role role = new Role(RoleType.ADMIN);
        role.setId(1L);

        User admin = new User();
        admin.setId(1L);
        admin.setName("Ada");
        admin.setLastName("Lovelace");
        admin.setEmail("ada@malva.com");
        admin.setPasswordHash("irrelevante");
        admin.setEnabled(true);
        admin.setRole(role);
        return admin;
    }

    @Test
    @DisplayName("una página del panel la trae, y sin 'unsafe-inline'")
    void adminPagesCarryThePolicy() throws Exception {
        when(productService.findAllActive(any())).thenReturn(Page.empty());
        when(categoryService.findAllActive(any())).thenReturn(Page.empty());
        when(ingredientService.findAllActive(any())).thenReturn(Page.empty());

        String policy = mockMvc.perform(get("/dashboard").with(user(admin())))
                .andReturn().getResponse().getHeader(HEADER);

        assertThat(policy).as("falta la cabecera en las páginas del panel").isNotNull();
        assertThat(policy)
                .as("con 'unsafe-inline' la política no compra nada: es el único "
                        + "motivo por el que se sacó el JavaScript embebido")
                .doesNotContain("unsafe-inline")
                .doesNotContain("unsafe-eval");
        assertThat(policy).contains("script-src 'self'");
        assertThat(policy).contains("style-src 'self'");
        assertThat(policy).contains("object-src 'none'");
        assertThat(policy).contains("frame-ancestors 'none'");
    }

    /**
     * Swagger UI arma su página con scripts y estilos inline. Está exceptuada
     * a propósito; si dejara de estarlo, la herramienta se ve en blanco y no
     * hay nada en los logs que lo explique.
     */
    @Test
    @DisplayName("Swagger UI queda exceptuada")
    void swaggerIsExempt() throws Exception {
        String policy = mockMvc.perform(get("/swagger-ui/index.html"))
                .andReturn().getResponse().getHeader(HEADER);

        assertThat(policy).isNull();
    }

    /**
     * El login es anónimo y pasa por la misma cadena: es la página donde un
     * script inyectado tendría más para robar.
     */
    @Test
    @DisplayName("el login también la trae")
    void theLoginPageCarriesItToo() throws Exception {
        assertThat(mockMvc.perform(get("/login")).andReturn().getResponse().getHeader(HEADER))
                .isNotNull();
    }
}
