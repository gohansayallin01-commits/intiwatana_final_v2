-- ============================================================================
-- V5 - ENCOMIENDAS: separacion de identificadores SOL / GUI y datos oficiales
--
-- Cierre funcional del flujo de encomiendas:
--   * La solicitud web se identifica con una referencia SOL y NO tiene guia
--     oficial, ni peso, ni dimensiones, ni tarifa, ni metodo de pago, ni pago.
--   * La guia oficial GUI solo existe cuando el personal autorizado confirma la
--     recepcion fisica, el peso, las dimensiones, la tarifa y el pago.
--
-- Migracion incremental y no destructiva: no se recrea ni se descarta ninguna
-- tabla del modulo de encomiendas ni de los modulos de pasajes.
-- ============================================================================

-- ── 1) Nuevas columnas ───────────────────────────────────────────────────────

ALTER TABLE encomiendas ADD COLUMN IF NOT EXISTS referencia_solicitud   character varying(30);
ALTER TABLE encomiendas ADD COLUMN IF NOT EXISTS remitente_telefono    character varying(20);
ALTER TABLE encomiendas ADD COLUMN IF NOT EXISTS clave_idempotencia      character varying(60);
ALTER TABLE encomiendas ADD COLUMN IF NOT EXISTS largo_cm               numeric(8,2);
ALTER TABLE encomiendas ADD COLUMN IF NOT EXISTS ancho_cm               numeric(8,2);
ALTER TABLE encomiendas ADD COLUMN IF NOT EXISTS alto_cm                numeric(8,2);

COMMENT ON COLUMN encomiendas.referencia_solicitud IS
    'Referencia interna de la solicitud previa (SOL-AAAA-NNNNNN). No es guia oficial y no habilita tracking.';
COMMENT ON COLUMN encomiendas.clave_idempotencia IS
    'Clave opcional de idempotencia del registro presencial: evita guia y pago duplicados al reintentar el alta.';
COMMENT ON COLUMN encomiendas.numero_guia IS
    'Guia oficial (GUI-AAAAMMDD-NNNNNN). Solo existe tras confirmar recepcion, medidas, tarifa y pago.';
COMMENT ON COLUMN encomiendas.remitente_telefono IS
    'Telefono de contacto informado por el remitente para este envio.';
COMMENT ON COLUMN encomiendas.largo_cm IS
    'Largo oficial medido en agencia al recibir el paquete.';
COMMENT ON COLUMN encomiendas.ancho_cm IS
    'Ancho oficial medido en agencia al recibir el paquete.';
COMMENT ON COLUMN encomiendas.alto_cm IS
    'Alto oficial medido en agencia al recibir el paquete.';

-- ── 2) Backfill de solicitudes pendientes preexistentes ──────────────────────
-- El flujo anterior generaba una guia GUI al crear la solicitud, antes de que
-- existiera recepcion fisica ni pago, y permitia capturar peso y costo. Esos
-- valores nunca fueron oficiales: el paquete todavia no ingresaba a la agencia.
-- A esas filas se les asigna su referencia SOL y se liberan TODOS los datos
-- oficiales (guia, peso, volumen, costo, metodo de pago y pago) para que la
-- invariante de la seccion 4 sea cierta. No se modifica ninguna fila que ya
-- tenga recepcion confirmada (estados distintos de SOLICITUD_PENDIENTE) y no se
-- elimina informacion de ninguna encomienda.

WITH pendientes AS (
    SELECT e.id
    FROM encomiendas e
    WHERE e.estado = 'SOLICITUD_PENDIENTE'
      AND (e.referencia_solicitud IS NULL
        OR e.numero_guia IS NOT NULL
        OR e.peso_kg IS NOT NULL
        OR e.volumen_m3 IS NOT NULL
        OR e.costo IS NOT NULL
        OR e.metodo_pago IS NOT NULL
        OR e.pago_id IS NOT NULL)
)
UPDATE encomiendas e
SET referencia_solicitud = 'SOL-' || to_char(coalesce(e.created_at, now()), 'YYYY')
                         || '-' || lpad(e.id::text, 6, '0'),
    numero_guia          = NULL,
    peso_kg              = NULL,
    volumen_m3           = NULL,
    costo                = NULL,
    metodo_pago          = NULL,
    pago_id              = NULL,
    updated_at           = now()
WHERE e.id IN (SELECT id FROM pendientes);

-- Secuencia para emitir la parte numerica de la referencia SOL. Se genera ANTES
-- del INSERT porque la referencia es obligatoria desde que nace la solicitud, y
-- se continua por encima del id maximo para no chocar con el backfill anterior.
CREATE SEQUENCE IF NOT EXISTS seq_encomienda_sol START WITH 1 INCREMENT BY 1;

SELECT setval('seq_encomienda_sol',
              GREATEST(COALESCE((SELECT MAX(id) FROM encomiendas), 0), 1));

-- ── 3) Unicidad de la referencia SOL ─────────────────────────────────────────

CREATE UNIQUE INDEX IF NOT EXISTS uq_encomiendas_referencia_solicitud
    ON encomiendas (referencia_solicitud)
    WHERE referencia_solicitud IS NOT NULL;

-- La clave de idempotencia del registro presencial tambien es unica: dos
-- peticiones simultaneas con la misma clave no pueden crear dos encomiendas.
CREATE UNIQUE INDEX IF NOT EXISTS uq_encomiendas_clave_idempotencia
    ON encomiendas (clave_idempotencia)
    WHERE clave_idempotencia IS NOT NULL;

-- ── 4) Invariante de negocio a nivel de base de datos ───────────────────────
-- Una solicitud pendiente jamas puede tener datos oficiales ni guia. La regla se
-- valida tambien en base de datos para que ningun acceso directo (SQL, consola,
-- procesobatch) pueda dejar una solicitud "semiconfirmada".

ALTER TABLE encomiendas DROP CONSTRAINT IF EXISTS chk_encomienda_solicitud_sin_datos_oficiales;

ALTER TABLE encomiendas ADD CONSTRAINT chk_encomienda_solicitud_sin_datos_oficiales CHECK (
        estado <> 'SOLICITUD_PENDIENTE'
     OR ( referencia_solicitud IS NOT NULL
      AND numero_guia   IS NULL
      AND peso_kg       IS NULL
      AND volumen_m3    IS NULL
      AND largo_cm      IS NULL
      AND ancho_cm      IS NULL
      AND alto_cm       IS NULL
      AND costo         IS NULL
      AND metodo_pago   IS NULL
      AND pago_id       IS NULL )
);

-- ── 5) Unicidad de documento de cliente ─────────────────────────────────────
-- La creacion de remitentes y destinatarios desde el flujo de encomiendas debe
-- reutilizar siempre el mismo cliente. V1 no declaro unicidad sobre
-- clientes.dni_ruc, por lo que dos altas concurrentes del mismo documento
-- podían duplicar al cliente. El indice unico se crea solo si los datos
-- existentes lo permiten; si ya hubiera duplicados historicos no se destruye
-- ninguna informacion y el flujo sigue protegido por validacion de servicio.

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM clientes
        WHERE dni_ruc IS NOT NULL
        GROUP BY dni_ruc
        HAVING COUNT(*) > 1
    ) THEN
        EXECUTE 'CREATE UNIQUE INDEX IF NOT EXISTS uq_clientes_dni_ruc
                 ON clientes (dni_ruc) WHERE dni_ruc IS NOT NULL';
    END IF;
END $$;