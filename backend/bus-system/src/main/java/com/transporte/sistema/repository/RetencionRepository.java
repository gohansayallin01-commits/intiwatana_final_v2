package com.transporte.sistema.repository;

import com.transporte.sistema.entity.Retencion;
import com.transporte.sistema.enums.EstadoRetencion;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RetencionRepository extends JpaRepository<Retencion, Long> {

    @EntityGraph(attributePaths = {"viaje", "viaje.ruta", "viaje.ruta.origen",
            "viaje.ruta.destino", "viaje.bus"})
    Optional<Retencion> findByReferencia(String referencia);

    /** Bloquea la fila de la reserva (evita confirmaciones/liberaciones dobles). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Retencion r WHERE r.referencia = :referencia")
    Optional<Retencion> findByReferenciaForUpdate(@Param("referencia") String referencia);

    Optional<Retencion> findByIdempotencyKey(String idempotencyKey);

    @EntityGraph(attributePaths = {"viaje", "viaje.ruta", "viaje.ruta.origen",
            "viaje.ruta.destino", "viaje.bus"})
    List<Retencion> findByCompradorDocumentoOrderByCreatedAtDesc(String compradorDocumento);

    @EntityGraph(attributePaths = {"viaje", "viaje.ruta", "viaje.ruta.origen",
            "viaje.ruta.destino", "viaje.bus"})
    Page<Retencion> findByEstadoIn(Collection<EstadoRetencion> estados, Pageable pageable);

    @Query("SELECT r FROM Retencion r WHERE r.estado IN :estados AND r.expiraEn < :ahora")
    List<Retencion> findVencidas(@Param("estados") Collection<EstadoRetencion> estados,
                                 @Param("ahora") LocalDateTime ahora);

    // ── Control de reservas pendientes (anti "reserva falsa") ───────────────

    /** ¿Existen reservas activas (vigentes) del documento para este viaje? */
    @Query("SELECT count(r) > 0 FROM Retencion r "
            + "WHERE r.compradorDocumento = :documento AND r.viaje.id = :viajeId "
            + "AND r.estado IN :activos AND r.expiraEn > :ahora")
    boolean existeActivaPorDocumentoYViaje(@Param("documento") String documento,
                                           @Param("viajeId") Long viajeId,
                                           @Param("activos") Collection<EstadoRetencion> activos,
                                           @Param("ahora") LocalDateTime ahora);

    /** Cantidad de reservas activas (vigentes) del documento. */
    @Query("SELECT count(r) FROM Retencion r "
            + "WHERE r.compradorDocumento = :documento "
            + "AND r.estado IN :activos AND r.expiraEn > :ahora")
    long countActivasPorDocumento(@Param("documento") String documento,
                                  @Param("activos") Collection<EstadoRetencion> activos,
                                  @Param("ahora") LocalDateTime ahora);

    /** Cantidad de reservas activas (vigentes) por teléfono (nominativo único). */
    @Query("SELECT count(r) FROM Retencion r "
            + "WHERE r.compradorTelefono = :telefono "
            + "AND r.estado IN :activos AND r.expiraEn > :ahora")
    long countActivasPorTelefono(@Param("telefono") String telefono,
                                 @Param("activos") Collection<EstadoRetencion> activos,
                                 @Param("ahora") LocalDateTime ahora);

    // ── Cierre operativo por viaje (agregaciones read-only) ─────────────────

    @Query("SELECT r.estado, COUNT(r) FROM Retencion r WHERE r.viaje.id = :viajeId GROUP BY r.estado")
    List<Object[]> conteoEstadosPorViaje(@Param("viajeId") Long viajeId);

    @Query("SELECT COALESCE(SUM(r.total), 0) FROM Retencion r "
            + "WHERE r.viaje.id = :viajeId AND r.estado = 'PENDIENTE_PAGO'")
    java.math.BigDecimal totalPendientePagoPorViaje(@Param("viajeId") Long viajeId);
}
