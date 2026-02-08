package com.malva_pastry_shop.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.malva_pastry_shop.backend.domain.storefront.StorefrontSectionProduct;

@Repository
public interface StorefrontSectionProductRepository extends JpaRepository<StorefrontSectionProduct, Long> {

    // Admin: productos de una sección con fetch de product y category
    @EntityGraph(attributePaths = { "product", "product.category" })
    List<StorefrontSectionProduct> findByStorefrontSectionIdOrderByDisplayOrderAscProductNameAsc(Long storefrontSectionId);

    // API pública: productos visibles de una sección
    @EntityGraph(attributePaths = { "product", "product.category" })
    List<StorefrontSectionProduct> findByStorefrontSectionIdAndProductVisibleTrueAndProductDeletedAtIsNullOrderByDisplayOrderAsc(Long storefrontSectionId);

    List<StorefrontSectionProduct> findByProductId(Long productId);

    boolean existsByStorefrontSectionIdAndProductId(Long storefrontSectionId, Long productId);

    Optional<StorefrontSectionProduct> findByStorefrontSectionIdAndProductId(Long storefrontSectionId, Long productId);

    long countByStorefrontSectionIdAndProductDeletedAtIsNull(Long storefrontSectionId);
}
