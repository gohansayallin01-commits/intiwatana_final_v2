package com.transporte.sistema.service;

import com.transporte.sistema.dto.request.SucursalRequest;
import com.transporte.sistema.dto.response.SucursalResponse;
import java.util.List;

public interface SucursalService {
    SucursalResponse crear(SucursalRequest request);
    SucursalResponse actualizar(Long id, SucursalRequest request);
    SucursalResponse obtenerPorId(Long id);
    List<SucursalResponse> listarActivas();
    List<SucursalResponse> listarTerminales();
    void eliminar(Long id);
}