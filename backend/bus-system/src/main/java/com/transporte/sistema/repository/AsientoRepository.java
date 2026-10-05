package com.transporte.sistema.repository;

import com.transporte.sistema.entity.Asiento;
import com.transporte.sistema.enums.EstadoAsiento;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AsientoRepository extends JpaRepository<Asiento, Long> {

    List<Asiento> findByViajeId(Long viajeId);

    List<Asiento> findByViajeIdAndEstado(Long viajeId, EstadoAsiento estado);

    Optional<Asiento> findByViajeIdAndNumeroAsiento(Long viajeId, String numeroAsiento);

    /** Bloquea el asiento para operaciones de retención/venta (evita doble venta). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Asiento a WHERE a.id = :id")
    Optional<Asiento> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT COUNT(a) FROM Asiento a WHERE a.viaje.id = :viajeId AND a.estado = 'DISPONIBLE'")
    long countDisponiblesByViajeId(@Param("viajeId") Long viajeId);

    @Query("SELECT a FROM Asiento a WHERE a.viaje.id = :viajeId AND a.id IN :ids")
    List<Asiento> findByViajeIdAndIdIn(@Param("viajeId") Long viajeId,
                                       @Param("ids") java.util.Collection<Long> ids);

    List<Asiento> findByRetencionId(Long retencionId);
}
