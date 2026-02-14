package com.malva_pastry_shop.backend.repository;

import com.malva_pastry_shop.backend.domain.inventory.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // Productos activos (no eliminados)
    @EntityGraph(attributePaths = { "category" })
    Page<Product> findByDeletedAtIsNull(Pageable pageable);

    // Productos activos por categoria
    @EntityGraph(attributePaths = { "category" })
    Page<Product> findByCategoryIdAndDeletedAtIsNull(Long categoryId, Pageable pageable);

    // Busqueda por nombre (solo activos)
    @EntityGraph(attributePaths = { "category" })
    Page<Product> findByNameContainingIgnoreCaseAndDeletedAtIsNull(String name, Pageable pageable);

    // Producto activo por ID (incluye category y createdBy para vista de detalle)
    @EntityGraph(attributePaths = { "category", "createdBy" })
    Optional<Product> findByIdAndDeletedAtIsNull(Long id);

    // Contar productos por categoria
    long countByCategoryId(Long categoryId);

    // Contar productos activos por categoria
    long countByCategoryIdAndDeletedAtIsNull(Long categoryId);

    // Validacion de nombre unico (case-insensitive)
    Optional<Product> findByNameIgnoreCase(String name);

    // Productos eliminados (papelera)
    @EntityGraph(attributePaths = { "category" })
    Page<Product> findByDeletedAtIsNotNull(Pageable pageable);

    // ========== Queries para API pública (vitrina) ==========

    // Productos visibles
    @EntityGraph(attributePaths = { "category" })
    Page<Product> findByVisibleTrueAndDeletedAtIsNull(Pageable pageable);

    // Productos visibles por categoría
    @EntityGraph(attributePaths = { "category" })
    Page<Product> findByVisibleTrueAndCategoryIdAndDeletedAtIsNull(Long categoryId, Pageable pageable);

    // Búsqueda por nombre en productos visibles
    @EntityGraph(attributePaths = { "category" })
    Page<Product> findByVisibleTrueAndNameContainingIgnoreCaseAndDeletedAtIsNull(String name, Pageable pageable);

    // Producto visible por ID
    @EntityGraph(attributePaths = { "category" })
    Optional<Product> findByIdAndVisibleTrueAndDeletedAtIsNull(Long id);
}
