package com.transporte.sistema.dto.request;

import com.transporte.sistema.enums.MetodoPago;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * DTO de la confirmacion de recepcion fisica de una SOLICITUD previa.
 *
 * Es el mismo paso operativo que el registro presencial: el backend completa
 * peso, medidas, volumen, tarifa, pago, guia oficial y primer movimiento en una
 * sola transaccion. No se admite pago contra entrega.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfirmarRecepcionEncomiendaRequest {

    @NotNull(message = "El peso real es obligatorio")
    @DecimalMin(value = "0.001", message = "El peso debe ser mayor a cero")
    private BigDecimal pesoKg;

    @NotNull(message = "El largo es obligatorio")
    @DecimalMin(value = "0.1", message = "El largo debe ser mayor a cero")
    private BigDecimal largoCm;

    @NotNull(message = "El ancho es obligatorio")
    @DecimalMin(value = "0.1", message = "El ancho debe ser mayor a cero")
    private BigDecimal anchoCm;

    @NotNull(message = "El alto es obligatorio")
    @DecimalMin(value = "0.1", message = "El alto debe ser mayor a cero")
    private BigDecimal altoCm;

    @DecimalMin(value = "0.00")
    private BigDecimal valorDeclarado;

    /**
     * Tarifa oficial a cobrar por el envio, capturada por el personal
     * autorizado. El backend la valida contra el importe del pago.
     */
    @NotNull(message = "La tarifa oficial es obligatoria")
    @DecimalMin(value = "0.01", message = "La tarifa oficial debe ser mayor a cero")
    private BigDecimal tarifaOficial;

    @NotNull(message = "El método de pago es obligatorio")
    private MetodoPago metodoPago;

    @Size(max = 500)
    private String observaciones;
}