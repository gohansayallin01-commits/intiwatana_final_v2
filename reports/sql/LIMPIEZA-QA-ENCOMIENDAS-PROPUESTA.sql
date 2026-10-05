-- =============================================================================
-- LIMPIEZA / SEPARACION DE DATOS QA - MODULO ENCOMIENDAS Y TRACKING
-- =============================================================================
-- NO EJECUTAR SIN RESPALDO Y AUTORIZACION EXPRESA.
--
-- Este script es una PROPUESTA revisable. Por defecto TERMINA EN ROLLBACK, es
-- decir, no persiste ningun cambio. Para aplicar la separacion, un responsable
-- autorizado debe:
--   1) Tomar un respaldo completo de la base (pg_dump).
--   2) Revisar la seccion de PREVISUALIZACION y confirmar los ids afectados.
--   3) Cambiar la ultima linea  ROLLBACK;  por  COMMIT;  y ejecutar.
--
-- No usa DROP, TRUNCATE ni DELETE masivo. La separacion es REVERSIBLE:
--   - Copia los registros QA a tablas de archivo.
--   - Marca las filas con la bandera es_qa = TRUE.
-- Ningun registro se borra. El borrado fisico queda COMENTADO y deshabilitado.
--
-- Base de datos de referencia: transporte_bd (PostgreSQL 17)
-- Fecha de propuesta: 2026-10-02
-- =============================================================================

\set ON_ERROR_STOP on

BEGIN;

-- -----------------------------------------------------------------------------
-- 0. GUARDA: abortar si ya existe una marca previa (evita doble ejecucion)
-- -----------------------------------------------------------------------------
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'encomiendas' AND column_name = 'es_qa'
    ) THEN
        RAISE EXCEPTION 'La columna encomiendas.es_qa ya existe. Revise el estado antes de volver a ejecutar.';
    END IF;
END $$;

-- -----------------------------------------------------------------------------
-- 1. PREVISUALIZACION (solo lectura) - datos QA claramente identificados
-- -----------------------------------------------------------------------------
-- Candidatas QA: 23, 25, 26, 27, 28, 29, 31, 33, 35, 36, 37, 38
SELECT e.id, e.numero_guia, e.estado, e.costo, e.peso_kg,
       e.destinatario_nombre, e.observaciones
FROM encomiendas e
WHERE e.id IN (23,25,26,27,28,29,31,33,35,36,37,38)
ORDER BY e.id;

-- Conteo de dependencias que se copiaran junto con las encomiendas QA.
SELECT 'movimientos' AS tabla, COUNT(*) AS filas
FROM movimientos_encomienda m
WHERE m.encomienda_id IN (23,25,26,27,28,29,31,33,35,36,37,38)
UNION ALL
SELECT 'pagos', COUNT(*)
FROM pagos p
WHERE p.id IN (
    SELECT e.pago_id FROM encomiendas e
    WHERE e.id IN (23,25,26,27,28,29,31,33,35,36,37,38) AND e.pago_id IS NOT NULL
);

-- -----------------------------------------------------------------------------
-- 2. RESPALDO EN TABLAS DE ARCHIVO (copias, no mueve datos)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS encomiendas_qa_archivo
    (LIKE encomiendas INCLUDING ALL);

CREATE TABLE IF NOT EXISTS movimientos_encomienda_qa_archivo
    (LIKE movimientos_encomienda INCLUDING ALL);

CREATE TABLE IF NOT EXISTS pagos_qa_archivo
    (LIKE pagos INCLUDING ALL);

-- Guarda: abortar si ya hay copias para estos ids (evita duplicar respaldo).
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM encomiendas_qa_archivo WHERE id IN (23,25,26,27,28,29,31,33,35,36,37,38)) THEN
        RAISE EXCEPTION 'Ya existen copias QA en encomiendas_qa_archivo. Revise antes de continuar.';
    END IF;
END $$;

INSERT INTO encomiendas_qa_archivo
SELECT * FROM encomiendas WHERE id IN (23,25,26,27,28,29,31,33,35,36,37,38);

INSERT INTO movimientos_encomienda_qa_archivo
SELECT * FROM movimientos_encomienda
WHERE encomienda_id IN (23,25,26,27,28,29,31,33,35,36,37,38);

INSERT INTO pagos_qa_archivo
SELECT * FROM pagos
WHERE id IN (
    SELECT e.pago_id FROM encomiendas e
    WHERE e.id IN (23,25,26,27,28,29,31,33,35,36,37,38) AND e.pago_id IS NOT NULL
);

-- -----------------------------------------------------------------------------
-- 3. MARCADO (separacion reversible)
-- -----------------------------------------------------------------------------
ALTER TABLE encomiendas ADD COLUMN IF NOT EXISTS es_qa BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE encomiendas
SET es_qa = TRUE
WHERE id IN (23,25,26,27,28,29,31,33,35,36,37,38);

-- -----------------------------------------------------------------------------
-- 4. OPCIONAL (DESHABILITADO): borrado fisico
--    Descomentar SOLO con respaldo verificado y autorizacion expresa.
--    Se mantiene apagado por politica de este cierre.
-- -----------------------------------------------------------------------------
-- DELETE FROM movimientos_encomienda
--  WHERE encomienda_id IN (23,25,26,27,28,29,31,33,35,36,37,38);
-- DELETE FROM encomiendas
--  WHERE id IN (23,25,26,27,28,29,31,33,35,36,37,38);

-- -----------------------------------------------------------------------------
-- 5. VERIFICACION POSTERIOR (dentro de la transaccion)
-- -----------------------------------------------------------------------------
SELECT COUNT(*) AS encomiendas_marcadas_qa FROM encomiendas WHERE es_qa = TRUE;
SELECT COUNT(*) AS copias_en_archivo FROM encomiendas_qa_archivo;

-- -----------------------------------------------------------------------------
-- 6. CIERRE
--    ROLLBACK por defecto => NO persiste nada.
--    Cambie a COMMIT; solo tras revisar y obtener autorizacion.
-- -----------------------------------------------------------------------------
ROLLBACK;
