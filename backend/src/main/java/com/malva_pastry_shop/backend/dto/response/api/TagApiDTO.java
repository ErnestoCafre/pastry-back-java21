package com.malva_pastry_shop.backend.dto.response.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Tag de producto")
public record TagApiDTO(
        @Schema(description = "ID del tag", example = "1")
        Long id,
        @Schema(description = "Nombre del tag", example = "Sin Gluten")
        String name,
        @Schema(description = "Slug URL-friendly", example = "sin-gluten")
        String slug,
        @Schema(description = "Descripción del tag", example = "Productos aptos para celíacos")
        String description) {
}
