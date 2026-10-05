package com.transporte.sistema.service;

import com.transporte.sistema.dto.request.BoletoRequest;
import com.transporte.sistema.dto.response.BoletoResponse;
import com.transporte.sistema.entity.Boleto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface BoletoService {
    /**
     * @param username usuario del JWT: se usa para exigir que el boleto pertenezca
     *                 a un viaje asignado al chofer autenticado (ROLE_ADMIN y el
     *                 resto de roles conservan el acceso que ya tenían).
     */
    BoletoResponse vender(BoletoRequest request);
    BoletoResponse obtenerPorId(Long id, String username);
    BoletoResponse obtenerPorNumero(String numeroBoleto, String username);
    BoletoResponse escanearQr(String codigoQr, String username);
    BoletoResponse cancelar(Long id, String motivo);
    BoletoResponse cancelarPorUsuario(Long id, String motivo, String username);
    Page<BoletoResponse> listarPorCliente(Long clienteId, Pageable pageable);
    Page<BoletoResponse> listarPorViaje(Long viajeId, Pageable pageable, String username);

    // Portal cliente
    List<BoletoResponse> misBoletos(String username);
    BoletoResponse comprarPasaje(BoletoRequest request, String username);

    /** Mapea una entidad Boleto a su DTO de respuesta (compartido). */
    BoletoResponse toResponse(Boleto boleto);
}
