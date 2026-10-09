package com.transporte.sistema.dto.response;

import com.transporte.sistema.enums.EstadoDeclaracionPago;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Estado de la declaración Yape adjunto a cada reserva (vista pública "Mis reservas").
 * La operación se entrega enmascarada; no se revela información ajena.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeclaracionYapeReservaResponse {
    private EstadoDeclaracionPago estado;
    private String operacionEnmascarada;
    private BigDecimal importeDeclarado;
    private String motivoRechazo;
    private LocalDateTime declaradoEn;
    private LocalDateTime verificadoEn;
    private Boolean exponeRenovar;          // true si está RECHAZADO y la reserva sigue vigente
}