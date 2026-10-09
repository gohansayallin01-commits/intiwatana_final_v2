package com.transporte.sistema.repository;

import com.transporte.sistema.entity.ParametroVentaHistorial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ParametroVentaHistorialRepository extends JpaRepository<ParametroVentaHistorial, Long> {

    List<ParametroVentaHistorial> findTop40ByParametroOrderByFechaDesc(String parametro);
}