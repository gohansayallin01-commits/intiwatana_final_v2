package com.transporte.sistema.service;

import com.transporte.sistema.dto.request.CrearPagoRequest;
import com.transporte.sistema.dto.response.PagoResponse;
import java.util.List;

public interface PagoService {
    PagoResponse procesarPago(CrearPagoRequest request);
    List<PagoResponse> listarTodos();
    PagoResponse obtenerPorId(Long id);
    PagoResponse actualizarPago(Long id, CrearPagoRequest request);
    void eliminarPago(Long id);
}