package com.transporte.sistema.projection;

/** Ingresos agrupados por método de pago. */
public interface ReporteIngresoMetodoProjection {
    String getMetodo();
    Long getCantidad();
    java.math.BigDecimal getTotal();
}
