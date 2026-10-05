-- =====================================================
-- INTIWATANA S.R.L. — Base de datos completa
-- PostgreSQL 14+ compatible
-- =====================================================
--
-- ⚠ ADVERTENCIA — ESTE ARCHIVO ES DESTRUCTIVO
-- Contiene DROP TABLE: al ejecutarse borra TODOS los datos de la base de datos
-- a la que se apunte. Está pensado únicamente para crear una base de datos
-- NUEVA desde cero.
--
-- NO lo ejecutes contra la base de datos del sistema: el respaldo oficial es
-- database/intiwatana_transporte_bd.backup (pg_dump -Fc), no este script.
-- Para restaurar el respaldo:
--   pg_restore -d transporte_bd database/intiwatana_transporte_bd.backup
--
-- Spring NO ejecuta este archivo automáticamente (spring.sql.init.mode no está
-- activo con PostgreSQL), pero no lo actives por descuido.
-- =====================================================

SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;

-- Drop tables in reverse order
DROP TABLE IF EXISTS movimientos_encomienda CASCADE;
DROP TABLE IF EXISTS boletos CASCADE;
DROP TABLE IF EXISTS encomiendas CASCADE;
DROP TABLE IF EXISTS pagos CASCADE;
DROP TABLE IF EXISTS asientos CASCADE;
DROP TABLE IF EXISTS viajes CASCADE;
DROP TABLE IF EXISTS rutas CASCADE;
DROP TABLE IF EXISTS buses CASCADE;
DROP TABLE IF EXISTS clientes CASCADE;
DROP TABLE IF EXISTS usuarios CASCADE;
DROP TABLE IF EXISTS sucursales CASCADE;
DROP TABLE IF EXISTS roles CASCADE;

-- ROLES
CREATE TABLE roles (
  id          BIGSERIAL PRIMARY KEY,
  nombre      VARCHAR(30) NOT NULL UNIQUE,
  descripcion VARCHAR(200)
);

-- SUCURSALES
CREATE TABLE sucursales (
  id           BIGSERIAL PRIMARY KEY,
  codigo       VARCHAR(20) UNIQUE,
  nombre       VARCHAR(150) NOT NULL,
  ciudad       VARCHAR(100) NOT NULL,
  provincia    VARCHAR(100),
  departamento VARCHAR(100),
  direccion    VARCHAR(300),
  telefono     VARCHAR(30),
  email        VARCHAR(150),
  es_terminal  BOOLEAN NOT NULL DEFAULT FALSE,
  latitud      NUMERIC(9,6),
  longitud     NUMERIC(9,6),
  activo       BOOLEAN NOT NULL DEFAULT TRUE,
  created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at   TIMESTAMP,
  deleted_at   TIMESTAMP
);

-- USUARIOS (personal)
CREATE TABLE usuarios (
  id            BIGSERIAL PRIMARY KEY,
  username      VARCHAR(50) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  nombres       VARCHAR(100) NOT NULL,
  apellidos     VARCHAR(100) NOT NULL,
  email         VARCHAR(150),
  telefono      VARCHAR(30),
  dni_ruc       VARCHAR(20),
  rol_id        BIGINT NOT NULL REFERENCES roles(id),
  sucursal_id   BIGINT REFERENCES sucursales(id),
  primer_login  BOOLEAN NOT NULL DEFAULT TRUE,
  activo        BOOLEAN NOT NULL DEFAULT TRUE,
  created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at    TIMESTAMP,
  deleted_at    TIMESTAMP
);

-- CLIENTES
CREATE TABLE clientes (
  id           BIGSERIAL PRIMARY KEY,
  nombres      VARCHAR(100) NOT NULL,
  apellidos    VARCHAR(100) NOT NULL,
  dni_ruc      VARCHAR(20) NOT NULL,
  email        VARCHAR(150),
  telefono     VARCHAR(30),
  ciudad       VARCHAR(100),
  direccion    VARCHAR(300),
  distrito     VARCHAR(100),
  razon_social VARCHAR(200),
  tipo_cliente VARCHAR(20) NOT NULL DEFAULT 'PERSONA'
                CHECK (tipo_cliente IN ('PERSONA','EMPRESA')),
  usuario_id   BIGINT REFERENCES usuarios(id),
  activo       BOOLEAN NOT NULL DEFAULT TRUE,
  created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at   TIMESTAMP,
  deleted_at   TIMESTAMP
);

