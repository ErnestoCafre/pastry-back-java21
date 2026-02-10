package com.malva_pastry_shop.backend.controller.admin;

import java.math.BigDecimal;

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
import com.malva_pastry_shop.backend.domain.inventory.Product;
import com.malva_pastry_shop.backend.dto.request.ProductRequest;
import com.malva_pastry_shop.backend.service.inventory.CategoryService;
import com.malva_pastry_shop.backend.service.storefront.ProductService;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Controller
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;
    private final CategoryService categoryService;

    public ProductController(ProductService productService,
            CategoryService categoryService) {
        this.productService = productService;
        this.categoryService = categoryService;
    }

    // ========== Listados ==========

    @GetMapping
    public String list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String search,
            Model model) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        Page<Product> products;

        if (search != null && !search.isBlank()) {
            products = productService.search(search, pageable);
            model.addAttribute("search", search);
        } else if (categoryId != null) {
            products = productService.findByCategoryId(categoryId, pageable);
            model.addAttribute("categoryId", categoryId);
        } else {
            products = productService.findAllActive(pageable);
        }

        model.addAttribute("products", products);
        model.addAttribute("categories", categoryService.findAllActive(Pageable.unpaged()));
        model.addAttribute("pageTitle", "Productos");
        return "products/list";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM_ADMIN')")
    @GetMapping("/deleted")
    public String listDeleted(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            Model model) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("deletedAt").descending());
        model.addAttribute("products", productService.findDeleted(pageable));
        model.addAttribute("pageTitle", "Productos Eliminados");
        return "products/deleted";
    }

    // ========== CRUD ==========

    @GetMapping("/{id}")
    public String show(@PathVariable Long id, Model model) {
        try {
            Product product = productService.findById(id);
            model.addAttribute("product", product);
            model.addAttribute("pageTitle", product.getName());
            return "products/show";
        } catch (EntityNotFoundException e) {
            return "redirect:/products";
        }
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("product", new ProductRequest());
        model.addAttribute("categories", categoryService.findAllActive(Pageable.unpaged()));
        model.addAttribute("pageTitle", "Nuevo Producto");
        return "products/create";
    }

    @PostMapping
    public String create(
            @Valid @ModelAttribute("product") ProductRequest request,
            BindingResult result,
            @AuthenticationPrincipal User currentUser,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            model.addAttribute("categories", categoryService.findAllActive(Pageable.unpaged()));
            model.addAttribute("pageTitle", "Nuevo Producto");
            return "products/create";
        }

        try {
            productService.create(request, currentUser);
            redirectAttributes.addFlashAttribute("success", "Producto creado exitosamente");
            return "redirect:/products";
        } catch (IllegalArgumentException | EntityNotFoundException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("categories", categoryService.findAllActive(Pageable.unpaged()));
            model.addAttribute("pageTitle", "Nuevo Producto");
            return "products/create";
        }
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        try {
            Product product = productService.findById(id);

            ProductRequest request = new ProductRequest();
            request.setName(product.getName());
            request.setDescription(product.getDescription());
            request.setPreparationDays(product.getPreparationDays());
            request.setBasePrice(product.getBasePrice());
            request.setCategoryId(product.getCategory() != null ? product.getCategory().getId() : null);
            request.setImageUrl(product.getImageUrl());

            model.addAttribute("product", request);
            model.addAttribute("productId", id);
            model.addAttribute("categories", categoryService.findAllActive(Pageable.unpaged()));
            model.addAttribute("pageTitle", "Editar Producto");
            return "products/edit";
        } catch (EntityNotFoundException e) {
            return "redirect:/products";
        }
    }

    @PostMapping("/{id}")
    public String update(
            @PathVariable Long id,
            @Valid @ModelAttribute("product") ProductRequest request,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            model.addAttribute("productId", id);
            model.addAttribute("categories", categoryService.findAllActive(Pageable.unpaged()));
            model.addAttribute("pageTitle", "Editar Producto");
            return "products/edit";
        }

        try {
            productService.update(id, request);
            redirectAttributes.addFlashAttribute("success", "Producto actualizado exitosamente");
            return "redirect:/products";
        } catch (IllegalArgumentException | EntityNotFoundException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("productId", id);
            model.addAttribute("categories", categoryService.findAllActive(Pageable.unpaged()));
            model.addAttribute("pageTitle", "Editar Producto");
            return "products/edit";
        }
    }

    // ========== Soft Delete ==========

    @PostMapping("/{id}/delete")
    public String delete(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser,
            RedirectAttributes redirectAttributes) {

        try {
            productService.softDelete(id, currentUser);
            redirectAttributes.addFlashAttribute("success", "Producto movido a la papelera");
        } catch (EntityNotFoundException e) {
            redirectAttributes.addFlashAttribute("error", "Producto no encontrado");
        }
        return "redirect:/products";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM_ADMIN')")
    @PostMapping("/{id}/restore")
    public String restore(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            productService.restore(id);
            redirectAttributes.addFlashAttribute("success", "Producto restaurado exitosamente");
        } catch (EntityNotFoundException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/products/deleted";
    }

    // ========== Hard Delete ==========

    @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM_ADMIN')")
    @PostMapping("/{id}/hard-delete")
    public String hardDelete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            productService.hardDelete(id);
            redirectAttributes.addFlashAttribute("success", "Producto eliminado permanentemente");
        } catch (EntityNotFoundException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/products/deleted";
    }

    // ========== Gestión de Tags por Producto ==========

    @GetMapping("/{id}/tags")
    public String listTags(@PathVariable Long id, Model model) {
        try {
            Product product = productService.findById(id);
            model.addAttribute("product", product);
            model.addAttribute("tags", productService.getProductTags(id));
            model.addAttribute("availableTags", productService.getAvailableTagsForProduct(id));
            model.addAttribute("pageTitle", "Tags de: " + product.getName());
            return "products/tags";
        } catch (EntityNotFoundException e) {
            return "redirect:/products";
        }
    }

    @PostMapping("/{id}/tags/{tagId}")
    public String addTag(
            @PathVariable Long id,
            @PathVariable Long tagId,
            RedirectAttributes redirectAttributes) {
        try {
            productService.addTagToProduct(id, tagId);
            redirectAttributes.addFlashAttribute("success", "Tag agregado al producto");
        } catch (EntityNotFoundException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/products/" + id + "/tags";
    }

    @PostMapping("/{id}/tags/{tagId}/remove")
    public String removeTag(
            @PathVariable Long id,
            @PathVariable Long tagId,
            RedirectAttributes redirectAttributes) {
        try {
            productService.removeTagFromProduct(id, tagId);
            redirectAttributes.addFlashAttribute("success", "Tag removido del producto");
        } catch (EntityNotFoundException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/products/" + id + "/tags";
    }

    // ========== Gestión de Recetas (Ingredientes) ==========

    @GetMapping("/{id}/recipe")
    public String showRecipe(@PathVariable Long id, Model model) {
        try {
            Product product = productService.findById(id);
            model.addAttribute("product", product);
            model.addAttribute("ingredients", productService.getProductIngredients(id));
            model.addAttribute("availableIngredients", productService.getAvailableIngredientsForProduct(id));
            model.addAttribute("recipeCost", productService.calculateRecipeCost(id));
            model.addAttribute("pageTitle", "Receta: " + product.getName());
            return "products/recipe";
        } catch (EntityNotFoundException e) {
            return "redirect:/products";
        }
    }

    @PostMapping("/{id}/recipe/ingredients/{ingredientId}")
    public String addIngredient(
            @PathVariable Long id,
            @PathVariable Long ingredientId,
            @RequestParam BigDecimal quantity,
            RedirectAttributes redirectAttributes) {
        try {
            productService.addIngredientToProduct(id, ingredientId, quantity);
            redirectAttributes.addFlashAttribute("success", "Ingrediente agregado a la receta");
        } catch (EntityNotFoundException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (IllegalStateException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/products/" + id + "/recipe";
    }

    @PostMapping("/{id}/recipe/ingredients/{ingredientId}/remove")
    public String removeIngredient(
            @PathVariable Long id,
            @PathVariable Long ingredientId,
            RedirectAttributes redirectAttributes) {
        try {
            productService.removeIngredientFromProduct(id, ingredientId);
            redirectAttributes.addFlashAttribute("success", "Ingrediente removido de la receta");
        } catch (EntityNotFoundException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/products/" + id + "/recipe";
    }

    @PostMapping("/{id}/recipe/ingredients/{ingredientId}/update")
    public String updateIngredientQuantity(
            @PathVariable Long id,
            @PathVariable Long ingredientId,
            @RequestParam BigDecimal quantity,
            RedirectAttributes redirectAttributes) {
        try {
            productService.updateIngredientQuantity(id, ingredientId, quantity);
            redirectAttributes.addFlashAttribute("success", "Cantidad actualizada");
        } catch (EntityNotFoundException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/products/" + id + "/recipe";
    }
}
