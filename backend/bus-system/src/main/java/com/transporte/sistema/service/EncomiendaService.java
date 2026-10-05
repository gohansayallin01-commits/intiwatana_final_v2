package com.transporte.sistema.service;

import com.transporte.sistema.dto.request.ActualizarEstadoEncomiendaRequest;
import com.transporte.sistema.dto.request.ConfirmarRecepcionEncomiendaRequest;
import com.transporte.sistema.dto.request.EncomiendaClienteRequest;
import com.transporte.sistema.dto.request.EncomiendaRequest;
import com.transporte.sistema.dto.response.EncomiendaResponse;
import com.transporte.sistema.dto.response.MovimientoEncomiendaResponse;
import com.transporte.sistema.dto.response.SolicitudPendienteResponse;
import com.transporte.sistema.dto.response.TrackingEncomiendaResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;

public interface EncomiendaService {
    /** Registro presencial en ventanilla: crea y oficializa en una sola operación. */
    EncomiendaResponse registrar(EncomiendaRequest request);
    EncomiendaResponse obtenerPorId(Long id);
    EncomiendaResponse obtenerPorGuia(String numeroGuia);
    EncomiendaResponse actualizarEstado(Long id, ActualizarEstadoEncomiendaRequest request);
    List<EncomiendaResponse> listarTodas();
    Page<EncomiendaResponse> listarPorRemitente(Long remitenteId, Pageable pageable);
    Page<EncomiendaResponse> listarPorSucursalOrigen(Long sucursalId, Pageable pageable);

    // Seguimiento público: vista reducida sin datos personales
    TrackingEncomiendaResponse consultarTracking(String numeroGuia);

    // Portal cliente
    List<EncomiendaResponse> misEncomiendas(String username);
    List<MovimientoEncomiendaResponse> obtenerMovimientos(Long encomiendaId, String username);

    // Solicitud desde portal cliente autenticado
    EncomiendaResponse solicitarEncomienda(EncomiendaClienteRequest request, String username);

    /**
     * Confirmación de recepción física por personal autorizado: fija peso, medidas,
     * volumen, tarifa y pago, emite la guía GUI y pasa a RECIBIDO.
     * Es idempotente: si la encomienda ya fue oficializada devuelve el estado
     * confirmado sin duplicar pago, guía ni movimiento.
     */
    EncomiendaResponse confirmarRecepcion(Long id, ConfirmarRecepcionEncomiendaRequest request);

    /**
     * Solicitudes pendientes recuperables por el personal autorizado, con datos
     * personales enmascarados.
     */
    Page<SolicitudPendienteResponse> listarSolicitudesPendientes(String q,
                                                                 Long sucursalOrigenId,
                                                                 Long sucursalDestinoId,
                                                                 LocalDateTime desde,
                                                                 LocalDateTime hasta,
                                                                 Pageable pageable);

    /** Busca una solicitud pendiente por su referencia SOL. */
    SolicitudPendienteResponse buscarSolicitudPorReferencia(String referenciaSolicitud);
}