package com.transporte.sistema.repository;

import com.transporte.sistema.entity.Encomienda;
import com.transporte.sistema.enums.EstadoEncomienda;
import com.transporte.sistema.projection.ReporteEncomiendaEstadoProjection;
import com.transporte.sistema.projection.ReporteEncomiendaRutaProjection;
import com.transporte.sistema.projection.ReporteTotalProjection;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EncomiendaRepository extends JpaRepository<Encomienda, Long> {

    @EntityGraph(attributePaths = {"sucursalOrigen", "sucursalDestino", "remitente", "destinatario"})
    @NonNull
    java.util.List<Encomienda> findAll();

    @EntityGraph(attributePaths = {"sucursalOrigen", "sucursalDestino", "remitente", "destinatario"})
    Optional<Encomienda> findByNumeroGuia(String numeroGuia);

    boolean existsByNumeroGuia(String numeroGuia);

    /**
     * Bloqueo pesimista de la encomienda para la confirmacion de recepcion.
     * Garantiza que dos peticiones simultaneas (doble clic, doble pestana o
     * reintento) sobre la MISMA solicitud no puedan crear dos pagos, dos guias
     * ni dos movimientos iniciales.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Encomienda e WHERE e.id = :id")
    Optional<Encomienda> findByIdForUpdate(@Param("id") Long id);

    @EntityGraph(attributePaths = {"sucursalOrigen", "sucursalDestino", "remitente", "destinatario"})
    Optional<Encomienda> findByReferenciaSolicitudIgnoreCase(String referenciaSolicitud);

    boolean existsByReferenciaSolicitud(String referenciaSolicitud);

    /** Encomienda creada previamente con esa clave de idempotencia, si existe. */
    @EntityGraph(attributePaths = {"sucursalOrigen", "sucursalDestino", "remitente", "destinatario"})
    Optional<Encomienda> findByClaveIdempotencia(String claveIdempotencia);

    /**
     * Correlativo de la referencia SOL tomado de la secuencia seq_encomienda_sol
     * (creada en la migracion V5). La referencia debe existir ANTES del INSERT,
     * porque chk_encomienda_solicitud_sin_datos_oficiales la exige desde que la
     * solicitud nace; por eso no puede derivarse del id autogenerado.
     */
    @Query(value = "SELECT nextval('seq_encomienda_sol')", nativeQuery = true)
    Long siguienteCorrelativoSolicitud();

    @EntityGraph(attributePaths = {"sucursalOrigen", "sucursalDestino", "remitente", "destinatario"})
    Page<Encomienda> findByRemitenteIdOrderByCreatedAtDesc(Long remitenteId, Pageable pageable);

    @EntityGraph(attributePaths = {"sucursalOrigen", "sucursalDestino", "remitente", "destinatario"})
    Page<Encomienda> findByDestinatarioIdOrderByCreatedAtDesc(Long destinatarioId, Pageable pageable);

    @EntityGraph(attributePaths = {"sucursalOrigen", "sucursalDestino", "remitente", "destinatario"})
    Page<Encomienda> findByEstadoOrderByCreatedAtDesc(EstadoEncomienda estado, Pageable pageable);

    @EntityGraph(attributePaths = {"sucursalOrigen", "sucursalDestino", "remitente", "destinatario"})
    Page<Encomienda> findBySucursalOrigenIdOrderByCreatedAtDesc(Long sucursalId, Pageable pageable);

    /**
     * Busqueda de SOLICITUDES PENDIENTES para el panel del cajero.
     * Criterios opcionales: referencia SOL, nombre del remitente o destinatario,
     * documento, telefono, sucursal de origen/destino y rango de fechas.
     */
    @Query("""
            SELECT e FROM Encomienda e
            LEFT JOIN e.remitente r
            LEFT JOIN e.destinatario d
            WHERE e.estado = :estado
              AND (e.activo IS NULL OR e.activo = true)
              AND ( :q IS NULL
                    OR LOWER(COALESCE(e.referenciaSolicitud, ''))  LIKE LOWER(CONCAT('%', :q, '%'))
                    OR LOWER(COALESCE(r.nombres, ''))              LIKE LOWER(CONCAT('%', :q, '%'))
                    OR LOWER(COALESCE(r.apellidos, ''))            LIKE LOWER(CONCAT('%', :q, '%'))
                    OR LOWER(COALESCE(r.razonSocial, ''))          LIKE LOWER(CONCAT('%', :q, '%'))
                    OR LOWER(COALESCE(r.dniRuc, ''))               LIKE LOWER(CONCAT('%', :q, '%'))
                    OR COALESCE(r.telefono, '')                    LIKE CONCAT('%', :q, '%')
                    OR COALESCE(e.remitenteTelefono, '')           LIKE CONCAT('%', :q, '%')
                    OR LOWER(COALESCE(e.destinatarioNombre, ''))   LIKE LOWER(CONCAT('%', :q, '%'))
                    OR LOWER(COALESCE(d.nombres, ''))              LIKE LOWER(CONCAT('%', :q, '%'))
                    OR LOWER(COALESCE(d.apellidos, ''))            LIKE LOWER(CONCAT('%', :q, '%'))
                    OR LOWER(COALESCE(d.dniRuc, ''))               LIKE LOWER(CONCAT('%', :q, '%'))
                    OR LOWER(COALESCE(e.sucursalOrigen.nombre, ''))  LIKE LOWER(CONCAT('%', :q, '%'))
                    OR LOWER(COALESCE(e.sucursalDestino.nombre, '')) LIKE LOWER(CONCAT('%', :q, '%')) )
              AND ( :sucursalOrigenId IS NULL  OR e.sucursalOrigen.id  = :sucursalOrigenId )
              AND ( :sucursalDestinoId IS NULL OR e.sucursalDestino.id = :sucursalDestinoId )
              AND ( :desde IS NULL OR e.createdAt >= :desde )
              AND ( :hasta IS NULL OR e.createdAt <= :hasta )
            """)
    Page<Encomienda> buscarSolicitudesPendientes(@Param("estado") EstadoEncomienda estado,
                                                 @Param("q") String q,
                                                 @Param("sucursalOrigenId") Long sucursalOrigenId,
                                                 @Param("sucursalDestinoId") Long sucursalDestinoId,
                                                 @Param("desde") LocalDateTime desde,
                                                 @Param("hasta") LocalDateTime hasta,
                                                 Pageable pageable);

    Optional<Encomienda> findByPagoId(Long pagoId);

    // ── Consultas del módulo de reportes ─────────────────────────────────────
    // Las encomiendas se anclan en su fecha de registro (createdAt).

    /** Cantidad de encomiendas y facturación total del período. */
    @Query("SELECT COUNT(e) AS cantidad, COALESCE(SUM(e.costo), 0) AS total FROM Encomienda e "
            + "WHERE e.createdAt >= :desde AND e.createdAt <= :hasta "
            + "AND (e.activo IS NULL OR e.activo = true)")
    ReporteTotalProjection encomiendasDelPeriodo(@Param("desde") LocalDateTime desde,
                                                 @Param("hasta") LocalDateTime hasta);

    /** Encomiendas agrupadas por estado, para ver el avance de cada envío. */
    @Query("SELECT CAST(e.estado AS string) AS estado, COUNT(e) AS cantidad, "
            + "COALESCE(SUM(e.costo), 0) AS ingresos FROM Encomienda e "
            + "WHERE e.createdAt >= :desde AND e.createdAt <= :hasta "
            + "AND (e.activo IS NULL OR e.activo = true) "
            + "GROUP BY e.estado ORDER BY cantidad DESC")
    List<ReporteEncomiendaEstadoProjection> encomiendasPorEstado(@Param("desde") LocalDateTime desde,
                                                                  @Param("hasta") LocalDateTime hasta);

    /** Encomiendas agrupadas por corredor sucursal origen → destino. */
    @Query("SELECT so.nombre AS origen, sd.nombre AS destino, COUNT(e) AS cantidad, "
            + "COALESCE(SUM(e.costo), 0) AS ingresos, COALESCE(SUM(e.pesoKg), 0) AS pesoKg "
            + "FROM Encomienda e JOIN e.sucursalOrigen so JOIN e.sucursalDestino sd "
            + "WHERE e.createdAt >= :desde AND e.createdAt <= :hasta "
            + "AND (e.activo IS NULL OR e.activo = true) "
            + "GROUP BY so.nombre, sd.nombre ORDER BY ingresos DESC")
    List<ReporteEncomiendaRutaProjection> encomiendasPorRuta(@Param("desde") LocalDateTime desde,
                                                              @Param("hasta") LocalDateTime hasta);
}