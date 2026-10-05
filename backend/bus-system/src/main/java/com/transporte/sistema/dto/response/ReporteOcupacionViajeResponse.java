package com.transporte.sistema.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Ocupación de un viaje. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReporteOcupacionViajeResponse {

    private Long viajeId;
    private String placa;
    private String estado;
    private Integer capacidadBus;
    private Long totalAsientos;
    private Long ocupados;
    private Long disponibles;
    /** Porcentaje de ocupación con dos decimales, ej: 82.50 */
    private BigDecimal ocupacionPorcentaje;
    private BigDecimal ingresos;
}
