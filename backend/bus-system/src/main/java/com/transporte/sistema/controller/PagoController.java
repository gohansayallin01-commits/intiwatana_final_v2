package com.transporte.sistema.controller;

import com.transporte.sistema.dto.request.CrearPagoRequest;
import com.transporte.sistema.dto.response.PagoResponse;
import com.transporte.sistema.service.PagoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * Endpoint para procesar pagos (simulado).
 * En producción, integrar con Culqi, Stripe, etc.
 */
@RestController
@RequestMapping("/api/v1/pagos")
@RequiredArgsConstructor
public class PagoController {

    private final PagoService pagoService;

    /**
     * POST /api/v1/pagos
     * Procesa un pago y crea el registro en BD.
     *
     * Restringido a ADMIN y CAJERO: si cualquier cliente autenticado pudiera
     * llamarlo, podría registrar pagos por importes arbitrarios y alterar los
     * ingresos reales que alimentan los reportes. El cobro del portal de
     * pasajeros y encomiendas se genera dentro del servicio de venta, no por
     * este endpoint.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CAJERO')")
    public ResponseEntity<PagoResponse> procesarPago(
            @Valid @RequestBody CrearPagoRequest request) {
        PagoResponse pago = pagoService.procesarPago(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(pago);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CAJERO')")
    public ResponseEntity<List<PagoResponse>> listarPagos() {
        return ResponseEntity.ok(pagoService.listarTodos());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAJERO')")
    public ResponseEntity<PagoResponse> obtenerPago(@PathVariable Long id) {
        return ResponseEntity.ok(pagoService.obtenerPorId(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAJERO')")
    public ResponseEntity<PagoResponse> actualizarPago(
            @PathVariable Long id,
            @Valid @RequestBody CrearPagoRequest request) {
        return ResponseEntity.ok(pagoService.actualizarPago(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminarPago(@PathVariable Long id) {
        pagoService.eliminarPago(id);
        return ResponseEntity.noContent().build();
    }
}