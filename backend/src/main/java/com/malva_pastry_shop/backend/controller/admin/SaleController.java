package com.malva_pastry_shop.backend.controller.admin;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
import com.malva_pastry_shop.backend.domain.sales.Sale;
import com.malva_pastry_shop.backend.domain.inventory.Product;
import com.malva_pastry_shop.backend.dto.request.SaleRequest;
import com.malva_pastry_shop.backend.service.sales.SaleService;
import com.malva_pastry_shop.backend.service.storefront.ProductService;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/sales")
public class SaleController {

    private final SaleService saleService;
    private final ProductService productService;

    public SaleController(SaleService saleService, ProductService productService) {
        this.saleService = saleService;
        this.productService = productService;
    }

    // ========== Listados ==========

    @GetMapping
    public String list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            Model model) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("saleDate").descending());
        Page<Sale> sales;
        BigDecimal totalSalesAmount;

        boolean hasSearch = search != null && !search.isBlank();
        boolean hasDates = startDate != null && endDate != null;

        if (hasSearch && hasDates) {
            // Filtro combinado: busqueda + rango de fechas
            LocalDateTime start = startDate.atStartOfDay();
            LocalDateTime end = endDate.atTime(LocalTime.MAX);
            sales = saleService.findByProductNameAndDateRange(search, start, end, pageable);
            totalSalesAmount = saleService.sumTotalAmountByProductNameAndDateRange(search, start, end);
            model.addAttribute("search", search);
            model.addAttribute("startDate", startDate);
            model.addAttribute("endDate", endDate);
        } else if (hasDates) {
            // Filtrar solo por rango de fechas
            LocalDateTime start = startDate.atStartOfDay();
            LocalDateTime end = endDate.atTime(LocalTime.MAX);
            sales = saleService.findByDateRange(start, end, pageable);
            totalSalesAmount = saleService.sumTotalAmountByDateRange(start, end);
            model.addAttribute("startDate", startDate);
            model.addAttribute("endDate", endDate);
        } else if (hasSearch) {
            // Buscar solo por nombre de producto
            sales = saleService.search(search, pageable);
            totalSalesAmount = saleService.sumTotalAmountByProductName(search);
            model.addAttribute("search", search);
        } else {
            sales = saleService.findAll(pageable);
            totalSalesAmount = saleService.sumTotalAmount();
        }

        model.addAttribute("sales", sales);
        model.addAttribute("totalSalesAmount", totalSalesAmount);
        model.addAttribute("pageTitle", "Ventas");
        return "sales/list";
    }

    // ========== Detalle ==========

    @GetMapping("/{id}")
    public String show(@PathVariable Long id, Model model) {
        Sale sale = saleService.findByIdWithDetails(id);
        var ingredients = saleService.getSaleIngredients(id);
        BigDecimal totalIngredientCost = saleService.calculateTotalIngredientCost(id);
        BigDecimal profitMargin = sale.getTotalAmount().subtract(totalIngredientCost);

        model.addAttribute("sale", sale);
        model.addAttribute("ingredients", ingredients);
        model.addAttribute("totalIngredientCost", totalIngredientCost);
        model.addAttribute("profitMargin", profitMargin);
        model.addAttribute("pageTitle", "Venta #" + sale.getId());
        return "sales/show";
    }

    // ========== CRUD ==========

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        // Obtener productos activos para el dropdown
        Pageable pageable = PageRequest.of(0, 1000, Sort.by("name").ascending());
        Page<Product> products = productService.findAllActive(pageable);

        model.addAttribute("sale", new SaleRequest());
        model.addAttribute("products", products.getContent());
        model.addAttribute("pageTitle", "Nueva Venta");
        return "sales/create";
    }

    @PostMapping
    public String create(
            @Valid @ModelAttribute("sale") SaleRequest request,
            BindingResult result,
            @AuthenticationPrincipal User currentUser,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            Pageable pageable = PageRequest.of(0, 1000, Sort.by("name").ascending());
            Page<Product> products = productService.findAllActive(pageable);
            model.addAttribute("products", products.getContent());
            model.addAttribute("pageTitle", "Nueva Venta");
            return "sales/create";
        }

        try {
            Sale sale = saleService.create(request, currentUser);
            redirectAttributes.addFlashAttribute("success", "Venta registrada exitosamente");
            return "redirect:/sales/" + sale.getId();
        } catch (EntityNotFoundException e) {
            model.addAttribute("error", "Producto no encontrado");
            Pageable pageable = PageRequest.of(0, 1000, Sort.by("name").ascending());
            Page<Product> products = productService.findAllActive(pageable);
            model.addAttribute("products", products.getContent());
            model.addAttribute("pageTitle", "Nueva Venta");
            return "sales/create";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            Pageable pageable = PageRequest.of(0, 1000, Sort.by("name").ascending());
            Page<Product> products = productService.findAllActive(pageable);
            model.addAttribute("products", products.getContent());
            model.addAttribute("pageTitle", "Nueva Venta");
            return "sales/create";
        }
    }
}
