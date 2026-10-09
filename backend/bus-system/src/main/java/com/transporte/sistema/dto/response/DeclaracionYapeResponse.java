package com.transporte.sistema.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Declaración Yape visible en la bandeja de verificación de caja
 * (una por reserva pendiente de verificar).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeclaracionYapeResponse {
    private Long id;
    private String referencia;
    private String documento;
    private String pasajero;
    private String viaje;            // "Lima → Huaraz"
    private String busPlaca;
    private LocalDateTime fechaHoraSalida;
    private String asientos;
    private BigDecimal totalEsperado;
    private String operacion;
    private BigDecimal importeDeclarado;
    private LocalDateTime fechaPagada;
    private String cuentaPagada;
    private LocalDateTime declaradoEn;
    private Long segundosRestantes;
    private String comprobanteUrl;
    private String estado;
    private String motivoRechazo;
    private LocalDateTime verificadoEn;
}