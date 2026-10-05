package com.transporte.sistema.repository;

import com.transporte.sistema.entity.Viaje;
import com.transporte.sistema.enums.EstadoViaje;
import com.transporte.sistema.projection.ReporteOcupacionProjection;
import com.transporte.sistema.projection.ReportePasajesRutaProjection;
import com.transporte.sistema.projection.ReporteSucursalProjection;
import com.transporte.sistema.projection.ReporteTotalProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;

import java.time.LocalDateTime;
import java.util.List;

public interface ViajeRepository extends JpaRepository<Viaje, Long> {

    @NonNull
    @EntityGraph(attributePaths = {"ruta", "ruta.origen", "ruta.destino", "bus", "chofer"})
    List<Viaje> findAll();

    @EntityGraph(attributePaths = {"ruta", "ruta.origen", "ruta.destino", "bus", "chofer"})
    List<Viaje> findByEstado(EstadoViaje estado);

    @EntityGraph(attributePaths = {"ruta", "ruta.origen", "ruta.destino", "bus", "chofer", "asientos"})
    @Query("SELECT v FROM Viaje v WHERE v.ruta.origen.id = :origenId " +
           "AND v.ruta.destino.id = :destinoId " +
           "AND v.estado = 'PROGRAMADO' " +
           "AND (CAST(:desde AS java.time.LocalDateTime) IS NULL OR v.fechaHoraSalida >= :desde) " +
           "AND (CAST(:hasta AS java.time.LocalDateTime) IS NULL OR v.fechaHoraSalida <= :hasta) " +
           "AND (v.activo IS NULL OR v.activo = true)")
    Page<Viaje> buscarDisponibles(
            @Param("origenId") Long origenId,
            @Param("destinoId") Long destinoId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            Pageable pageable);

    @EntityGraph(attributePaths = {"ruta", "ruta.origen", "ruta.destino", "bus", "chofer"})
    Page<Viaje> findByChoferIdOrderByFechaHoraSalidaDesc(Long choferId, Pageable pageable);

    @Query("SELECT v FROM Viaje v WHERE v.bus.id = :busId " +
           "AND v.fechaHoraSalida BETWEEN :inicio AND :fin " +
           "AND v.estado <> 'CANCELADO'")
    List<Viaje> findByBusIdEnRango(
            @Param("busId") Long busId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin);

    // ── Consultas del módulo de reportes ─────────────────────────────────────
    // Los reportes de viajes se anclan en la fecha de SALIDA del viaje, que es
    // lo que el administrador quiere ver ("ventas de los viajes del día").

    /** Número de viajes del período, ignorando los cancelados. */
    @Query("SELECT COUNT(v) AS cantidad, COALESCE(SUM(v.bus.capacidadAsientos), 0) AS total "
            + "FROM Viaje v "
            + "WHERE v.fechaHoraSalida >= :desde AND v.fechaHoraSalida <= :hasta "
            + "AND v.estado <> com.transporte.sistema.enums.EstadoViaje.CANCELADO")
    ReporteTotalProjection viajesDelPeriodo(@Param("desde") LocalDateTime desde,
                                            @Param("hasta") LocalDateTime hasta);

    /**
     * Ventas de boletos agrupadas por ruta.
     * Los boletos cancelados se excluyen con la cláusula IS NULL para no
     * convertir el LEFT JOIN en un JOIN interno y perder rutas sin ventas.
     */
    @Query("SELECT r.origen.nombre AS origen, r.destino.nombre AS destino, "
            + "COUNT(DISTINCT v.id) AS viajes, COUNT(b.id) AS boletos, "
            + "COALESCE(SUM(b.precioPagado), 0) AS ingresos "
            + "FROM Viaje v JOIN v.ruta r LEFT JOIN v.boletos b "
            + "WHERE v.fechaHoraSalida >= :desde AND v.fechaHoraSalida <= :hasta "
            + "AND v.estado <> com.transporte.sistema.enums.EstadoViaje.CANCELADO "
            + "AND (b.estado IS NULL OR b.estado <> com.transporte.sistema.enums.EstadoBoleto.CANCELADO) "
            + "GROUP BY r.origen.nombre, r.destino.nombre "
            + "ORDER BY ingresos DESC")
    List<ReportePasajesRutaProjection> ventasPorRuta(@Param("desde") LocalDateTime desde,
                                                      @Param("hasta") LocalDateTime hasta);