-- BUSES
CREATE TABLE buses (
  id                 BIGSERIAL PRIMARY KEY,
  placa              VARCHAR(10) NOT NULL UNIQUE,
  marca              VARCHAR(80) NOT NULL,
  modelo             VARCHAR(80),
  tipo               VARCHAR(20) NOT NULL DEFAULT 'ECONOMICO'
                     CHECK (tipo IN ('ECONOMICO','SEMI_CAMA','CAMA','CAMA_SUITE')),
  capacidad_asientos INT NOT NULL,
  num_pisos          INT NOT NULL DEFAULT 1,
  anio_fabricacion   INT,
  foto_url           VARCHAR(500),
  observaciones      VARCHAR(500),
  activo             BOOLEAN NOT NULL DEFAULT TRUE,
  created_at         TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at         TIMESTAMP,
  deleted_at         TIMESTAMP
);

-- RUTAS
CREATE TABLE rutas (
  id                      BIGSERIAL PRIMARY KEY,
  codigo                  VARCHAR(30) UNIQUE,
  sucursal_origen_id      BIGINT NOT NULL REFERENCES sucursales(id),
  sucursal_destino_id     BIGINT NOT NULL REFERENCES sucursales(id),
  distancia_km            NUMERIC(8,2),
  duracion_horas_estimada NUMERIC(5,2),
  precio_base             NUMERIC(10,2),
  descripcion             VARCHAR(500),
  activo                  BOOLEAN NOT NULL DEFAULT TRUE,
  created_at              TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at              TIMESTAMP,
  deleted_at              TIMESTAMP
);

-- VIAJES
CREATE TABLE viajes (
  id                          BIGSERIAL PRIMARY KEY,
  ruta_id                     BIGINT NOT NULL REFERENCES rutas(id),
  bus_id                      BIGINT NOT NULL REFERENCES buses(id),
  chofer_id                   BIGINT REFERENCES usuarios(id),
  fecha_hora_salida           TIMESTAMP NOT NULL,
  fecha_hora_llegada_estimada TIMESTAMP,
  fecha_hora_llegada_real     TIMESTAMP,
  precio_adulto               NUMERIC(10,2) NOT NULL,
  precio_nino                 NUMERIC(10,2),
  estado                      VARCHAR(20) NOT NULL DEFAULT 'PROGRAMADO'
                              CHECK (estado IN ('PROGRAMADO','EN_CURSO','FINALIZADO','CANCELADO')),
  observaciones               VARCHAR(500),
  activo                      BOOLEAN NOT NULL DEFAULT TRUE,
  created_at                  TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at                  TIMESTAMP,
  deleted_at                  TIMESTAMP
);

-- ASIENTOS
CREATE TABLE asientos (
  id             BIGSERIAL PRIMARY KEY,
  viaje_id       BIGINT NOT NULL REFERENCES viajes(id),
  numero_asiento VARCHAR(10) NOT NULL,
  fila           INT,
  columna        INT,
  piso           INT NOT NULL DEFAULT 1,
  tipo           VARCHAR(20) NOT NULL DEFAULT 'VENTANA'
                 CHECK (tipo IN ('VENTANA','PASILLO','CAMA_DOBLE')),
  estado         VARCHAR(20) NOT NULL DEFAULT 'DISPONIBLE'
                 CHECK (estado IN ('DISPONIBLE','RESERVADO','VENDIDO','BLOQUEADO')),
  activo         BOOLEAN NOT NULL DEFAULT TRUE,
  created_at     TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at     TIMESTAMP,
  deleted_at     TIMESTAMP,
  UNIQUE(viaje_id, numero_asiento)
);

