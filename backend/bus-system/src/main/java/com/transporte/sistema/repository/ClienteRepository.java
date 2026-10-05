package com.transporte.sistema.repository;

import com.transporte.sistema.entity.Cliente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    @EntityGraph(attributePaths = {"usuario"})
    Optional<Cliente> findByDniRuc(String dniRuc);
Optional<Cliente> findByUsuarioId(Long usuarioId);
    @EntityGraph(attributePaths = {"usuario"})
    Optional<Cliente> findByUsuarioUsername(String username);

    boolean existsByDniRuc(String dniRuc);

    /**
     * Búsqueda unificada de clientes por tipo/número de documento, nombre, razón
     * social o teléfono. La usan el módulo de clientes y el flujo de encomiendas
     * para reutilizar siempre el mismo remitente o destinatario.
     */
    @Query("SELECT c FROM Cliente c WHERE " +
           "LOWER(c.nombres) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(c.apellidos) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(c.razonSocial) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "c.dniRuc LIKE CONCAT('%', :q, '%') OR " +
           "COALESCE(c.telefono, '') LIKE CONCAT('%', :q, '%')")
    Page<Cliente> buscarPorNombreODocumento(@Param("q") String q, Pageable pageable);
}