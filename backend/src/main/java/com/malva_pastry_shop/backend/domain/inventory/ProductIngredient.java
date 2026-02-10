package com.malva_pastry_shop.backend.domain.inventory;

import java.math.BigDecimal;

import com.malva_pastry_shop.backend.domain.common.TimestampedEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "product_ingredients")
@Getter
@Setter
@NoArgsConstructor
public class ProductIngredient extends TimestampedEntity {

    @NotNull(message = "El producto es requerido")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false, foreignKey = @ForeignKey(name = "fk_product_ingredient_product"))
    private Product product;

    @NotNull(message = "El ingrediente es requerido")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ingredient_id", nullable = false, foreignKey = @ForeignKey(name = "fk_product_ingredient_ingredient"))
    private Ingredient ingredient;

    @NotNull(message = "La cantidad es requerida")
    @Positive(message = "La cantidad debe ser mayor a cero")
    @Digits(integer = 10, fraction = 4, message = "La cantidad debe tener maximo 10 digitos enteros y 4 decimales")
    @Column(nullable = false, precision = 14, scale = 4)
    private BigDecimal quantity;

    // ==================== CONSTRUCTORES ====================

    public ProductIngredient(Product product, Ingredient ingredient, BigDecimal quantity) {
        this.product = product;
        this.ingredient = ingredient;
        this.quantity = quantity;
    }

    @Override
    public String toString() {
        return "ProductIngredient [id=" + getId()
                + ", productId=" + (product != null ? product.getId() : "null")
                + ", ingredientId=" + (ingredient != null ? ingredient.getId() : "null")
                + ", quantity=" + quantity + "]";
    }
}
