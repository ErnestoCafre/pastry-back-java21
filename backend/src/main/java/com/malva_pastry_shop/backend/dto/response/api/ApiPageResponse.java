package com.malva_pastry_shop.backend.dto.response.api;

import java.util.List;

import org.springframework.data.domain.Page;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Respuesta paginada genérica")
public record ApiPageResponse<T>(
        @Schema(description = "Lista de elementos de la página actual")
        List<T> content,
        @Schema(description = "Número de página (base 0)", example = "0")
        int page,
        @Schema(description = "Cantidad de elementos por página", example = "12")
        int size,
        @Schema(description = "Total de elementos en todas las páginas", example = "45")
        long totalElements,
        @Schema(description = "Total de páginas disponibles", example = "4")
        int totalPages,
        @Schema(description = "Indica si es la primera página", example = "true")
        boolean first,
        @Schema(description = "Indica si es la última página", example = "false")
        boolean last) {

    public static <T> ApiPageResponse<T> from(Page<T> page) {
        return new ApiPageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast());
    }
}
