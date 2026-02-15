package com.malva_pastry_shop.backend.dto.response.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Respuesta de error de la API")
public record ErrorResponse(
        @Schema(description = "Mensaje descriptivo del error", example = "Producto no encontrado con ID: 99")
        String error) {
}
