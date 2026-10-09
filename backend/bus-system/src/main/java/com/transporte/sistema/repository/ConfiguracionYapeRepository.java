package com.transporte.sistema.repository;

import com.transporte.sistema.entity.ConfiguracionYape;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConfiguracionYapeRepository extends JpaRepository<ConfiguracionYape, Long> {

    List<ConfiguracionYape> findByActivoTrueOrderByUpdatedAtDesc();

    Optional<ConfiguracionYape> findFirstByActivoTrueOrderByIdDesc();

    /** Archivo (soft-delete) de la configuración usando activo = false. */
    List<ConfiguracionYape> findAllByOrderByCreatedAtDesc();
}