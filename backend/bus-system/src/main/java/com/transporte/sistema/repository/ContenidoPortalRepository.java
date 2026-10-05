package com.transporte.sistema.repository;

import com.transporte.sistema.entity.ContenidoPortal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContenidoPortalRepository extends JpaRepository<ContenidoPortal, Long> {

    Optional<ContenidoPortal> findBySeccion(String seccion);

    List<ContenidoPortal> findAllByOrderBySeccionAsc();
}
