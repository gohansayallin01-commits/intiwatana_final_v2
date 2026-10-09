package com.transporte.sistema.service;

import com.transporte.sistema.dto.request.ConfirmarReservaRequest;
import com.transporte.sistema.dto.request.CrearRetencionRequest;
import com.transporte.sistema.dto.response.BoletoResponse;
import com.transporte.sistema.dto.response.RecuperacionResponse;
import com.transporte.sistema.dto.response.RetencionResponse;
import com.transporte.sistema.enums.EstadoRetencion;
import com.transporte.sistema.enums.MetodoPago;

import java.util.List;

/**
 * Flujo comercial real: retención de asientos -> reserva pendiente -> pago
 * confirmado por caja -> boleto/QR. Una sola disponibilidad compartida por la
 * web y la venta presencial.
 */
public interface VentaService {

    /** Crea una retención temporal de asientos (web/invitado). */
    RetencionResponse crearRetencion(CrearRetencionRequest request);

    /** Consulta segura de una reserva (requiere el documento del comprador). */
    RetencionResponse obtenerPorReferencia(String referencia, String documento);

    /** Reserva la retención como PENDIENTE_PAGO (se paga en caja). */
    RetencionResponse reservarPendientePago(String referencia, MetodoPago metodoPago);

    /** Registra el pago real y emite boletos + QR (ADMIN/CAJERO). Idempotente. */
    List<BoletoResponse> confirmarPago(String referencia, ConfirmarReservaRequest request, String username);

    /** Libera explícitamente una reserva no confirmada (ADMIN/CAJERO). */
    RetencionResponse liberar(String referencia, String motivo, String username);

    /** Lista reservas por estado (ADMIN/CAJERO). */
    List<RetencionResponse> listarReservas(List<EstadoRetencion> estados);

    /** Recupera reservas/boletos por documento, sin necesidad de cuenta. */
    RecuperacionResponse recuperar(String documento, String referencia);

    /** Constancia PDF de una reserva pendiente (sin QR de embarque). */
    byte[] constanciaPdf(String referencia, String documento);

    /** Boleto PDF de una venta confirmada (con QR de embarque). */
    byte[] boletoPdf(String numeroBoleto, String documento);

    /** Libera reservas vencidas (mantenimiento / auditoría). Devuelve cuántas liberó. */
    int liberarVencidas();
}
