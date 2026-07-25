package com.malva_pastry_shop.backend.service;

import com.malva_pastry_shop.backend.domain.auth.Role;
import com.malva_pastry_shop.backend.domain.auth.RoleType;
import com.malva_pastry_shop.backend.domain.auth.User;
import com.malva_pastry_shop.backend.dto.request.CreateUserRequest;
import com.malva_pastry_shop.backend.dto.request.UpdateUserRequest;
import com.malva_pastry_shop.backend.repository.RoleRepository;
import com.malva_pastry_shop.backend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con email: " + username));
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con email: " + email));
    }

    // ========== CRUD Operations ==========

    public Page<User> findAll(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado con ID: " + id));
    }

    @Transactional
    public User createUser(CreateUserRequest request) {
        // Validar email duplicado
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Ya existe un usuario con el email: " + request.getEmail());
        }

        // Obtener rol: usar el especificado o EMPLOYEE por defecto
        Role userRole;
        if (request.getRoleId() != null) {
            userRole = roleRepository.findById(request.getRoleId())
                    .orElseThrow(() -> new EntityNotFoundException("Rol no encontrado con ID: " + request.getRoleId()));
        } else {
            userRole = roleRepository.findByName(RoleType.EMPLOYEE)
                    .orElseThrow(() -> new EntityNotFoundException("Rol EMPLOYEE no encontrado"));
        }

        User user = new User();
        user.setName(request.getName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(userRole);
        user.setEnabled(true);

        return userRepository.save(user);
    }

    @Transactional
    public User updateUser(Long id, UpdateUserRequest request) {
        User user = findById(id);

        // Validar email duplicado (excluyendo el usuario actual)
        if (userRepository.existsByEmailAndIdNot(request.getEmail(), id)) {
            throw new IllegalArgumentException("Ya existe otro usuario con el email: " + request.getEmail());
        }

        // Obtener el nuevo rol
        Role newRole = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new EntityNotFoundException("Rol no encontrado con ID: " + request.getRoleId()));

        user.setName(request.getName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setRole(newRole);
        user.setEnabled(request.getEnabled());

        // Actualizar password solo si se proporciona
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        return userRepository.save(user);
    }

    @Transactional
    public User toggleEnabled(Long id) {
        User user = findById(id);

        user.setEnabled(!user.getEnabled());
        return userRepository.save(user);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
}
