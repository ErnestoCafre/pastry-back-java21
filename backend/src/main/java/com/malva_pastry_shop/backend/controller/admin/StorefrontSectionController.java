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
import com.malva_pastry_shop.backend.domain.storefront.StorefrontSection;
import com.malva_pastry_shop.backend.dto.request.StorefrontSectionRequest;
import com.malva_pastry_shop.backend.service.storefront.StorefrontSectionService;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/sections")
public class StorefrontSectionController {

    private final StorefrontSectionService sectionService;

    public StorefrontSectionController(StorefrontSectionService sectionService) {
        this.sectionService = sectionService;
    }

    // ========== Listados ==========

    @GetMapping
    public String list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String search,
            Model model) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("displayOrder").ascending().and(Sort.by("name").ascending()));
        Page<StorefrontSection> sections;

        if (search != null && !search.isBlank()) {
            sections = sectionService.search(search, pageable);
            model.addAttribute("search", search);
        } else {
            sections = sectionService.findAllActive(pageable);
        }

        model.addAttribute("sections", sections);
        model.addAttribute("pageTitle", "Secciones");
        return "sections/list";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM_ADMIN')")
    @GetMapping("/deleted")
    public String listDeleted(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            Model model) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("deletedAt").descending());
        model.addAttribute("sections", sectionService.findDeleted(pageable));
        model.addAttribute("pageTitle", "Secciones Eliminadas");
        return "sections/deleted";
    }

    // ========== CRUD ==========

    @GetMapping("/{id}")
    public String show(@PathVariable Long id, Model model) {
        try {
            StorefrontSection section = sectionService.findById(id);
            long productCount = sectionService.countProducts(id);

            model.addAttribute("section", section);
            model.addAttribute("productCount", productCount);
            model.addAttribute("pageTitle", section.getName());
            return "sections/show";
        } catch (EntityNotFoundException e) {
            return "redirect:/sections";
        }
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("section", new StorefrontSectionRequest());
        model.addAttribute("pageTitle", "Nueva Sección");
        return "sections/create";
    }

    @PostMapping
    public String create(
            @Valid @ModelAttribute("section") StorefrontSectionRequest request,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            model.addAttribute("pageTitle", "Nueva Sección");
            return "sections/create";
        }

        try {
            sectionService.create(request);
            redirectAttributes.addFlashAttribute("success", "Sección creada exitosamente");
            return "redirect:/sections";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("pageTitle", "Nueva Sección");
            return "sections/create";
        }
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        try {
            StorefrontSection section = sectionService.findById(id);

            StorefrontSectionRequest request = new StorefrontSectionRequest();
            request.setName(section.getName());
            request.setDescription(section.getDescription());
            request.setDisplayOrder(section.getDisplayOrder());
            request.setVisible(section.getVisible());

            model.addAttribute("section", request);
            model.addAttribute("sectionId", id);
            model.addAttribute("pageTitle", "Editar Sección");
            return "sections/edit";
        } catch (EntityNotFoundException e) {
            return "redirect:/sections";
        }
    }

    @PostMapping("/{id}")
    public String update(
            @PathVariable Long id,
            @Valid @ModelAttribute("section") StorefrontSectionRequest request,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            model.addAttribute("sectionId", id);
            model.addAttribute("pageTitle", "Editar Sección");
            return "sections/edit";
        }

        try {
            sectionService.update(id, request);
            redirectAttributes.addFlashAttribute("success", "Sección actualizada exitosamente");
            return "redirect:/sections";
        } catch (IllegalArgumentException | EntityNotFoundException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("sectionId", id);
            model.addAttribute("pageTitle", "Editar Sección");
            return "sections/edit";
        }
    }

    // ========== Soft Delete ==========

    @PostMapping("/{id}/delete")
    public String delete(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser,
            RedirectAttributes redirectAttributes) {

        try {
            sectionService.softDelete(id, currentUser);
            redirectAttributes.addFlashAttribute("success", "Sección movida a la papelera");
        } catch (EntityNotFoundException e) {
            redirectAttributes.addFlashAttribute("error", "Sección no encontrada");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/sections";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM_ADMIN')")
    @PostMapping("/{id}/restore")
    public String restore(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            sectionService.restore(id);
            redirectAttributes.addFlashAttribute("success", "Sección restaurada exitosamente");
        } catch (EntityNotFoundException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/sections/deleted";
    }

    // ========== Hard Delete ==========

    @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM_ADMIN')")
    @PostMapping("/{id}/hard-delete")
    public String hardDelete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            sectionService.hardDelete(id);
            redirectAttributes.addFlashAttribute("success", "Sección eliminada permanentemente");
        } catch (EntityNotFoundException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/sections/deleted";
    }

    // ========== Gestión de Productos por Sección ==========

    @GetMapping("/{id}/products")
    public String listProducts(@PathVariable Long id, Model model) {
        try {
            StorefrontSection section = sectionService.findById(id);
            model.addAttribute("section", section);
            model.addAttribute("sectionProducts", sectionService.getSectionProducts(id));
            model.addAttribute("availableProducts", sectionService.getAvailableProductsForSection(id));
            model.addAttribute("pageTitle", "Productos de: " + section.getName());
            return "sections/products";
        } catch (EntityNotFoundException e) {
            return "redirect:/sections";
        }
    }

    @PostMapping("/{id}/products/{productId}")
    public String addProduct(
            @PathVariable Long id,
            @PathVariable Long productId,
            RedirectAttributes redirectAttributes) {
        try {
            sectionService.addProductToSection(id, productId);
            redirectAttributes.addFlashAttribute("success", "Producto agregado a la sección");
        } catch (EntityNotFoundException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/sections/" + id + "/products";
    }

    @PostMapping("/{id}/products/{productId}/remove")
    public String removeProduct(
            @PathVariable Long id,
            @PathVariable Long productId,
            RedirectAttributes redirectAttributes) {
        try {
            sectionService.removeProductFromSection(id, productId);
            redirectAttributes.addFlashAttribute("success", "Producto removido de la sección");
        } catch (EntityNotFoundException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/sections/" + id + "/products";
    }

    @PostMapping("/{id}/products/{productId}/update-order")
    public String updateOrder(
            @PathVariable Long id,
            @PathVariable Long productId,
            @RequestParam(required = false) Integer displayOrder,
            RedirectAttributes redirectAttributes) {
        try {
            sectionService.updateProductDisplayOrder(id, productId, displayOrder);
            redirectAttributes.addFlashAttribute("success", "Orden actualizado");
        } catch (EntityNotFoundException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/sections/" + id + "/products";
    }
}