-- PAGOS
CREATE TABLE pagos (
  id         BIGSERIAL PRIMARY KEY,
  monto      NUMERIC(10,2) NOT NULL,
  metodo     VARCHAR(30) NOT NULL,
  estado     VARCHAR(20) NOT NULL DEFAULT 'COMPLETADO',
  referencia VARCHAR(100),
  observacion VARCHAR(300),
  fecha_pago TIMESTAMP NOT NULL DEFAULT NOW(),
  cajero_id  BIGINT REFERENCES usuarios(id),
  activo     BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP,
  deleted_at TIMESTAMP
);

-- BOLETOS
CREATE TABLE boletos (
  id              BIGSERIAL PRIMARY KEY,
  numero_boleto   VARCHAR(30) NOT NULL UNIQUE,
  viaje_id        BIGINT NOT NULL REFERENCES viajes(id),
  asiento_id      BIGINT NOT NULL REFERENCES asientos(id),
  cliente_id      BIGINT NOT NULL REFERENCES clientes(id),
  cajero_id       BIGINT REFERENCES usuarios(id),
  pago_id         BIGINT REFERENCES pagos(id),
  precio_pagado   NUMERIC(10,2) NOT NULL,
  estado          VARCHAR(20) NOT NULL DEFAULT 'ACTIVO'
                  CHECK (estado IN ('ACTIVO','USADO','CANCELADO','REEMBOLSADO')),
  codigo_qr       VARCHAR(500),
  qr_imagen_url   VARCHAR(500),
  observaciones   VARCHAR(300),
  fecha_hora_uso  TIMESTAMP,
  activo          BOOLEAN NOT NULL DEFAULT TRUE,
  created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at      TIMESTAMP,
  deleted_at      TIMESTAMP
);

-- ENCOMIENDAS
CREATE TABLE encomiendas (
  id                   BIGSERIAL PRIMARY KEY,
  numero_guia          VARCHAR(30) UNIQUE,
  viaje_id             BIGINT REFERENCES viajes(id),
  remitente_id         BIGINT REFERENCES clientes(id),
  -- NULL cuando la encomienda se pidió desde el portal web: ahí el destinatario
  -- no es un Cliente registrado y su nombre queda en destinatario_nombre.
  destinatario_id      BIGINT REFERENCES clientes(id),
  destinatario_nombre  VARCHAR(200),
  sucursal_origen_id   BIGINT REFERENCES sucursales(id),
  sucursal_destino_id  BIGINT REFERENCES sucursales(id),
  cajero_registro_id   BIGINT REFERENCES usuarios(id),
  cajero_entrega_id    BIGINT REFERENCES usuarios(id),
  pago_id              BIGINT REFERENCES pagos(id),
  descripcion_contenido VARCHAR(300),
  peso_kg              NUMERIC(8,2),
  volumen_m3           NUMERIC(8,4),
  valor_declarado      NUMERIC(10,2),
  costo                NUMERIC(10,2),
  metodo_pago          VARCHAR(30),
  estado               VARCHAR(30) NOT NULL DEFAULT 'RECIBIDO',
  codigo_qr            VARCHAR(500),
  observaciones        VARCHAR(500),
  activo               BOOLEAN NOT NULL DEFAULT TRUE,
  created_at           TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at           TIMESTAMP,
  deleted_at           TIMESTAMP
);

-- MOVIMIENTOS ENCOMIENDA
CREATE TABLE movimientos_encomienda (
  id                    BIGSERIAL PRIMARY KEY,
  encomienda_id         BIGINT NOT NULL REFERENCES encomiendas(id),
  estado_anterior       VARCHAR(30),
  estado_nuevo          VARCHAR(30) NOT NULL,
  fecha_hora            TIMESTAMP NOT NULL DEFAULT NOW(),
  observacion           VARCHAR(500),
  sucursal_actual_id    BIGINT REFERENCES sucursales(id),
  usuario_responsable_id BIGINT REFERENCES usuarios(id)
);

-- =====================================================
-- SECUENCIAS (reset)
-- =====================================================
SELECT setval('roles_id_seq', 10);
SELECT setval('sucursales_id_seq', 20);
SELECT setval('usuarios_id_seq', 20);
SELECT setval('clientes_id_seq', 20);
SELECT setval('buses_id_seq', 20);
SELECT setval('rutas_id_seq', 20);
SELECT setval('viajes_id_seq', 30);
SELECT setval('asientos_id_seq', 500);
SELECT setval('boletos_id_seq', 20);
SELECT setval('encomiendas_id_seq', 20);

