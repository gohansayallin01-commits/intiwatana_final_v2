-- ============================================================
-- V9: PAGO SEGURO — RESERVA TEMPORAL, VERIFICACION YAPE, CIERRE
-- Aditiva. No modifica migraciones aplicadas.
-- ============================================================

-- ── Parámetros de reserva/pago configurables por el ADMIN ──────────────────
CREATE TABLE IF NOT EXISTS parametro_venta (
    parametro       VARCHAR(60)  PRIMARY KEY,
    valor           VARCHAR(100) NOT NULL,
    rango_min       INTEGER      NOT NULL,
    rango_max       INTEGER      NOT NULL,
    unidad          VARCHAR(20)  NOT NULL DEFAULT 'minutos',
    actualizado_por VARCHAR(60),
    motivo          VARCHAR(300),
    actualizado_en  TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS parametro_venta_historial (
    id             BIGSERIAL PRIMARY KEY,
    parametro      VARCHAR(60)  NOT NULL,
    valor_anterior VARCHAR(100),
    valor_nuevo    VARCHAR(100) NOT NULL,
    actualizado_por VARCHAR(60),
    rol            VARCHAR(30),
    motivo         VARCHAR(300),
    fecha          TIMESTAMP    NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_parametro_venta_historial
    ON parametro_venta_historial (parametro, fecha DESC);

-- Valores iniciales propuestos (rango acotado; solo el ADMIN puede cambiarlos).
INSERT INTO parametro_venta (parametro, valor, rango_min, rango_max, actualizado_por, motivo)
SELECT 'RETENCION_WEB_MINUTOS', '5',   3,   10, 'sistema', 'Valor inicial propuesto (matriz pago seguro)'
WHERE NOT EXISTS (SELECT 1 FROM parametro_venta WHERE parametro = 'RETENCION_WEB_MINUTOS');
INSERT INTO parametro_venta (parametro, valor, rango_min, rango_max, actualizado_por, motivo)
SELECT 'PAGO_PENDIENTE_MINUTOS', '30', 10,   60, 'sistema', 'Valor inicial propuesto (matriz pago seguro)'
WHERE NOT EXISTS (SELECT 1 FROM parametro_venta WHERE parametro = 'PAGO_PENDIENTE_MINUTOS');
INSERT INTO parametro_venta (parametro, valor, rango_min, rango_max, actualizado_por, motivo)
SELECT 'CIERRE_WEB_MINUTOS', '30', 15,  120, 'sistema', 'Valor inicial propuesto (matriz pago seguro)'
WHERE NOT EXISTS (SELECT 1 FROM parametro_venta WHERE parametro = 'CIERRE_WEB_MINUTOS');
INSERT INTO parametro_venta (parametro, valor, rango_min, rango_max, actualizado_por, motivo)
SELECT 'TOLERANCIA_YAPE_MINUTOS', '5', 0,   10, 'sistema', 'Valor inicial propuesto (matriz pago seguro)'
WHERE NOT EXISTS (SELECT 1 FROM parametro_venta WHERE parametro = 'TOLERANCIA_YAPE_MINUTOS');

-- ── Configuración del QR oficial Yape (solo ADMIN; historial de cambios) ──
CREATE TABLE IF NOT EXISTS configuracion_yape (
    id                  BIGSERIAL PRIMARY KEY,
    activo              BOOLEAN      NOT NULL DEFAULT false,
    receptor_nombre     VARCHAR(150),
    numero_visible      VARCHAR(30),
    sucursal_id         BIGINT,
    sucursal_nombre     VARCHAR(150),
    descripcion         VARCHAR(255),
    instrucciones       VARCHAR(500),
    esquema             VARCHAR(30),
    imagen_url          VARCHAR(300),
    imagen_nombre_interno VARCHAR(200),
    imagen_checksum     VARCHAR(64),
    imagen_peso_bytes   BIGINT,
    imagen_ancho        INTEGER,
    imagen_alto         INTEGER,
    vigente_desde       TIMESTAMP,
    vigente_hasta       TIMESTAMP,
    observaciones       VARCHAR(300),
    actualizado_por     VARCHAR(60),
    created_at          TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP    NOT NULL DEFAULT now(),
    deleted_at          TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_configuracion_yape_activo
    ON configuracion_yape (activo, updated_at DESC);
CREATE UNIQUE INDEX IF NOT EXISTS uq_configuracion_yape_unica_activa
    ON configuracion_yape (activo) WHERE activo = true;

CREATE TABLE IF NOT EXISTS configuracion_yape_historial (
    id              BIGSERIAL PRIMARY KEY,
    configuracion_id BIGINT NOT NULL,
    campo           VARCHAR(50)  NOT NULL,
    valor_anterior  TEXT,
    valor_nuevo     TEXT        NOT NULL,
    actualizado_por VARCHAR(60),
    rol             VARCHAR(30),
    motivo          VARCHAR(300),
    fecha           TIMESTAMP   NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_configuracion_yape_historial
    ON configuracion_yape_historial (configuracion_id, fecha DESC);

-- ── Declaración de pago Yape del cliente + verificación de caja ───────────
CREATE TABLE IF NOT EXISTS declaracion_pago (
    id              BIGSERIAL PRIMARY KEY,
    retencion_id    BIGINT       NOT NULL,
    operacion       VARCHAR(60)  NOT NULL,
    importe         NUMERIC(10,2) NOT NULL,
    fecha_pagada    TIMESTAMP,
    cuenta_pagada   VARCHAR(40),
    comprobante_url VARCHAR(300),
    comprobante_nombre_interno VARCHAR(200),
    comprobante_checksum VARCHAR(64),
    comprobante_mime VARCHAR(60),
    estado          VARCHAR(25)  NOT NULL,
    declarado_documento VARCHAR(15) NOT NULL,
    declarado_telefono VARCHAR(20),
    declarado_en    TIMESTAMP    NOT NULL DEFAULT now(),
    verificador_id  BIGINT,
    verificado_en   TIMESTAMP,
    motivo_rechazo  VARCHAR(300),
    observaciones   VARCHAR(300),
    activo          BOOLEAN      NOT NULL DEFAULT true,
    CONSTRAINT declaracion_pago_retencion_fk FOREIGN KEY (retencion_id) REFERENCES retenciones (id),
    CONSTRAINT declaracion_pago_estado_check CHECK (
        estado IN ('PENDIENTE_VERIFICACION','APROBADO','RECHAZADO'))
);
CREATE INDEX IF NOT EXISTS idx_declaracion_pago_estado
    ON declaracion_pago (estado, declarado_en DESC);
CREATE INDEX IF NOT EXISTS idx_declaracion_pago_documento
    ON declaracion_pago (declarado_documento, declarado_en DESC);
CREATE INDEX IF NOT EXISTS idx_declaracion_pago_operacion
    ON declaracion_pago (operacion);

-- ── Ventana única de tolerancia de verificación (no renovable) ─────────────
ALTER TABLE retenciones
    ADD COLUMN IF NOT EXISTS tolerancia_usada BOOLEAN NOT NULL DEFAULT false;

-- ── Una operación Yape no puede utilizarse más de una vez ─────────────────
-- Se crea únicamente si no existen duplicados heredados en pagos.referencia.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_class c
        JOIN pg_namespace n ON n.oid = c.relnamespace
        WHERE n.nspname = current_schema() AND c.relname = 'uq_pagos_referencia'
    ) THEN
        IF NOT EXISTS (
            SELECT 1 FROM pagos
            WHERE referencia IS NOT NULL
            GROUP BY referencia HAVING COUNT(*) > 1
        ) THEN
            CREATE UNIQUE INDEX uq_pagos_referencia
                ON pagos (referencia) WHERE referencia IS NOT NULL;
        END IF;
    END IF;
END $$;