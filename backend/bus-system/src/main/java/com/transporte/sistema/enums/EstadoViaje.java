package com.transporte.sistema.enums;

public enum EstadoViaje {
    PROGRAMADO,     // Viaje creado, venta de boletos activa
    EN_CURSO,       // Bus en ruta
    FINALIZADO,     // Llegó al destino
    CANCELADO       // Cancelado por cualquier motivo
}
