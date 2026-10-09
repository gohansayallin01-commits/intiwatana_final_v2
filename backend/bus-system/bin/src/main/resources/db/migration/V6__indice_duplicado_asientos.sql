-- ============================================================================
-- V6 - INDICE DUPLICADO EN ASIENTOS
--
-- La tabla asientos tiene DOS restricciones UNIQUE sobre las mismas columnas:
--   * asientos_viaje_id_numero_asiento_key  (constraint sin nombre explicito)
--   * uk_asiento_viaje_numero               (constraint con nombre explicito)
-- Ambas garantizan exactamente lo mismo: no repetir (viaje_id, numero_asiento).
--
-- CONSECUENCIA PRACTICA: no es un fallo funcional (la garantia de unicidad se
-- cumple igual), pero si es deuda real del esquema:
--   1. Cada escritura mantiene DOS indices identicos: coste duplicado en
--      insert/update y en almacenamiento, y dos estructuras que VACUUM mantiene.
--   2. INSERT ... ON CONFLICT sin columna objetivo puede resolver contra la
--      constraint inesperada, y el mapeo de JPA puede discrepar de PostgreSQL.
-- 3. El coste no se ve al leer el codigo: solo aparece al inspeccionar el DDL.
--
-- Se conserva la constraint CON NOMBRE (uk_asiento_viaje_numero) porque es la
-- que el codigo y las consultas pueden referenciar de forma explicita, y se
-- elimina la duplicada sin nombre.
--
-- Migracion incremental, idempotente y no destructiva: solo elimina la
-- constraint duplicada. No descarta ni recrea la tabla asientos, no toca datos
-- y no altera el indice de la constraint conservada.
-- ============================================================================

DO $$
DECLARE
    objetivo  constant regclass := 'asientos'::regclass;
    duplicada text;
    dep       oid;
    fk_oid    oid;
    elimina   integer := 0;
    dependencias integer;
BEGIN
    -- Constraints UNIQUE de asientos cuyas columnas son EXACTAMENTE
    -- (viaje_id, numero_asiento). El nombre es el que.postgres genera al no
    -- declararlo en el DDL, asi que no se puede suponer: se detecta.
    FOR duplicada IN
        SELECT c.conname
        FROM pg_constraint c
        WHERE c.conrelid = objetivo
          AND c.contype = 'u'
          AND (
            SELECT array_agg(a.attname::text ORDER BY k.ord)
            FROM unnest(c.conkey) WITH ORDINALITY AS k(attnum, ord)
            JOIN pg_attribute a ON a.attrelid = objetivo AND a.attnum = k.attnum
          ) = ARRAY['viaje_id', 'numero_asiento']
        -- Se conservan las de nombre explicito; se elimina la de nombre automatico.
        AND c.conname NOT LIKE 'uk\_%'
        AND c.conname NOT LIKE 'uq\_%'
    LOOP
        SELECT oid INTO dep FROM pg_constraint
        WHERE conrelid = objetivo AND conname = duplicada;

        -- Si alguna FK depende de ESTA constraint, no se elimina: dejarla huerfana
        -- invalidaria la FK. Se comprueba con pg_depend, que es la fuente real.
        dependencias := 0;
        FOR fk_oid IN
            SELECT c.oid FROM pg_constraint c
            WHERE c.contype = 'f' AND c.confrelid = objetivo
        LOOP
            IF EXISTS (
                SELECT 1 FROM pg_depend d
                WHERE d.classid = 'pg_constraint'::regclass
                  AND d.refclassid = 'pg_constraint'::regclass
                  AND d.objid = fk_oid
                  AND d.refobjid = dep
            ) THEN
                dependencias := dependencias + 1;
            END IF;
        END LOOP;

        IF dependencias > 0 THEN
            RAISE NOTICE 'V6: % tiene % FK dependiente(s): se conserva.',
                duplicada, dependencias;
        ELSE
            EXECUTE format('ALTER TABLE asientos DROP CONSTRAINT %I', duplicada);
            elimina := elimina + 1;
            RAISE NOTICE 'V6: constraint duplicada % eliminada.', duplicada;
        END IF;
    END LOOP;

    IF elimina = 0 THEN
        RAISE NOTICE 'V6: no habia constraint duplicada que eliminar.';
    END IF;
END $$;

-- Verificacion posterior: debe quedar exactamente UNA UNIQUE sobre
-- (viaje_id, numero_asiento). Si la eliminacion no surtio efecto, la migracion
-- falla aqui en lugar de dejar un esquema con dos indices sin avisar.
DO $$
DECLARE
    unicas integer;
BEGIN
    SELECT count(*) INTO unicas
    FROM pg_constraint c
    WHERE c.conrelid = 'asientos'::regclass
      AND c.contype = 'u'
      AND (
        SELECT array_agg(a.attname::text ORDER BY k.ord)
        FROM unnest(c.conkey) WITH ORDINALITY AS k(attnum, ord)
        JOIN pg_attribute a ON a.attrelid = 'asientos'::regclass AND a.attnum = k.attnum
      ) = ARRAY['viaje_id', 'numero_asiento'];

    IF unicas <> 1 THEN
        RAISE EXCEPTION
            'V6: se esperaba 1 UNIQUE sobre (viaje_id, numero_asiento) y hay %', unicas;
    END IF;
END $$;
