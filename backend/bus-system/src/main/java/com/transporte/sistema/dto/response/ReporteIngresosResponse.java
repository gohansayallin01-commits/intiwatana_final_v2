package com.transporte.sistema.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Reporte de ingresos del período, desglosado por origen y método de pago. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReporteIngresosResponse {

    private LocalDateTime desde;
    private LocalDateTime hasta;
    private BigDecimal total;
    private BigDecimal totalPasajes;
    private BigDecimal totalEncomiendas;
    private Long transacciones;
    private List<IngresoPorMetodo> porMetodo;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class IngresoPorMetodo {
        private String metodo;
        private Long cantidad;
        private BigDecimal total;
    }
}
