package com.transporte.sistema.enums;

/**
 * Ciclo de vida comercial de una reserva (retención de asientos).
 *
 * Es un estado PROPIO de la reserva, distinto de {@link EstadoAsiento}
 * (situación física del asiento) y de {@link EstadoBoleto}/{@link EstadoPago}.
 *
 *   RETENIDO       -> asientos bloqueados temporalmente (aún sin decisión de pago)
 *   PENDIENTE_PAGO -> el comprador eligió reservar; falta pagar en caja/terminal
 *   CONFIRMADO     -> pago registrado realmente; se emitieron boletos y QR
 *   VENCIDO        -> expiró el tiempo de retención/pago; asientos liberados
 *   CANCELADO      -> liberada explícitamente por personal autorizado
 */
public enum EstadoRetencion {
    RETENIDO,
    PENDIENTE_PAGO,
    CONFIRMADO,
    VENCIDO,
    CANCELADO
}
