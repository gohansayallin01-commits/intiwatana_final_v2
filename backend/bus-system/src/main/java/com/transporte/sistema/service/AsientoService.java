package com.transporte.sistema.service;

import com.transporte.sistema.dto.request.ActualizarAsientoRequest;
import com.transporte.sistema.dto.response.AsientoResponse;

public interface AsientoService {
    AsientoResponse actualizar(Long id, ActualizarAsientoRequest request);
    AsientoResponse obtenerPorId(Long id);
}
