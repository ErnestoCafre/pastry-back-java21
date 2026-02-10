package com.malva_pastry_shop.backend.domain.publicuser;

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
@Table(name = "favorites", uniqueConstraints = {
        @UniqueConstraint(name = "uk_favorite_public_user_product", columnNames = { "public_user_id", "product_id" })
})
@Getter
@Setter
@NoArgsConstructor
public class Favorite extends TimestampedEntity {

    @NotNull(message = "El usuario es requerido")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "public_user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_favorite_public_user"))
    private PublicUser publicUser;

    @NotNull(message = "El producto es requerido")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false, foreignKey = @ForeignKey(name = "fk_favorite_product"))
    private Product product;

    public Favorite(PublicUser publicUser, Product product) {
        this.publicUser = publicUser;
        this.product = product;
    }

    @Override
    public String toString() {
        return "Favorite [id=" + getId()
                + ", publicUserId=" + (publicUser != null ? publicUser.getId() : "null")
                + ", productId=" + (product != null ? product.getId() : "null") + "]";
    }
}
