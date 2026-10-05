package com.transporte.sistema.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Reporte de encomiendas del período. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReporteEncomiendasResponse {

    private LocalDateTime desde;
    private LocalDateTime hasta;
    private Long total;
    private BigDecimal ingresos;
    private BigDecimal pesoTotalKg;
    private BigDecimal ticketPromedio;
    private List<Estado> porEstado;
    private List<Corredor> porRuta;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Estado {
        private String estado;
        private Long cantidad;
        private BigDecimal ingresos;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Corredor {
        private String origen;
        private String destino;
        private Long cantidad;
        private BigDecimal ingresos;
        private BigDecimal pesoKg;
    }
}
