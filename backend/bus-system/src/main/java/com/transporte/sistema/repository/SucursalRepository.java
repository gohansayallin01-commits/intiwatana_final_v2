package com.transporte.sistema.repository;

import com.transporte.sistema.entity.Sucursal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface SucursalRepository extends JpaRepository<Sucursal, Long> {

    Optional<Sucursal> findByCodigo(String codigo);

    /** Retorna sucursales activas o con activo=null (registros anteriores al fix) */
    @Query("SELECT s FROM Sucursal s WHERE s.activo IS NULL OR s.activo = true")
    List<Sucursal> findByActivoTrue();

    @Query("SELECT s FROM Sucursal s WHERE s.esTerminal = true AND (s.activo IS NULL OR s.activo = true)")
    List<Sucursal> findByEsTerminalTrueAndActivoTrue();

    boolean existsByCodigo(String codigo);
}
