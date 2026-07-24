package com.malva_pastry_shop.backend.domain.sales;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.malva_pastry_shop.backend.domain.common.TimestampedEntity;
import com.malva_pastry_shop.backend.domain.inventory.Ingredient;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "sale_ingredients")
@Getter
@Setter
@NoArgsConstructor
public class SaleIngredient extends TimestampedEntity {

    @NotNull(message = "La venta es requerida")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_id", nullable = false, foreignKey = @ForeignKey(name = "fk_sale_ingredient_sale"))
    private Sale sale;

    /**
     * Referencia al ingrediente. La columna admite null para poder conservar el
     * historico, pero la FK es NO ACTION (no hay ON DELETE SET NULL en el
     * esquema): un ingrediente usado en una receta no se puede borrar.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ingredient_id", foreignKey = @ForeignKey(name = "fk_sale_ingredient_ingredient"))
    private Ingredient ingredient;

    /**
     * Snapshot del nombre del ingrediente al momento de la venta.
     */
    @NotBlank(message = "El nombre del ingrediente es requerido")
    @Size(max = 100, message = "El nombre del ingrediente no puede exceder 100 caracteres")
    @Column(name = "ingredient_name", nullable = false, length = 100)
    private String ingredientName;

    /**
     * Cantidad usada: cantidad de receta * cantidad vendida.
     */
    @NotNull(message = "La cantidad usada es requerida")
    @DecimalMin(value = "0.0", inclusive = false, message = "La cantidad usada debe ser mayor a 0")
    @Digits(integer = 10, fraction = 4, message = "La cantidad usada debe tener maximo 10 digitos enteros y 4 decimales")
    @Column(name = "quantity_used", nullable = false, precision = 14, scale = 4)
    private BigDecimal quantityUsed;

    /**
     * Snapshot del costo unitario del ingrediente al momento de la venta.
     */
    @NotNull(message = "El costo unitario es requerido")
    @DecimalMin(value = "0.0", inclusive = true, message = "El costo unitario debe ser mayor o igual a 0")
    @Digits(integer = 10, fraction = 2, message = "El costo unitario debe tener maximo 10 digitos enteros y 2 decimales")
    @Column(name = "unit_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitCost;

    /**
     * Snapshot de la unidad de medida del ingrediente.
     */
    @NotBlank(message = "La unidad de medida es requerida")
    @Size(max = 20, message = "La unidad de medida no puede exceder 20 caracteres")
    @Column(name = "unit_of_measure", nullable = false, length = 20)
    private String unitOfMeasure;

    /**
     * Costo total: quantityUsed * unitCost.
     */
    @NotNull(message = "El costo total es requerido")
    @DecimalMin(value = "0.0", inclusive = true, message = "El costo total debe ser mayor o igual a 0")
    @Digits(integer = 10, fraction = 2, message = "El costo total debe tener maximo 10 digitos enteros y 2 decimales")
    @Column(name = "total_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalCost;

    // ==================== CONSTRUCTORES ====================

    public SaleIngredient(Sale sale, Ingredient ingredient, String ingredientName,
            BigDecimal quantityUsed, BigDecimal unitCost, String unitOfMeasure) {
        this.sale = sale;
        this.ingredient = ingredient;
        this.ingredientName = ingredientName;
        this.quantityUsed = quantityUsed;
        this.unitCost = unitCost;
        this.unitOfMeasure = unitOfMeasure;
        // quantityUsed llega con escala 4 (NUMERIC(14,4)) y unitCost con escala 2
        // (NUMERIC(12,2)), por lo que el producto tiene escala 6. Sin redondear,
        // @Digits(fraction = 2) falla en la validacion PreInsert de Hibernate y
        // la venta no se puede persistir. Se redondea a la escala de la columna.
        this.totalCost = quantityUsed.multiply(unitCost).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public String toString() {
        return "SaleIngredient [id=" + getId()
                + ", saleId=" + (sale != null ? sale.getId() : "null")
                + ", ingredientName=" + ingredientName
                + ", quantityUsed=" + quantityUsed
                + ", unitCost=" + unitCost
                + ", totalCost=" + totalCost + "]";
    }
}
