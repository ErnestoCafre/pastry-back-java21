package com.malva_pastry_shop.backend.controller.admin;

import com.malva_pastry_shop.backend.domain.auth.Role;
import com.malva_pastry_shop.backend.domain.auth.RoleType;
import com.malva_pastry_shop.backend.domain.auth.User;
import com.malva_pastry_shop.backend.dto.request.CreateUserRequest;
import com.malva_pastry_shop.backend.dto.request.UpdateUserRequest;
import com.malva_pastry_shop.backend.repository.RoleRepository;
import com.malva_pastry_shop.backend.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserController Tests")
class UserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private Model model;

    @Mock
    private BindingResult bindingResult;

    @Mock
    private RedirectAttributes redirectAttributes;

    @InjectMocks
    private UserController userController;

    private User testUser;
    private Role employeeRole;
    private Role adminRole;

    @BeforeEach
    void setUp() {
        employeeRole = new Role(RoleType.EMPLOYEE);
        employeeRole.setId(1L);

        adminRole = new Role(RoleType.ADMIN);
        adminRole.setId(2L);

        testUser = new User();
        testUser.setId(1L);
        testUser.setName("Juan");
        testUser.setLastName("Perez");
        testUser.setEmail("juan@test.com");
        testUser.setPasswordHash("hashedPassword");
        testUser.setRole(employeeRole);
        testUser.setEnabled(true);
    }

    @Nested
    @DisplayName("list() Tests")
    class ListTests {

        @Test
        @DisplayName("Debe retornar vista de lista con usuarios paginados")
        void list_ReturnsListViewWithPaginatedUsers() {
            Page<User> usersPage = new PageImpl<>(List.of(testUser));
            Pageable expectedPageable = PageRequest.of(0, 10, Sort.by("id").descending());
            when(userService.findAll(any(Pageable.class))).thenReturn(usersPage);

            String result = userController.list(0, 10, model);

            assertThat(result).isEqualTo("users/list");
            verify(model).addAttribute("users", usersPage);
            verify(model).addAttribute("pageTitle", "Usuarios");
            verify(userService).findAll(any(Pageable.class));
        }

        @Test
        @DisplayName("Debe usar valores por defecto para paginacion")
        void list_UsesDefaultPaginationValues() {
            Page<User> emptyPage = new PageImpl<>(List.of());
            when(userService.findAll(any(Pageable.class))).thenReturn(emptyPage);

            String result = userController.list(0, 10, model);

            assertThat(result).isEqualTo("users/list");
            verify(userService).findAll(any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("showCreateForm() Tests")
    class ShowCreateFormTests {

        @Test
        @DisplayName("Debe retornar vista de creacion con formulario vacio")
        void showCreateForm_ReturnsCreateViewWithEmptyForm() {
            String result = userController.showCreateForm(model);

            assertThat(result).isEqualTo("users/form");
            verify(model).addAttribute(eq("user"), any(CreateUserRequest.class));
            verify(model).addAttribute("pageTitle", "Nuevo Usuario");
        }
    }

    @Nested
    @DisplayName("create() Tests")
    class CreateTests {

        private CreateUserRequest createRequest;

        @BeforeEach
        void setUp() {
            createRequest = new CreateUserRequest();
            createRequest.setName("Nuevo");
            createRequest.setLastName("Usuario");
            createRequest.setEmail("nuevo@test.com");
            createRequest.setPassword("password123");
        }

        @Test
        @DisplayName("Debe crear usuario y redirigir a lista con mensaje de exito")
        void create_WithValidData_RedirectsToListWithSuccess() {
            when(bindingResult.hasErrors()).thenReturn(false);
            when(userService.createUser(createRequest)).thenReturn(testUser);

            String result = userController.create(createRequest, bindingResult, model, redirectAttributes);

            assertThat(result).isEqualTo("redirect:/users");
            verify(userService).createUser(createRequest);
            verify(redirectAttributes).addFlashAttribute("success", "Usuario creado exitosamente");
        }

        @Test
        @DisplayName("Debe retornar vista de creacion cuando hay errores de validacion")
        void create_WithValidationErrors_ReturnsCreateView() {
            when(bindingResult.hasErrors()).thenReturn(true);

            String result = userController.create(createRequest, bindingResult, model, redirectAttributes);

            assertThat(result).isEqualTo("users/form");
            verify(model).addAttribute("pageTitle", "Nuevo Usuario");
            verify(userService, never()).createUser(any());
        }

        @Test
        @DisplayName("Debe retornar vista de creacion cuando el email esta duplicado")
        void create_WithDuplicateEmail_ReturnsCreateViewWithError() {
            when(bindingResult.hasErrors()).thenReturn(false);
            when(userService.createUser(createRequest))
                    .thenThrow(new IllegalArgumentException("Ya existe un usuario con el email: nuevo@test.com"));

            String result = userController.create(createRequest, bindingResult, model, redirectAttributes);

            assertThat(result).isEqualTo("users/form");
            verify(model).addAttribute("error", "Ya existe un usuario con el email: nuevo@test.com");
            verify(model).addAttribute("pageTitle", "Nuevo Usuario");
        }

        @Test
        @DisplayName("Debe retornar vista de creacion cuando el rol no existe")
        void create_WithInvalidRole_ReturnsCreateViewWithError() {
            when(bindingResult.hasErrors()).thenReturn(false);
            when(userService.createUser(createRequest))
                    .thenThrow(new EntityNotFoundException("Rol no encontrado con ID: 99"));

            String result = userController.create(createRequest, bindingResult, model, redirectAttributes);

            assertThat(result).isEqualTo("users/form");
            verify(model).addAttribute("error", "Rol no encontrado con ID: 99");
            verify(model).addAttribute("pageTitle", "Nuevo Usuario");
        }
    }

    @Nested
    @DisplayName("showEditForm() Tests")
    class ShowEditFormTests {

        @Test
        @DisplayName("Debe retornar vista de edicion con datos del usuario")
        void showEditForm_WithExistingUser_ReturnsEditView() {
            List<Role> roles = List.of(employeeRole, adminRole);
            when(userService.findById(1L)).thenReturn(testUser);
            when(roleRepository.findAll()).thenReturn(roles);

            String result = userController.showEditForm(1L, model);

            assertThat(result).isEqualTo("users/form");
            verify(model).addAttribute(eq("user"), any(UpdateUserRequest.class));
            verify(model).addAttribute("userId", 1L);
            verify(model).addAttribute("roles", roles);
            verify(model).addAttribute("pageTitle", "Editar Usuario");
        }

        /**
         * Ya no redirige: deja pasar la excepción para que
         * {@code AdminNotFoundAdvice} la convierta en un 404 de verdad. El
         * redirect silencioso hacía que un id inexistente y un enlace viejo se
         * vieran igual que una visita normal al listado.
         */
        @Test
        @DisplayName("Debe dejar pasar la excepcion cuando el usuario no existe")
        void showEditForm_WithNonExistingUser_PropagatesNotFound() {
            when(userService.findById(99L)).thenThrow(new EntityNotFoundException("Usuario no encontrado"));

            assertThatThrownBy(() -> userController.showEditForm(99L, model))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("update() Tests")
    class UpdateTests {

        private UpdateUserRequest updateRequest;

        @BeforeEach
        void setUp() {
            updateRequest = new UpdateUserRequest();
            updateRequest.setName("Juan Actualizado");
            updateRequest.setLastName("Perez Modificado");
            updateRequest.setEmail("juan.actualizado@test.com");
            updateRequest.setRoleId(2L);
            updateRequest.setEnabled(true);
        }

        @Test
        @DisplayName("Debe actualizar usuario y redirigir a lista con mensaje de exito")
        void update_WithValidData_RedirectsToListWithSuccess() {
            when(bindingResult.hasErrors()).thenReturn(false);
            when(userService.updateUser(1L, updateRequest)).thenReturn(testUser);

            String result = userController.update(1L, updateRequest, bindingResult, model, redirectAttributes);

            assertThat(result).isEqualTo("redirect:/users");
            verify(userService).updateUser(1L, updateRequest);
            verify(redirectAttributes).addFlashAttribute("success", "Usuario actualizado exitosamente");
        }

        @Test
        @DisplayName("Debe retornar vista de edicion cuando hay errores de validacion")
        void update_WithValidationErrors_ReturnsEditView() {
            List<Role> roles = List.of(employeeRole, adminRole);
            when(bindingResult.hasErrors()).thenReturn(true);
            when(roleRepository.findAll()).thenReturn(roles);

            String result = userController.update(1L, updateRequest, bindingResult, model, redirectAttributes);

            assertThat(result).isEqualTo("users/form");
            verify(model).addAttribute("userId", 1L);
            verify(model).addAttribute("roles", roles);
            verify(model).addAttribute("pageTitle", "Editar Usuario");
            verify(userService, never()).updateUser(anyLong(), any());
        }

        @Test
        @DisplayName("Debe retornar vista de edicion cuando el email esta duplicado")
        void update_WithDuplicateEmail_ReturnsEditViewWithError() {
            List<Role> roles = List.of(employeeRole, adminRole);
            when(bindingResult.hasErrors()).thenReturn(false);
            when(userService.updateUser(1L, updateRequest))
                    .thenThrow(new IllegalArgumentException("Ya existe otro usuario con el email"));
            when(roleRepository.findAll()).thenReturn(roles);

            String result = userController.update(1L, updateRequest, bindingResult, model, redirectAttributes);

            assertThat(result).isEqualTo("users/form");
            verify(model).addAttribute(eq("error"), anyString());
            verify(model).addAttribute("userId", 1L);
            verify(model).addAttribute("roles", roles);
        }

        @Test
        @DisplayName("Debe redirigir a lista cuando el usuario no existe")
        void update_WithNonExistingUser_RedirectsToList() {
            when(bindingResult.hasErrors()).thenReturn(false);
            when(userService.updateUser(1L, updateRequest))
                    .thenThrow(new EntityNotFoundException("Usuario no encontrado"));

            String result = userController.update(1L, updateRequest, bindingResult, model, redirectAttributes);

            assertThat(result).isEqualTo("redirect:/users");
        }
    }

    @Nested
    @DisplayName("toggleEnabled() Tests")
    class ToggleEnabledTests {

        @Test
        @DisplayName("Debe activar usuario y redirigir con mensaje de exito")
        void toggleEnabled_WhenUserDisabled_ActivatesAndRedirects() {
            User disabledUser = new User();
            disabledUser.setId(1L);
            disabledUser.setEnabled(true);
            when(userService.toggleEnabled(1L)).thenReturn(disabledUser);

            String result = userController.toggleEnabled(1L, redirectAttributes);

            assertThat(result).isEqualTo("redirect:/users");
            verify(redirectAttributes).addFlashAttribute("success", "Usuario activado exitosamente");
        }

        @Test
        @DisplayName("Debe desactivar usuario y redirigir con mensaje de exito")
        void toggleEnabled_WhenUserEnabled_DeactivatesAndRedirects() {
            User enabledUser = new User();
            enabledUser.setId(1L);
            enabledUser.setEnabled(false);
            when(userService.toggleEnabled(1L)).thenReturn(enabledUser);

            String result = userController.toggleEnabled(1L, redirectAttributes);

            assertThat(result).isEqualTo("redirect:/users");
            verify(redirectAttributes).addFlashAttribute("success", "Usuario desactivado exitosamente");
        }

        @Test
        @DisplayName("Debe redirigir con mensaje de error cuando el usuario no existe")
        void toggleEnabled_WhenUserNotFound_RedirectsWithError() {
            when(userService.toggleEnabled(99L)).thenThrow(new EntityNotFoundException("Usuario no encontrado"));

            String result = userController.toggleEnabled(99L, redirectAttributes);

            assertThat(result).isEqualTo("redirect:/users");
            verify(redirectAttributes).addFlashAttribute("error", "Usuario no encontrado");
        }
    }
}
