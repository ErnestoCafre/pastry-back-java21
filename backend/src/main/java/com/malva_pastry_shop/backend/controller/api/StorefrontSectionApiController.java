package com.malva_pastry_shop.backend.controller.api;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.malva_pastry_shop.backend.dto.response.api.ErrorResponse;
import com.malva_pastry_shop.backend.dto.response.api.StorefrontSectionApiDTO;
import com.malva_pastry_shop.backend.service.storefront.StorefrontSectionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/sections")
@Tag(name = "Sections", description = "Secciones de la vitrina")
public class StorefrontSectionApiController {

    private final StorefrontSectionService sectionService;

    public StorefrontSectionApiController(StorefrontSectionService sectionService) {
        this.sectionService = sectionService;
    }

    @GetMapping
    @Operation(summary = "Listar secciones", description = "Obtiene secciones visibles con sus productos ordenados por prioridad")
    @ApiResponse(responseCode = "200", description = "Lista de secciones con productos")
    public ResponseEntity<List<StorefrontSectionApiDTO>> listSections() {
        return ResponseEntity.ok(sectionService.findVisibleSectionsWithProducts());
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Detalle de sección", description = "Obtiene una sección por su slug con sus productos visibles")
    @ApiResponse(responseCode = "200", description = "Sección encontrada")
    @ApiResponse(responseCode = "404", description = "Sección no encontrada",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<StorefrontSectionApiDTO> getSection(
            @Parameter(description = "Slug de la sección", example = "destacados")
            @PathVariable String slug) {
        return ResponseEntity.ok(sectionService.findVisibleSectionBySlug(slug));
    }
}
