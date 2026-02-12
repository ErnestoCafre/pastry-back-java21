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
import com.malva_pastry_shop.backend.domain.storefront.Tag;
import com.malva_pastry_shop.backend.dto.request.TagRequest;
import com.malva_pastry_shop.backend.service.inventory.ProductService;
import com.malva_pastry_shop.backend.service.storefront.TagService;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/tags")
public class TagController {

    private final TagService tagService;
    private final ProductService productService;

    public TagController(TagService tagService, ProductService productService) {
        this.tagService = tagService;
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
        Page<Tag> tags;

        if (search != null && !search.isBlank()) {
            tags = tagService.search(search, pageable);
            model.addAttribute("search", search);
        } else {
            tags = tagService.findAllActive(pageable);
        }

        model.addAttribute("tags", tags);
        model.addAttribute("pageTitle", "Tags");
        return "tags/list";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM_ADMIN')")
    @GetMapping("/deleted")
    public String listDeleted(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            Model model) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("deletedAt").descending());
        model.addAttribute("tags", tagService.findDeleted(pageable));
        model.addAttribute("pageTitle", "Tags Eliminados");
        return "tags/deleted";
    }

    // ========== CRUD ==========

    @GetMapping("/{id}")
    public String show(@PathVariable Long id, Model model) {
        try {
            Tag tag = tagService.findById(id);
            model.addAttribute("tag", tag);
            model.addAttribute("usageCount", productService.countProductsByTag(id));
            model.addAttribute("pageTitle", tag.getName());
            return "tags/show";
        } catch (EntityNotFoundException e) {
            return "redirect:/tags";
        }
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("tag", new TagRequest());
        model.addAttribute("pageTitle", "Nuevo Tag");
        return "tags/create";
    }

    @PostMapping
    public String create(
            @Valid @ModelAttribute("tag") TagRequest request,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            model.addAttribute("pageTitle", "Nuevo Tag");
            return "tags/create";
        }

        try {
            tagService.create(request);
            redirectAttributes.addFlashAttribute("success", "Tag creado exitosamente");
            return "redirect:/tags";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("pageTitle", "Nuevo Tag");
            return "tags/create";
        }
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        try {
            Tag tag = tagService.findById(id);

            TagRequest request = new TagRequest();
            request.setName(tag.getName());
            request.setDescription(tag.getDescription());

            model.addAttribute("tag", request);
            model.addAttribute("tagId", id);
            model.addAttribute("pageTitle", "Editar Tag");
            return "tags/edit";
        } catch (EntityNotFoundException e) {
            return "redirect:/tags";
        }
    }

    @PostMapping("/{id}")
    public String update(
            @PathVariable Long id,
            @Valid @ModelAttribute("tag") TagRequest request,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            model.addAttribute("tagId", id);
            model.addAttribute("pageTitle", "Editar Tag");
            return "tags/edit";
        }

        try {
            tagService.update(id, request);
            redirectAttributes.addFlashAttribute("success", "Tag actualizado exitosamente");
            return "redirect:/tags";
        } catch (IllegalArgumentException | EntityNotFoundException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("tagId", id);
            model.addAttribute("pageTitle", "Editar Tag");
            return "tags/edit";
        }
    }

    // ========== Soft Delete ==========

    @PostMapping("/{id}/delete")
    public String delete(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser,
            RedirectAttributes redirectAttributes) {

        try {
            tagService.softDelete(id, currentUser);
            redirectAttributes.addFlashAttribute("success", "Tag movido a la papelera");
        } catch (EntityNotFoundException e) {
            redirectAttributes.addFlashAttribute("error", "Tag no encontrado");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/tags";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM_ADMIN')")
    @PostMapping("/{id}/restore")
    public String restore(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            tagService.restore(id);
            redirectAttributes.addFlashAttribute("success", "Tag restaurado exitosamente");
        } catch (EntityNotFoundException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/tags/deleted";
    }

    // ========== Hard Delete ==========

    @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM_ADMIN')")
    @PostMapping("/{id}/hard-delete")
    public String hardDelete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            tagService.hardDelete(id);
            redirectAttributes.addFlashAttribute("success", "Tag eliminado permanentemente");
        } catch (EntityNotFoundException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/tags/deleted";
    }

    // ========== Gestión de Productos por Tag ==========

    @GetMapping("/{id}/products")
    public String listProducts(@PathVariable Long id, Model model) {
        try {
            Tag tag = tagService.findById(id);
            model.addAttribute("tag", tag);
            model.addAttribute("products", productService.getProductsByTag(id));
            model.addAttribute("availableProducts", productService.getAvailableProductsForTag(id));
            model.addAttribute("pageTitle", "Productos con tag: " + tag.getName());
            return "tags/products";
        } catch (EntityNotFoundException e) {
            return "redirect:/tags";
        }
    }

    @PostMapping("/{id}/products/{productId}")
    public String addProduct(
            @PathVariable Long id,
            @PathVariable Long productId,
            RedirectAttributes redirectAttributes) {
        try {
            productService.addProductToTag(id, productId);
            redirectAttributes.addFlashAttribute("success", "Producto agregado al tag");
        } catch (EntityNotFoundException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/tags/" + id + "/products";
    }

    @PostMapping("/{id}/products/{productId}/remove")
    public String removeProduct(
            @PathVariable Long id,
            @PathVariable Long productId,
            RedirectAttributes redirectAttributes) {
        try {
            productService.removeProductFromTag(id, productId);
            redirectAttributes.addFlashAttribute("success", "Producto removido del tag");
        } catch (EntityNotFoundException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/tags/" + id + "/products";
    }
}
