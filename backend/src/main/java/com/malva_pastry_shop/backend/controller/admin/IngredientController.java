package com.malva_pastry_shop.backend.controller.admin;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

import com.malva_pastry_shop.backend.domain.auth.User;
import com.malva_pastry_shop.backend.domain.inventory.Ingredient;
import com.malva_pastry_shop.backend.domain.inventory.UnitOfMeasure;
import com.malva_pastry_shop.backend.dto.request.IngredientRequest;
import com.malva_pastry_shop.backend.service.inventory.IngredientService;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/ingredients")
public class IngredientController {

    private final IngredientService ingredientService;

    public IngredientController(IngredientService ingredientService) {
        this.ingredientService = ingredientService;
    }

    // ========== Listados ==========

    @GetMapping
    public String list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String search,
            Model model) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        Page<Ingredient> ingredients;

        if (search != null && !search.isBlank()) {
            ingredients = ingredientService.search(search, pageable);
            model.addAttribute("search", search);
        } else {
            ingredients = ingredientService.findAllActive(pageable);
        }

        model.addAttribute("ingredients", ingredients);
        model.addAttribute("pageTitle", "Ingredientes");
        return "ingredients/list";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/deleted")
    public String listDeleted(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            Model model) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("deletedAt").descending());
        model.addAttribute("ingredients", ingredientService.findDeleted(pageable));
        model.addAttribute("pageTitle", "Ingredientes Eliminados");
        return "ingredients/deleted";
    }

    // ========== CRUD ==========

    @GetMapping("/{id}")
    public String show(@PathVariable Long id, Model model) {
        try {
            Ingredient ingredient = ingredientService.findById(id);
            long usageCount = ingredientService.countProductsUsingIngredient(id);

            model.addAttribute("ingredient", ingredient);
            model.addAttribute("usageCount", usageCount);
            model.addAttribute("pageTitle", ingredient.getName());
            return "ingredients/show";
        } catch (EntityNotFoundException e) {
            return "redirect:/ingredients";
        }
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("ingredient", new IngredientRequest());
        model.addAttribute("unitsOfMeasure", UnitOfMeasure.values());
        model.addAttribute("pageTitle", "Nuevo Ingrediente");
        return "ingredients/create";
    }

    @PostMapping
    public String create(
            @Valid @ModelAttribute("ingredient") IngredientRequest request,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            model.addAttribute("unitsOfMeasure", UnitOfMeasure.values());
            model.addAttribute("pageTitle", "Nuevo Ingrediente");
            return "ingredients/create";
        }

        try {
            ingredientService.create(request);
            redirectAttributes.addFlashAttribute("success", "Ingrediente creado exitosamente");
            return "redirect:/ingredients";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("unitsOfMeasure", UnitOfMeasure.values());
            model.addAttribute("pageTitle", "Nuevo Ingrediente");
            return "ingredients/create";
        }
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        try {
            Ingredient ingredient = ingredientService.findById(id);

            IngredientRequest request = new IngredientRequest();
            request.setName(ingredient.getName());
            request.setDescription(ingredient.getDescription());
            request.setUnitCost(ingredient.getUnitCost());
            request.setUnitOfMeasure(ingredient.getUnitOfMeasure());

            model.addAttribute("ingredient", request);
            model.addAttribute("ingredientId", id);
            model.addAttribute("unitsOfMeasure", UnitOfMeasure.values());
            model.addAttribute("pageTitle", "Editar Ingrediente");
            return "ingredients/edit";
        } catch (EntityNotFoundException e) {
            return "redirect:/ingredients";
        }
    }

    @PostMapping("/{id}")
    public String update(
            @PathVariable Long id,
            @Valid @ModelAttribute("ingredient") IngredientRequest request,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            model.addAttribute("ingredientId", id);
            model.addAttribute("unitsOfMeasure", UnitOfMeasure.values());
            model.addAttribute("pageTitle", "Editar Ingrediente");
            return "ingredients/edit";
        }

        try {
            ingredientService.update(id, request);
            redirectAttributes.addFlashAttribute("success", "Ingrediente actualizado exitosamente");
            return "redirect:/ingredients";
        } catch (IllegalArgumentException | EntityNotFoundException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("ingredientId", id);
            model.addAttribute("unitsOfMeasure", UnitOfMeasure.values());
            model.addAttribute("pageTitle", "Editar Ingrediente");
            return "ingredients/edit";
        }
    }

    // ========== Soft Delete ==========

    @PostMapping("/{id}/delete")
    public String delete(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser,
            RedirectAttributes redirectAttributes) {

        try {
            ingredientService.softDelete(id, currentUser);
            redirectAttributes.addFlashAttribute("success", "Ingrediente movido a la papelera");
        } catch (EntityNotFoundException e) {
            redirectAttributes.addFlashAttribute("error", "Ingrediente no encontrado");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/ingredients";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/restore")
    public String restore(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            ingredientService.restore(id);
            redirectAttributes.addFlashAttribute("success", "Ingrediente restaurado exitosamente");
        } catch (EntityNotFoundException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/ingredients/deleted";
    }

    // ========== Hard Delete ==========

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/hard-delete")
    public String hardDelete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            ingredientService.hardDelete(id);
            redirectAttributes.addFlashAttribute("success", "Ingrediente eliminado permanentemente");
        } catch (EntityNotFoundException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/ingredients/deleted";
    }
}
