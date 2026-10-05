package com.transporte.sistema.enums;

public enum EstadoAsiento {
    DISPONIBLE,
    RESERVADO,  // Reserva temporal (sin pagar aún)
    VENDIDO,    // Pagado y confirmado
    BLOQUEADO   // No disponible (avería, chofer, etc.)
}
