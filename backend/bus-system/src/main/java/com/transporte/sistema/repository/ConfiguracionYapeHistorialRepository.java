package com.transporte.sistema.repository;

import com.transporte.sistema.entity.ConfiguracionYapeHistorial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConfiguracionYapeHistorialRepository extends JpaRepository<ConfiguracionYapeHistorial, Long> {

    List<ConfiguracionYapeHistorial> findTop40ByConfiguracionIdOrderByFechaDesc(Long configuracionId);
}