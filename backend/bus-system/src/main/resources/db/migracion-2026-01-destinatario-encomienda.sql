-- =============================================================================
-- Migración: destinatario de encomiendas del portal web
-- =============================================================================
-- Fecha    : 2026-01
-- Autor    : auditoría INTIWATANA
-- Motor    : PostgreSQL 14+
-- Ámbito   : SOLO estructura. No modifica ni borra datos de negocio.
--
-- POR QUÉ
-- --------
-- La entidad `Encomienda` tenía:
--     @JoinColumn(name = "destinatario_id", nullable = false)
--     ...
--     .destinatario(remitente)   // placeholder
-- Es decir, el servicio guardaba al REMITENTE en el campo destinatario porque
-- el portal web sólo pide el nombre y teléfono de quien recibe, y eso no es un
-- Cliente del sistema. Dos consecuencias:
--   1. Todos los reportes mostraban al emisor como si fuera el destinatario.
--   2. La columna no podía ser NOT NULL si se quería representar al destinatario
--      real de una encomienda creada desde el portal.
--
-- La solución aplicada en el código: `destinatario_id` pasa a ser NULLABLE y se
-- añade `destinatario_nombre` para el caso de destinatario no registrado.
--
-- REQUISITOS ANTES DE EJECUTAR
-- ----------------------------
--   1. Tener el respaldo verificado en
--      database/intiwatana_transporte_bd.backup
--   2. Haber revisado el estado real de la tabla con las consultas de
--      verificación de la sección final de este archivo.
--
-- La migración es IDEMPOTENTE: puede ejecutarse más de una vez sin efecto
-- adicional, y es reversible (ver sección "REVERSIÓN").
-- =============================================================================

BEGIN;

-- -----------------------------------------------------------------------------
-- 1. Verificación previa: qué estado tiene hoy la tabla
-- -----------------------------------------------------------------------------
\echo '--- Estado actual de encomiendas ---'
SELECT
    COUNT(*)                                        AS total_encomiendas,
    COUNT(*) FILTER (WHERE destinatario_id IS NULL)  AS sin_destinatario_id,
    COUNT(*) FILTER (WHERE destinatario_id IS NOT NULL) AS con_destinatario_id,
    COUNT(*) FILTER (
        WHERE destinatario_id IS NOT NULL
          AND remitente_id IS NOT NULL
          AND destinatario_id = remitente_id
    ) AS destinatario_igual_a_remitente
FROM encomiendas;

\echo '--- Restricción actual sobre destinatario_id ---'
SELECT con.conname,
       pg_get_constraintdef(con.oid) AS definicion
FROM pg_constraint con
JOIN pg_class rel ON rel.oid = con.conrelid
WHERE rel.relname = 'encomiendas'
  AND con.contype = 'c'
  AND pg_get_constraintdef(con.oid) ILIKE '%destinatario_id%';

\echo '--- ¿Existe ya la columna destinatario_nombre? ---'
SELECT column_name, data_type, character_maximum_length, is_nullable
FROM information_schema.columns
WHERE table_name = 'encomiendas'
  AND column_name = 'destinatario_nombre';

-- -----------------------------------------------------------------------------
-- 2. Añadir la columna (no-op si ya existe)
-- -----------------------------------------------------------------------------
ALTER TABLE encomiendas
    ADD COLUMN IF NOT EXISTS destinatario_nombre VARCHAR(200);

-- -----------------------------------------------------------------------------
-- 3. Relajar el NOT NULL de destinatario_id (no-op si ya es nullable)
-- -----------------------------------------------------------------------------
ALTER TABLE encomiendas
    ALTER COLUMN destinatario_id DROP NOT NULL;

-- -----------------------------------------------------------------------------
-- 4. Índice parcial: sólo tiene sentido el nombre cuando no hay Cliente asociado
-- -----------------------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_encomiendas_destinatario_nombre
    ON encomiendas (destinatario_nombre)
    WHERE destinatario_nombre IS NOT NULL;

COMMENT ON COLUMN encomiendas.destinatario_nombre IS
    'Nombre del destinatario cuando no está registrado como Cliente (portal web)';
COMMENT ON COLUMN encomiendas.destinatario_id IS
    'Cliente destinatario. NULL en encomiendas creadas desde el portal web.';

