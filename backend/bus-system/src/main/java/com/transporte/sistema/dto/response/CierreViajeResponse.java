package com.transporte.sistema.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Cierre operativo por viaje (agregación derivada, READ-ONLY).
 * No registra un nuevo modelo contable: el consolidado financiero es el
 * Resumen de Caja por fecha. Los pendientes aparecen por separado y NO se
 * suman a lo cobrado (evita doble conteo).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CierreViajeResponse {
    private Long viajeId;
    private String origen;
    private String destino;
    private String busPlaca;
    private LocalDateTime fechaHoraSalida;
    private Integer capacidadAsientos;
    private Integer asientosVendidos;
    private Integer boletosActivos;
    private Integer pasajerosConfirmados;
    private Integer reservasPendientes;
    private Integer reservasRetenidas;
    private Integer reservasVencidas;
    private Integer reservasLiberadas;
    private BigDecimal totalCobrado;
    private BigDecimal totalEfectivo;
    private BigDecimal totalYape;
    private BigDecimal totalOtrosMetodos;
    private BigDecimal totalPendientePago;
    private Integer declaracionesPendientes;
    private Integer declaracionesRechazadas;
    private boolean deficitario;     // true si hay pendientes sin verificar
    private LocalDateTime generadoEn;
    private List<String> boletosEnmarcados = new ArrayList<>();
}