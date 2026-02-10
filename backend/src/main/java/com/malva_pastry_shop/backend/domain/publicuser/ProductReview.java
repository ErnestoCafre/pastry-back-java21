package com.malva_pastry_shop.backend.domain.publicuser;

import com.malva_pastry_shop.backend.domain.auth.User;
import com.malva_pastry_shop.backend.domain.common.TimestampedEntity;
import com.malva_pastry_shop.backend.domain.inventory.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "product_reviews", uniqueConstraints = {
        @UniqueConstraint(name = "uk_review_public_user_product", columnNames = { "public_user_id", "product_id" })
})
@Getter
@Setter
@NoArgsConstructor
public class ProductReview extends TimestampedEntity {

    @NotNull(message = "El usuario es requerido")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "public_user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_review_public_user"))
    private PublicUser publicUser;

    @NotNull(message = "El producto es requerido")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false, foreignKey = @ForeignKey(name = "fk_review_product"))
    private Product product;

    @NotBlank(message = "El contenido de la reseña es requerido")
    @Size(min = 10, max = 1000, message = "La reseña debe tener entre 10 y 1000 caracteres")
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @NotNull(message = "La calificación es requerida")
    @Min(value = 1, message = "La calificación debe ser al menos 1")
    @Max(value = 5, message = "La calificación no puede ser mayor a 5")
    @Column(nullable = false)
    private Integer rating;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReviewStatus status = ReviewStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "moderated_by_id", foreignKey = @ForeignKey(name = "fk_review_moderated_by"))
    private User moderatedBy;

    @Column(name = "moderated_at")
    private LocalDateTime moderatedAt;

    public ProductReview(PublicUser publicUser, Product product, String content, Integer rating) {
        this.publicUser = publicUser;
        this.product = product;
        this.content = content;
        this.rating = rating;
        this.status = ReviewStatus.PENDING;
    }

    public void approve(User admin) {
        this.status = ReviewStatus.APPROVED;
        this.moderatedBy = admin;
        this.moderatedAt = LocalDateTime.now();
    }

    public void reject(User admin) {
        this.status = ReviewStatus.REJECTED;
        this.moderatedBy = admin;
        this.moderatedAt = LocalDateTime.now();
    }

    public boolean isApproved() {
        return status == ReviewStatus.APPROVED;
    }

    public boolean isPending() {
        return status == ReviewStatus.PENDING;
    }

    @Override
    public String toString() {
        return "ProductReview [id=" + getId()
                + ", publicUserId=" + (publicUser != null ? publicUser.getId() : "null")
                + ", productId=" + (product != null ? product.getId() : "null")
                + ", rating=" + rating
                + ", status=" + status + "]";
    }
}
