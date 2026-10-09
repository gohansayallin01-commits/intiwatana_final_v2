package com.transporte.sistema.enums;

/**
 * Ciclo de una declaración de pago Yape registrada por el cliente.
 *
 * La declaración es SOLO evidencia de un pago declarado: no genera boleto ni
 * asiento vendido. Recién cuando la caja la aprueba (via {@code confirmarPago})
 * se emite el boleto y el QR de embarque.
 */
public enum EstadoDeclaracionPago {
    PENDIENTE_VERIFICACION,
    APROBADO,
    RECHAZADO
}