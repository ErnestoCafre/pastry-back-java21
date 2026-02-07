package com.malva_pastry_shop.backend.domain.storefront;

import com.malva_pastry_shop.backend.domain.common.TimestampedEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "storefront_section_products", uniqueConstraints = {
        @UniqueConstraint(name = "uk_section_product", columnNames = { "storefront_section_id", "product_id" })
})
@Getter
@Setter
@NoArgsConstructor
public class StorefrontSectionProduct extends TimestampedEntity {

    @NotNull(message = "La sección es requerida")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "storefront_section_id", nullable = false, foreignKey = @ForeignKey(name = "fk_section_product_section"))
    private StorefrontSection storefrontSection;

    @NotNull(message = "El producto es requerido")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false, foreignKey = @ForeignKey(name = "fk_section_product_product"))
    private Product product;

    @Column(name = "display_order")
    private Integer displayOrder;

    // ==================== CONSTRUCTORES ====================

    public StorefrontSectionProduct(StorefrontSection storefrontSection, Product product) {
        this.storefrontSection = storefrontSection;
        this.product = product;
    }

    public StorefrontSectionProduct(StorefrontSection storefrontSection, Product product, Integer displayOrder) {
        this.storefrontSection = storefrontSection;
        this.product = product;
        this.displayOrder = displayOrder;
    }

    @Override
    public String toString() {
        return "StorefrontSectionProduct [id=" + getId()
                + ", sectionId=" + (storefrontSection != null ? storefrontSection.getId() : "null")
                + ", productId=" + (product != null ? product.getId() : "null") + "]";
    }
}
