package com.transporte.sistema.projection;

import java.math.BigDecimal;

/** Suma y cantidad de un conjunto de registros, para consultas agregadas. */
public interface ReporteTotalProjection {
    BigDecimal getTotal();
    Long getCantidad();
}
