package com.malva_pastry_shop.backend.repository;

import com.malva_pastry_shop.backend.domain.inventory.Ingredient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IngredientRepository extends JpaRepository<Ingredient, Long> {

    // ========== Consultas activos (no eliminados) ==========

    Page<Ingredient> findByDeletedAtIsNull(Pageable pageable);

    Page<Ingredient> findByNameContainingIgnoreCaseAndDeletedAtIsNull(String name, Pageable pageable);

    Optional<Ingredient> findByIdAndDeletedAtIsNull(Long id);

    List<Ingredient> findByDeletedAtIsNullOrderByNameAsc();

    // ========== Consultas papelera (eliminados) ==========

    // deletedBy es obligatorio en el fetch: la vista muestra quien elimino y con
    // open-in-view=false un proxy lazy romperia el render de la plantilla.
    @EntityGraph(attributePaths = { "deletedBy" })
    Page<Ingredient> findByDeletedAtIsNotNull(Pageable pageable);

    // ========== Validacion de nombre unico (case-insensitive) ==========

    Optional<Ingredient> findByNameIgnoreCase(String name);
}
