package com.transporte.sistema.controller;

import com.transporte.sistema.dto.request.RutaRequest;
import com.transporte.sistema.dto.response.ApiResponse;
import com.transporte.sistema.dto.response.RutaResponse;
import com.transporte.sistema.dto.response.SucursalResponse;
import com.transporte.sistema.entity.Ruta;
import com.transporte.sistema.exception.ConflictoException;
import com.transporte.sistema.exception.RecursoNoEncontradoException;
import com.transporte.sistema.repository.RutaRepository;
import com.transporte.sistema.repository.SucursalRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

/**
 * CRUD de rutas. Los GETs son pÃºblicos (usados en portal de consulta).
 */
@RestController
@RequestMapping("/api/v1/rutas")
@RequiredArgsConstructor
@SuppressWarnings("DataFlowIssue")
public class RutaController {

    private final RutaRepository rutaRepository;
    private final SucursalRepository sucursalRepository;

    @GetMapping
    public ResponseEntity<List<RutaResponse>> listar() {
        List<RutaResponse> rutas = rutaRepository.findAll().stream()
                .filter(r -> !Boolean.FALSE.equals(r.getActivo()))
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(rutas);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RutaResponse> obtener(@PathVariable Long id) {
        Ruta ruta = rutaRepository.findByIdWithGraph(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Ruta", id));
        return ResponseEntity.ok(toResponse(ruta));
    }

    @GetMapping("/origen/{origenId}")
    public ResponseEntity<List<RutaResponse>> listarPorOrigen(@PathVariable Long origenId) {
        return ResponseEntity.ok(
                rutaRepository.findByOrigenIdAndActivoTrue(origenId).stream()
                        .map(this::toResponse).toList());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RutaResponse> crear(@Valid @RequestBody RutaRequest request) {
        if (rutaRepository.existsByCodigo(request.getCodigo()))
            throw new ConflictoException("Ya existe una ruta con cÃ³digo: " + request.getCodigo());

        var origen = sucursalRepository.findById(request.getOrigenId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Sucursal origen", request.getOrigenId()));
        var destino = sucursalRepository.findById(request.getDestinoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Sucursal destino", request.getDestinoId()));

        Ruta ruta = Ruta.builder()
                .codigo(request.getCodigo())
                .origen(origen)
                .destino(destino)
                .distanciaKm(request.getDistanciaKm())
                .duracionHorasEstimada(request.getDuracionHorasEstimada())
                .precioBase(request.getPrecioBase())
                .descripcion(request.getDescripcion())
                .activo(true)
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(rutaRepository.save(ruta)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RutaResponse> actualizar(@PathVariable Long id,
                                                    @Valid @RequestBody RutaRequest request) {
        Ruta ruta = rutaRepository.findByIdWithGraph(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Ruta", id));

        // Si el cÃ³digo cambiÃ³, verificar que no exista otro con ese cÃ³digo
        if (!ruta.getCodigo().equals(request.getCodigo()) && rutaRepository.existsByCodigo(request.getCodigo()))
            throw new ConflictoException("Ya existe una ruta con cÃ³digo: " + request.getCodigo());

        var origen = sucursalRepository.findById(request.getOrigenId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Sucursal origen", request.getOrigenId()));
        var destino = sucursalRepository.findById(request.getDestinoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Sucursal destino", request.getDestinoId()));

        ruta.setCodigo(request.getCodigo());
        ruta.setOrigen(origen);
        ruta.setDestino(destino);
        ruta.setDistanciaKm(request.getDistanciaKm());
        ruta.setDuracionHorasEstimada(request.getDuracionHorasEstimada());
        ruta.setPrecioBase(request.getPrecioBase());
        ruta.setDescripcion(request.getDescripcion());

        Ruta rutaGuardada = rutaRepository.save(ruta);
        Ruta rutaCompleta = rutaRepository.findByIdWithGraph(rutaGuardada.getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Ruta", rutaGuardada.getId()));
        return ResponseEntity.ok(toResponse(rutaCompleta));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable Long id) {
        Ruta ruta = rutaRepository.findByIdWithGraph(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Ruta", id));
        ruta.softDelete();
        rutaRepository.save(ruta);
        return ResponseEntity.ok(ApiResponse.ok("Ruta eliminada", null));
    }

    private RutaResponse toResponse(Ruta r) {
        SucursalResponse origen = r.getOrigen() != null ? SucursalResponse.builder()
                .id(Objects.requireNonNull(r.getOrigen().getId()))
                .codigo(r.getOrigen().getCodigo())
                .nombre(r.getOrigen().getNombre()).ciudad(r.getOrigen().getCiudad())
                .departamento(r.getOrigen().getDepartamento())
                .esTerminal(Objects.requireNonNull(r.getOrigen().getEsTerminal())).build() : null;

        SucursalResponse destino = r.getDestino() != null ? SucursalResponse.builder()
                .id(Objects.requireNonNull(r.getDestino().getId()))
                .codigo(r.getDestino().getCodigo())
                .nombre(r.getDestino().getNombre()).ciudad(r.getDestino().getCiudad())
                .departamento(r.getDestino().getDepartamento())
                .esTerminal(Objects.requireNonNull(r.getDestino().getEsTerminal())).build() : null;

        return RutaResponse.builder()
                .id(Objects.requireNonNull(r.getId())).codigo(r.getCodigo())
                .origen(origen).destino(destino)
                .distanciaKm(r.getDistanciaKm())
                .duracionHorasEstimada(r.getDuracionHorasEstimada())
                .precioBase(r.getPrecioBase())
                .build();
    }
}
