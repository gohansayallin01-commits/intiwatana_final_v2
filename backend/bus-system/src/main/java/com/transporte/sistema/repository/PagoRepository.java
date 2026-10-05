package com.transporte.sistema.repository;

import com.transporte.sistema.entity.Pago;
import com.transporte.sistema.projection.ReporteIngresoMetodoProjection;
import com.transporte.sistema.projection.ReporteTotalProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PagoRepository extends JpaRepository<Pago, Long> {

    List<Pago> findByActivoTrueOrderByFechaPagoDesc();

    Optional<Pago> findByIdAndActivoTrue(Long id);

    // ── Consultas del módulo de reportes ─────────────────────────────────────
    // Los ingresos se anemia por la fecha del pago y sólo se contabilizan los
    // pagos en estado COMPLETADO: un pago contra entrega permanece PENDIENTE
    // hasta que la encomienda se entrega.

    /** Ingresos totales (pasajes + encomiendas) del período. */
    @Query("SELECT COALESCE(SUM(p.monto), 0) AS total, COUNT(p) AS cantidad FROM Pago p "
            + "WHERE p.fechaPago >= :desde AND p.fechaPago <= :hasta "
            + "AND p.estado = com.transporte.sistema.enums.EstadoPago.COMPLETADO")
    ReporteTotalProjection ingresosTotales(@Param("desde") LocalDateTime desde,
                                           @Param("hasta") LocalDateTime hasta);

    /** Ingresos del período correspondientes a venta de pasajes. */
    @Query("SELECT COALESCE(SUM(p.monto), 0) AS total, COUNT(p) AS cantidad FROM Pago p "
            + "WHERE p.fechaPago >= :desde AND p.fechaPago <= :hasta "
            + "AND p.estado = com.transporte.sistema.enums.EstadoPago.COMPLETADO "
            + "AND p IN (SELECT b.pago FROM Boleto b WHERE b.pago IS NOT NULL)")
    ReporteTotalProjection ingresosPasajes(@Param("desde") LocalDateTime desde,
                                           @Param("hasta") LocalDateTime hasta);

    /** Ingresos del período correspondientes a encomiendas. */
    @Query("SELECT COALESCE(SUM(p.monto), 0) AS total, COUNT(p) AS cantidad FROM Pago p "
            + "WHERE p.fechaPago >= :desde AND p.fechaPago <= :hasta "
            + "AND p.estado = com.transporte.sistema.enums.EstadoPago.COMPLETADO "
            + "AND p IN (SELECT e.pago FROM Encomienda e WHERE e.pago IS NOT NULL)")
    ReporteTotalProjection ingresosEncomiendas(@Param("desde") LocalDateTime desde,
                                                @Param("hasta") LocalDateTime hasta);

    /** Desglose de ingresos por método de pago. */
    @Query("SELECT COALESCE(CAST(p.metodo AS string), 'SIN_METODO') AS metodo, "
            + "COUNT(p) AS cantidad, COALESCE(SUM(p.monto), 0) AS total FROM Pago p "
            + "WHERE p.fechaPago >= :desde AND p.fechaPago <= :hasta "
            + "AND p.estado = com.transporte.sistema.enums.EstadoPago.COMPLETADO "
            + "GROUP BY COALESCE(CAST(p.metodo AS string), 'SIN_METODO') "
            + "ORDER BY total DESC")
    List<ReporteIngresoMetodoProjection> ingresosPorMetodo(@Param("desde") LocalDateTime desde,
                                                            @Param("hasta") LocalDateTime hasta);

    /**
     * Pagos del período con su fecha, para agrupar la serie diaria.
     * Se agrega en Java porque HQL no ofrece un truncamiento de fecha portable.
     */
    @Query("SELECT p.fechaPago, p.monto, "
            + "CASE WHEN p IN (SELECT b.pago FROM Boleto b WHERE b.pago IS NOT NULL) THEN true ELSE false END "
            + "FROM Pago p "
            + "WHERE p.fechaPago >= :desde AND p.fechaPago <= :hasta "
            + "AND p.estado = com.transporte.sistema.enums.EstadoPago.COMPLETADO "
            + "ORDER BY p.fechaPago ASC")
    List<Object[]> ingresosSerieDiaria(@Param("desde") LocalDateTime desde,
                                       @Param("hasta") LocalDateTime hasta);
}
