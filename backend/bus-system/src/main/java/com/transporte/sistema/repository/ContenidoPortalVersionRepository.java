package com.transporte.sistema.repository;

import com.transporte.sistema.entity.ContenidoPortalVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContenidoPortalVersionRepository extends JpaRepository<ContenidoPortalVersion, Long> {

    List<ContenidoPortalVersion> findBySeccionOrderByVersionDescIdDesc(String seccion);

    Optional<ContenidoPortalVersion> findByIdAndSeccion(Long id, String seccion);
}