    /**
     * Ocupación de asientos por viaje. Un asiento está ocupado cuando tiene un
     * boleto asignado que no fue cancelado.
     */
    @Query("SELECT v.id AS viajeId, v.bus.placa AS placa, v.bus.capacidadAsientos AS capacidad, "
            + "COUNT(DISTINCT a.id) AS totalAsientos, "
            + "COUNT(DISTINCT b.id) AS ocupados, "
            + "COALESCE(SUM(b.precioPagado), 0) AS ingresos, "
            + "CAST(v.estado AS string) AS estado "
            + "FROM Viaje v LEFT JOIN v.asientos a LEFT JOIN v.boletos b "
            + "WHERE v.fechaHoraSalida >= :desde AND v.fechaHoraSalida <= :hasta "
            + "AND v.estado <> com.transporte.sistema.enums.EstadoViaje.CANCELADO "
            + "AND (b.estado IS NULL OR b.estado <> com.transporte.sistema.enums.EstadoBoleto.CANCELADO) "
            + "GROUP BY v.id, v.bus.placa, v.bus.capacidadAsientos, v.estado "
            + "ORDER BY v.fechaHoraSalida ASC")
    List<ReporteOcupacionProjection> ocupacionPorViaje(@Param("desde") LocalDateTime desde,
                                                        @Param("hasta") LocalDateTime hasta);

    /**
     * Actividad por sucursal. Una sucursal aparece si es origen de algún viaje,
     * destino de algún viaje, o registró encomiendas en el período.
     */
    @Query("SELECT s.id AS sucursalId, s.nombre AS sucursal, s.ciudad AS ciudad, s.esTerminal AS esTerminal, "
            + "(SELECT COUNT(v) FROM Viaje v WHERE v.ruta.origen = s "
            + "   AND v.fechaHoraSalida >= :desde AND v.fechaHoraSalida <= :hasta "
            + "   AND v.estado <> com.transporte.sistema.enums.EstadoViaje.CANCELADO) AS viajesOrigen, "
            + "(SELECT COUNT(v) FROM Viaje v WHERE v.ruta.destino = s "
            + "   AND v.fechaHoraSalida >= :desde AND v.fechaHoraSalida <= :hasta "
            + "   AND v.estado <> com.transporte.sistema.enums.EstadoViaje.CANCELADO) AS viajesDestino, "
            + "(SELECT COUNT(b) FROM Boleto b WHERE b.viaje.ruta.origen = s "
            + "   AND b.viaje.fechaHoraSalida >= :desde AND b.viaje.fechaHoraSalida <= :hasta "
            + "   AND b.estado <> com.transporte.sistema.enums.EstadoBoleto.CANCELADO) AS boletosVendidos, "
            + "(SELECT COALESCE(SUM(b.precioPagado), 0) FROM Boleto b WHERE b.viaje.ruta.origen = s "
            + "   AND b.viaje.fechaHoraSalida >= :desde AND b.viaje.fechaHoraSalida <= :hasta "
            + "   AND b.estado <> com.transporte.sistema.enums.EstadoBoleto.CANCELADO) AS ingresosPasajes, "
            + "(SELECT COUNT(e) FROM Encomienda e WHERE e.sucursalOrigen = s "
            + "   AND e.createdAt >= :desde AND e.createdAt <= :hasta) AS encomiendasRegistradas, "
            + "(SELECT COALESCE(SUM(e.costo), 0) FROM Encomienda e WHERE e.sucursalOrigen = s "
            + "   AND e.createdAt >= :desde AND e.createdAt <= :hasta) AS ingresosEncomiendas "
            + "FROM Sucursal s "
            + "WHERE EXISTS (SELECT v FROM Viaje v WHERE v.ruta.origen = s "
            + "              AND v.fechaHoraSalida >= :desde AND v.fechaHoraSalida <= :hasta "
            + "              AND v.estado <> com.transporte.sistema.enums.EstadoViaje.CANCELADO) "
            + "   OR EXISTS (SELECT v FROM Viaje v WHERE v.ruta.destino = s "
            + "              AND v.fechaHoraSalida >= :desde AND v.fechaHoraSalida <= :hasta "
            + "              AND v.estado <> com.transporte.sistema.enums.EstadoViaje.CANCELADO) "
            + "   OR EXISTS (SELECT e FROM Encomienda e WHERE e.sucursalOrigen = s "
            + "              AND e.createdAt >= :desde AND e.createdAt <= :hasta) "
            + "ORDER BY s.nombre ASC")
    List<ReporteSucursalProjection> actividadPorSucursal(@Param("desde") LocalDateTime desde,
                                                          @Param("hasta") LocalDateTime hasta);
}

