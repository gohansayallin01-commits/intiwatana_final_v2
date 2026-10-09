-- ============================================================================
-- V8 - CONFIGURACION DE ASIENTOS POR VEHICULO
--
-- Cada bus define su propia distribucion de asientos (no por modelo). La
-- cantidad de asientos derivada de la configuracion debe coincidir con
-- capacidad_asientos; al generar los asientos de un viaje se usa esta
-- configuracion, de modo que el mapa muestre siempre la capacidad real.
--
-- La configuracion se guarda como JSON (texto). Los vehiculos existentes se
-- inicializan automaticamente al arrancar la aplicacion (backfill) con una
-- distribucion coherente con su capacidad; el administrador puede ajustarla.
--
-- Migracion incremental, idempotente y no destructiva: solo agrega la columna.
-- ============================================================================

ALTER TABLE buses ADD COLUMN IF NOT EXISTS configuracion_asientos TEXT;

COMMENT ON COLUMN buses.configuracion_asientos IS
    'JSON con la distribucion de asientos del vehiculo (pisos y asientos por fila)';
