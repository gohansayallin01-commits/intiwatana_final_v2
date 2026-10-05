package com.transporte.sistema.projection;

import java.math.BigDecimal;

/** Encomiendas agrupadas por sucursal de origen y destino. */
public interface ReporteEncomiendaRutaProjection {
    String getOrigen();
    String getDestino();
    Long getCantidad();
    BigDecimal getIngresos();
    BigDecimal getPesoKg();
}
