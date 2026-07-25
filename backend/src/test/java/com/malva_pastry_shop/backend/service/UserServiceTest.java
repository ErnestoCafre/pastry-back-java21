package com.malva_pastry_shop.backend.service;

import com.malva_pastry_shop.backend.domain.auth.Role;
import com.malva_pastry_shop.backend.domain.auth.RoleType;
import com.malva_pastry_shop.backend.domain.auth.User;
import com.malva_pastry_shop.backend.dto.request.CreateUserRequest;
import com.malva_pastry_shop.backend.dto.request.UpdateUserRequest;
import com.malva_pastry_shop.backend.repository.RoleRepository;
import com.malva_pastry_shop.backend.repository.UserRepository;
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
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

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
    @DisplayName("loadUserByUsername Tests")
    class LoadUserByUsernameTests {

        @Test
        @DisplayName("Debe retornar UserDetails cuando el usuario existe")
        void loadUserByUsername_WhenUserExists_ReturnsUserDetails() {
            when(userRepository.findByEmail("juan@test.com")).thenReturn(Optional.of(testUser));

            UserDetails result = userService.loadUserByUsername("juan@test.com");

            assertThat(result).isNotNull();
            assertThat(result.getUsername()).isEqualTo("juan@test.com");
            verify(userRepository).findByEmail("juan@test.com");
        }

        @Test
        @DisplayName("Debe lanzar UsernameNotFoundException cuando el usuario no existe")
        void loadUserByUsername_WhenUserNotFound_ThrowsException() {
            when(userRepository.findByEmail("noexiste@test.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.loadUserByUsername("noexiste@test.com"))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessageContaining("Usuario no encontrado con email: noexiste@test.com");
        }
    }

    @Nested
    @DisplayName("findByEmail Tests")
    class FindByEmailTests {

        @Test
        @DisplayName("Debe retornar usuario cuando existe")
        void findByEmail_WhenUserExists_ReturnsUser() {
            when(userRepository.findByEmail("juan@test.com")).thenReturn(Optional.of(testUser));

            User result = userService.findByEmail("juan@test.com");

            assertThat(result).isNotNull();
            assertThat(result.getEmail()).isEqualTo("juan@test.com");
        }

        @Test
        @DisplayName("Debe lanzar excepcion cuando el usuario no existe")
        void findByEmail_WhenUserNotFound_ThrowsException() {
            when(userRepository.findByEmail("noexiste@test.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.findByEmail("noexiste@test.com"))
                    .isInstanceOf(UsernameNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("findAll Tests")
    class FindAllTests {

        @Test
        @DisplayName("Debe retornar pagina de usuarios")
        void findAll_ReturnsPageOfUsers() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<User> expectedPage = new PageImpl<>(List.of(testUser));
            when(userRepository.findAll(pageable)).thenReturn(expectedPage);

            Page<User> result = userService.findAll(pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getEmail()).isEqualTo("juan@test.com");
            verify(userRepository).findAll(pageable);
        }
    }

    @Nested
    @DisplayName("findById Tests")
    class FindByIdTests {

        @Test
        @DisplayName("Debe retornar usuario cuando existe")
        void findById_WhenUserExists_ReturnsUser() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            User result = userService.findById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Debe lanzar EntityNotFoundException cuando el usuario no existe")
        void findById_WhenUserNotFound_ThrowsException() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.findById(99L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Usuario no encontrado con ID: 99");
        }
    }

    @Nested
    @DisplayName("createUser Tests")
    class CreateUserTests {

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
        @DisplayName("Debe crear usuario exitosamente")
        void createUser_WithValidData_CreatesUser() {
            when(userRepository.existsByEmail("nuevo@test.com")).thenReturn(false);
            when(roleRepository.findByName(RoleType.EMPLOYEE)).thenReturn(Optional.of(employeeRole));
            when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User saved = invocation.getArgument(0);
                saved.setId(2L);
                return saved;
            });

            User result = userService.createUser(createRequest);

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("Nuevo");
            assertThat(result.getLastName()).isEqualTo("Usuario");
            assertThat(result.getEmail()).isEqualTo("nuevo@test.com");
            assertThat(result.getPasswordHash()).isEqualTo("encodedPassword");
            assertThat(result.getRole()).isEqualTo(employeeRole);
            assertThat(result.getEnabled()).isTrue();

            verify(userRepository).existsByEmail("nuevo@test.com");
            verify(roleRepository).findByName(RoleType.EMPLOYEE);
            verify(passwordEncoder).encode("password123");
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Debe lanzar excepcion cuando el email ya existe")
        void createUser_WithDuplicateEmail_ThrowsException() {
            when(userRepository.existsByEmail("nuevo@test.com")).thenReturn(true);

            assertThatThrownBy(() -> userService.createUser(createRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Ya existe un usuario con el email: nuevo@test.com");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Debe lanzar excepcion cuando el rol EMPLOYEE por defecto no existe")
        void createUser_WhenRoleNotFound_ThrowsException() {
            when(userRepository.existsByEmail("nuevo@test.com")).thenReturn(false);
            when(roleRepository.findByName(RoleType.EMPLOYEE)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.createUser(createRequest))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Rol EMPLOYEE no encontrado");

            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateUser Tests")
    class UpdateUserTests {

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
        @DisplayName("Debe actualizar usuario exitosamente sin cambiar password")
        void updateUser_WithValidDataWithoutPassword_UpdatesUser() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.existsByEmailAndIdNot("juan.actualizado@test.com", 1L)).thenReturn(false);
            when(roleRepository.findById(2L)).thenReturn(Optional.of(adminRole));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            User result = userService.updateUser(1L, updateRequest);

            assertThat(result.getName()).isEqualTo("Juan Actualizado");
            assertThat(result.getLastName()).isEqualTo("Perez Modificado");
            assertThat(result.getEmail()).isEqualTo("juan.actualizado@test.com");
            assertThat(result.getRole()).isEqualTo(adminRole);
            assertThat(result.getPasswordHash()).isEqualTo("hashedPassword");

            verify(passwordEncoder, never()).encode(anyString());
        }

        @Test
        @DisplayName("Debe actualizar usuario con nuevo password")
        void updateUser_WithNewPassword_UpdatesPassword() {
            updateRequest.setPassword("nuevoPassword123");
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.existsByEmailAndIdNot("juan.actualizado@test.com", 1L)).thenReturn(false);
            when(roleRepository.findById(2L)).thenReturn(Optional.of(adminRole));
            when(passwordEncoder.encode("nuevoPassword123")).thenReturn("newEncodedPassword");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            User result = userService.updateUser(1L, updateRequest);

            assertThat(result.getPasswordHash()).isEqualTo("newEncodedPassword");
            verify(passwordEncoder).encode("nuevoPassword123");
        }

        @Test
        @DisplayName("No debe actualizar password cuando es blank")
        void updateUser_WithBlankPassword_DoesNotUpdatePassword() {
            updateRequest.setPassword("   ");
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.existsByEmailAndIdNot("juan.actualizado@test.com", 1L)).thenReturn(false);
            when(roleRepository.findById(2L)).thenReturn(Optional.of(adminRole));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            User result = userService.updateUser(1L, updateRequest);

            assertThat(result.getPasswordHash()).isEqualTo("hashedPassword");
            verify(passwordEncoder, never()).encode(anyString());
        }

        @Test
        @DisplayName("Debe lanzar excepcion cuando el email ya existe en otro usuario")
        void updateUser_WithDuplicateEmail_ThrowsException() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.existsByEmailAndIdNot("juan.actualizado@test.com", 1L)).thenReturn(true);

            assertThatThrownBy(() -> userService.updateUser(1L, updateRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Ya existe otro usuario con el email: juan.actualizado@test.com");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Debe lanzar excepcion cuando el usuario no existe")
        void updateUser_WhenUserNotFound_ThrowsException() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.updateUser(99L, updateRequest))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Usuario no encontrado con ID: 99");
        }

        @Test
        @DisplayName("Debe lanzar excepcion cuando el rol no existe")
        void updateUser_WhenRoleNotFound_ThrowsException() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.existsByEmailAndIdNot("juan.actualizado@test.com", 1L)).thenReturn(false);
            when(roleRepository.findById(2L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.updateUser(1L, updateRequest))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Rol no encontrado con ID: 2");
        }
    }

    @Nested
    @DisplayName("toggleEnabled Tests")
    class ToggleEnabledTests {

        @Test
        @DisplayName("Debe desactivar usuario activo")
        void toggleEnabled_WhenUserEnabled_DisablesUser() {
            testUser.setEnabled(true);
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            User result = userService.toggleEnabled(1L);

            assertThat(result.getEnabled()).isFalse();
            verify(userRepository).save(testUser);
        }

        @Test
        @DisplayName("Debe activar usuario inactivo")
        void toggleEnabled_WhenUserDisabled_EnablesUser() {
            testUser.setEnabled(false);
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            User result = userService.toggleEnabled(1L);

            assertThat(result.getEnabled()).isTrue();
            verify(userRepository).save(testUser);
        }

        @Test
        @DisplayName("Debe lanzar excepcion cuando el usuario no existe")
        void toggleEnabled_WhenUserNotFound_ThrowsException() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.toggleEnabled(99L))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("existsByEmail Tests")
    class ExistsByEmailTests {

        @Test
        @DisplayName("Debe retornar true cuando el email existe")
        void existsByEmail_WhenEmailExists_ReturnsTrue() {
            when(userRepository.existsByEmail("juan@test.com")).thenReturn(true);

            boolean result = userService.existsByEmail("juan@test.com");

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Debe retornar false cuando el email no existe")
        void existsByEmail_WhenEmailNotExists_ReturnsFalse() {
            when(userRepository.existsByEmail("noexiste@test.com")).thenReturn(false);

            boolean result = userService.existsByEmail("noexiste@test.com");

            assertThat(result).isFalse();
        }
    }
}
