package com.transporte.sistema.service.impl;

import com.transporte.sistema.dto.request.ActualizarAsientoRequest;
import com.transporte.sistema.dto.response.AsientoResponse;
import com.transporte.sistema.entity.Asiento;
import com.transporte.sistema.enums.EstadoAsiento;
import com.transporte.sistema.exception.NegocioException;
import com.transporte.sistema.exception.RecursoNoEncontradoException;
import com.transporte.sistema.repository.AsientoRepository;
import com.transporte.sistema.service.AsientoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsientoServiceImpl implements AsientoService {

    private final AsientoRepository asientoRepository;

    @Override
    @Transactional
    public AsientoResponse actualizar(Long id, ActualizarAsientoRequest request) {
        Asiento asiento = asientoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Asiento", id));

        // Validación: No permitir cambiar a BLOQUEADO si está VENDIDO o RESERVADO
        if (request.getEstado() == EstadoAsiento.BLOQUEADO && 
            (asiento.getEstado() == EstadoAsiento.VENDIDO || asiento.getEstado() == EstadoAsiento.RESERVADO)) {
            throw new NegocioException("No se puede poner en mantenimiento un asiento que ya está vendido o reservado.");
        }

        if (request.getEstado() != null) {
            asiento.setEstado(request.getEstado());
        }
        if (request.getTipo() != null) {
            asiento.setTipo(request.getTipo());
        }
        if (request.getPrecio() != null) {
            asiento.setPrecio(request.getPrecio());
        }

        asientoRepository.save(asiento);
        log.info("Asiento {} actualizado: estado={}, tipo={}, precio={}", 
                 id, asiento.getEstado(), asiento.getTipo(), asiento.getPrecio());
        
        return toAsientoResponse(asiento);
    }

    @Override
    @Transactional(readOnly = true)
    public AsientoResponse obtenerPorId(Long id) {
        Asiento asiento = asientoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Asiento", id));
        return toAsientoResponse(asiento);
    }

    private AsientoResponse toAsientoResponse(Asiento a) {
        return AsientoResponse.builder()
                .id(a.getId())
                .numeroAsiento(a.getNumeroAsiento())
                .fila(a.getFila())
                .columna(a.getColumna())
                .piso(a.getPiso())
                .tipo(a.getTipo())
                .estado(a.getEstado())
                .precio(a.getPrecio())
                .build();
    }
}
