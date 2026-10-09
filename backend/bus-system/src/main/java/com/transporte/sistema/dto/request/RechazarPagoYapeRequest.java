package com.transporte.sistema.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/** Motivo obligatorio para rechazar una declaración de pago Yape. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RechazarPagoYapeRequest {

    @NotBlank(message = "El motivo del rechazo es obligatorio")
    @Size(max = 300, message = "El motivo no puede superar 300 caracteres")
    private String motivo;
}