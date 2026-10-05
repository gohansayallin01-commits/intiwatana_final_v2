package com.transporte.sistema.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Reporte de venta de pasajes del período, agrupado por ruta. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportePasajesResponse {

    private LocalDateTime desde;
    private LocalDateTime hasta;
    private Long viajes;
    private Long boletosVendidos;
    private BigDecimal ingresos;
    private BigDecimal precioPromedio;
    private List<Ruta> porRuta;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Ruta {
        private String origen;
        private String destino;
        private Long viajes;
        private Long boletos;
        private BigDecimal ingresos;
    }
}
