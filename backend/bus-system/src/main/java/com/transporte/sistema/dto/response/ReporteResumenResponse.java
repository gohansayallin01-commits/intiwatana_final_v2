package com.transporte.sistema.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Indicadores generales de un período, para la cabecera de la pantalla de reportes.
 *
 * Todos los importes provienen de consultas reales sobre PostgreSQL.
 * Los ingresos se cuentan por la fecha del pago; los viajes y boletos por la
 * fecha de salida del viaje; las encomiendas por su fecha de registro.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReporteResumenResponse {

    private LocalDateTime desde;
    private LocalDateTime hasta;
    private LocalDateTime generadoEn;

    // Pasajes
    private Long viajes;
    private Long boletosVendidos;
    private BigDecimal ingresosPasajes;
    private BigDecimal precioPromedioPasaje;
    private BigDecimal ocupacionPromedio;

    // Encomiendas
    private Long encomiendas;
    private BigDecimal ingresosEncomiendas;
    private BigDecimal pesoTotalKg;

    // Total
    private BigDecimal ingresosTotales;
    private Long transacciones;

    private List<ReporteIngresosResponse.IngresoPorMetodo> ingresosPorMetodo;
}
