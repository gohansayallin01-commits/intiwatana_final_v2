package com.transporte.sistema.repository;

import com.transporte.sistema.entity.DeclaracionPago;
import com.transporte.sistema.enums.EstadoDeclaracionPago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface DeclaracionPagoRepository extends JpaRepository<DeclaracionPago, Long> {

    List<DeclaracionPago> findByEstadoOrderByDeclaradoEnAsc(EstadoDeclaracionPago estado);

    List<DeclaracionPago> findByDeclaradoEnGreaterThanEqualOrderByDeclaradoEnDesc(LocalDateTime desde);

    List<DeclaracionPago> findByRetencionIdOrderByDeclaradoEnDesc(Long retencionId);

    boolean existsByRetencionIdAndEstadoIn(Long retencionId, Collection<EstadoDeclaracionPago> estados);

    boolean existsByOperacionAndEstadoIn(String operacion, Collection<EstadoDeclaracionPago> estados);

    /** Declaraciones vigentes (pendiente o aprobada) de un documento. */
    @Query("SELECT d FROM DeclaracionPago d WHERE d.retencion.compradorDocumento = :documento "
            + "ORDER BY d.declaradoEn DESC")
    List<DeclaracionPago> findByDocumento(@Param("documento") String documento);

    @Query("SELECT d FROM DeclaracionPago d INNER JOIN d.retencion r "
            + "WHERE d.estado = :estado AND r.estado IN :retencionEstados "
            + "ORDER BY d.declaradoEn ASC")
    List<DeclaracionPago> findByEstadoAndRetencionEstado(
            @Param("estado") EstadoDeclaracionPago estado,
            @Param("retencionEstados") Collection<com.transporte.sistema.enums.EstadoRetencion> retencionEstados);

    @Query("SELECT d.estado, COUNT(d) FROM DeclaracionPago d WHERE d.retencion.viaje.id = :viajeId "
            + "GROUP BY d.estado")
    List<Object[]> conteoEstadosPorViaje(@Param("viajeId") Long viajeId);

    @Query("SELECT COUNT(d) FROM DeclaracionPago d INNER JOIN d.retencion r "
            + "WHERE r.viaje.id = :viajeId AND d.estado = 'PENDIENTE_VERIFICACION'")
    long countPendientesPorViaje(@Param("viajeId") Long viajeId);

    @Query("SELECT COUNT(d) FROM DeclaracionPago d INNER JOIN d.retencion r "
            + "WHERE r.viaje.id = :viajeId AND d.estado = 'RECHAZADO'")
    long countRechazadasPorViaje(@Param("viajeId") Long viajeId);
}