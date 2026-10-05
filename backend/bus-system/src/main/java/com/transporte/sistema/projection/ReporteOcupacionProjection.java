package com.transporte.sistema.projection;

import java.math.BigDecimal;

/** Ocupación de asientos de un viaje. */
public interface ReporteOcupacionProjection {
    Long getViajeId();
    String getPlaca();
    Integer getCapacidad();
    Long getTotalAsientos();
    Long getOcupados();
    BigDecimal getIngresos();
    String getEstado();
}
