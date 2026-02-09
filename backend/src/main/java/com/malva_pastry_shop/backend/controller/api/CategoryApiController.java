package com.malva_pastry_shop.backend.controller.api;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.malva_pastry_shop.backend.domain.inventory.Category;
import com.malva_pastry_shop.backend.dto.response.api.ApiPageResponse;
import com.malva_pastry_shop.backend.dto.response.api.CategoryApiDTO;
import com.malva_pastry_shop.backend.service.inventory.CategoryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * API REST pública para consultar categorías.
 * Todos los endpoints son de solo lectura y no requieren autenticación.
 */
@RestController
@RequestMapping("/api/v1/categories")
@Tag(name = "Categories", description = "Categorías de productos")
public class CategoryApiController {

    private final CategoryService categoryService;

    public CategoryApiController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /**
     * Lista categorías activas con paginación.
     */
    @GetMapping
    @Operation(summary = "Listar categorías", description = "Obtiene categorías activas con paginación")
    public ResponseEntity<ApiPageResponse<CategoryApiDTO>> listCategories(
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {

        Page<CategoryApiDTO> page = categoryService.findAllActive(pageable)
                .map(c -> new CategoryApiDTO(c.getId(), c.getName(), c.getDescription()));

        return ResponseEntity.ok(ApiPageResponse.from(page));
    }

    /**
     * Obtiene el detalle de una categoría por ID.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Detalle de categoría", description = "Obtiene una categoría por su ID")
    public ResponseEntity<CategoryApiDTO> getCategory(@PathVariable Long id) {
        Category category = categoryService.findById(id);
        return ResponseEntity.ok(new CategoryApiDTO(
                category.getId(),
                category.getName(),
                category.getDescription()));
    }
}
