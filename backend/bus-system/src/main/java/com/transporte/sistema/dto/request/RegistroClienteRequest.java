package com.transporte.sistema.dto.request;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegistroClienteRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 150)
    private String nombres;

    @NotBlank(message = "El apellido es obligatorio")
    @Size(max = 150)
    private String apellidos;

    @NotBlank(message = "El DNI es obligatorio")
    @Size(min = 7, max = 15, message = "DNI/RUC debe tener entre 7 y 15 caracteres")
    private String dniRuc;

    @JsonSetter(nulls = Nulls.AS_EMPTY)
    @Size(max = 20)
    private String telefono;

    @JsonSetter(nulls = Nulls.AS_EMPTY)
    @Email(message = "Email inválido")
    @Size(max = 150)
    private String email;

    @NotBlank(message = "El usuario es obligatorio")
    @Size(min = 4, max = 80, message = "El usuario debe tener entre 4 y 80 caracteres")
    private String username;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
    private String password;
}
