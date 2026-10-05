package com.transporte.sistema.repository;

import com.transporte.sistema.entity.ImagenPortal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImagenPortalRepository extends JpaRepository<ImagenPortal, Long> {

    List<ImagenPortal> findByActivoTrueOrderByIdDesc();

    List<ImagenPortal> findByChecksumAndActivoTrue(String checksum);
}
