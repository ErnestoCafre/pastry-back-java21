package com.malva_pastry_shop.backend.controller.api;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.malva_pastry_shop.backend.dto.response.api.TagApiDTO;
import com.malva_pastry_shop.backend.service.storefront.TagService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * API REST pública para consultar tags.
 * Todos los endpoints son de solo lectura y no requieren autenticación.
 */
@RestController
@RequestMapping("/api/v1/tags")
@Tag(name = "Tags", description = "Tags de productos")
public class TagApiController {

    private final TagService tagService;

    public TagApiController(TagService tagService) {
        this.tagService = tagService;
    }

    /**
     * Lista todos los tags activos.
     * No usa paginación ya que normalmente hay pocos tags.
     */
    @GetMapping
    @Operation(summary = "Listar tags", description = "Obtiene todos los tags activos ordenados por nombre")
    public ResponseEntity<List<TagApiDTO>> listTags() {
        List<TagApiDTO> tags = tagService.findAllForSelect().stream()
                .map(t -> new TagApiDTO(t.getId(), t.getName(), t.getSlug(), t.getDescription()))
                .toList();

        return ResponseEntity.ok(tags);
    }
}
