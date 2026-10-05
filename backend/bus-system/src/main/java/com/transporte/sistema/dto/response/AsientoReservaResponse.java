package com.transporte.sistema.dto.response;

import lombok.*;

import java.math.BigDecimal;

/** Asiento dentro de una reserva, con su número y precio oficial. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AsientoReservaResponse {
    private Long id;
    private String numeroAsiento;
    private BigDecimal precio;
}
