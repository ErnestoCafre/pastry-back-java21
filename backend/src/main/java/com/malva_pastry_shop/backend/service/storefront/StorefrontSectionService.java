package com.malva_pastry_shop.backend.service.storefront;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.malva_pastry_shop.backend.domain.auth.User;
import com.malva_pastry_shop.backend.domain.storefront.Product;
import com.malva_pastry_shop.backend.domain.storefront.StorefrontSection;
import com.malva_pastry_shop.backend.domain.storefront.StorefrontSectionProduct;
import com.malva_pastry_shop.backend.dto.request.StorefrontSectionRequest;
import com.malva_pastry_shop.backend.dto.response.api.ProductApiDTO;
import com.malva_pastry_shop.backend.dto.response.api.StorefrontSectionApiDTO;
import com.malva_pastry_shop.backend.repository.ProductRepository;
import com.malva_pastry_shop.backend.repository.StorefrontSectionProductRepository;
import com.malva_pastry_shop.backend.repository.StorefrontSectionRepository;
import com.malva_pastry_shop.backend.util.SlugUtil;

import jakarta.persistence.EntityNotFoundException;

@Service
public class StorefrontSectionService {

    private final StorefrontSectionRepository sectionRepository;
    private final StorefrontSectionProductRepository sectionProductRepository;
    private final ProductRepository productRepository;

    public StorefrontSectionService(StorefrontSectionRepository sectionRepository,
            StorefrontSectionProductRepository sectionProductRepository,
            ProductRepository productRepository) {
        this.sectionRepository = sectionRepository;
        this.sectionProductRepository = sectionProductRepository;
        this.productRepository = productRepository;
    }

    // ========== Consultas ==========

    public Page<StorefrontSection> findAllActive(Pageable pageable) {
        return sectionRepository.findByDeletedAtIsNull(pageable);
    }

    public Page<StorefrontSection> search(String name, Pageable pageable) {
        return sectionRepository.findByNameContainingIgnoreCaseAndDeletedAtIsNull(name, pageable);
    }

    public StorefrontSection findById(Long id) {
        return sectionRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Sección no encontrada con ID: " + id));
    }

    public StorefrontSection findBySlug(String slug) {
        return sectionRepository.findBySlugAndDeletedAtIsNull(slug)
                .orElseThrow(() -> new EntityNotFoundException("Sección no encontrada con slug: " + slug));
    }

    public List<StorefrontSection> findAllForSelect() {
        return sectionRepository.findByDeletedAtIsNullOrderByNameAsc();
    }

    public Page<StorefrontSection> findDeleted(Pageable pageable) {
        return sectionRepository.findByDeletedAtIsNotNull(pageable);
    }

    // ========== CRUD ==========

    @Transactional
    public StorefrontSection create(StorefrontSectionRequest request) {
        validateSectionName(request.getName(), null);

        StorefrontSection section = new StorefrontSection();
        section.setName(request.getName());
        section.setSlug(SlugUtil.generateSlug(request.getName()));
        section.setDescription(request.getDescription());
        section.setDisplayOrder(request.getDisplayOrder());
        section.setVisible(request.getVisible() != null ? request.getVisible() : true);

        return sectionRepository.save(section);
    }

    @Transactional
    public StorefrontSection update(Long id, StorefrontSectionRequest request) {
        StorefrontSection section = findById(id);
        validateSectionName(request.getName(), id);

        section.setName(request.getName());
        section.setSlug(SlugUtil.generateSlug(request.getName()));
        section.setDescription(request.getDescription());
        section.setDisplayOrder(request.getDisplayOrder());
        section.setVisible(request.getVisible() != null ? request.getVisible() : true);

        return sectionRepository.save(section);
    }

    // ========== Soft Delete ==========

    @Transactional
    public void softDelete(Long id, User deletedBy) {
        StorefrontSection section = findById(id);
        section.softDelete(deletedBy);
        sectionRepository.save(section);
    }

    @Transactional
    public StorefrontSection restore(Long id) {
        StorefrontSection section = sectionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Sección no encontrada"));

        if (section.getDeletedAt() == null) {
            throw new IllegalStateException("La sección no está eliminada");
        }

        sectionRepository.findByNameIgnoreCase(section.getName()).ifPresent(existing -> {
            if (!existing.getId().equals(id) && !existing.isDeleted()) {
                throw new IllegalStateException("Ya existe una sección activa con el nombre: " + section.getName());
            }
        });

        section.restore();
        return sectionRepository.save(section);
    }

    // ========== Hard Delete ==========

    @Transactional
    public void hardDelete(Long id) {
        StorefrontSection section = sectionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Sección no encontrada"));

        if (section.getDeletedAt() == null) {
            throw new IllegalStateException(
                    "Solo se pueden eliminar permanentemente las secciones que están en la papelera");
        }

        sectionRepository.delete(section);
    }

    // ========== Validaciones ==========

