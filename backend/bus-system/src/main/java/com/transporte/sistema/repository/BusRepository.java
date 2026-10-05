package com.transporte.sistema.repository;

import com.transporte.sistema.entity.Bus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;
import java.util.List;

public interface BusRepository extends JpaRepository<Bus, Long> {

    /** Retorna buses activos (activo = true) o con activo no seteado (null = activo por defecto) */
    @Query("SELECT b FROM Bus b WHERE b.activo IS NULL OR b.activo = true")
    List<Bus> findByActivoTrue();

    Optional<Bus> findByPlaca(String placa);

    boolean existsByPlaca(String placa);
}
