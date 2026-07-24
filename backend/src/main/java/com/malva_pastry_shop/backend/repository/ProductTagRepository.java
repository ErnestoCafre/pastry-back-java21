package com.malva_pastry_shop.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.malva_pastry_shop.backend.domain.storefront.ProductTag;

@Repository
public interface ProductTagRepository extends JpaRepository<ProductTag, Long> {

    // Tags de un producto (con fetch de tag para evitar N+1)
    @EntityGraph(attributePaths = {"tag"})
    List<ProductTag> findByProductId(Long productId);

    // Productos de un tag (con fetch de product para evitar N+1)
    @EntityGraph(attributePaths = {"product"})
    List<ProductTag> findByTagId(Long tagId);

    boolean existsByProductIdAndTagId(Long productId, Long tagId);

    // Productos asociados a un tag (para validar el borrado permanente)
    long countByTagId(Long tagId);

    @EntityGraph(attributePaths = {"tag"})
    Optional<ProductTag> findByProductIdAndTagId(Long productId, Long tagId);

    void deleteByProductId(Long productId);
}
