package com.transporte.sistema.controller;

import com.transporte.sistema.dto.response.AuditoriaResponse;
import com.transporte.sistema.entity.Auditoria;
import com.transporte.sistema.repository.AuditoriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/auditoria")
@RequiredArgsConstructor
public class AuditoriaController {

    private final AuditoriaRepository auditoriaRepository;

    /** Bitácora de operaciones — solo ADMIN. */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AuditoriaResponse>> listar(
            @RequestParam(required = false) Long usuarioId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "200") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(Math.max(size, 1), 500));
        List<Auditoria> registros = (usuarioId != null)
                ? auditoriaRepository.findByUsuarioIdOrderByFechaHoraDesc(usuarioId, pageable)
                : auditoriaRepository.findAllByOrderByFechaHoraDesc(pageable);

        return ResponseEntity.ok(registros.stream().map(a -> AuditoriaResponse.builder()
                .id(a.getId())
                .usuarioId(a.getUsuarioId())
                .username(a.getUsername())
                .rol(a.getRol())
                .accion(a.getAccion())
                .entidad(a.getEntidad())
                .entidadId(a.getEntidadId())
                .fechaHora(a.getFechaHora())
                .resultado(a.getResultado())
                .detalle(a.getDetalle())
                .build()).toList());
    }
}