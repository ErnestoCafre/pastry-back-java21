package com.malva_pastry_shop.backend.repository;

import com.malva_pastry_shop.backend.domain.storefront.StorefrontSection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StorefrontSectionRepository extends JpaRepository<StorefrontSection, Long> {

    // ========== Consultas activas (no eliminadas) ==========

    Page<StorefrontSection> findByDeletedAtIsNull(Pageable pageable);

    Optional<StorefrontSection> findByIdAndDeletedAtIsNull(Long id);

    Optional<StorefrontSection> findBySlugAndDeletedAtIsNull(String slug);

    Page<StorefrontSection> findByNameContainingIgnoreCaseAndDeletedAtIsNull(String name, Pageable pageable);

    List<StorefrontSection> findByVisibleTrueAndDeletedAtIsNullOrderByDisplayOrderAscNameAsc();

    List<StorefrontSection> findByDeletedAtIsNullOrderByNameAsc();

    // ========== Consultas papelera (eliminadas) ==========

    // deletedBy es obligatorio en el fetch: la vista muestra quien elimino y con
    // open-in-view=false un proxy lazy romperia el render de la plantilla.
    @EntityGraph(attributePaths = { "deletedBy" })
    Page<StorefrontSection> findByDeletedAtIsNotNull(Pageable pageable);

    // ========== Validacion de nombre unico (case-insensitive) ==========

    Optional<StorefrontSection> findByNameIgnoreCase(String name);
}
