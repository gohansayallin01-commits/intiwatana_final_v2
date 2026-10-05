package com.transporte.sistema.controller;

import com.transporte.sistema.entity.Rol;
import com.transporte.sistema.repository.RolRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoint de lectura de roles del sistema.
 * Restringido a ADMIN: el catálogo de roles es información interna y solo lo
 * necesita el formulario de creación/edición de usuarios de AdminDashboard.
 */
@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class RolController {

    private final RolRepository rolRepository;

    /** Lista todos los roles disponibles (ver SecurityConfig y @PreAuthorize de clase). */
    @GetMapping
    public ResponseEntity<List<RolResponse>> listar() {
        List<RolResponse> roles = rolRepository.findAll().stream()
                .map(r -> new RolResponse(r.getId(), r.getNombre().name(), r.getDescripcion()))
                .toList();
        return ResponseEntity.ok(roles);
    }

    @Data
    static class RolResponse {
        private final Long id;
        private final String nombre;
        private final String descripcion;
    }
}
