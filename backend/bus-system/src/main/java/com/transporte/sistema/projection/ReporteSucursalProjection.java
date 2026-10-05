package com.transporte.sistema.projection;

import java.math.BigDecimal;

/** Actividad consolidada por sucursal. */
public interface ReporteSucursalProjection {
    Long getSucursalId();
    String getSucursal();
    String getCiudad();
    Boolean getEsTerminal();
    Long getViajesOrigen();
    Long getViajesDestino();
    Long getBoletosVendidos();
    BigDecimal getIngresosPasajes();
    Long getEncomiendasRegistradas();
    BigDecimal getIngresosEncomiendas();
}
