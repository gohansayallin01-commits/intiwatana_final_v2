package com.transporte.sistema.enums;

public enum EstadoEncomienda {
    SOLICITUD_PENDIENTE,    // Solicitud del portal cliente: aún NO es recepción oficial
    RECIBIDO,               // Recepción física confirmada por personal autorizado en sucursal origen
    EN_ALMACEN,             // En almacén esperando despacho
    CARGADO_EN_BUS,         // Cargado en el bus (despachado)
    EN_TRANSITO,            // En el bus, en camino (equivalente histórico de CARGADO_EN_BUS)
    LLEGADO_A_DESTINO,      // Descargado en la sucursal de destino
    EN_DESTINO,             // Llegó a sucursal destino
    LISTO_ENTREGA,          // Disponible para recoger
    ENTREGADO,              // Entregado al destinatario
    DEVUELTO,               // Devuelto al remitente
    PERDIDO                 // Extraviado (estado crítico)
}