-- -----------------------------------------------------------------------------
-- 5. Copiar el nombre real cuando el destinatario SÍ es un Cliente.
--    Sólo rellena los registros con nombre todavía vacío: no sobrescribe nada.
-- -----------------------------------------------------------------------------
UPDATE encomiendas e
   SET destinatario_nombre = c.nombres || ' ' || c.apellidos
  FROM clientes c
 WHERE e.destinatario_id = c.id
   AND (e.destinatario_nombre IS NULL OR e.destinatario_nombre = '')
   AND c.nombres IS NOT NULL;

-- -----------------------------------------------------------------------------
-- 6. Verificación posterior
-- -----------------------------------------------------------------------------
\echo '--- Resultado tras la migración ---'
SELECT
    COUNT(*)                                           AS total,
    COUNT(*) FILTER (WHERE destinatario_id IS NULL)     AS sin_cliente_destinatario,
    COUNT(*) FILTER (WHERE destinatario_nombre IS NOT NULL) AS con_nombre_destinatario,
    COUNT(*) FILTER (WHERE destinatario_nombre IS NULL) AS sin_nombre_destinatario
FROM encomiendas;

-- Si hay registros con destinatario_id NULL y destinatario_nombre NULL, el
-- nombre del destinatario solo existe dentro de `observaciones` (así lo guarda
-- el código anterior). Hay que revisarlos a mano:
\echo '--- Requieren revisión manual (sin id y sin nombre) ---'
SELECT id, numero_guia, remitente_id, observaciones
FROM encomiendas
WHERE destinatario_id IS NULL
  AND (destinatario_nombre IS NULL OR destinatario_nombre = '')
ORDER BY id;

-- -----------------------------------------------------------------------------
-- 7. REPARACIÓN DE DATOS HISTÓRICOS (OPCIONAL — revisar antes de activar)
-- -----------------------------------------------------------------------------
-- Las encomiendas creadas antes de la corrección guardaron al REMITENTE como
-- destinatario, y el nombre real quedó dentro de `observaciones`, con el formato:
--     Destinatario: JUAN PEREZ | Tel: 999888777 | notas del remitente
-- El paso 5 no las corrige porque para ellas `destinatario_id` SÍ apunta a un
-- Cliente: el equivocado.
--
-- Se deja desactivado por defecto porque implica analizar texto libre. Ejecute
-- primero el SELECT de PREVISUALIZACIÓN y confirme que los nombres extraídos son
-- correctos; solo entonces habilite el bloque UPDATE.
--
-- PREVISUALIZACIÓN (no modifica nada):
--
--   SELECT e.id,
--          e.numero_guia,
--          c.nombres || ' ' || c.apellidos AS nombre_actual,
--          trim(split_part(split_part(e.observaciones, '|', 1), 'Destinatario:', 2))
--            AS nombre_real_detectado
--     FROM encomiendas e
--     JOIN clientes c ON c.id = e.destinatario_id
--    WHERE e.remitente_id = e.destinatario_id
--      AND e.observaciones LIKE 'Destinatario:%'
--    ORDER BY e.id;
--
-- APLICAR (tras revisar la previsualización):
--
--   BEGIN;
--     UPDATE encomiendas e
--        SET destinatario_nombre = trim(split_part(split_part(e.observaciones, '|', 1), 'Destinatario:', 2))
--       FROM clientes c
--      WHERE c.id = e.destinatario_id
--        AND e.remitente_id = e.destinatario_id
--        AND e.observaciones LIKE 'Destinatario:%'
--        AND trim(split_part(split_part(e.observaciones, '|', 1), 'Destinatario:', 2)) <> ''
--        AND (e.destinatario_nombre IS NULL OR e.destinatario_nombre = '');
--   COMMIT;
--
-- NO se borra la columna ni se altera el id del destinatario: se conserva el
-- dato original por trazabilidad y se registra el nombre real en el campo nuevo.

COMMIT;

-- =============================================================================
-- REVERSIÓN
-- =============================================================================
-- BEGIN;
--   DELETE FROM encomiendas WHERE destinatario_id IS NULL;  -- ¡DESTRUCTIVO!
--   DROP INDEX IF EXISTS idx_encomiendas_destinatario_nombre;
--   ALTER TABLE encomiendas DROP COLUMN IF EXISTS destinatario_nombre;
--   UPDATE encomiendas SET destinatario_id = remitente_id
--    WHERE destinatario_id IS NULL;                        -- ¡DESTRUCTIVO!
--   ALTER TABLE encomiendas ALTER COLUMN destinatario_id SET NOT NULL;
-- COMMIT;
--
-- La reversión solo es posible si NO hay encomiendas de portal registradas
-- (porque su destinatario no es un Cliente y no se puede reconstruir el id).
-- =============================================================================
