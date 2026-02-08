package com.malva_pastry_shop.backend.repository;

import com.malva_pastry_shop.backend.domain.storefront.StorefrontSection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    Page<StorefrontSection> findByDeletedAtIsNotNull(Pageable pageable);

    // ========== Validacion de nombre unico (case-insensitive) ==========

    Optional<StorefrontSection> findByNameIgnoreCase(String name);
}
