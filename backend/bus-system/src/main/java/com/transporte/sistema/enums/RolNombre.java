package com.transporte.sistema.enums;

public enum RolNombre {
    ROLE_ADMIN,                  // Acceso total al sistema
    ROLE_CAJERO,                 // Vende boletos y registra encomiendas (rol legacy, se mantiene por compatibilidad)
    ROLE_VENDEDOR,               // Venta de boletos y atención en sucursal
    ROLE_OPERADOR_ENCOMIENDAS,   // Gestión operativa de encomiendas (carga/descarga/entrega)
    ROLE_CHOFER,                 // Consulta sus viajes y escanea boletos
    ROLE_CLIENTE                 // Portal de autoservicio
}
