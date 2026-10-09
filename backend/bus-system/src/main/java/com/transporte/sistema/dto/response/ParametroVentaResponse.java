package com.transporte.sistema.dto.response;

import lombok.*;

import java.time.LocalDateTime;

/** Parámetro de reserva/pago con su rango habilitado (vista admin). */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParametroVentaResponse {
    private String parametro;
    private Integer valor;
    private Integer rangoMin;
    private Integer rangoMax;
    private String unidad;
    private String actualizadoPor;
    private String motivo;
    private LocalDateTime actualizadoEn;
}