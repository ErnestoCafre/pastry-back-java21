package com.malva_pastry_shop.backend.dto.response.api;

import java.math.BigDecimal;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Detalle completo de un producto")
public record ProductApiDTO(
        @Schema(description = "ID del producto", example = "1")
        Long id,
        @Schema(description = "Nombre del producto", example = "Torta de Chocolate")
        String name,
        @Schema(description = "Descripción del producto", example = "Torta artesanal con cobertura de ganache")
        String description,
        @Schema(description = "Precio base", example = "25.50")
        BigDecimal basePrice,
        @Schema(description = "Días de preparación", example = "2")
        Integer preparationDays,
        @Schema(description = "URL de la imagen del producto", example = "https://example.com/images/torta-chocolate.jpg")
        String imageUrl,
        CategoryApiDTO category,
        List<TagApiDTO> tags) {

    @Schema(description = "Versión simplificada de un producto para listados")
    public record Simple(
            @Schema(description = "ID del producto", example = "1")
            Long id,
            @Schema(description = "Nombre del producto", example = "Torta de Chocolate")
            String name,
            @Schema(description = "Precio base", example = "25.50")
            BigDecimal basePrice,
            @Schema(description = "URL de la imagen", example = "https://example.com/images/torta-chocolate.jpg")
            String imageUrl,
            @Schema(description = "Nombre de la categoría", example = "Tortas")
            String categoryName) {
    }
}
