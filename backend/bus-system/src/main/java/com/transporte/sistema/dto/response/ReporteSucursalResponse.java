package com.transporte.sistema.dto.response;

import lombok.*;

import java.math.BigDecimal;

/** Actividad consolidada de una sucursal en el período. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReporteSucursalResponse {

    private Long sucursalId;
    private String sucursal;
    private String ciudad;
    private Boolean esTerminal;
    private Long viajesOrigen;
    private Long viajesDestino;
    private Long boletosVendidos;
    private BigDecimal ingresosPasajes;
    private Long encomiendasRegistradas;
    private BigDecimal ingresosEncomiendas;
    /** IngresosPasajes + ingresosEncomiendas */
    private BigDecimal ingresosTotales;
}
