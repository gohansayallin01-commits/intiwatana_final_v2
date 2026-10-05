package com.transporte.sistema.controller;

import com.transporte.sistema.dto.request.ActualizarAsientoRequest;
import com.transporte.sistema.dto.response.AsientoResponse;
import com.transporte.sistema.service.AsientoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/asientos")
@RequiredArgsConstructor
public class AsientoController {

    private final AsientoService asientoService;

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<AsientoResponse> actualizar(
            @PathVariable Long id,
            @RequestBody ActualizarAsientoRequest request) {
        return ResponseEntity.ok(asientoService.actualizar(id, request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAJERO', 'CHOFER')")
    public ResponseEntity<AsientoResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(asientoService.obtenerPorId(id));
    }
}
