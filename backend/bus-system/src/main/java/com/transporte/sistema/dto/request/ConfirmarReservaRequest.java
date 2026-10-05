package com.transporte.sistema.dto.request;

import com.transporte.sistema.enums.MetodoPago;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

/** Confirmación de pago de una reserva hecha por personal de caja/administración. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfirmarReservaRequest {

    @NotNull(message = "El método de pago es obligatorio")
    private MetodoPago metodoPago;

    @Size(max = 200)
    private String referenciaPago;

    @Size(max = 300)
    private String observaciones;

    /** Documento del comprador (opcional si lo registra el personal). */
    @Size(max = 15)
    private String documento;
}
