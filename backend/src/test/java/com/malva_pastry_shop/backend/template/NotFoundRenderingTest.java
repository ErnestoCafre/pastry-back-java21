package com.malva_pastry_shop.backend.template;

import com.malva_pastry_shop.backend.controller.admin.CategoryController;
import com.malva_pastry_shop.backend.controller.admin.UserController;
import com.malva_pastry_shop.backend.domain.auth.Role;
import com.malva_pastry_shop.backend.domain.auth.RoleType;
import com.malva_pastry_shop.backend.domain.auth.User;
import com.malva_pastry_shop.backend.repository.RoleRepository;
import com.malva_pastry_shop.backend.service.UserService;
import com.malva_pastry_shop.backend.service.inventory.CategoryService;
import com.malva_pastry_shop.backend.service.inventory.ProductService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pedir algo que no existe tiene que dar 404, no un 200 disimulado.
 *
 * <p>Los 17 handlers GET atrapaban {@code EntityNotFoundException} y devolvían
 * {@code redirect:} al listado. Para el usuario, un enlace roto, un id viejo y
 * un borrado desde otra pestaña se veían los tres igual: aparecía el listado,
 * sin ninguna explicación. Para cualquier herramienta —un crawler, un monitor,
 * este mismo repo haciendo un barrido— la respuesta era un 200 perfecto.
 *
 * <p>El límite importa tanto como el cambio: <b>los POST siguen redirigiendo
 * con un mensaje</b>. Quien borra algo que otro ya borró no necesita un 404,
 * necesita enterarse de que ya no está.
 */
@WebMvcTest(controllers = { CategoryController.class, UserController.class })
@DisplayName("Recursos que no existen")
class NotFoundRenderingTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private CategoryService categoryService;
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

    @Test
    @DisplayName("una ficha inexistente da 404 y ofrece volver a su listado")
    void missingDetailAnswers404() throws Exception {
        when(categoryService.findById(anyLong()))
                .thenThrow(new EntityNotFoundException("Categoría no encontrada con ID: 999"));

        String html = mockMvc.perform(get("/categories/999").with(user(admin)))
                .andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("No encontramos eso");
        assertThat(html).contains("href=\"/categories\"");
        assertThat(html).as("la página tiene que llegar entera").contains("</html>");
    }

    @Test
    @DisplayName("el formulario de edición de algo que no existe también da 404")
    void missingEditFormAnswers404() throws Exception {
        when(userService.findById(anyLong()))
                .thenThrow(new EntityNotFoundException("Usuario no encontrado"));

        String html = mockMvc.perform(get("/users/999/edit").with(user(admin)))
                .andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("href=\"/users\"");
    }

    /**
     * El enlace de salida sale del primer segmento de la URL pedida, así que
     * tiene que estar acotado a las secciones conocidas: si no, la página
     * construye un enlace con lo que mandó el cliente.
     */
    @Test
    @DisplayName("el enlace de salida no se arma con lo que mandó el cliente")
    void theWayOutIsNotBuiltFromTheRequest() throws Exception {
        when(categoryService.findById(anyLong()))
                .thenThrow(new EntityNotFoundException("no existe"));

        String html = mockMvc.perform(get("/categories/999").with(user(admin)))
                .andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).doesNotContain("href=\"/999\"");
    }

    @Test
    @DisplayName("un POST sobre algo que ya no está sigue redirigiendo con aviso")
    void postOnAMissingResourceStillRedirects() throws Exception {
        when(categoryService.findById(anyLong()))
                .thenThrow(new EntityNotFoundException("no existe"));

        mockMvc.perform(post("/categories/999/delete").with(user(admin)).with(csrf()))
                .andExpect(status().is3xxRedirection());
    }
}
