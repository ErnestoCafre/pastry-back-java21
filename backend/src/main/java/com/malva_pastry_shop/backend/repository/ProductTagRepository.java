package com.malva_pastry_shop.backend.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.malva_pastry_shop.backend.domain.storefront.ProductTag;

@Repository
public interface ProductTagRepository extends JpaRepository<ProductTag, Long> {

    // Tags de un producto (con fetch de tag para evitar N+1)
    @EntityGraph(attributePaths = {"tag"})
    List<ProductTag> findByProductId(Long productId);

    // Productos de un tag. Trae tambien product.category: la vista
    // tags/products.html la muestra en cada fila y, con open-in-view=false, sin
    // este fetch salta LazyInitializationException a mitad del render. Como la
    // respuesta ya se empezo a enviar, el resultado era una pagina cortada con
    // HTTP 200 en vez de un error. Mismo criterio que
    // StorefrontSectionProductRepository.
    @EntityGraph(attributePaths = {"product", "product.category"})
    List<ProductTag> findByTagId(Long tagId);

    boolean existsByProductIdAndTagId(Long productId, Long tagId);

    // Productos asociados a un tag (para validar el borrado permanente)
    long countByTagId(Long tagId);

    // Productos activos por tag, para TODOS los tags de una pagina de una sola
    // vez. Reemplaza un bucle que por cada tag traia sus ProductTag con
    // product y product.category en el EntityGraph y los contaba en memoria:
    // 50 filas eran 50 consultas hidratando entidades completas para devolver
    // un numero. Los tags sin productos NO salen en el resultado.
    @Query("""
            select pt.tag.id as id, count(pt) as total
            from ProductTag pt
            where pt.tag.id in :tagIds and pt.product.deletedAt is null
            group by pt.tag.id
            """)
    List<IdCount> countActiveProductsByTagIds(@Param("tagIds") Collection<Long> tagIds);

    @EntityGraph(attributePaths = {"tag"})
    Optional<ProductTag> findByProductIdAndTagId(Long productId, Long tagId);

    void deleteByProductId(Long productId);
}
