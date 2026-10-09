package com.transporte.sistema.dto.request;

import com.transporte.sistema.enums.MetodoPago;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

/**
 * Venta presencial realizada por personal de caja.
 *
 * Reutiliza el mismo modelo de disponibilidad que la web (reserva {@code Retencion}
 * por viaje + asiento) pero se resuelve en una única operación atómica: se bloquean
 * los asientos, se registra el pago y se emiten los boletos con QR.
 *
 * En caja el cobro es en efectivo y el pasajero es un cliente ya existente
 * (buscado o registrado en el momento antes de cobrar).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VentaPresencialRequest {

    @NotNull(message = "El viaje es obligatorio")
    private Long viajeId;

    @NotEmpty(message = "Debe seleccionar al menos un asiento")
    @Size(max = 10, message = "Como máximo 10 asientos por venta")
    private List<Long> asientoIds;

    @NotNull(message = "Debe seleccionar un cliente para registrar la venta")
    private Long clienteId;

    @NotNull(message = "El método de pago es obligatorio")
    private MetodoPago metodoPago;

    /** Número de operación / observación del cobro (opcional en efectivo). */
    @Size(max = 200)
    private String referenciaPago;

    @Size(max = 300)
    private String observaciones;

    /** Clave de idempotencia (opcional). Reintentos devuelven los mismos boletos. */
    @Size(max = 100)
    private String idempotencyKey;
}
