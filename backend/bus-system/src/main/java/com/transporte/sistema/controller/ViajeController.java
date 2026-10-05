package com.transporte.sistema.controller;

import com.transporte.sistema.dto.request.ViajeRequest;
import com.transporte.sistema.dto.response.ApiResponse;
import com.transporte.sistema.dto.response.AsientoResponse;
import com.transporte.sistema.dto.response.ViajeResponse;
import com.transporte.sistema.enums.EstadoViaje;
import com.transporte.sistema.exception.RecursoNoEncontradoException;
import com.transporte.sistema.repository.UsuarioRepository;
import com.transporte.sistema.service.ViajeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/viajes")
@RequiredArgsConstructor
public class ViajeController {

    private final ViajeService viajeService;
    private final UsuarioRepository usuarioRepository;

    /** Listar todos los viajes - solo ADMIN */
    @GetMapping
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<ViajeResponse>> listar() {
        return ResponseEntity.ok(viajeService.listar());
    }

    /** Actualizar viaje - solo ADMIN */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ViajeResponse> actualizar(@PathVariable Long id,
                                                     @Valid @RequestBody ViajeRequest request) {
        return ResponseEntity.ok(viajeService.actualizar(id, request));
    }

    /** Buscar viajes disponibles - PÚBLICO */
@GetMapping("/disponibles")
public ResponseEntity<Page<ViajeResponse>> buscarDisponibles(
        @RequestParam Long origenId,
        @RequestParam Long destinoId,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size) {
    PageRequest pageable = PageRequest.of(page, size, Sort.by("fechaHoraSalida").ascending());
    return ResponseEntity.ok(viajeService.buscarDisponibles(origenId, destinoId, desde, hasta, pageable));
}
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ViajeResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(viajeService.obtenerPorId(id));
    }

    @GetMapping("/{id}/asientos")
    public ResponseEntity<List<AsientoResponse>> obtenerAsientos(@PathVariable Long id,
                                                               Authentication auth) {
        return ResponseEntity.ok(viajeService.obtenerAsientos(id, nombreUsuario(auth)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ViajeResponse> crear(@Valid @RequestBody ViajeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(viajeService.crear(request));
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('ADMIN','CHOFER')")
    public ResponseEntity<ViajeResponse> cambiarEstado(
            Authentication authentication,
            @PathVariable Long id,
            @RequestParam EstadoViaje estado) {
        Long usuarioId = usuarioRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"))
                .getId();
        return ResponseEntity.ok(viajeService.cambiarEstado(id, estado, usuarioId));
    }

    /**
     * Mis viajes: usa el usuario autenticado del contexto de seguridad.
     * El choferId se obtiene de la BD por username, no del request (seguridad).
     */
    @GetMapping("/mis-viajes")
    @PreAuthorize("hasAnyRole('ADMIN','CHOFER')")
    public ResponseEntity<Page<ViajeResponse>> misViajes(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long choferId = usuarioRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"))
                .getId();
        return ResponseEntity.ok(viajeService.listarPorChofer(
                choferId, PageRequest.of(page, size)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable Long id) {
        viajeService.eliminar(id);
        return ResponseEntity.ok(ApiResponse.ok("Viaje eliminado", null));
    }

    /** Nombre de usuario del JWT, o null si la petición es anónima (ruta pública). */
    private String nombreUsuario(Authentication auth) {
        return auth == null ? null : auth.getName();
    }
}
