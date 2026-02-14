package com.malva_pastry_shop.backend.domain.inventory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.malva_pastry_shop.backend.domain.auth.User;
import com.malva_pastry_shop.backend.domain.common.SoftDeletableEntity;
import com.malva_pastry_shop.backend.domain.storefront.ProductTag;
import com.malva_pastry_shop.backend.domain.storefront.StorefrontSectionProduct;

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
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
public class Product extends SoftDeletableEntity {

    @NotBlank(message = "El nombre del producto no puede estar vacio")
    @Size(max = 100, message = "El nombre del producto no puede exceder los 100 caracteres")
    @Column(nullable = false, length = 100)
    private String name;

    @Size(max = 500, message = "La descripcion no puede exceder los 500 caracteres")
    @Column(columnDefinition = "TEXT")
    private String description;

    @Min(value = 0, message = "Los dias de preparacion deben ser mayores o iguales a 0")
    @Column(name = "preparation_days")
    private Integer preparationDays;

    @Size(max = 500, message = "La URL de imagen no puede exceder los 500 caracteres")
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    /**
     * Precio base del producto (puede variar en la venta).
     * Nullable para permitir productos sin precio definido inicialmente.
     */
    @DecimalMin(value = "0.0", inclusive = true, message = "El precio base debe ser mayor o igual a 0")
    @Digits(integer = 10, fraction = 2, message = "El precio base debe tener maximo 10 digitos enteros y 2 decimales")
    @Column(name = "base_price", precision = 12, scale = 2)
    private BigDecimal basePrice;

    /**
     * Indica si el producto es visible en la vitrina publica.
     * Por defecto false para que los productos nuevos no sean visibles.
     */
    @Column(nullable = false)
    private Boolean visible = false;

    /**
     * Relacion con User (creador del producto).
     * SET NULL on delete para mantener historico de productos.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "fk_product_user"))
    private User createdBy;

    /**
     * Relacion con Category.
     * SET NULL on delete para no perder productos si se elimina categoria.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", foreignKey = @ForeignKey(name = "fk_product_category"))
    private Category category;

    /**
     * Relacion con Ingredientes de la Receta.
     * Cascade ALL + orphanRemoval para gestion automatica del ciclo de vida.
     */
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("id ASC")
    private List<ProductIngredient> productIngredients = new ArrayList<>();

    /**
     * Relacion con Tags del Producto.
     * Cascade ALL + orphanRemoval para gestion automatica del ciclo de vida.
     */
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("id ASC")
    private List<ProductTag> productTags = new ArrayList<>();

    /**
     * Relacion con Secciones de la Vitrina.
     * Solo cascade REMOVE para limpiar registros al eliminar producto.
     * La gestion de asociaciones se hace via StorefrontSectionProductRepository.
     */
    @OneToMany(mappedBy = "product", cascade = CascadeType.REMOVE, fetch = FetchType.LAZY)
    @OrderBy("displayOrder ASC")
    private List<StorefrontSectionProduct> sectionProducts = new ArrayList<>();

    // ==================== CONSTRUCTORES ====================

    public Product(String name, BigDecimal basePrice) {
        this.name = name;
        this.basePrice = basePrice;
    }
}
