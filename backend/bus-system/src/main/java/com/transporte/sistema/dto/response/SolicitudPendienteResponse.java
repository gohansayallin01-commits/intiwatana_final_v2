package com.transporte.sistema.dto.response;

import com.transporte.sistema.enums.EstadoEncomienda;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Vista de una SOLICITUD PENDIENTE para el panel del cajero.
 *
 * Muestra lo que el cliente declaró y lo indispensable para atenderla, con los
 * datos personales enmascarados (documento y teléfono). No expone internals de
 * la base (auditoría, ids internos de pago) ni datos de otros clientes.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolicitudPendienteResponse {

    private Long id;
    /** Referencia SOL visible para el personal autorizado. */
    private String referenciaSolicitud;
    private EstadoEncomienda estado;
    private String remitenteNombre;
    /** Documento del remitente enmascarado (****1234). */
    private String remitenteDocumento;
    /** Teléfono del remitente enmascarado. */
    private String remitenteTelefono;
    private String destinatarioNombre;
    /** Teléfono del destinatario declarado por el cliente, enmascarado. */
    private String destinatarioTelefono;
    private SucursalResponse sucursalOrigen;
    private SucursalResponse sucursalDestino;
    private String descripcionContenido;
    private String tipoEnvio;
    private String cuidadoEspecial;
    private String observaciones;
    private LocalDateTime createdAt;
    /** Antigüedad legible de la solicitud (p. ej. "hace 2 días"). */
    private String antiguedad;
    /** Siempre null: una solicitud pendiente no tiene guía oficial. */
    private String numeroGuia;
    /** Siempre null: la tarifa la fija el personal al recibir el paquete. */
    private BigDecimal costo;
}