package com.transporte.sistema.dto.request;

import com.transporte.sistema.enums.MetodoPago;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

/**
 * Solicitud de retención temporal de asientos hecha desde la web (invitado o
 * cliente autenticado). NO crea boletos ni registra pago: sólo bloquea los
 * asientos por un tiempo acotado.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrearRetencionRequest {

    @NotNull(message = "El viaje es obligatorio")
    private Long viajeId;

    @NotEmpty(message = "Debe seleccionar al menos un asiento")
    @Size(max = 10, message = "Como máximo 10 asientos por reserva")
    private List<Long> asientoIds;

    /** Clave de idempotencia del cliente (opcional). Reintentos devuelven la misma reserva. */
    @Size(max = 100)
    private String idempotencyKey;

    @Valid
    @NotNull(message = "Los datos del comprador son obligatorios")
    private DatosCompradorRequest comprador;

    private MetodoPago metodoPagoPreferido;

    @Size(max = 300)
    private String observaciones;
}