-- =====================================================
-- DATOS INICIALES
-- =====================================================

-- Roles
INSERT INTO roles (id, nombre, descripcion) VALUES
(1, 'ROLE_ADMIN',   'Acceso total al sistema'),
(2, 'ROLE_CAJERO',  'Vende boletos y registra encomiendas'),
(3, 'ROLE_CHOFER',  'Gestiona sus viajes asignados'),
(4, 'ROLE_CLIENTE', 'Portal de autoservicio de pasajes');

-- Sucursales (es_terminal = TRUE para aparecer en el selector público)
INSERT INTO sucursales (id, codigo, nombre, ciudad, provincia, departamento, direccion, telefono, es_terminal, activo, created_at) VALUES
(1, 'AYA-01', 'Terminal Huamanga',       'Huamanga',     'Huamanga',     'Ayacucho',   'Jr. Libertad 120, Ayacucho',    '066-312000', TRUE, TRUE, NOW()),
(2, 'VIL-01', 'Terminal Vilcas Huamán',  'Vilcas Huaman','Vilcas Huamán','Ayacucho',   'Plaza Principal s/n',           '066-421000', TRUE, TRUE, NOW()),
(3, 'ACC-01', 'Agencia Accomarca',        'Accomarca',    'Vilcas Huamán','Ayacucho',   'Av. Principal s/n',             NULL,         TRUE, TRUE, NOW()),
(4, 'HUA-01', 'Terminal Huarcas',         'Huarcas',      'Huamanga',     'Ayacucho',   'Jr. San Martín 45',             NULL,         TRUE, TRUE, NOW()),
(5, 'CAN-01', 'Agencia Canaria',          'Canaria',      'Víctor Fajardo','Ayacucho',  'Jr. Grau 12',                   NULL,         TRUE, TRUE, NOW()),
(6, 'FAJ-01', 'Terminal Huancapi',        'Huancapi',     'Víctor Fajardo','Ayacucho',  'Plaza de Armas s/n',            '066-451200', TRUE, TRUE, NOW());

-- Usuarios de DEMOSTRACION (DDL historico; NO lo ejecuta Flyway).
-- Las contrasenas y sus hashes NO se versionan en el repositorio: en desarrollo
-- las genera DataInitializer; en produccion se cargan por variables de entorno.
INSERT INTO usuarios (id, username, password_hash, nombres, apellidos, email, telefono, dni_ruc, rol_id, sucursal_id, primer_login, activo, created_at) VALUES
(1,  'admin',   '<HASH_BCRYPT>', 'José',   'Pariona',   'admin@intiwatana.pe',   '966001122', '09876543', 1, 1, FALSE, TRUE, NOW()),
(2,  'cajero1', '<HASH_BCRYPT>', 'María',  'Quispe',    'cajero@intiwatana.pe',  '955002233', '12345678', 2, 1, FALSE, TRUE, NOW()),
(3,  'chofer1', '<HASH_BCRYPT>', 'Carlos', 'Flores',    'chofer1@intiwatana.pe', '944003344', '87654321', 3, 1, FALSE, TRUE, NOW()),
(4,  'chofer2', '<HASH_BCRYPT>', 'Pedro',  'Condori',   'chofer2@intiwatana.pe', '933004455', '76543210', 3, 2, FALSE, TRUE, NOW()),
(5,  'chofer3', '<HASH_BCRYPT>', 'Luis',   'Huamán',    'chofer3@intiwatana.pe', '922005566', '65432109', 3, 1, FALSE, TRUE, NOW());

-- Buses
INSERT INTO buses (id, placa, marca, modelo, tipo, capacidad_asientos, num_pisos, anio_fabricacion, activo, created_at) VALUES
(1, 'ABC-123', 'Mercedes Benz', 'OF-1722', 'ECONOMICO',  40, 1, 2019, TRUE, NOW()),
(2, 'DEF-456', 'Volvo',         'B420R',   'SEMI_CAMA',  36, 1, 2020, TRUE, NOW()),
(3, 'GHI-789', 'Scania',        'K380EB',  'CAMA',       28, 2, 2021, TRUE, NOW()),
(4, 'JKL-012', 'Mercedes Benz', 'OF-1621', 'ECONOMICO',  40, 1, 2018, TRUE, NOW()),
(5, 'MNO-345', 'Volvo',         'B380R',   'ECONOMICO',  40, 1, 2022, TRUE, NOW());

