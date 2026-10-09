-- =============================================================================
-- INTIWATANA - Migracion V2: catalogos base
-- =============================================================================
-- Datos MINIMOS indispensable para que la aplicacion funcione. NO contiene
-- usuarios, sucursales, buses, rutas, viajes, encomiendas, pagos ni boletos:
-- esos son datos operativos y se cargan desde el panel administrativo.
--
-- Los roles se insertan sin id explicito (lo genera la secuencia) y son
-- idempotentes sobre la restriccion UNIQUE(nombre).
--
-- IMPORTANTE: NO se crea ningun usuario con contrasena conocida. El primer
-- administrador debe crearse por un procedimiento manual y seguro (ver
-- reports/DESPLIEGUE-PRODUCCION-INTIWATANA.md).
-- =============================================================================

INSERT INTO roles (nombre, descripcion) VALUES
    ('ROLE_ADMIN',               'Acceso total al sistema'),
    ('ROLE_CAJERO',              'Cajero de sucursal (rol legacy)'),
    ('ROLE_VENDEDOR',            'Vendedor de boletos y atencion en sucursal'),
    ('ROLE_OPERADOR_ENCOMIENDAS','Operador de encomiendas'),
    ('ROLE_CHOFER',              'Chofer de bus'),
    ('ROLE_CLIENTE',             'Cliente portal')
ON CONFLICT (nombre) DO NOTHING;
