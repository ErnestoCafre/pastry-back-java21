package com.malva_pastry_shop.backend.domain.storefront;

import java.util.ArrayList;
import java.util.List;

import com.malva_pastry_shop.backend.domain.common.SoftDeletableEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "storefront_sections")
@Getter
@Setter
@NoArgsConstructor
public class StorefrontSection extends SoftDeletableEntity {

    @NotBlank(message = "El nombre de la sección es requerido")
    @Size(max = 100, message = "El nombre de la sección no puede exceder los 100 caracteres")
    @Column(nullable = false, length = 100)
    private String name;

    @NotBlank(message = "El slug de la sección es requerido")
    @Size(max = 100, message = "El slug de la sección no puede exceder 100 caracteres")
    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    @Size(max = 500, message = "La descripción no puede exceder los 500 caracteres")
    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(nullable = false)
    private Boolean visible = true;

    @OneToMany(mappedBy = "storefrontSection", fetch = FetchType.LAZY)
    @OrderBy("displayOrder ASC")
    private List<StorefrontSectionProduct> sectionProducts = new ArrayList<>();

    // ==================== CONSTRUCTORES ====================

    public StorefrontSection(String name) {
        this.name = name;
    }

    public StorefrontSection(String name, String description) {
        this.name = name;
        this.description = description;
    }
}
