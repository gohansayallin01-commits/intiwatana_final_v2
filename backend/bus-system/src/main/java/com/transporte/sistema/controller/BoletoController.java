package com.transporte.sistema.controller;

import com.transporte.sistema.dto.request.BoletoRequest;
import com.transporte.sistema.dto.response.BoletoResponse;
import com.transporte.sistema.service.BoletoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/boletos")
@RequiredArgsConstructor
public class BoletoController {

    private final BoletoService boletoService;

    // ── Endpoints existentes (admin/cajero) ──────────────────────────────────

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAJERO', 'VENDEDOR', 'CHOFER')")
    public ResponseEntity<BoletoResponse> obtenerPorId(@PathVariable Long id,
                                                      Authentication auth) {
        return ResponseEntity.ok(boletoService.obtenerPorId(id, auth.getName()));
    }

    @GetMapping("/numero/{numeroBoleto}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAJERO', 'VENDEDOR', 'CHOFER')")
    public ResponseEntity<BoletoResponse> obtenerPorNumero(@PathVariable String numeroBoleto,
                                                          Authentication auth) {
        return ResponseEntity.ok(boletoService.obtenerPorNumero(numeroBoleto, auth.getName()));
    }

    @GetMapping("/cliente/{clienteId}")
    @PreAuthorize("hasAnyRole('ADMIN','CAJERO','VENDEDOR')")
    public ResponseEntity<Page<BoletoResponse>> listarPorCliente(
            @PathVariable Long clienteId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(boletoService.listarPorCliente(
                clienteId, PageRequest.of(page, size, Sort.by("createdAt").descending())));
    }

    @GetMapping("/viaje/{viajeId}")
    @PreAuthorize("hasAnyRole('ADMIN','CAJERO','VENDEDOR','CHOFER')")
    public ResponseEntity<Page<BoletoResponse>> listarPorViaje(
            @PathVariable Long viajeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            Authentication auth) {
        return ResponseEntity.ok(boletoService.listarPorViaje(viajeId,
                PageRequest.of(page, size), auth.getName()));
    }

    @PostMapping("/vender")
    @PreAuthorize("hasAnyRole('ADMIN','CAJERO','VENDEDOR')")
    public ResponseEntity<BoletoResponse> vender(@Valid @RequestBody BoletoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(boletoService.vender(request));
    }

    @PostMapping("/escanear")
    @PreAuthorize("hasAnyRole('ADMIN','CHOFER')")
    public ResponseEntity<BoletoResponse> escanear(@RequestParam String codigoQr,
                                                  Authentication auth) {
        return ResponseEntity.ok(boletoService.escanearQr(codigoQr, auth.getName()));
    }

    @PatchMapping("/{id}/cancelar")
    @PreAuthorize("hasAnyRole('ADMIN','CAJERO','VENDEDOR','CLIENTE')")
    public ResponseEntity<BoletoResponse> cancelar(
            @PathVariable Long id,
            @RequestParam(required = false) String motivo,
            Principal principal) {
        return ResponseEntity.ok(boletoService.cancelarPorUsuario(id, motivo, principal.getName()));
    }

    // ── Portal cliente ────────────────────────────────────────────────────────

    /**
     * GET /api/v1/boletos/mis-boletos
     * Historial de boletos del cliente autenticado.
     */
    @GetMapping("/mis-boletos")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<List<BoletoResponse>> misBoletos(Principal principal) {
        return ResponseEntity.ok(boletoService.misBoletos(principal.getName()));
    }

    /**
     * POST /api/v1/boletos/comprar
     * El cliente compra/reserva su pasaje directamente desde el portal.
     * El clienteId se ignora del request — se determina por el JWT.
     */
    @PostMapping("/comprar")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<BoletoResponse> comprar(
            @Valid @RequestBody BoletoRequest request,
            Principal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(boletoService.comprarPasaje(request, principal.getName()));
    }
}
