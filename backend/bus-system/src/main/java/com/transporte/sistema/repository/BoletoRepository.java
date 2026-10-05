package com.transporte.sistema.repository;

import com.transporte.sistema.entity.Boleto;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface BoletoRepository extends JpaRepository<Boleto, Long> {

    Optional<Boleto> findByNumeroBoleto(String numeroBoleto);
    Optional<Boleto> findByPagoId(Long pagoId);

    /** Todos los boletos emitidos con un mismo pago (una reserva puede tener varios). */
    java.util.List<Boleto> findAllByPagoId(Long pagoId);

    /**
     * Busca el boleto bloqueando la fila (SELECT ... FOR UPDATE).
     * Evita que dos escaneos simultaneos del mismo QR_validen dos veces el mismo boleto.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Boleto b WHERE b.numeroBoleto = :numero")
    Optional<Boleto> findByNumeroBoletoForUpdate(@Param("numero") String numero);

    Page<Boleto> findByClienteIdOrderByCreatedAtDesc(Long clienteId, Pageable pageable);

    Page<Boleto> findByViajeId(Long viajeId, Pageable pageable);

    /** Boletos (no cancelados) de un documento, para recuperación sin cuenta. */
    @Query("SELECT b FROM Boleto b WHERE b.cliente.dniRuc = :documento " +
            "AND b.estado <> com.transporte.sistema.enums.EstadoBoleto.CANCELADO " +
            "ORDER BY b.createdAt DESC")
    java.util.List<Boleto> findVigentesPorDocumento(@Param("documento") String documento);

    /**
     * Cuenta boletos ACTIVOS de un viaje.
     * Usado para validar que no se elimine un viaje con pasajeros.
     */
    @Query("SELECT COUNT(b) FROM Boleto b WHERE b.viaje.id = :viajeId AND b.estado = 'ACTIVO'")
    long countByViajeIdAndEstadoActivo(@Param("viajeId") Long viajeId);

    /**
     * Boletos vendidos en el período por FECHA DE PAGO (mismo ancla que
     * ingresosPasajes), excluyendo cancelados. Sin esto, un boleto vendido hoy
     * para un viaje futuro no contaba en el resumen del día.
     */
    @Query("SELECT COUNT(b) FROM Boleto b WHERE b.pago IS NOT NULL " +
            "AND b.pago.fechaPago >= :desde AND b.pago.fechaPago <= :hasta " +
            "AND b.estado <> 'CANCELADO'")
    long boletosVendidosDelPeriodo(@Param("desde") LocalDateTime desde,
                                   @Param("hasta") LocalDateTime hasta);
}
