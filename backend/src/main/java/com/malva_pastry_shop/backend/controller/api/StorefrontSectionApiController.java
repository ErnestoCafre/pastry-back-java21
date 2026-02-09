package com.malva_pastry_shop.backend.controller.api;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.malva_pastry_shop.backend.dto.response.api.StorefrontSectionApiDTO;
import com.malva_pastry_shop.backend.service.storefront.StorefrontSectionService;

import io.swagger.v3.oas.annotations.Operation;
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
    @Operation(summary = "Listar secciones", description = "Obtiene secciones visibles con sus productos ordenados")
    public ResponseEntity<List<StorefrontSectionApiDTO>> listSections() {
        return ResponseEntity.ok(sectionService.findVisibleSectionsWithProducts());
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Detalle de sección", description = "Obtiene una sección por slug con sus productos")
    public ResponseEntity<StorefrontSectionApiDTO> getSection(@PathVariable String slug) {
        return ResponseEntity.ok(sectionService.findVisibleSectionBySlug(slug));
    }
}
