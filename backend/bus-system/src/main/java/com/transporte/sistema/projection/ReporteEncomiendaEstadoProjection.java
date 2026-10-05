package com.transporte.sistema.projection;

import java.math.BigDecimal;

/** Encomiendas agrupadas por estado. */
public interface ReporteEncomiendaEstadoProjection {
    String getEstado();
    Long getCantidad();
    BigDecimal getIngresos();
}
