package com.malva_pastry_shop.backend.service.storefront;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.malva_pastry_shop.backend.domain.auth.User;
import com.malva_pastry_shop.backend.domain.inventory.Ingredient;
import com.malva_pastry_shop.backend.domain.inventory.ProductIngredient;
import com.malva_pastry_shop.backend.domain.inventory.Category;
import com.malva_pastry_shop.backend.domain.inventory.Product;
import com.malva_pastry_shop.backend.domain.storefront.ProductTag;
import com.malva_pastry_shop.backend.domain.storefront.Tag;
import com.malva_pastry_shop.backend.dto.request.ProductRequest;
import com.malva_pastry_shop.backend.repository.CategoryRepository;
import com.malva_pastry_shop.backend.repository.IngredientRepository;
import com.malva_pastry_shop.backend.repository.ProductIngredientRepository;
import com.malva_pastry_shop.backend.repository.ProductRepository;
import com.malva_pastry_shop.backend.repository.ProductTagRepository;
import com.malva_pastry_shop.backend.repository.TagRepository;

import java.math.BigDecimal;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final ProductTagRepository productTagRepository;
    private final IngredientRepository ingredientRepository;
    private final ProductIngredientRepository productIngredientRepository;

    public ProductService(ProductRepository productRepository,
            CategoryRepository categoryRepository,
            TagRepository tagRepository,
            ProductTagRepository productTagRepository,
            IngredientRepository ingredientRepository,
            ProductIngredientRepository productIngredientRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.tagRepository = tagRepository;
        this.productTagRepository = productTagRepository;
        this.ingredientRepository = ingredientRepository;
        this.productIngredientRepository = productIngredientRepository;
    }

    // ========== Consultas ==========

    public Page<Product> findAllActive(Pageable pageable) {
        return productRepository.findByDeletedAtIsNull(pageable);
    }

    public Page<Product> findByCategoryId(Long categoryId, Pageable pageable) {
        return productRepository.findByCategoryIdAndDeletedAtIsNull(categoryId, pageable);
    }

    public Page<Product> search(String name, Pageable pageable) {
        return productRepository.findByNameContainingIgnoreCaseAndDeletedAtIsNull(name, pageable);
    }

    public Product findById(Long id) {
        return productRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado con ID: " + id));
    }

    public Page<Product> findDeleted(Pageable pageable) {
        return productRepository.findByDeletedAtIsNotNull(pageable);
    }

    // ========== Consultas API pública (vitrina) ==========

    @Transactional(readOnly = true)
    public Page<Product> findVisibleProducts(String name, Long categoryId, Pageable pageable) {
        if (name != null && !name.isBlank()) {
            return productRepository
                    .findByVisibleTrueAndNameContainingIgnoreCaseAndDeletedAtIsNull(name, pageable);
        } else if (categoryId != null) {
            return productRepository
                    .findByVisibleTrueAndCategoryIdAndDeletedAtIsNull(categoryId, pageable);
        }
        return productRepository.findByVisibleTrueAndDeletedAtIsNull(pageable);
    }

    @Transactional(readOnly = true)
    public Product findVisibleById(Long id) {
        return productRepository.findByIdAndVisibleTrueAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado"));
    }

    // ========== CRUD ==========

    @Transactional
    public Product create(ProductRequest request, User createdBy) {
        validateProductName(request.getName(), null);

        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPreparationDays(request.getPreparationDays());
        product.setBasePrice(request.getBasePrice());
        product.setImageUrl(request.getImageUrl());
        product.setCreatedBy(createdBy);

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new EntityNotFoundException("Categoría no encontrada"));
            product.setCategory(category);
        }

        return productRepository.save(product);
    }

    @Transactional
    public Product update(Long id, ProductRequest request) {
        Product product = findById(id);
        validateProductName(request.getName(), id);

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPreparationDays(request.getPreparationDays());
        product.setBasePrice(request.getBasePrice());
        product.setImageUrl(request.getImageUrl());

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new EntityNotFoundException("Categoría no encontrada"));
            product.setCategory(category);
        } else {
            product.setCategory(null);
        }

        return productRepository.save(product);
    }

    // ========== Soft Delete ==========

    @Transactional
    public void softDelete(Long id, User deletedBy) {
        Product product = findById(id);
        product.softDelete(deletedBy);
        productRepository.save(product);
    }

    @Transactional
    public Product restore(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado"));

        if (product.getDeletedAt() == null) {
            throw new IllegalStateException("El producto no está eliminado");
        }

        productRepository.findByNameIgnoreCase(product.getName()).ifPresent(existing -> {
            if (!existing.getId().equals(id) && !existing.isDeleted()) {
                throw new IllegalStateException("Ya existe un producto activo con el nombre: " + product.getName());
            }
        });

        product.restore();
        return productRepository.save(product);
    }

    // ========== Hard Delete ==========

    @Transactional
    public void hardDelete(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado"));

        if (product.getDeletedAt() == null) {
            throw new IllegalStateException(
                    "Solo se pueden eliminar permanentemente los productos que están en la papelera");
        }

        productRepository.delete(product);
    }

    // ========== Validaciones ==========

    private void validateProductName(String name, Long excludeId) {
        productRepository.findByNameIgnoreCase(name).ifPresent(existing -> {
            if (excludeId != null && existing.getId().equals(excludeId)) {
                return;
            }

            if (existing.isDeleted()) {
                throw new IllegalArgumentException(
                        "Ya existe un producto con el nombre '" + name + "' en la papelera. " +
                                "Puedes restaurarlo o eliminarlo permanentemente antes de crear uno nuevo.");
            } else {
                throw new IllegalArgumentException("Ya existe un producto con el nombre: " + name);
            }
        });
    }

    // ========== Gestión de Tags ==========

    /**
     * Obtiene los tags activos de un producto.
     * Usa el repositorio directamente para evitar problemas de lazy loading.
     */
    @Transactional(readOnly = true)
    public List<Tag> getProductTags(Long productId) {
        // Verificar que el producto existe
        findById(productId);

        // Usar el repositorio directamente para evitar lazy loading
        return productTagRepository.findByProductId(productId).stream()
                .map(ProductTag::getTag)
                .filter(tag -> !tag.isDeleted())
                .sorted(Comparator.comparing(Tag::getName))
                .collect(Collectors.toList());
    }

    /**
     * Obtiene tags activos que NO están asociados a un producto.
     */
    @Transactional(readOnly = true)
    public List<Tag> getAvailableTagsForProduct(Long productId) {
        // Verificar que el producto existe
        findById(productId);

        // Obtener IDs de tags ya asociados
        Set<Long> currentTagIds = productTagRepository.findByProductId(productId).stream()
                .map(pt -> pt.getTag().getId())
                .collect(Collectors.toSet());

        return tagRepository.findByDeletedAtIsNullOrderByNameAsc().stream()
                .filter(tag -> !currentTagIds.contains(tag.getId()))
                .collect(Collectors.toList());
    }

    /**
     * Agrega un tag a un producto.
     */
    @Transactional
    public void addTagToProduct(Long productId, Long tagId) {
        Product product = findById(productId);
        Tag tag = tagRepository.findByIdAndDeletedAtIsNull(tagId)
                .orElseThrow(() -> new EntityNotFoundException("Tag no encontrado"));

        // Verificar si ya existe usando el repositorio (query optimizada)
        if (productTagRepository.existsByProductIdAndTagId(productId, tagId)) {
            throw new IllegalStateException("El producto ya tiene este tag");
        }

        ProductTag productTag = new ProductTag(product, tag);
        productTagRepository.save(productTag);
    }

    /**
     * Quita un tag de un producto.
     */
    @Transactional
    public void removeTagFromProduct(Long productId, Long tagId) {
        // Verificar que el producto existe
        findById(productId);

        ProductTag productTag = productTagRepository.findByProductIdAndTagId(productId, tagId)
                .orElseThrow(() -> new EntityNotFoundException("El producto no tiene este tag"));

        productTagRepository.delete(productTag);
    }

    // ========== Consultas por Tag ==========

    /**
     * Obtiene los productos activos de un tag.
     */
    public List<Product> getProductsByTag(Long tagId) {
        tagRepository.findByIdAndDeletedAtIsNull(tagId)
                .orElseThrow(() -> new EntityNotFoundException("Tag no encontrado"));

        return productTagRepository.findByTagId(tagId).stream()
                .map(ProductTag::getProduct)
                .filter(product -> !product.isDeleted())
                .sorted(Comparator.comparing(Product::getName))
                .collect(Collectors.toList());
    }

    /**
     * Obtiene productos activos que NO están asociados a un tag.
     */
    public List<Product> getAvailableProductsForTag(Long tagId) {
        tagRepository.findByIdAndDeletedAtIsNull(tagId)
                .orElseThrow(() -> new EntityNotFoundException("Tag no encontrado"));

        Set<Long> currentProductIds = productTagRepository.findByTagId(tagId).stream()
                .map(pt -> pt.getProduct().getId())
                .collect(Collectors.toSet());

        return productRepository.findByDeletedAtIsNull(Pageable.unpaged()).stream()
                .filter(product -> !currentProductIds.contains(product.getId()))
                .sorted(Comparator.comparing(Product::getName))
                .collect(Collectors.toList());
    }

    /**
     * Cuenta cuántos productos activos usan un tag.
     */
    public long countProductsByTag(Long tagId) {
        return productTagRepository.findByTagId(tagId).stream()
                .map(ProductTag::getProduct)
                .filter(product -> !product.isDeleted())
                .count();
    }

    /**
     * Agrega un producto a un tag (Alias para addTagToProduct).
     */
    @Transactional
    public void addProductToTag(Long tagId, Long productId) {
        addTagToProduct(productId, tagId);
    }

    /**
     * Quita un producto de un tag (Alias para removeTagFromProduct).
     */
    @Transactional
    public void removeProductFromTag(Long tagId, Long productId) {
        removeTagFromProduct(productId, tagId);
    }

    // ========== Gestión de Recetas (Ingredientes del Producto) ==========

    /**
     * Obtiene los ingredientes activos de un producto (receta).
     * Usa el repositorio directamente para evitar problemas de lazy loading.
     */
    @Transactional(readOnly = true)
    public List<ProductIngredient> getProductIngredients(Long productId) {
        // Verificar que el producto existe
        findById(productId);

        // Usar el repositorio directamente para evitar lazy loading
        return productIngredientRepository.findByProductId(productId).stream()
                .filter(pi -> !pi.getIngredient().isDeleted())
                .sorted(Comparator.comparing(pi -> pi.getIngredient().getName()))
                .collect(Collectors.toList());
    }

    /**
     * Obtiene ingredientes activos que NO están en la receta del producto.
     */
    @Transactional(readOnly = true)
    public List<Ingredient> getAvailableIngredientsForProduct(Long productId) {
        // Verificar que el producto existe
        findById(productId);

        // Obtener IDs de ingredientes ya en la receta
        Set<Long> currentIngredientIds = productIngredientRepository.findByProductId(productId).stream()
                .map(pi -> pi.getIngredient().getId())
                .collect(Collectors.toSet());

        return ingredientRepository.findByDeletedAtIsNullOrderByNameAsc().stream()
                .filter(ingredient -> !currentIngredientIds.contains(ingredient.getId()))
                .collect(Collectors.toList());
    }

    /**
     * Agrega un ingrediente a la receta del producto.
     */
    @Transactional
    public void addIngredientToProduct(Long productId, Long ingredientId, BigDecimal quantity) {
        Product product = findById(productId);
        Ingredient ingredient = ingredientRepository.findByIdAndDeletedAtIsNull(ingredientId)
                .orElseThrow(() -> new EntityNotFoundException("Ingrediente no encontrado"));

        // Verificar si ya existe usando el repositorio (query optimizada)
        if (productIngredientRepository.existsByProductIdAndIngredientId(productId, ingredientId)) {
            throw new IllegalStateException("El producto ya tiene este ingrediente en su receta");
        }

        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a cero");
        }

        ProductIngredient productIngredient = new ProductIngredient(product, ingredient, quantity);
        productIngredientRepository.save(productIngredient);
    }

    /**
     * Quita un ingrediente de la receta del producto.
     */
    @Transactional
    public void removeIngredientFromProduct(Long productId, Long ingredientId) {
        // Verificar que el producto existe
        findById(productId);

        ProductIngredient productIngredient = productIngredientRepository
                .findByProductIdAndIngredientId(productId, ingredientId)
                .orElseThrow(() -> new EntityNotFoundException("El producto no tiene este ingrediente en su receta"));

        productIngredientRepository.delete(productIngredient);
    }

    /**
     * Actualiza la cantidad de un ingrediente en la receta del producto.
     */
    @Transactional
    public void updateIngredientQuantity(Long productId, Long ingredientId, BigDecimal quantity) {
        // Verificar que el producto existe
        findById(productId);

        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a cero");
        }

        ProductIngredient productIngredient = productIngredientRepository
                .findByProductIdAndIngredientId(productId, ingredientId)
                .orElseThrow(() -> new EntityNotFoundException("El producto no tiene este ingrediente en su receta"));

        productIngredient.setQuantity(quantity);
        productIngredientRepository.save(productIngredient);
    }

    /**
     * Calcula el costo total de los ingredientes de un producto.
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateRecipeCost(Long productId) {
        List<ProductIngredient> ingredients = getProductIngredients(productId);
        return ingredients.stream()
                .map(pi -> pi.getIngredient().getUnitCost().multiply(pi.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