-- Rutas
INSERT INTO rutas (id, codigo, sucursal_origen_id, sucursal_destino_id, distancia_km, duracion_horas_estimada, precio_base, activo, created_at) VALUES
(1,  'HUA-VIL-01', 1, 2, 75,  2.5, 12.00, TRUE, NOW()),
(2,  'VIL-HUA-01', 2, 1, 75,  2.5, 12.00, TRUE, NOW()),
(3,  'HUA-ACC-01', 1, 3, 90,  3.0, 14.00, TRUE, NOW()),
(4,  'ACC-HUA-01', 3, 1, 90,  3.0, 14.00, TRUE, NOW()),
(5,  'HUA-HRC-01', 1, 4, 40,  1.5,  8.00, TRUE, NOW()),
(6,  'HRC-HUA-01', 4, 1, 40,  1.5,  8.00, TRUE, NOW()),
(7,  'HUA-CAN-01', 1, 5, 120, 4.0, 18.00, TRUE, NOW()),
(8,  'CAN-HUA-01', 5, 1, 120, 4.0, 18.00, TRUE, NOW()),
(9,  'HUA-HCA-01', 1, 6, 150, 5.0, 22.00, TRUE, NOW()),
(10, 'HCA-HUA-01', 6, 1, 150, 5.0, 22.00, TRUE, NOW());

-- Viajes (próximos días)
INSERT INTO viajes (id, ruta_id, bus_id, chofer_id, fecha_hora_salida, fecha_hora_llegada_estimada, precio_adulto, precio_nino, estado, activo, created_at) VALUES
(1,  1, 1, 3, NOW() + INTERVAL '1 day 6 hours',   NOW() + INTERVAL '1 day 8 hours 30 min',  12.00,  7.00, 'PROGRAMADO', TRUE, NOW()),
(2,  1, 2, 4, NOW() + INTERVAL '1 day 9 hours',   NOW() + INTERVAL '1 day 11 hours 30 min', 12.00,  7.00, 'PROGRAMADO', TRUE, NOW()),
(3,  1, 4, 5, NOW() + INTERVAL '1 day 14 hours',  NOW() + INTERVAL '1 day 16 hours 30 min', 12.00,  7.00, 'PROGRAMADO', TRUE, NOW()),
(4,  2, 1, 3, NOW() + INTERVAL '2 days 6 hours',  NOW() + INTERVAL '2 days 8 hours 30 min', 12.00,  7.00, 'PROGRAMADO', TRUE, NOW()),
(5,  3, 3, 4, NOW() + INTERVAL '1 day 7 hours',   NOW() + INTERVAL '1 day 10 hours',        14.00,  8.00, 'PROGRAMADO', TRUE, NOW()),
(6,  5, 5, 5, NOW() + INTERVAL '1 day 8 hours',   NOW() + INTERVAL '1 day 9 hours 30 min',   8.00,  5.00, 'PROGRAMADO', TRUE, NOW()),
(7,  7, 2, 3, NOW() + INTERVAL '2 days 6 hours',  NOW() + INTERVAL '2 days 10 hours',       18.00, 10.00, 'PROGRAMADO', TRUE, NOW()),
(8,  9, 3, 4, NOW() + INTERVAL '3 days 5 hours',  NOW() + INTERVAL '3 days 10 hours',       22.00, 12.00, 'PROGRAMADO', TRUE, NOW()),
(9,  1, 4, 5, NOW() - INTERVAL '1 day',           NOW() - INTERVAL '22 hours 30 min',       12.00,  7.00, 'FINALIZADO', TRUE, NOW()),
(10, 2, 5, 3, NOW() - INTERVAL '2 days',          NOW() - INTERVAL '1 day 21 hours',        12.00,  7.00, 'FINALIZADO', TRUE, NOW());

