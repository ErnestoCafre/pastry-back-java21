package com.malva_pastry_shop.backend.controller.admin;

import com.malva_pastry_shop.backend.domain.auth.User;
import com.malva_pastry_shop.backend.dto.request.CreateUserRequest;
import com.malva_pastry_shop.backend.dto.request.UpdateUserRequest;
import com.malva_pastry_shop.backend.repository.RoleRepository;
import com.malva_pastry_shop.backend.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;
    private final RoleRepository roleRepository;

    public UserController(UserService userService, RoleRepository roleRepository) {
        this.userService = userService;
        this.roleRepository = roleRepository;
    }

    @GetMapping
    public String list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            Model model) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<User> users = userService.findAll(pageable);

        model.addAttribute("users", users);
        model.addAttribute("pageTitle", "Usuarios");
        return "users/list";
    }

    @GetMapping("/{id}")
    public String show(@PathVariable Long id, Model model) {
        User user = userService.findById(id);
        model.addAttribute("user", user);
        model.addAttribute("pageTitle", "Usuario: " + user.getFullName());
        return "users/show";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("user", new CreateUserRequest());
        model.addAttribute("roles", roleRepository.findAll());
        model.addAttribute("pageTitle", "Nuevo Usuario");
        return "users/form";
    }

    @PostMapping
    public String create(
            @Valid @ModelAttribute("user") CreateUserRequest request,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            model.addAttribute("roles", roleRepository.findAll());
            model.addAttribute("pageTitle", "Nuevo Usuario");
            return "users/form";
        }

        try {
            userService.createUser(request);
            redirectAttributes.addFlashAttribute("success", "Usuario creado exitosamente");
            return "redirect:/users";
        } catch (IllegalArgumentException | EntityNotFoundException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("roles", roleRepository.findAll());
            model.addAttribute("pageTitle", "Nuevo Usuario");
            return "users/form";
        }
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        User user = userService.findById(id);

        UpdateUserRequest request = new UpdateUserRequest();
        request.setName(user.getName());
        request.setLastName(user.getLastName());
        request.setEmail(user.getEmail());
        request.setRoleId(user.getRole().getId());
        request.setEnabled(user.getEnabled());

        model.addAttribute("user", request);
        model.addAttribute("userId", id);
        model.addAttribute("roles", roleRepository.findAll());
        model.addAttribute("pageTitle", "Editar Usuario");
        return "users/form";
    }

    @PostMapping("/{id}")
    public String update(
            @PathVariable Long id,
            @Valid @ModelAttribute("user") UpdateUserRequest request,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            model.addAttribute("userId", id);
            model.addAttribute("roles", roleRepository.findAll());
            model.addAttribute("pageTitle", "Editar Usuario");
            return "users/form";
        }

        try {
            userService.updateUser(id, request);
            redirectAttributes.addFlashAttribute("success", "Usuario actualizado exitosamente");
            return "redirect:/users";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("userId", id);
            model.addAttribute("roles", roleRepository.findAll());
            model.addAttribute("pageTitle", "Editar Usuario");
            return "users/form";
        } catch (EntityNotFoundException e) {
            return "redirect:/users";
        }
    }

    @PostMapping("/{id}/toggle")
    public String toggleEnabled(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            User user = userService.toggleEnabled(id);
            String status = user.getEnabled() ? "activado" : "desactivado";
            redirectAttributes.addFlashAttribute("success", "Usuario " + status + " exitosamente");
        } catch (EntityNotFoundException e) {
            redirectAttributes.addFlashAttribute("error", "Usuario no encontrado");
        }
        return "redirect:/users";
    }
}
