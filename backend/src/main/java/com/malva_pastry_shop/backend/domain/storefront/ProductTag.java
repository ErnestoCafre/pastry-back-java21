package com.malva_pastry_shop.backend.domain.storefront;

import com.malva_pastry_shop.backend.domain.common.TimestampedEntity;
import com.malva_pastry_shop.backend.domain.inventory.Product;

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
@Table(name = "product_tags", uniqueConstraints = {
        @UniqueConstraint(name = "uk_product_tag", columnNames = { "product_id", "tag_id" })
})
@Getter
@Setter
@NoArgsConstructor
public class ProductTag extends TimestampedEntity {

    @NotNull(message = "El producto es requerido")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false, foreignKey = @ForeignKey(name = "fk_product_tag_product"))
    private Product product;

    @NotNull(message = "El tag es requerido")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", nullable = false, foreignKey = @ForeignKey(name = "fk_product_tag_tag"))
    private Tag tag;

    // ==================== CONSTRUCTORES ====================

    public ProductTag(Product product, Tag tag) {
        this.product = product;
        this.tag = tag;
    }

    @Override
    public String toString() {
        return "ProductTag [id=" + getId()
                + ", productId=" + (product != null ? product.getId() : "null")
                + ", tagId=" + (tag != null ? tag.getId() : "null") + "]";
    }
}
