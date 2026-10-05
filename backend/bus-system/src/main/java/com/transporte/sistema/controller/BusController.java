package com.transporte.sistema.controller;

import com.transporte.sistema.dto.request.BusRequest;
import com.transporte.sistema.dto.response.ApiResponse;
import com.transporte.sistema.dto.response.BusResponse;
import com.transporte.sistema.entity.Bus;
import com.transporte.sistema.exception.ConflictoException;
import com.transporte.sistema.exception.RecursoNoEncontradoException;
import com.transporte.sistema.repository.BusRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

/**
 * CRUD de buses/vehículos. Solo ADMIN puede crear/modificar.
 */
@RestController
@RequestMapping("/api/v1/buses")
@RequiredArgsConstructor
@SuppressWarnings("DataFlowIssue")
public class BusController {

    private final BusRepository busRepository;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<BusResponse>> listar() {
        return ResponseEntity.ok(
                busRepository.findByActivoTrue().stream().map(this::toResponse).toList());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BusResponse> obtener(@PathVariable Long id) {
        Bus bus = busRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Bus", id));
        return ResponseEntity.ok(toResponse(bus));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BusResponse> crear(@Valid @RequestBody BusRequest request) {
        if (busRepository.existsByPlaca(request.getPlaca()))
            throw new ConflictoException("Ya existe un bus con placa: " + request.getPlaca());
        Bus bus = Bus.builder()
                .placa(request.getPlaca().toUpperCase())
                .marca(request.getMarca())
                .modelo(request.getModelo())
                .anioFabricacion(request.getAnioFabricacion())
                .capacidadAsientos(request.getCapacidadAsientos())
                .numPisos(request.getNumPisos() != null ? request.getNumPisos() : 1)
                .tipo(request.getTipo())
                .observaciones(request.getObservaciones())
                .activo(true)
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(busRepository.save(bus)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BusResponse> actualizar(@PathVariable Long id,
                                                   @Valid @RequestBody BusRequest request) {
        Bus bus = busRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Bus", id));
        bus.setMarca(request.getMarca());
        bus.setModelo(request.getModelo());
        bus.setAnioFabricacion(request.getAnioFabricacion());
        bus.setCapacidadAsientos(request.getCapacidadAsientos());
        bus.setTipo(request.getTipo());
        bus.setObservaciones(request.getObservaciones());
        return ResponseEntity.ok(toResponse(busRepository.save(bus)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable Long id) {
        Bus bus = busRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Bus", id));
        bus.softDelete();
        busRepository.save(bus);
        return ResponseEntity.ok(ApiResponse.ok("Bus eliminado", null));
    }

    private BusResponse toResponse(Bus b) {
        return BusResponse.builder()
                .id(Objects.requireNonNull(b.getId()))
                .placa(b.getPlaca()).marca(b.getMarca())
                .modelo(b.getModelo()).anioFabricacion(b.getAnioFabricacion())
                .capacidadAsientos(b.getCapacidadAsientos()).numPisos(b.getNumPisos())
                .tipo(b.getTipo()).activo(Objects.requireNonNull(b.getActivo())).build();
    }
}
