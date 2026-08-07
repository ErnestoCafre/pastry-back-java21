package com.malva_pastry_shop.backend.service.inventory;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.malva_pastry_shop.backend.domain.auth.User;
import com.malva_pastry_shop.backend.domain.inventory.Category;
import com.malva_pastry_shop.backend.dto.request.CategoryRequest;
import com.malva_pastry_shop.backend.repository.CategoryRepository;
import com.malva_pastry_shop.backend.repository.IdCount;
import com.malva_pastry_shop.backend.repository.ProductRepository;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public CategoryService(CategoryRepository categoryRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    // ========== Consultas ==========

    public Page<Category> findAllActive(Pageable pageable) {
        return categoryRepository.findByDeletedAtIsNull(pageable);
    }

    public Page<Category> search(String name, Pageable pageable) {
        return categoryRepository.findByNameContainingIgnoreCaseAndDeletedAtIsNull(name, pageable);
    }

    public Category findById(Long id) {
        return categoryRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Categoría no encontrada con ID: " + id));
    }

    public Page<Category> findDeleted(Pageable pageable) {
        return categoryRepository.findByDeletedAtIsNotNull(pageable);
    }

    // ========== CRUD ==========

    @Transactional
    public Category create(CategoryRequest request) {
        validateCategoryName(request.getName(), null);

        Category category = new Category();
        category.setName(request.getName());
        category.setDescription(request.getDescription());

        return categoryRepository.save(category);
    }

    @Transactional
    public Category update(Long id, CategoryRequest request) {
        Category category = findById(id);
        validateCategoryName(request.getName(), id);

        category.setName(request.getName());
        category.setDescription(request.getDescription());

        return categoryRepository.save(category);
    }

    // ========== Soft Delete ==========

    @Transactional
    public void softDelete(Long id, User deletedBy) {
        Category category = findById(id);

        long productCount = productRepository.countByCategoryIdAndDeletedAtIsNull(id);
        if (productCount > 0) {
            throw new IllegalStateException(
                    "No se puede eliminar la categoría porque tiene " + productCount
                            + " producto(s) activo(s) asociado(s)");
        }

        category.softDelete(deletedBy);
        categoryRepository.save(category);
    }

    @Transactional
    public Category restore(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Categoría no encontrada"));

        if (category.getDeletedAt() == null) {
            throw new IllegalStateException("La categoría no está eliminada");
        }

        // Verificar que no exista otra categoría activa con el mismo nombre
        // (case-insensitive)
        categoryRepository.findByNameIgnoreCase(category.getName()).ifPresent(existing -> {
            if (!existing.getId().equals(id) && !existing.isDeleted()) {
                throw new IllegalStateException("Ya existe una categoría activa con el nombre: " + category.getName());
            }
        });

        category.restore();
        return categoryRepository.save(category);
    }

    // ========== Hard Delete ==========

    @Transactional
    public void hardDelete(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Categoría no encontrada"));

        // Solo se puede hacer hard delete si está en papelera
        if (category.getDeletedAt() == null) {
            throw new IllegalStateException(
                    "Solo se pueden eliminar permanentemente las categorías que están en la papelera");
        }

        // Verificar que no tenga productos (ni activos ni eliminados)
        long totalProducts = productRepository.countByCategoryId(id);
        if (totalProducts > 0) {
            throw new IllegalStateException(
                    "No se puede eliminar permanentemente la categoría porque tiene " + totalProducts
                            + " producto(s) asociado(s)");
        }

        categoryRepository.delete(category);
    }

    // ========== Utilidades ==========

    public long countProducts(Long categoryId) {
        return productRepository.countByCategoryIdAndDeletedAtIsNull(categoryId);
    }

    /**
     * Lo mismo para varias categorías a la vez, en una sola consulta.
     *
     * <p>El listado lo necesita por fila. Llamarlo dentro del bucle que arma el
     * modelo convierte una página de 50 filas en 51 consultas, que es lo que
     * hacía el listado de tags antes de existir esta variante.
     *
     * <p>Una categoría sin productos <b>no aparece</b> en el mapa: un
     * {@code GROUP BY} no devuelve filas para los grupos vacíos. La plantilla
     * trata la ausencia como cero.
     */
    @Transactional(readOnly = true)
    public Map<Long, Long> countProductsByCategories(Collection<Long> categoryIds) {
        if (categoryIds.isEmpty()) {
            return Map.of();
        }
        return productRepository.countActiveByCategoryIds(categoryIds).stream()
                .collect(Collectors.toMap(IdCount::getId, IdCount::getTotal));
    }

    // ========== Validaciones ==========

    /**
     * Valida que el nombre de la categoría sea único (case-insensitive).
     * Verifica tanto categorías activas como en papelera.
     *
     * @param name      Nombre a validar
     * @param excludeId ID de la categoría a excluir (para updates), null para
     *                  creates
     */
    private void validateCategoryName(String name, Long excludeId) {
        categoryRepository.findByNameIgnoreCase(name).ifPresent(existing -> {
            // Si es update y es la misma categoría, no hay conflicto
            if (excludeId != null && existing.getId().equals(excludeId)) {
                return;
            }

            if (existing.isDeleted()) {
                throw new IllegalArgumentException(
                        "Ya existe una categoría con el nombre '" + name + "' en la papelera. " +
                                "Puedes restaurarla o eliminarla permanentemente antes de crear una nueva.");
            } else {
                throw new IllegalArgumentException("Ya existe una categoría con el nombre: " + name);
            }
        });
    }
}
