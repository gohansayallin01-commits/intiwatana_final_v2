package com.transporte.sistema.controller;

import com.transporte.sistema.dto.request.ActualizarEstadoEncomiendaRequest;
import com.transporte.sistema.dto.request.ConfirmarRecepcionEncomiendaRequest;
import com.transporte.sistema.dto.request.EncomiendaClienteRequest;
import com.transporte.sistema.dto.request.EncomiendaRequest;
import com.transporte.sistema.dto.response.EncomiendaResponse;
import com.transporte.sistema.dto.response.MovimientoEncomiendaResponse;
import com.transporte.sistema.dto.response.SolicitudPendienteResponse;
import com.transporte.sistema.dto.response.TrackingEncomiendaResponse;
import com.transporte.sistema.exception.NegocioException;
import com.transporte.sistema.service.EncomiendaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/encomiendas")
@RequiredArgsConstructor
public class EncomiendaController {

    private final EncomiendaService encomiendaService;

    /** Listar todas - solo personal de administración y operación de encomiendas */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','CAJERO','VENDEDOR','OPERADOR_ENCOMIENDAS')")
    public ResponseEntity<List<EncomiendaResponse>> listarTodas() {
        return ResponseEntity.ok(encomiendaService.listarTodas());
    }

    /** Tracking público por número de guía */
    /**
     * Tracking público por número de guía.
     * Devuelve una vista reducida del envío: no expone remitente, documentos,
     * teléfonos ni el personal responsable de los movimientos.
     */
    @GetMapping("/tracking/{numeroGuia}")
    public ResponseEntity<TrackingEncomiendaResponse> tracking(@PathVariable String numeroGuia) {
        String guia = numeroGuia == null ? "" : numeroGuia.trim().toUpperCase();
        if (!guia.matches("GUI-\\d{8}-\\d{6}")) {
            throw new NegocioException(
                    "Formato de guía inválido. Usa el formato GUI-AAAAMMDD-000001.");
        }
        return ResponseEntity.ok(encomiendaService.consultarTracking(guia));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','CAJERO','VENDEDOR','OPERADOR_ENCOMIENDAS')")
    public ResponseEntity<EncomiendaResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(encomiendaService.obtenerPorId(id));
    }

    @GetMapping("/remitente/{remitenteId}")
    @PreAuthorize("hasAnyRole('ADMIN','CAJERO','VENDEDOR','OPERADOR_ENCOMIENDAS')")
    public ResponseEntity<Page<EncomiendaResponse>> listarPorRemitente(
            @PathVariable Long remitenteId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(encomiendaService.listarPorRemitente(
                remitenteId, PageRequest.of(page, size, Sort.by("createdAt").descending())));
    }

    @GetMapping("/sucursal/{sucursalId}")
    @PreAuthorize("hasAnyRole('ADMIN','CAJERO','VENDEDOR','OPERADOR_ENCOMIENDAS')")
    public ResponseEntity<Page<EncomiendaResponse>> listarPorSucursal(
            @PathVariable Long sucursalId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(encomiendaService.listarPorSucursalOrigen(
                sucursalId, PageRequest.of(page, size, Sort.by("createdAt").descending())));
    }

    /** Registro PRESENCIAL en agencia — el cliente está físicamente en ventanilla */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','CAJERO','VENDEDOR','OPERADOR_ENCOMIENDAS')")
    public ResponseEntity<EncomiendaResponse> registrar(@Valid @RequestBody EncomiendaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(encomiendaService.registrar(request));
    }

    /**
     * Solicitud previa desde el portal web. Devuelve la referencia SOL y el estado
     * pendiente; nunca una guía oficial (aún no hay recepción ni pago).
     */
    @PostMapping("/solicitar")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<EncomiendaResponse> solicitar(
            @Valid @RequestBody EncomiendaClienteRequest request,
            Principal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(encomiendaService.solicitarEncomienda(request, principal.getName()));
    }

    /**
     * Confirmación de recepción física por personal autorizado: fija peso, medidas,
     * volumen, tarifa y pago, emite la guía GUI oficial y pasa a RECIBIDO.
     * Idempotente: repetir la operación devuelve el estado ya confirmado.
     */
    @PostMapping("/{id}/confirmar-recepcion")
    @PreAuthorize("hasAnyRole('ADMIN','CAJERO','VENDEDOR','OPERADOR_ENCOMIENDAS')")
    public ResponseEntity<EncomiendaResponse> confirmarRecepcion(
            @PathVariable Long id,
            @Valid @RequestBody ConfirmarRecepcionEncomiendaRequest request) {
        return ResponseEntity.ok(encomiendaService.confirmarRecepcion(id, request));
    }

    /**
     * Solicitudes web pendientes de recibir, recuperables por el personal autorizado.
     * Búsqueda por referencia SOL, remitente, destinatario, documento, teléfono,
     * origen, destino y rango de fechas. Los datos personales se devuelven
     * enmascarados.
     */
    @GetMapping("/solicitudes-pendientes")
    @PreAuthorize("hasAnyRole('ADMIN','CAJERO','VENDEDOR','OPERADOR_ENCOMIENDAS')")
    public ResponseEntity<Page<SolicitudPendienteResponse>> listarSolicitudesPendientes(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long sucursalOrigenId,
            @RequestParam(required = false) Long sucursalDestinoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(encomiendaService.listarSolicitudesPendientes(
                q, sucursalOrigenId, sucursalDestinoId,
                desde != null ? desde.atStartOfDay() : null,
                hasta != null ? hasta.atTime(23, 59, 59) : null,
                PageRequest.of(page, Math.min(size, 100), Sort.by("createdAt").descending())));
    }

    /** Recupera una solicitud pendiente concreta por su referencia SOL. */
    @GetMapping("/solicitudes-pendientes/{referencia}")
    @PreAuthorize("hasAnyRole('ADMIN','CAJERO','VENDEDOR','OPERADOR_ENCOMIENDAS')")
    public ResponseEntity<SolicitudPendienteResponse> buscarSolicitudPorReferencia(
            @PathVariable String referencia) {
        return ResponseEntity.ok(encomiendaService.buscarSolicitudPorReferencia(referencia));
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('ADMIN','CAJERO','VENDEDOR','OPERADOR_ENCOMIENDAS')")
    public ResponseEntity<EncomiendaResponse> actualizarEstado(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarEstadoEncomiendaRequest request) {
        return ResponseEntity.ok(encomiendaService.actualizarEstado(id, request));
    }

    /** Encomiendas del cliente autenticado como remitente */
    @GetMapping("/mis-encomiendas")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<List<EncomiendaResponse>> misEncomiendas(Principal principal) {
        return ResponseEntity.ok(encomiendaService.misEncomiendas(principal.getName()));
    }

    /** Movimientos/tracking de una encomienda */
    @GetMapping("/{id}/movimientos")
    @PreAuthorize("hasAnyRole('ADMIN','CAJERO','VENDEDOR','OPERADOR_ENCOMIENDAS','CLIENTE')")
    public ResponseEntity<List<MovimientoEncomiendaResponse>> movimientos(
            @PathVariable Long id,
            Principal principal) {
        return ResponseEntity.ok(encomiendaService.obtenerMovimientos(id, principal.getName()));
    }
}