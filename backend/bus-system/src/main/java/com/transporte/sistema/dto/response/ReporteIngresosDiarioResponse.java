package com.transporte.sistema.dto.response;

import lombok.*;

import java.math.BigDecimal;

/** Un día dentro de la serie temporal de ingresos. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReporteIngresosDiarioResponse {

    private String fecha;
    private Long transacciones;
    private BigDecimal ingresosPasajes;
    private BigDecimal ingresosEncomiendas;
    private BigDecimal total;
}
