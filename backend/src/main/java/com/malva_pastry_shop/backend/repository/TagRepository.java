package com.malva_pastry_shop.backend.repository;

import com.malva_pastry_shop.backend.domain.storefront.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {

    // ========== Consultas activos (no eliminados) ==========

    Page<Tag> findByDeletedAtIsNull(Pageable pageable);

    Page<Tag> findByNameContainingIgnoreCaseAndDeletedAtIsNull(String name, Pageable pageable);

    Optional<Tag> findByIdAndDeletedAtIsNull(Long id);

    List<Tag> findByDeletedAtIsNullOrderByNameAsc();

    // ========== Consultas papelera (eliminados) ==========

    // deletedBy es obligatorio en el fetch: la vista muestra quien elimino y con
    // open-in-view=false un proxy lazy romperia el render de la plantilla.
    @EntityGraph(attributePaths = { "deletedBy" })
    Page<Tag> findByDeletedAtIsNotNull(Pageable pageable);

    // ========== Validacion de nombre unico (case-insensitive) ==========

    Optional<Tag> findByNameIgnoreCase(String name);

    // ========== Busqueda por IDs ==========

    List<Tag> findByIdInAndDeletedAtIsNull(List<Long> ids);
}
