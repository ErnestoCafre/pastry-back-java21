package com.malva_pastry_shop.backend.dto.response.api;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Sección de la vitrina con sus productos")
public record StorefrontSectionApiDTO(
        @Schema(description = "ID de la sección", example = "1")
        Long id,
        @Schema(description = "Nombre de la sección", example = "Destacados")
        String name,
        @Schema(description = "Slug URL-friendly", example = "destacados")
        String slug,
        @Schema(description = "Descripción de la sección", example = "Nuestros productos más populares")
        String description,
        List<ProductApiDTO.Simple> products) {

    @Schema(description = "Versión simplificada de una sección sin productos")
    public record Simple(
            @Schema(description = "ID de la sección", example = "1")
            Long id,
            @Schema(description = "Nombre de la sección", example = "Destacados")
            String name,
            @Schema(description = "Slug URL-friendly", example = "destacados")
            String slug,
            @Schema(description = "Descripción de la sección", example = "Nuestros productos más populares")
            String description) {
    }
}
