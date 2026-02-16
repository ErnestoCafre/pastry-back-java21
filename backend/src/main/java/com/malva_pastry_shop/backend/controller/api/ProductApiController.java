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

import com.malva_pastry_shop.backend.domain.inventory.Product;
import com.malva_pastry_shop.backend.domain.storefront.Tag;
import com.malva_pastry_shop.backend.dto.response.api.ApiPageResponse;
import com.malva_pastry_shop.backend.dto.response.api.CategoryApiDTO;
import com.malva_pastry_shop.backend.dto.response.api.ErrorResponse;
import com.malva_pastry_shop.backend.dto.response.api.ProductApiDTO;
import com.malva_pastry_shop.backend.dto.response.api.TagApiDTO;
import com.malva_pastry_shop.backend.service.inventory.ProductService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

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

        @GetMapping
        @Operation(summary = "Listar productos", description = "Obtiene productos visibles con paginación y filtros opcionales por nombre y/o categoría")
        @ApiResponse(responseCode = "200", description = "Lista paginada de productos")
        public ResponseEntity<ApiPageResponse<ProductApiDTO.Simple>> listProducts(
                        @Parameter(description = "Filtrar por nombre (búsqueda parcial, case-insensitive)", example = "chocolate")
                        @RequestParam(required = false) String name,
                        @Parameter(description = "Filtrar por ID de categoría", example = "1")
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

        @GetMapping("/{id}")
        @Operation(summary = "Detalle de producto", description = "Obtiene un producto visible con su categoría y tags")
        @ApiResponse(responseCode = "200", description = "Producto encontrado")
        @ApiResponse(responseCode = "404", description = "Producto no encontrado",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
        public ResponseEntity<ProductApiDTO> getProduct(
                        @Parameter(description = "ID del producto", example = "1")
                        @PathVariable Long id) {
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
                                                .map(t -> new TagApiDTO(t.getId(), t.getName(), t.getSlug(),
                                                                t.getDescription()))
                                                .toList());

                return ResponseEntity.ok(dto);
        }
}