-- Generar asientos para viajes 1-8 (bus 1=40 asientos, bus 2=36, bus 3=28, bus 4=40, bus 5=40)
-- Viaje 1 (bus 1 ABC-123: 40 asientos, 10 filas x 4 col)
INSERT INTO asientos (viaje_id, numero_asiento, fila, columna, piso, tipo, estado, activo, created_at)
SELECT 1, LPAD(n::text, 2, '0'),
  CEIL(n/4.0)::int,
  ((n-1)%4)+1,
  1,
  CASE WHEN ((n-1)%4) IN (0,3) THEN 'VENTANA' ELSE 'PASILLO' END,
  CASE WHEN n <= 8 THEN 'VENDIDO' WHEN n <= 10 THEN 'RESERVADO' ELSE 'DISPONIBLE' END,
  TRUE, NOW()
FROM generate_series(1,40) AS n;

-- Viaje 2 (bus 2 DEF-456: 36 asientos)
INSERT INTO asientos (viaje_id, numero_asiento, fila, columna, piso, tipo, estado, activo, created_at)
SELECT 2, LPAD(n::text, 2, '0'),
  CEIL(n/4.0)::int, ((n-1)%4)+1, 1,
  CASE WHEN ((n-1)%4) IN (0,3) THEN 'VENTANA' ELSE 'PASILLO' END,
  CASE WHEN n <= 4 THEN 'VENDIDO' ELSE 'DISPONIBLE' END,
  TRUE, NOW()
FROM generate_series(1,36) AS n;

-- Viaje 3 (bus 4: 40 asientos)
INSERT INTO asientos (viaje_id, numero_asiento, fila, columna, piso, tipo, estado, activo, created_at)
SELECT 3, LPAD(n::text, 2, '0'),
  CEIL(n/4.0)::int, ((n-1)%4)+1, 1,
  CASE WHEN ((n-1)%4) IN (0,3) THEN 'VENTANA' ELSE 'PASILLO' END,
  'DISPONIBLE', TRUE, NOW()
FROM generate_series(1,40) AS n;

-- Viaje 4
INSERT INTO asientos (viaje_id, numero_asiento, fila, columna, piso, tipo, estado, activo, created_at)
SELECT 4, LPAD(n::text, 2, '0'),
  CEIL(n/4.0)::int, ((n-1)%4)+1, 1,
  CASE WHEN ((n-1)%4) IN (0,3) THEN 'VENTANA' ELSE 'PASILLO' END,
  CASE WHEN n <= 12 THEN 'VENDIDO' ELSE 'DISPONIBLE' END,
  TRUE, NOW()
FROM generate_series(1,40) AS n;

-- Viajes 5-8
INSERT INTO asientos (viaje_id, numero_asiento, fila, columna, piso, tipo, estado, activo, created_at)
SELECT v, LPAD(n::text, 2, '0'),
  CEIL(n/4.0)::int, ((n-1)%4)+1, 1,
  CASE WHEN ((n-1)%4) IN (0,3) THEN 'VENTANA' ELSE 'PASILLO' END,
  'DISPONIBLE', TRUE, NOW()
FROM generate_series(5,8) AS v
CROSS JOIN generate_series(1,28) AS n;

-- Clientes de muestra
INSERT INTO clientes (id, nombres, apellidos, dni_ruc, email, telefono, tipo_cliente, activo, created_at) VALUES
(1, 'Ana',    'García',   '12345678', 'ana@email.com',    '955001122', 'PERSONA', TRUE, NOW()),
(2, 'Luis',   'Torres',   '87654321', 'luis@email.com',   '944002233', 'PERSONA', TRUE, NOW()),
(3, 'Rosa',   'Mamani',   '11223344', 'rosa@email.com',   '933003344', 'PERSONA', TRUE, NOW()),
(4, 'Jorge',  'Ccasa',    '44332211', 'jorge@email.com',  '922004455', 'PERSONA', TRUE, NOW()),
(5, 'Carmen', 'Palomino', '55667788', 'carmen@email.com', '911005566', 'PERSONA', TRUE, NOW());

