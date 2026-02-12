package com.malva_pastry_shop.backend.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;

import com.malva_pastry_shop.backend.domain.auth.User;
import com.malva_pastry_shop.backend.domain.inventory.Category;
import com.malva_pastry_shop.backend.dto.request.CategoryRequest;
import com.malva_pastry_shop.backend.service.inventory.CategoryService;
import com.malva_pastry_shop.backend.service.inventory.ProductService;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Controller
@RequestMapping("/categories")
public class CategoryController {

    private final CategoryService categoryService;
    private final ProductService productService;

    public CategoryController(CategoryService categoryService, ProductService productService) {
        this.categoryService = categoryService;
        this.productService = productService;
    }

    // ========== Listados ==========

    @GetMapping
    public String list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String search,
            Model model) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        Page<Category> categories;

        if (search != null && !search.isBlank()) {
            categories = categoryService.search(search, pageable);
            model.addAttribute("search", search);
        } else {
            categories = categoryService.findAllActive(pageable);
        }

        model.addAttribute("categories", categories);
        model.addAttribute("pageTitle", "Categorías");
        return "categories/list";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM_ADMIN')")
    @GetMapping("/deleted")
    public String listDeleted(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            Model model) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("deletedAt").descending());
        model.addAttribute("categories", categoryService.findDeleted(pageable));
        model.addAttribute("pageTitle", "Categorías Eliminadas");
        return "categories/deleted";
    }

    @GetMapping("/{id}/products")
    public String listProducts(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            Model model) {

        try {
            Category category = categoryService.findById(id);
            Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());

            model.addAttribute("category", category);
            model.addAttribute("products", productService.findByCategoryId(id, pageable));
            model.addAttribute("pageTitle", "Productos de " + category.getName());
            return "categories/products";
        } catch (EntityNotFoundException e) {
            return "redirect:/categories";
        }
    }

    // ========== CRUD ==========

    @GetMapping("/{id}")
    public String show(@PathVariable Long id, Model model) {
        try {
            Category category = categoryService.findById(id);
            long productCount = categoryService.countProducts(id);

            model.addAttribute("category", category);
            model.addAttribute("productCount", productCount);
            model.addAttribute("pageTitle", category.getName());
            return "categories/show";
        } catch (EntityNotFoundException e) {
            return "redirect:/categories";
        }
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("category", new CategoryRequest());
        model.addAttribute("pageTitle", "Nueva Categoría");
        return "categories/create";
    }

    @PostMapping
    public String create(
            @Valid @ModelAttribute("category") CategoryRequest request,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            model.addAttribute("pageTitle", "Nueva Categoría");
            return "categories/create";
        }

        try {
            categoryService.create(request);
            redirectAttributes.addFlashAttribute("success", "Categoría creada exitosamente");
            return "redirect:/categories";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("pageTitle", "Nueva Categoría");
            return "categories/create";
        }
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        try {
            Category category = categoryService.findById(id);

            CategoryRequest request = new CategoryRequest();
            request.setName(category.getName());
            request.setDescription(category.getDescription());

            model.addAttribute("category", request);
            model.addAttribute("categoryId", id);
            model.addAttribute("pageTitle", "Editar Categoría");
            return "categories/edit";
        } catch (EntityNotFoundException e) {
            return "redirect:/categories";
        }
    }

    @PostMapping("/{id}")
    public String update(
            @PathVariable Long id,
            @Valid @ModelAttribute("category") CategoryRequest request,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            model.addAttribute("categoryId", id);
            model.addAttribute("pageTitle", "Editar Categoría");
            return "categories/edit";
        }

        try {
            categoryService.update(id, request);
            redirectAttributes.addFlashAttribute("success", "Categoría actualizada exitosamente");
            return "redirect:/categories";
        } catch (IllegalArgumentException | EntityNotFoundException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("categoryId", id);
            model.addAttribute("pageTitle", "Editar Categoría");
            return "categories/edit";
        }
    }

    // ========== Soft Delete ==========

    @PostMapping("/{id}/delete")
    public String delete(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser,
            RedirectAttributes redirectAttributes) {

        try {
            categoryService.softDelete(id, currentUser);
            redirectAttributes.addFlashAttribute("success", "Categoría movida a la papelera");
        } catch (EntityNotFoundException e) {
            redirectAttributes.addFlashAttribute("error", "Categoría no encontrada");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/categories";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM_ADMIN')")
    @PostMapping("/{id}/restore")
    public String restore(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            categoryService.restore(id);
            redirectAttributes.addFlashAttribute("success", "Categoría restaurada exitosamente");
        } catch (EntityNotFoundException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/categories/deleted";
    }

    // ========== Hard Delete ==========

    @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM_ADMIN')")
    @PostMapping("/{id}/hard-delete")
    public String hardDelete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            categoryService.hardDelete(id);
            redirectAttributes.addFlashAttribute("success", "Categoría eliminada permanentemente");
        } catch (EntityNotFoundException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/categories/deleted";
    }
}
