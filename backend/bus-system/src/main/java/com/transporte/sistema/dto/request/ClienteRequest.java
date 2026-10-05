package com.transporte.sistema.dto.request;

import com.transporte.sistema.enums.TipoCliente;
import jakarta.validation.constraints.*;
import lombok.*;

/** DTO para crear o actualizar un cliente */
@Data@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClienteRequest {

    @NotBlank(message = "El DNI/RUC es obligatorio")
    @Size(min = 8, max = 11, message = "DNI debe tener 8 dígitos o RUC 11")
    private String dniRuc;

    @Size(max = 150)
    private String nombres;

    @Size(max = 150)
    private String apellidos;

    @Size(max = 250)
    private String razonSocial;

    @Size(max = 20)
    private String telefono;

    @Email
    @Size(max = 150)
    private String email;

    @Size(max = 300)
    private String direccion;

    @Size(max = 100)
    private String distrito;

    @Size(max = 100)
    private String ciudad;

    @NotNull(message = "El tipo de cliente es obligatorio")
    private TipoCliente tipoCliente;
}
