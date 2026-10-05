package com.transporte.sistema.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class SucursalRequest {

    @NotBlank @Size(max = 20)
    private String codigo;

    @NotBlank @Size(max = 150)
    private String nombre;

    @Size(max = 300)
    private String direccion;

    @NotBlank @Size(max = 100)
    private String ciudad;

    @Size(max = 100)
    private String provincia;

    @NotBlank @Size(max = 100)
    private String departamento;

    @Size(max = 20)
    private String telefono;

    @Email @Size(max = 150)
    private String email;

    @Builder.Default
    private Boolean esTerminal = false;
    private Double latitud;
    private Double longitud;
}
