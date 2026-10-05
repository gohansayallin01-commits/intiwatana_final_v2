package com.transporte.sistema.service;

import com.transporte.sistema.dto.request.ViajeRequest;
import com.transporte.sistema.dto.response.AsientoResponse;
import com.transporte.sistema.dto.response.ViajeResponse;
import com.transporte.sistema.enums.EstadoViaje;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;

public interface ViajeService {
    ViajeResponse crear(ViajeRequest request);
    ViajeResponse actualizar(Long id, ViajeRequest request);
    List<ViajeResponse> listar();
    ViajeResponse obtenerPorId(Long id);
    Page<ViajeResponse> buscarDisponibles(
            Long origenId, Long destinoId, LocalDateTime desde, LocalDateTime hasta, Pageable pageable);
    /**
     * Mapa de asientos del viaje. Si el usuario del JWT es ROLE_CHOFER solo se
     * permite consultar viajes asignados; ADMIN y el acceso anónimo de la compra
     * pública conservan el comportamiento actual.
     */
    List<AsientoResponse> obtenerAsientos(Long viajeId, String username);
    ViajeResponse cambiarEstado(Long id, EstadoViaje nuevoEstado, Long usuarioId);
    Page<ViajeResponse> listarPorChofer(Long choferId, Pageable pageable);
    List<ViajeResponse> listarPorChoferIdList(Long choferId);
    void eliminar(Long id);
}