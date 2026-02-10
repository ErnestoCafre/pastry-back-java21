package com.malva_pastry_shop.backend.domain.sales;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.malva_pastry_shop.backend.domain.auth.User;
import com.malva_pastry_shop.backend.domain.common.TimestampedEntity;
import com.malva_pastry_shop.backend.domain.inventory.Product;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "sales")
@Getter
@Setter
@NoArgsConstructor
public class Sale extends TimestampedEntity {

    @NotNull(message = "La fecha de venta es requerida")
    @Column(name = "sale_date", nullable = false)
    private LocalDateTime saleDate;

    @NotNull(message = "El usuario que registra es requerido")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registered_by_id", nullable = false, foreignKey = @ForeignKey(name = "fk_sale_user"))
    private User registeredBy;

    /**
     * Referencia al producto (puede ser null si el producto se elimina).
     * SET NULL on delete para mantener historico de ventas.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", foreignKey = @ForeignKey(name = "fk_sale_product"))
    private Product product;

    /**
     * Snapshot del nombre del producto al momento de la venta.
     */
    @NotBlank(message = "El nombre del producto es requerido")
    @Size(max = 100, message = "El nombre del producto no puede exceder 100 caracteres")
    @Column(name = "product_name", nullable = false, length = 100)
    private String productName;

    @NotNull(message = "La cantidad es requerida")
    @Min(value = 1, message = "La cantidad debe ser al menos 1")
    @Column(nullable = false)
    private Integer quantity;

    @NotNull(message = "El precio unitario es requerido")
    @DecimalMin(value = "0.0", inclusive = true, message = "El precio unitario debe ser mayor o igual a 0")
    @Digits(integer = 10, fraction = 2, message = "El precio unitario debe tener maximo 10 digitos enteros y 2 decimales")
    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @NotNull(message = "El monto total es requerido")
    @DecimalMin(value = "0.0", inclusive = true, message = "El monto total debe ser mayor o igual a 0")
    @Digits(integer = 10, fraction = 2, message = "El monto total debe tener maximo 10 digitos enteros y 2 decimales")
    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Size(max = 500, message = "Las notas no pueden exceder 500 caracteres")
    @Column(columnDefinition = "TEXT")
    private String notes;

    // ==================== DATOS DEL CLIENTE (Opcionales) ====================

    @Size(max = 150, message = "El nombre del cliente no puede exceder 150 caracteres")
    @Column(name = "customer_name", length = 150)
    private String customerName;

    @Size(max = 20, message = "El DNI no puede exceder 20 caracteres")
    @Column(name = "customer_dni", length = 20)
    private String customerDni;

    @Size(max = 20, message = "El telefono no puede exceder 20 caracteres")
    @Column(name = "customer_phone", length = 20)
    private String customerPhone;

    /**
     * Snapshot de los ingredientes usados en esta venta.
     */
    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("id ASC")
    private List<SaleIngredient> saleIngredients = new ArrayList<>();

    // ==================== CONSTRUCTORES ====================

    public Sale(LocalDateTime saleDate, User registeredBy, Product product, String productName,
            Integer quantity, BigDecimal unitPrice) {
        this.saleDate = saleDate;
        this.registeredBy = registeredBy;
        this.product = product;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.totalAmount = unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    // ==================== METODOS DE AYUDA ====================

    /**
     * Agrega un ingrediente al snapshot de la venta.
     */
    public void addSaleIngredient(SaleIngredient saleIngredient) {
        saleIngredients.add(saleIngredient);
        saleIngredient.setSale(this);
    }

    /**
     * Calcula el costo total de ingredientes usados en esta venta.
     */
    public BigDecimal calculateTotalIngredientCost() {
        return saleIngredients.stream()
                .map(SaleIngredient::getTotalCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
