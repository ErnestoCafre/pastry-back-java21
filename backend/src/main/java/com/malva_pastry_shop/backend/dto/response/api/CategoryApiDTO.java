package com.malva_pastry_shop.backend.dto.response.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Categoría de productos")
public record CategoryApiDTO(
        @Schema(description = "ID de la categoría", example = "1")
        Long id,
        @Schema(description = "Nombre de la categoría", example = "Tortas")
        String name,
        @Schema(description = "Descripción de la categoría", example = "Tortas artesanales para toda ocasión")
        String description) {
}
