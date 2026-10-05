package com.transporte.sistema.projection;

import java.math.BigDecimal;

/** Ventas de boletos agrupadas por ruta. */
public interface ReportePasajesRutaProjection {
    String getOrigen();
    String getDestino();
    Long getViajes();
    Long getBoletos();
    BigDecimal getIngresos();
}