    private void validateSectionName(String name, Long excludeId) {
        sectionRepository.findByNameIgnoreCase(name).ifPresent(existing -> {
            if (excludeId != null && existing.getId().equals(excludeId)) {
                return;
            }

            if (existing.isDeleted()) {
                throw new IllegalArgumentException(
                        "Ya existe una sección con el nombre '" + name + "' en la papelera. " +
                                "Puedes restaurarla o eliminarla permanentemente antes de crear una nueva.");
            } else {
                throw new IllegalArgumentException("Ya existe una sección con el nombre: " + name);
            }
        });
    }

    // ========== Gestión de Productos ==========

    @Transactional(readOnly = true)
    public List<StorefrontSectionProduct> getSectionProducts(Long sectionId) {
        findById(sectionId);
        return sectionProductRepository.findByStorefrontSectionIdOrderByDisplayOrderAscProductNameAsc(sectionId);
    }

    @Transactional(readOnly = true)
    public List<Product> getAvailableProductsForSection(Long sectionId) {
        findById(sectionId);

        Set<Long> currentProductIds = sectionProductRepository
                .findByStorefrontSectionIdOrderByDisplayOrderAscProductNameAsc(sectionId).stream()
                .map(sp -> sp.getProduct().getId())
                .collect(Collectors.toSet());

        return productRepository.findByDeletedAtIsNull(Pageable.unpaged()).stream()
                .filter(product -> !currentProductIds.contains(product.getId()))
                .sorted(Comparator.comparing(Product::getName))
                .collect(Collectors.toList());
    }

    @Transactional
    public void addProductToSection(Long sectionId, Long productId) {
        StorefrontSection section = findById(sectionId);
        Product product = productRepository.findByIdAndDeletedAtIsNull(productId)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado"));

        if (sectionProductRepository.existsByStorefrontSectionIdAndProductId(sectionId, productId)) {
            throw new IllegalStateException("El producto ya está en esta sección");
        }

        StorefrontSectionProduct sp = new StorefrontSectionProduct(section, product);
        sectionProductRepository.save(sp);
    }

    @Transactional
    public void removeProductFromSection(Long sectionId, Long productId) {
        findById(sectionId);

        StorefrontSectionProduct sp = sectionProductRepository
                .findByStorefrontSectionIdAndProductId(sectionId, productId)
                .orElseThrow(() -> new EntityNotFoundException("El producto no está en esta sección"));

        sectionProductRepository.delete(sp);
    }

    @Transactional
    public void updateProductDisplayOrder(Long sectionId, Long productId, Integer displayOrder) {
        findById(sectionId);

        StorefrontSectionProduct sp = sectionProductRepository
                .findByStorefrontSectionIdAndProductId(sectionId, productId)
                .orElseThrow(() -> new EntityNotFoundException("El producto no está en esta sección"));

        sp.setDisplayOrder(displayOrder);
        sectionProductRepository.save(sp);
    }

    public long countProducts(Long sectionId) {
        return sectionProductRepository.countByStorefrontSectionIdAndProductDeletedAtIsNull(sectionId);
    }

    // ========== API pública ==========

    @Transactional(readOnly = true)
    public List<StorefrontSectionApiDTO> findVisibleSectionsWithProducts() {
        List<StorefrontSection> sections = sectionRepository
                .findByVisibleTrueAndDeletedAtIsNullOrderByDisplayOrderAscNameAsc();

        return sections.stream().map(section -> {
            List<StorefrontSectionProduct> sectionProducts = sectionProductRepository
                    .findByStorefrontSectionIdAndProductVisibleTrueAndProductDeletedAtIsNullOrderByDisplayOrderAsc(
                            section.getId());

            List<ProductApiDTO.Simple> productDTOs = sectionProducts.stream()
                    .map(sp -> {
                        Product p = sp.getProduct();
                        return new ProductApiDTO.Simple(
                                p.getId(),
                                p.getName(),
                                p.getBasePrice(),
                                p.getImageUrl(),
                                p.getCategory() != null ? p.getCategory().getName() : null);
                    })
                    .collect(Collectors.toList());

            return new StorefrontSectionApiDTO(
                    section.getId(),
                    section.getName(),
                    section.getSlug(),
                    section.getDescription(),
                    productDTOs);
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public StorefrontSectionApiDTO findVisibleSectionBySlug(String slug) {
        StorefrontSection section = sectionRepository.findBySlugAndDeletedAtIsNull(slug)
                .orElseThrow(() -> new EntityNotFoundException("Sección no encontrada"));

        List<StorefrontSectionProduct> sectionProducts = sectionProductRepository
                .findByStorefrontSectionIdAndProductVisibleTrueAndProductDeletedAtIsNullOrderByDisplayOrderAsc(
                        section.getId());

        List<ProductApiDTO.Simple> productDTOs = sectionProducts.stream()
                .map(sp -> {
                    Product p = sp.getProduct();
                    return new ProductApiDTO.Simple(
                            p.getId(),
                            p.getName(),
                            p.getBasePrice(),
                            p.getImageUrl(),
                            p.getCategory() != null ? p.getCategory().getName() : null);
                })
                .collect(Collectors.toList());

        return new StorefrontSectionApiDTO(
                section.getId(),
                section.getName(),
                section.getSlug(),
                section.getDescription(),
                productDTOs);
    }
}
