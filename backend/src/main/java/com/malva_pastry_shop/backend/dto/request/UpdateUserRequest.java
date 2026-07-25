package com.malva_pastry_shop.backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateUserRequest {

    @NotBlank(message = "El nombre es requerido")
    @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
    private String name;

    @Size(max = 100, message = "El apellido no puede exceder 100 caracteres")
    private String lastName;

    @NotBlank(message = "El email es requerido")
    @Email(message = "El email debe tener un formato valido")
    private String email;

    @NotNull(message = "El rol es requerido")
    private Long roleId;

    @NotNull(message = "El estado es requerido")
    private Boolean enabled;

    // Password opcional para actualizacion
    @Size(min = 6, max = 32, message = "La contrasena debe tener entre 6 y 32 caracteres")
    private String password;
}
