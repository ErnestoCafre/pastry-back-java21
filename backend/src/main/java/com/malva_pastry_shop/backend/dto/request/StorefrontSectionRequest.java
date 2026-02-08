package com.malva_pastry_shop.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class StorefrontSectionRequest {

    @NotBlank(message = "El nombre de la sección es requerido")
    @Size(max = 100, message = "El nombre de la sección no puede exceder los 100 caracteres")
    private String name;

    @Size(max = 500, message = "La descripción no puede exceder los 500 caracteres")
    private String description;

    private Integer displayOrder;

    private Boolean visible = true;
}
