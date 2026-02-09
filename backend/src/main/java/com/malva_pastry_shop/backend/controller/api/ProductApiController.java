package com.malva_pastry_shop.backend.controller.api;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.malva_pastry_shop.backend.domain.storefront.Product;
import com.malva_pastry_shop.backend.domain.storefront.Tag;
import com.malva_pastry_shop.backend.dto.response.api.ApiPageResponse;
import com.malva_pastry_shop.backend.dto.response.api.CategoryApiDTO;
import com.malva_pastry_shop.backend.dto.response.api.ProductApiDTO;
import com.malva_pastry_shop.backend.dto.response.api.TagApiDTO;
import com.malva_pastry_shop.backend.service.storefront.ProductService;

import io.swagger.v3.oas.annotations.Operation;

/**
 * API REST pública para consultar productos visibles en la vitrina.
 * Solo muestra productos con visible=true, ordenados por displayOrder.
 */
@RestController
@RequestMapping("/api/v1/products")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Products", description = "Catálogo de productos")
public class ProductApiController {

    private final ProductService productService;

    public ProductApiController(ProductService productService) {
        this.productService = productService;
    }

    /**
     * Lista productos visibles con paginación.
     * Ordenados por displayOrder (nulls last) y luego por nombre.
     */
    @GetMapping
    @Operation(summary = "Listar productos", description = "Obtiene productos visibles ordenados por prioridad")
    public ResponseEntity<ApiPageResponse<ProductApiDTO.Simple>> listProducts(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long categoryId,
            @PageableDefault(size = 12, sort = "name") Pageable pageable) {

        Page<Product> products = productService.findVisibleProducts(name, categoryId, pageable);

        Page<ProductApiDTO.Simple> dtoPage = products.map(p -> new ProductApiDTO.Simple(
                p.getId(),
                p.getName(),
                p.getBasePrice(),
                p.getImageUrl(),
                p.getCategory() != null ? p.getCategory().getName() : null));

        return ResponseEntity.ok(ApiPageResponse.from(dtoPage));
    }

    /**
     * Obtiene el detalle de un producto visible por ID.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Detalle de producto", description = "Obtiene un producto visible con su categoría y tags")
    public ResponseEntity<ProductApiDTO> getProduct(@PathVariable Long id) {
        Product product = productService.findVisibleById(id);
        List<Tag> tags = productService.getProductTags(id);

        ProductApiDTO dto = new ProductApiDTO(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getBasePrice(),
                product.getPreparationDays(),
                product.getImageUrl(),
                product.getCategory() != null
                        ? new CategoryApiDTO(
                                product.getCategory().getId(),
                                product.getCategory().getName(),
                                product.getCategory().getDescription())
                        : null,
                tags.stream()
                        .map(t -> new TagApiDTO(t.getId(), t.getName(), t.getSlug(), t.getDescription()))
                        .toList());

        return ResponseEntity.ok(dto);
    }
}
