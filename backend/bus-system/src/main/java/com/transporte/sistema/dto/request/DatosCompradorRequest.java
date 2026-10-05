package com.transporte.sistema.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/** Datos mínimos del comprador/pasajero para una reserva (sin exigir cuenta). */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DatosCompradorRequest {

    @NotBlank(message = "Los nombres son obligatorios")
    @Size(max = 150)
    private String nombres;

    @NotBlank(message = "Los apellidos son obligatorios")
    @Size(max = 150)
    private String apellidos;

    @NotBlank(message = "El documento (DNI/RUC) es obligatorio")
    @Size(max = 15)
    private String documento;

    @NotBlank(message = "El teléfono es obligatorio")
    @Size(max = 20)
    private String telefono;

    @Email(message = "El correo no es válido")
    @Size(max = 150)
    private String email;
}
