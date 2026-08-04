package com.malva_pastry_shop.backend.template;

import com.malva_pastry_shop.backend.controller.admin.ErrorPageController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.csrf.InvalidCsrfTokenException;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * La página a la que Spring Security hace forward al denegar acceso.
 *
 * <p>Antes {@code /error/403} no tenía handler, así que toda denegación
 * terminaba en un 404 con la Whitelabel Error Page. El caso que lo destapó fue
 * un token CSRF viejo al iniciar sesión: en vez de "reintentá", el usuario veía
 * un 404 sin explicación.
 */
@WebMvcTest(controllers = ErrorPageController.class)
@DisplayName("Página de acceso denegado")
class ErrorPageRenderingTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("falta de permisos: ofrece volver al panel")
    void missingPermissionShowsForbiddenCopy() throws Exception {
        String html = mockMvc.perform(get("/error/403")
                        .requestAttr(WebAttributes.ACCESS_DENIED_403,
                                new AccessDeniedException("Access is denied")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("No tenés permiso");
        assertThat(html).contains("Volver al panel");
        assertThat(html).doesNotContain("Tu sesión expiró");
    }

    /**
     * CsrfException extiende AccessDeniedException, así que llega por el mismo
     * forward. No es un problema de permisos y el mensaje no debe sugerirlo.
     */
    @Test
    @DisplayName("token CSRF viejo: habla de sesión expirada, no de permisos")
    void staleCsrfTokenShowsSessionExpiredCopy() throws Exception {
        String html = mockMvc.perform(post("/error/403")
                        .requestAttr(WebAttributes.ACCESS_DENIED_403,
                                new InvalidCsrfTokenException(
                                        new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "esperado"),
                                        "recibido")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("Tu sesión expiró");
        assertThat(html).contains("Ir al inicio de sesión");
        assertThat(html).doesNotContain("No tenés permiso");
    }

    /**
     * El forward de accessDeniedPage conserva el método original, así que la
     * ruta tiene que responder a POST además de GET.
     */
    @Test
    @DisplayName("responde tanto a GET como a POST")
    void handlesBothMethods() throws Exception {
        mockMvc.perform(get("/error/403")).andExpect(status().isOk());
        mockMvc.perform(post("/error/403")).andExpect(status().isOk());
    }
}
