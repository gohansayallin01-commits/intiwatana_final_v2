package com.transporte.sistema.repository;

import com.transporte.sistema.entity.Auditoria;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {

    List<Auditoria> findAllByOrderByFechaHoraDesc(Pageable pageable);

    List<Auditoria> findByUsuarioIdOrderByFechaHoraDesc(Long usuarioId, Pageable pageable);
}