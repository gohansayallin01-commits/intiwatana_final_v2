package com.transporte.sistema.dto.response;

import lombok.*;

import java.util.List;

/** Resultado de la recuperación de reservas/boletos por documento (sin cuenta). */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecuperacionResponse {
    private List<RetencionResponse> reservas;
    private List<BoletoResponse> boletos;
}
