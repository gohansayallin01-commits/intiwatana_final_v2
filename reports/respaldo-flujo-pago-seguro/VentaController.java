package com.transporte.sistema.controller;

import com.transporte.sistema.dto.request.ConfirmarReservaRequest;
import com.transporte.sistema.dto.request.CrearRetencionRequest;
import com.transporte.sistema.dto.request.VentaPresencialRequest;
import com.transporte.sistema.dto.response.BoletoResponse;
import com.transporte.sistema.dto.response.RecuperacionResponse;
import com.transporte.sistema.dto.response.RetencionResponse;
import com.transporte.sistema.enums.EstadoRetencion;
import com.transporte.sistema.enums.MetodoPago;
import com.transporte.sistema.service.VentaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Flujo comercial de compra (web invitado + caja).
 *
 * El acceso público se limita a operar sobre una referencia no predecible y,
 * cuando aplica, exige el documento del comprador. La confirmación de pago y la
 * liberación de reservas son exclusivas de personal autorizado.
 */
@RestController
@RequestMapping("/api/v1/ventas")
@RequiredArgsConstructor
public class VentaController {

    private final VentaService ventaService;

    // ── Flujo público (invitado) ────────────────────────────────────────────

    /** Retiene temporalmente los asientos seleccionados y devuelve la referencia. */
    @PostMapping("/retencion")
    @PreAuthorize("permitAll()")
    public ResponseEntity<RetencionResponse> crearRetencion(
            @Valid @RequestBody CrearRetencionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ventaService.crearRetencion(request));
    }

    /** Consulta el estado de una reserva (requiere documento del comprador). */
    @GetMapping("/retencion/{referencia}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<RetencionResponse> obtenerRetencion(
            @PathVariable String referencia,
            @RequestParam String documento) {
        return ResponseEntity.ok(ventaService.obtenerPorReferencia(referencia, documento));
    }

    /** Deja la reserva como PENDIENTE_PAGO para pagarla en caja/terminal. */
    @PostMapping("/retencion/{referencia}/reservar")
    @PreAuthorize("permitAll()")
    public ResponseEntity<RetencionResponse> reservarPendientePago(
            @PathVariable String referencia,
            @RequestParam(required = false) MetodoPago metodoPago) {
        return ResponseEntity.ok(ventaService.reservarPendientePago(referencia, metodoPago));
    }

    /** Constancia PDF de una reserva pendiente (NO válida para embarque). */
    @GetMapping("/retencion/{referencia}/constancia.pdf")
    @PreAuthorize("permitAll()")
    public ResponseEntity<byte[]> constanciaPdf(@PathVariable String referencia,
                                                @RequestParam String documento) {
        byte[] pdf = ventaService.constanciaPdf(referencia, documento);
        return pdf("constancia-" + referencia + ".pdf", pdf);
    }

    /** Boleto PDF de una venta confirmada (con QR de embarque). */
    @GetMapping("/boleto/{numeroBoleto}/pdf")
    @PreAuthorize("permitAll()")
    public ResponseEntity<byte[]> boletoPdf(@PathVariable String numeroBoleto,
                                            @RequestParam String documento) {
        byte[] pdf = ventaService.boletoPdf(numeroBoleto, documento);
        return pdf("boleto-" + numeroBoleto + ".pdf", pdf);
    }

    /** Recuperación de reservas y boletos por documento, sin necesidad de cuenta. */
    @GetMapping("/recuperar")
    @PreAuthorize("permitAll()")
    public ResponseEntity<RecuperacionResponse> recuperar(
            @RequestParam String documento,
            @RequestParam(required = false) String referencia) {
        return ResponseEntity.ok(ventaService.recuperar(documento, referencia));
    }

    // ── Gestión interna (caja/admin) ────────────────────────────────────────

    /** Lista reservas retenidas / pendientes de pago. */
    @GetMapping("/reservas")
    @PreAuthorize("hasAnyRole('ADMIN','CAJERO','VENDEDOR')")
    public ResponseEntity<List<RetencionResponse>> listarReservas(
            @RequestParam(required = false) List<EstadoRetencion> estado) {
        return ResponseEntity.ok(ventaService.listarReservas(estado));
    }

    /** Registra el pago real y emite los boletos + QR. Idempotente. */
    @PostMapping("/reservas/{referencia}/confirmar")
    @PreAuthorize("hasAnyRole('ADMIN','CAJERO','VENDEDOR')")
    public ResponseEntity<List<BoletoResponse>> confirmar(
            @PathVariable String referencia,
            @Valid @RequestBody ConfirmarReservaRequest request,
            Authentication auth) {
        return ResponseEntity.ok(ventaService.confirmarPago(referencia, request, auth.getName()));
    }

    /** Venta presencial de caja: bloquea asiento(s), cobra en efectivo y emite boleto(s) + QR. */
    @PostMapping("/presencial")
    @PreAuthorize("hasAnyRole('ADMIN','CAJERO','VENDEDOR')")
    public ResponseEntity<List<BoletoResponse>> ventaPresencial(
            @Valid @RequestBody VentaPresencialRequest request,
            Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ventaService.ventaPresencial(request, auth.getName()));
    }

    /** Libera (cancela) una reserva no confirmada. */
    @PostMapping("/reservas/{referencia}/liberar")
    @PreAuthorize("hasAnyRole('ADMIN','CAJERO','VENDEDOR')")
    public ResponseEntity<RetencionResponse> liberar(
            @PathVariable String referencia,
            @RequestParam(required = false) String motivo,
            Authentication auth) {
        return ResponseEntity.ok(ventaService.liberar(referencia, motivo, auth.getName()));
    }

    private ResponseEntity<byte[]> pdf(String nombre, byte[] contenido) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombre + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(contenido);
    }
}
