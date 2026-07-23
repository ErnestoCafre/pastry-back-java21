package com.malva_pastry_shop.backend.service.storefront;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.malva_pastry_shop.backend.domain.auth.User;
import com.malva_pastry_shop.backend.domain.storefront.Tag;
import com.malva_pastry_shop.backend.dto.request.TagRequest;
import com.malva_pastry_shop.backend.repository.ProductTagRepository;
import com.malva_pastry_shop.backend.repository.TagRepository;
import com.malva_pastry_shop.backend.util.SlugUtil;

import jakarta.persistence.EntityNotFoundException;

@Service
public class TagService {

    private final TagRepository tagRepository;
    private final ProductTagRepository productTagRepository;

    public TagService(TagRepository tagRepository, ProductTagRepository productTagRepository) {
        this.tagRepository = tagRepository;
        this.productTagRepository = productTagRepository;
    }

    // ========== Consultas ==========

    public Page<Tag> findAllActive(Pageable pageable) {
        return tagRepository.findByDeletedAtIsNull(pageable);
    }

    public Page<Tag> search(String name, Pageable pageable) {
        return tagRepository.findByNameContainingIgnoreCaseAndDeletedAtIsNull(name, pageable);
    }

    public Tag findById(Long id) {
        return tagRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Tag no encontrado con ID: " + id));
    }

    public List<Tag> findAllForSelect() {
        return tagRepository.findByDeletedAtIsNullOrderByNameAsc();
    }

    public Page<Tag> findDeleted(Pageable pageable) {
        return tagRepository.findByDeletedAtIsNotNull(pageable);
    }

    // ========== CRUD ==========

    @Transactional
    public Tag create(TagRequest request) {
        validateTagName(request.getName(), null);

        Tag tag = new Tag();
        tag.setName(request.getName());
        tag.setSlug(SlugUtil.generateSlug(request.getName()));
        tag.setDescription(request.getDescription());

        return tagRepository.save(tag);
    }

    @Transactional
    public Tag update(Long id, TagRequest request) {
        Tag tag = findById(id);
        validateTagName(request.getName(), id);

        tag.setName(request.getName());
        tag.setSlug(SlugUtil.generateSlug(request.getName()));
        tag.setDescription(request.getDescription());

        return tagRepository.save(tag);
    }

    // ========== Soft Delete ==========

    @Transactional
    public void softDelete(Long id, User deletedBy) {
        Tag tag = findById(id);
        tag.softDelete(deletedBy);
        tagRepository.save(tag);
    }

    @Transactional
    public Tag restore(Long id) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Tag no encontrado"));

        if (tag.getDeletedAt() == null) {
            throw new IllegalStateException("El tag no está eliminado");
        }

        // Verificar que no exista otro tag activo con el mismo nombre (case-insensitive)
        tagRepository.findByNameIgnoreCase(tag.getName()).ifPresent(existing -> {
            if (!existing.getId().equals(id) && !existing.isDeleted()) {
                throw new IllegalStateException("Ya existe un tag activo con el nombre: " + tag.getName());
            }
        });

        tag.restore();
        return tagRepository.save(tag);
    }

    // ========== Hard Delete ==========

    @Transactional
    public void hardDelete(Long id) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Tag no encontrado"));

        if (tag.getDeletedAt() == null) {
            throw new IllegalStateException("Solo se pueden eliminar permanentemente los tags que están en la papelera");
        }

        // product_tags no se limpia en cascada desde Tag, y su FK rechaza el
        // borrado: sin esta guarda la base de datos lanzaria
        // DataIntegrityViolationException (error 500 en el panel).
        long usageCount = productTagRepository.countByTagId(id);
        if (usageCount > 0) {
            throw new IllegalStateException(
                    "No se puede eliminar permanentemente el tag porque está asociado a " + usageCount
                            + " producto(s)");
        }

        tagRepository.delete(tag);
    }

    // ========== Validaciones ==========

    /**
     * Valida que el nombre del tag sea único (case-insensitive).
     * Verifica tanto tags activos como en papelera.
     *
     * @param name      Nombre a validar
     * @param excludeId ID del tag a excluir (para updates), null para creates
     */
    private void validateTagName(String name, Long excludeId) {
        tagRepository.findByNameIgnoreCase(name).ifPresent(existingTag -> {
            // Si es update y es el mismo tag, no hay conflicto
            if (excludeId != null && existingTag.getId().equals(excludeId)) {
                return;
            }

            if (existingTag.isDeleted()) {
                throw new IllegalArgumentException(
                        "Ya existe un tag con el nombre '" + name + "' en la papelera. " +
                                "Puedes restaurarlo o eliminarlo permanentemente antes de crear uno nuevo.");
            } else {
                throw new IllegalArgumentException("Ya existe un tag con el nombre: " + name);
            }
        });
    }
}
