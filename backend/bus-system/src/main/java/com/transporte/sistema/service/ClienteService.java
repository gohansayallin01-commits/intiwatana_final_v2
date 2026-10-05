package com.transporte.sistema.service;

import com.transporte.sistema.dto.request.ClienteRequest;
import com.transporte.sistema.dto.request.RegistroClienteRequest;
import com.transporte.sistema.dto.response.ClienteResponse;
import com.transporte.sistema.dto.response.LoginResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ClienteService {
    ClienteResponse crear(ClienteRequest request);
    ClienteResponse actualizar(Long id, ClienteRequest request);
    ClienteResponse obtenerPorId(Long id);
    ClienteResponse obtenerPorDniRuc(String dniRuc);
    Page<ClienteResponse> buscar(String query, Pageable pageable);
    Page<ClienteResponse> listarTodos(Pageable pageable);
    void eliminar(Long id);

    // ── Portal self-service ──────────────────────────────
    /** Registro público: crea Cliente + Usuario(ROLE_CLIENTE) y devuelve JWT */
    LoginResponse registrarConCuenta(RegistroClienteRequest request);

    /** Perfil del cliente autenticado (por username del JWT) */
    ClienteResponse obtenerPorUsername(String username);
}
