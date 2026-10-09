package com.transporte.sistema.dto.response;

import lombok.*;

import java.time.LocalDateTime;

/** Cambio registrado de un parámetro de reserva/pago. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParametroVentaHistorialResponse {
    private String parametro;
    private String valorAnterior;
    private String valorNuevo;
    private String actualizadoPor;
    private String rol;
    private String motivo;
    private LocalDateTime fecha;
}