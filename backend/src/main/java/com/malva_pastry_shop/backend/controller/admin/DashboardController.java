package com.malva_pastry_shop.backend.controller.admin;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.malva_pastry_shop.backend.domain.auth.User;
import com.malva_pastry_shop.backend.service.inventory.IngredientService;
import com.malva_pastry_shop.backend.service.sales.SaleService;
import com.malva_pastry_shop.backend.service.inventory.CategoryService;
import com.malva_pastry_shop.backend.service.inventory.ProductService;

import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final IngredientService ingredientService;
    private final SaleService saleService;

    public DashboardController(ProductService productService,
                               CategoryService categoryService,
                               IngredientService ingredientService,
                               SaleService saleService) {
        this.productService = productService;
        this.categoryService = categoryService;
        this.ingredientService = ingredientService;
        this.saleService = saleService;
    }

    @GetMapping("/")
    public String index() {
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal User user, Model model) {
        model.addAttribute("user", user);
        model.addAttribute("pageTitle", "Dashboard");

        // Contadores de entidades (reutilizando servicios existentes)
        model.addAttribute("totalProductos", productService.findAllActive(Pageable.unpaged()).getTotalElements());
        model.addAttribute("totalCategorias", categoryService.findAllActive(Pageable.unpaged()).getTotalElements());
        model.addAttribute("totalIngredientes", ingredientService.findAllActive(Pageable.unpaged()).getTotalElements());

        // Ventas e ingresos del dia
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);
        model.addAttribute("ventasHoy", saleService.countSalesInRange(startOfDay, endOfDay));
        model.addAttribute("ingresoHoy", saleService.totalRevenueInRange(startOfDay, endOfDay));

        // Ingresos del mes
        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        model.addAttribute("ingresoMes", saleService.totalRevenueInRange(startOfMonth, endOfDay));

        return "dashboard/index";
    }
}
