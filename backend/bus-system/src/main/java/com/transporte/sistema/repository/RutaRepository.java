package com.transporte.sistema.repository;

import com.transporte.sistema.entity.Ruta;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.Optional;

public interface RutaRepository extends JpaRepository<Ruta, Long> {

    @NonNull
    @EntityGraph(attributePaths = {"origen", "destino"})
    List<Ruta> findAll();

    @EntityGraph(attributePaths = {"origen", "destino"})
    List<Ruta> findByOrigenIdAndActivoTrue(Long origenId);

    @EntityGraph(attributePaths = {"origen", "destino"})
    List<Ruta> findByOrigenIdAndDestinoIdAndActivoTrue(Long origenId, Long destinoId);

    @EntityGraph(attributePaths = {"origen", "destino"})
    @Query("SELECT r FROM Ruta r LEFT JOIN FETCH r.origen LEFT JOIN FETCH r.destino WHERE r.id = :id")
    Optional<Ruta> findByIdWithGraph(@Param("id") Long id);

    Optional<Ruta> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);
}
