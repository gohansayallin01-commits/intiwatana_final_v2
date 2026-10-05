package com.transporte.sistema.enums;

public enum EstadoBoleto {
    ACTIVO,         // Boleto válido
    USADO,          // Ya fue escaneado/abordó
    CANCELADO,      // Cancelado con posible reembolso
    REEMBOLSADO     // Monto devuelto al cliente
}
