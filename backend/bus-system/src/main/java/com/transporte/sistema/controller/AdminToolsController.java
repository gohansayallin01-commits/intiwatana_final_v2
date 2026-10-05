package com.transporte.sistema.controller;

import com.transporte.sistema.config.DataInitializer;
import com.transporte.sistema.dto.response.ApiResponse;
import com.transporte.sistema.repository.ViajeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Herramientas de administración: acciones manuales solo para ADMIN.
 */
@RestController
@RequestMapping("/api/v1/admin/tools")
@Profile("!prod")
@RequiredArgsConstructor
public class AdminToolsController {

    private final DataInitializer dataInitializer;
    private final ViajeRepository viajeRepository;

    /**
     * Genera viajes de prueba si la tabla está vacía.
     * Útil cuando se añaden rutas/buses después del primer arranque.
     */
    @PostMapping("/generar-viajes-prueba")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> generarViajesPrueba() {
        long antes = viajeRepository.count();
        if (antes > 0) {
            return ResponseEntity.ok(
                ApiResponse.ok("Ya existen " + antes + " viaje(s). No se generaron duplicados.", null)
            );
        }
        dataInitializer.crearViajesDePrueba();
        long despues = viajeRepository.count();
        long creados  = despues - antes;
        return ResponseEntity.ok(
            ApiResponse.ok("Se generaron " + creados + " viaje(s) de prueba correctamente.", null)
        );
    }
}
