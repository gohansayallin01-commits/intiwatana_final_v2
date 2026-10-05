# Auditoría integral — Sistema de Transportes INTIWATANA S.R.L.

**Fecha de ejecución:** 30/09/2026
**Objeto auditado:** Proyecto `intiwatana_final_v2` (backend Spring Boot + frontend React/Vite + PostgreSQL).
**Tipo de auditoría:** Integral (código + base de datos + ejecución + pruebas funcionales).
**Regla de la auditoría:** NO se modificó el sistema. Solo inspección, arranque y pruebas (las pruebas generaron datos de prueba, listados en §7).

---

## 0. Resumen ejecutivo y veredicto

El sistema está **IMPLEMENTADO Y OPERATIVO**: el backend compila y arranca, la
base de datos real se conecta y los flujos críticos (venta de boletos, escaneo
QR, encomiendas, tracking, portal del cliente, reportes y control de acceso por
rol) funcionan de extremo a extremo.

| Resultado | Cantidad |
|---|---|
| Secciones que CUMPLEN | **13 / 23** |
| Secciones que CUMPLEN PARCIALMENTE | **10 / 23** |
| Secciones que NO CUMPLEN | 0 / 23 |
| Sin verificar / No aplica | 0 / 23 |

**Veredicto:** APROBADO CON OBSERVACIONES. El núcleo operativo cumple; las 10
secciones parciales se deben a (a) discrepancias de denominación contra el brief
(no existe el rol VENDEDOR/OPERADOR_ENCOMIENDAS ni los estados de encomienda
`CARGADO_EN_BUS`/`LLEGADO_A_DESTINO`), (b) datos heredados en la base con
asientos vendidos sin boleto, (c) problemas de semántica HTTP (403 en vez de
401, 500 en vez de 415), (d) incoherencia entre indicadores del resumen de
reportes, y (e) catálogo de destinos pre-cargado (hardcode) en el frontend.

---

## 1. Metodología y evidencia

1. **Inventario de código** de todas las capas (config, seguridad, controllers,
   servicios, DTO, enums, repositorios) mapeando seguridad por endpoint.
2. **Auditoría de la base de datos real** `transporte_bd` (PostgreSQL 17.5):
   esquema, PK/FK, únicos, CHECK, índices, secuencias, conteos y datos seed.
3. **Arranque del backend** en ejecución real (puerto 8080) y **pruebas
   funcionales** por HTTP contra PostgreSQL.
4. **Verificación de integridad** de los datos tras cada prueba (consultas `psql`).
5. **Lectura del frontend** (rutas, protección por rol, catálogo de datos).
6. Comparación de todo lo anterior contra el **brief** de requisitos del usuario.

Evidencias generadas (archivos temporales de trabajo):
`%TEMP%\opencode\audit_db_full.out`, `audit_db_fix.out`, `test_accesos_1/2/3.txt`,
`test_boleto.txt`, `test_encomienda.txt`, `test_reportes.txt`, `test_cliente.txt`,
`test_cliente2.txt`, `test_pagos_admin.txt`, `verif_db_testeos.txt`, `verif_final.txt`,
`tokens.json`, `flujo_*.ps1`, `start_backend.ps1`.

---

## 2. Entorno ejecutado para la auditoría

| Componente | Detalle |
|---|---|
| Backend | Spring Boot 3.3.0 / Java 17 (Temurin 17), puerto 8080, ejecutado con 89 jars de dependencias + `target/classes` |
| Base de datos | PostgreSQL 17.5, base `transporte_bd` (~8 875 kB), usuario `postgres` (clave <REDACTADO>) |
| Frontend | Vite + React 19, puerto 5173, proxy `/api` → 8080 (solo lectura en esta auditoría) |
| JWT | HS256 (JJWT 0.12.3), expiración 24 h; secreto de desarrollo por defecto con advertencia en log (`JwtUtil.java`) |
| Log del backend | `backend\bus-system\logs\sistema-transporte.log` |

Credenciales de prueba (creadas por `DataInitializer`, BCrypt): `admin` (ADMIN),
`cajero1` (CAJERO), `chofer1`–`chofer3` (CHOFER); claves de demo <REDACTADO> por saneamiento.

---

## 3. Matriz de cumplimiento (23 secciones)

| # | Sección | Estado |
|---|---|---|
| 1 | Autenticación y sesión (login/JWT) | **CUMPLE** |
| 2 | Usuarios, roles y sucursales | **CUMPLE PARCIALMENTE** |
| 3 | Clientes (registro público y CRUD) | **CUMPLE** |
| 4 | Buses y flota | **CUMPLE** |
| 5 | Rutas y destinos | **CUMPLE PARCIALMENTE** |
| 6 | Viajes y programación | **CUMPLE** |
| 7 | Selección de asientos y disponibilidad | **CUMPLE PARCIALMENTE** |
| 8 | Venta de boletos | **CUMPLE** |
| 9 | Pagos y métodos de pago | **CUMPLE** |
| 10 | QR y escaneo (validación chofer) | **CUMPLE** |
| 11 | Cancelación y reembolso | **CUMPLE** |
| 12 | Registro de encomiendas | **CUMPLE** |
| 13 | Tracking público de encomiendas | **CUMPLE** |
| 14 | Cambios de estado y movimientos | **CUMPLE PARCIALMENTE** |
| 15 | Portal del cliente (autoservicio) | **CUMPLE PARCIALMENTE** |
| 16 | Panel del chofer | **CUMPLE PARCIALMENTE** |
| 17 | Panel de caja/administración | **CUMPLE** |
| 18 | Reportes de ventas e ingresos | **CUMPLE PARCIALMENTE** |
| 19 | Reporte de ocupación | **CUMPLE PARCIALMENTE** |
| 20 | Reporte de encomiendas | **CUMPLE** |
| 21 | Exportación CSV | **CUMPLE** |
| 22 | Seguridad y control de acceso por rol (RBAC) | **CUMPLE PARCIALMENTE** |
| 23 | Integración y datos (BD real, seed, frontend-backend) | **CUMPLE PARCIALMENTE** |

---

## 4. Detalle por sección

### 4.1 Autenticación y sesión (login/JWT) — CUMPLE

- `SecurityConfig.java` publica `/api/v1/auth/**`; `AuthController` expone
  `POST /api/v1/auth/login` → `LoginResponse` con `token`, `rol`, `usuarioId`,
  `sucursalNombre` y (`clienteId` para clientes). `AuthServiceImpl` valida con
  BCrypt. JWT firmado con HS256 y verificado por `JwtAuthenticationFilter`.
- **Prueba:** login de los tres roles → 200 con token y rol correctos; contraseña
  incorrecta → **401**. Sin token en endpoint protegido → 403 (ver hallazgos H-4).
- **Nota:** `refresh-expiration-ms` está configurado pero **no existe** endpoint
  ni emisión de refresh token (ver H-5).

### 4.2 Usuarios, roles y sucursales — CUMPLE PARCIALMENTE

- 4 roles en BD (`ROLE_ADMIN`, `ROLE_CAJERO`, `ROLE_CHOFER`, `ROLE_CLIENTE`);
  6 sucursales (`GET /sucursales` probado, 200). CRUD de usuarios
  (`/api/v1/usuarios`, solo ADMIN) probado: cajero → 403, admin → 201.
- **El brief pide roles ADMIN / VENDEDOR / OPERADOR_ENCOMIENDAS y no existen**:
  la caja es `ROLE_CAJERO` y la operación de encomiendas recae en ADMIN/CAJERO y
  la validación de boletos en `ROLE_CHOFER`. La estructura funcional cubre el
  negocio, pero la denominación no coincide con el brief (ver H-1).

### 4.3 Clientes (registro público y CRUD) — CUMPLE

- `POST /api/v1/clientes/registro` es público y devuelve `LoginResponse` con
  JWT + `clienteId` (probado: registro dni 79999709 → clienteId 21; `percy7096`
  → clienteId 22). CRUD restringido a ADMIN/CAJERO/CHOFER; `mi-perfil` para
  CLIENTE (probado 200). CHECK en BD `clientes_tipo_cliente_check`
  (PERSONA/EMPRESA).

### 4.4 Buses y flota — CUMPLE

- Entidad, repositorio y `BusesController` presentes; `buses_tipo_check` en BD;
  5 buses en la base. Lectura pública permitida por `SecurityConfig`.
  (No sometido a prueba funcional por API en esta corrida; verificado por código y BD.)

### 4.5 Rutas y destinos — CUMPLE PARCIALMENTE

- Backend: 10 rutas reales en BD con precios 8–22; `GET /rutas` público según
  `SecurityConfig`.
- **El frontend no consume el catálogo real**: `src\data\destinos.jsx` es un
  catálogo escrito a mano (127 líneas) con horarios y precios «referenciales»
  como `S/ 10–15` y terminales fijos, y la pantalla de compra los muestra con la
  nota «*Horarios y precios referenciales. Confirmar disponibilidad en
  terminal» (`App.jsx`, modal). Ver H-8.

### 4.6 Viajes y programación — CUMPLE

- `GET /api/v1/viajes` público (probado 200) con 10 viajes (8 PROGRAMADO a
  futuro y 2 FINALIZADO); estados `PROGRAMADO/EN_CURSO/FINALIZADO/CANCELADO`
  (enum + CHECK en BD). `GET /viajes/{id}/asientos` público (probado 200).
  `mis-viajes` para chofer (probado 200). Actualización de viajes es solo ADMIN.
  `POST /admin/tools/generar-viajes-prueba` (ADMIN) probado 200 sin duplicar.

### 4.7 Selección de asientos y disponibilidad — CUMPLE PARCIALMENTE

- Disponibilidad real con estados `DISPONIBLE/RESERVADO/VENDIDO/BLOQUEADO`
  (enum + CHECK). La venta bloquea el asiento y la cancelación lo libera
  (probado con el asiento 545). Doble venta del mismo asiento → **400** con
  mensaje de negocio. Existe bloqueo pesimista en `BoletoRepository`
  (`findByNumeroBoletoForUpdate`).
- **Base con integridad rota (H-3):** asientos iniciales en `VENDIDO/RESERVADO`
  sin ningún boleto (viaje 1: 30 disponibles / 8 vendidos / 2 reservados;
  viaje 2: 32/4/0; viaje 4: 28/12/0) mientras `boletos` estaba vacío y
  `boletos_id_seq = 20` evidenciaba filas borradas.

### 4.8 Venta de boletos — CUMPLE

- `POST /b/vender` (ADMIN/CAJERO) probado: `BOL-20260930-000021` (asiento 11,
  S/ 12.00, EFECTIVO, QR base64). `POST /boletos/comprar` (CLIENTE) probado
  (`BOL-20260930-000023`, S/ 12.00, YAPE). Numeración `BOL-AAAAMMDD-0000NN`,
  descuento de asientos, pago `COMPLETADO` asociado.
- **Hallazgo menor H-6:** `/comprar` exige `clienteId` en el body
  (`BoletoRequest.java:20-21`) aunque el controlador lo ignore del request.

### 4.9 Pagos y métodos de pago — CUMPLE

- `MetodoPago` (EFECTIVO, YAPE, PLIN, TARJETA_CREDITO, TARJETA_DEBITO,
  TRANSFERENCIA, CONTRA_ENTREGA). `PagoController` POST+GET solo
  ADMIN/CAJERO (probados 201 y 200). El pago contra entrega queda
  `PENDIENTE` y el resto `COMPLETADO` (verificado en `pagos`: id 4 =
  CONTRA_ENTREGA PENDIENTE). Es una **simulación de pago** (no hay pasarela
  bancaria), coherente con un proyecto académico.

### 4.10 QR y escaneo (validación chofer) — CUMPLE

- QR generado con ZXing como imagen base64 en cada boleto. `POST
  /boletos/{id}/escanear` restringido a ADMIN/CHOFER (probado: cajero → 403;
  chofer → boleto pasa a `USADO`). Reutilización del QR invalidada por estado.

### 4.11 Cancelación y reembolso — CUMPLE

- `POST /boletos/{id}/cancelar` (ADMIN/CAJERO): probado con
  `BOL-20260930-000022` → `CANCELADO`, asiento liberado a `DISPONIBLE`.
  Cancelar un boleto ya usado → 400. Estado `REEMBOLSADO` existe en el enum.
  No hay módulo de devolución de importes (queda como estado de registro).

### 4.12 Registro de encomiendas — CUMPLE

- Registro ADMIN/CAJERO probado: `GUI-20260930-000021` (S/ 12.50, EFECTIVO,
  remitente Ana García → destinatario Luis Torres, Terminal Huamanga → Terminal
  Vilcas Huamán) y `GUI-20260930-000022` (portal, CONTRA_ENTREGA, S/ 10.00).
  Genera guía correlativa, pago y movimiento inicial.

### 4.13 Tracking público de encomiendas — CUMPLE

- `GET /encomiendas/tracking/{guia}` público: probado 200 con 6 movimientos,
  y 404 para guía inexistente. Devuelve una vista reducida (sin remitente,
  documentos ni responsable), alineado con la corrección previa S-3. El frontend
  consume el endpoint (modal «Rastrear encomienda» en `App.jsx`).

### 4.14 Cambios de estado y movimientos — CUMPLE PARCIALMENTE

- Transiciones ADMIN/CAJERO validadas: la cadena completa
  RECIBIDO → EN_ALMACEN → EN_TRANSITO → EN_DESTINO → LISTO_ENTREGA → ENTREGADO
  funcionó; una transición inválida (ENTREGADO → EN_TRANSITO) → **400**.
  `GET /encomiendas/{id}/movimientos` devuelve 6 movimientos con
  `usuario_responsable_id = 2` (cajero1).
- **Los estados del brief `CARGADO_EN_BUS` y `LLEGADO_A_DESTINO` NO existen**;
  además, `encomiendas.estado` no tiene CHECK en BD (varchar libre, la app lo
  valida) — ver H-2/H-9.

### 4.15 Portal del cliente (autoservicio) — CUMPLE PARCIALMENTE

- Backend: `comprar`, `mis-boletos`, `encomiendas/solicitar`,
  `mis-encomiendas`, `mi-perfil` (todos CLIENTE). Probados: `mis-boletos`=1,
  `mis-encomiendas`=1, solicitud de encomienda OK (estado RECIBIDO), compra OK.
  Authz: CAJERO/ADMIN/sin-token → **403** en `mis-boletos` y `mis-encomiendas`.
- **Frontend:** la ruta `/pasajes` es **pública** (`App.jsx:562`, sin
  `RutaProtegida`); el carrito se puede armar sin sesión, aunque el pago
  (`/boletos/comprar`) exige rol CLIENTE — ver H-7.

### 4.16 Panel del chofer — CUMPLE PARCIALMENTE

- `ChoferDashboard` con `mis-viajes` (probado 200) y escaneo de boletos
  (probado → USADO). El chofer **no** registra ni cambia estados de encomiendas
  (eso es ADMIN/CAJERO). Si el brief asigna esas funciones a un
  OPERADOR_ENCOMIENDAS, ese rol no existe (H-1). `POST /boletos/vender` como
  chofer → 403 (correcto).

### 4.17 Panel de caja/administración — CUMPLE

- AdminDashboard integra venta, asientos, encomiendas, usuarios y reportes.
  Pruebas: admin → 200 en `/encomiendas`, `/usuarios`, `/reportes/resumen`,
  `/admin/tools/generar-viajes-prueba`; cajero → 403 en `/reportes/**` y
  `/usuarios` (solo ADMIN los ve, como define `@PreAuthorize`).

### 4.18 Reportes de ventas e ingresos — CUMPLE PARCIALMENTE

- `/api/v1/reportes/**` exclusivo ADMIN: provee `resumen`, `ingresos`,
  `ingresos/serie-diaria`, `pasajes`, `ocupacion`, `encomiendas`, `sucursales`
  y sus versiones CSV. Todos respondieron 200.
- Resumen verificado contra BD con 3 ventas: `viajes=2`,
  `boletosVendidos=0`, `ingresosPasajes=24.00`, `precioPromedio=12.00`,
  `ocupacionPromedio=0.00`, `encomiendas=1`, `ingresosEncomiendas=12.50`,
  `pesoTotalKg=2.500`, `ingresosTotales=36.50`, `transacciones=3`,
  desglose por método [EFECTIVO 2/S/ 24.50, YAPE 1/S/ 12.00].
- **Incoherencia H-10:** `boletosVendidos` y `ocupacionPromedio` se calculan por
  **fecha de salida del viaje** mientras `ingresosPasajes` por **fecha de pago**;
  al vender boletos de viajes futuros aparece `ingresos > 0` con
  `boletosVendidos = 0`.

### 4.19 Reporte de ocupación — CUMPLE PARCIALMENTE

- Endpoint `/reportes/ocupacion` responde 200 con ocupación por viaje. Comparte
  el ancla temporal de la sección 4.18 (H-10) y, con datos base huérfanos (H-3),
  podría reportar asientos vendidos sin boleto.

### 4.20 Reporte de encomiendas — CUMPLE

- `/reportes/encomiendas` (+CSV) probado 200: totales y desglose por estado y
  corredor, con el criterio de «fecha de registro».

### 4.21 Exportación CSV — CUMPLE

- `CsvExportador` con UTF-8 BOM, RFC 4180 y neutralización de fórmulas Excel;
  12 endpoints `…/csv` probados (200, `text/csv;charset=UTF-8`). La batería
  previa de 38 pruebas unitarias (10 `CsvExportadorTest` + 28
  `ReporteServiceImplTest`) valida este módulo.

### 4.22 Seguridad y control de acceso por rol (RBAC) — CUMPLE PARCIALMENTE

- SecurityConfig + `@EnableMethodSecurity` + `@PreAuthorize` por endpoint,
  inventariado completo. Sesión stateless (JWT), CORS para 5173/3000,
  contraseñas BCrypt, DTOs públicos reducidos para tracking (S-3). Matriz de
  acceso probada en más de 12 casos (admin/cajero/chofer/cliente/sin-token)
  con resultados correctos.
- Hallazgos: **H-4** sin token → 403 en vez de 401; **H-5** sin refresh token;
  **H-11** request sin `Content-Type` JSON a `/boletos/vender` → 500
  (`HttpMediaTypeNotSupportedException` no manejada), debería ser 415; secretos
  de desarrollo por defecto en `application.yml` con advertencia en log
  (mitigado por variables de entorno, correcciones previas S-5/S-6).

### 4.23 Integración y datos — CUMPLE PARCIALMENTE

- Frontend↔backend conectados por proxy `/api` y `fetch` con token
  (AuthContext, login, registro, mis-boletos, encomiendas, reportes, usuarios).
- BD real PostgreSQL con `ddl-auto: update` (riesgo de producción, deuda
  conocida). Seed real: 10 rutas, 5 buses, 10 viajes, 6 sucursales, 5 clientes.
- Pendientes de la auditoría previa no resueltos: catálogo `DESTINOS` hardcodeado
  (H-8), `.env.example` apunta a `transporte_db` (H-12), sin respaldo
  `pg_dump` verificado (H-13), y la integridad base de asientos (H-3).

---

## 5. Hallazgos consolidados

| # | Severidad | Hallazgo | Sección |
|---|---|---|---|
| H-1 | Alta | No existen los roles del brief `VENDEDOR` y `OPERADOR_ENCOMIENDAS`; el sistema usa CAJERO (venta) y CHOFER (operación). | 2, 16 |
| H-2 | Alta | No existen los estados de encomienda `CARGADO_EN_BUS` / `LLEGADO_A_DESTINO` pedidos en el brief. | 14 |
| H-3 | Alta | Base con asientos `VENDIDO/RESERVADO` sin boletos asociados (filas borradas, `boletos_id_seq=20`). | 7, 19, 23 |
| H-4 | Media | Endpoint protegido sin token responde 403 (debería ser 401). | 1, 22 |
| H-5 | Media | `refresh-expiration-ms` configurado pero sin implementación de refresh token. | 1, 22 |
| H-6 | Media | `/boletos/comprar` exige `clienteId` en el body aunque lo ignore (`@NotNull`). | 8, 15 |
| H-7 | Media | Ruta `/pasajes` pública en el frontend (se puede armar carrito sin sesión). | 15 |
| H-8 | Media | Catálogo de destinos/horarios/precios hardcodeado en `destinos.jsx` («referencial», no consume el backend). | 5, 23 |
| H-9 | Media | `encomiendas.estado` sin CHECK en BD (la app valida; el esquema no). | 14 |
| H-10 | Media | Resumen de reportes mezcla anclas temporales: ingresos por fecha de pago vs. boletos/ocupación por fecha de salida. | 18, 19 |
| H-11 | Media | `HttpMediaTypeNotSupportedException` no manejada → HTTP 500 en vez de 415. | 22 |
| H-12 | Baja | `.env.example` declara `transporte_db`, `application.yml` usa `transporte_bd`. | 23 |
| H-13 | Baja | Sin respaldo verificado de la base (`pg_dump`) y `ddl-auto: update` en lugar de migraciones versionadas. | 23 |
| H-14 | Baja | Índice único duplicado en `asientos(viaje_id, numero_asiento)` (`asientos_viaje_id_numero_asiento_key` + `uk_asiento_viaje_numero`). | 7 |

---

## 6. Pruebas funcionales ejecutadas (resumen)

| Flujo | Prueba | Resultado |
|---|---|---|
| Login | admin / cajero / chofer / password errónea | 200/200/200/401 |
| Acceso público | `/sucursales`, `/roles`, `/viajes`, `/viajes/1/asientos` | 200 |
| RBAC | cajero→reportes/usuarios/encomiendas 403; admin→200; sin-token→403 | Correcto |
| Boleto | venta (BOL-…-21), doble venta misma asiento, escaneo cajero→403, escaneo chofer→USADO, cancelar tras uso→400, cancelar→CANCELADO | Correcto |
| Encomienda | registro (GUI-…-21), 5 transiciones de estado, transición inválida→400, tracking 6 movimientos | Correcto |
| Portal cliente | registro 201, comprar (BOL-…-23), mis-boletos, solicitar (GUI-…-22), mis-encomiendas | Correcto |
| Pagos | POST 201 / GET 200 (pago de prueba S/ 25.00); CONTRA_ENTREGA queda PENDIENTE | Correcto |
| Reportes | 11 endpoints/resumen + CSVs | 200 |
| Data tools | admin generar-viajes-prueba | 200 (sin duplicar) |
| Usuarios | admin crea cajero_aud | 201 |

---

## 7. Datos generados por la auditoría y limpieza

Se crearon datos de prueba en `transporte_bd` (se recomienda limpiarlos si se
desea volver al estado inicial):

| Tabla | Registros |
|---|---|
| `boletos` | ids 21 (USADO), 22 (CANCELADO), 23 (ACTIVO) — BOL-20260930-0000NN |
| `encomiendas` | ids 21 (ENTREGADO), 22 (RECIBIDO) — GUI-20260930-0000NN |
| `pagos` | ids 1–6 (el 4 es CONTRA_ENTREGA PENDIENTE; el 6 es de prueba del flujo de pagos) |
| `movimientos_encomienda` | +6 movimientos responsabilizados a `usuario_id=2` |
| `clientes` | ids 21–22 (dni 79999709, 78887096) |
| `usuarios` | id 23 `cajero_aud` (rol 2, sucursal 1) |

Comando de limpieza manual (a criterio del usuario): eliminar en orden
`movimientos_encomienda`, `encomiendas`, `boletos`, `pagos`, `clientes`,
`usuarios` para `id >= 21` (y `usuario_id >= 21`), y restaurar a
`DISPONIBLE` los asientos 11, 545 y el asiento del boleto 23.

---

## 8. Discrepancias principales contra el brief

1. **Roles:** brief pide `ADMIN/VENDEDOR/OPERADOR_ENCOMIENDAS`; el sistema define
   `ROLE_ADMIN/ROLE_CAJERO/ROLE_CHOFER/ROLE_CLIENTE` (H-1).
2. **Estados de encomienda:** brief pide `CARGADO_EN_BUS` y `LLEGADO_A_DESTINO`;
   el sistema usa `EN_TRANSITO` / `EN_DESTINO` (H-2).
3. **Documento del docente (PDF «PROYECTO WEB ENTREGABLE 2.0 SEM 14»):** NO
   corresponde al sistema transportes (es un website informativo del curso
   Arquitectura Web); se documenta como **NO APLICA** y no se usó como fuente.

---

## 9. Recomendaciones priorizadas

1. Alinear denominaciones con el brief (roles y estados de encomienda) o
   documentar la equivalencia funcional.
2. Reparar/limpiar los asientos vendidos sin boleto (H-3) antes de reportar
   ocupación.
3. Unificar el criterio temporal del módulo de reportes (H-10).
4. Devolver 401 sin token y 415 para Content-Type no soportado (H-4, H-11).
5. Conectar el catálogo de destinos/horarios al backend (H-8) o marcarlo
   claramente como demo.
6. Añadir CHECK de estado de encomienda en BD (H-9) y emitir el respaldo
   `pg_dump` (H-13).

---

## 10. Cómo ejecutar el sistema (receta verificada)

```powershell
# 1. PostgreSQL 17 en ejecución (base transporte_bd; usuario postgres, clave externa al repositorio)
# 2. Backend (Java 17 Temurin + 89 jars de dependencias; login F5 ya diagnosticado)
powershell.exe -File "%TEMP%\opencode\start_backend.ps1"   # deja el backend UP en :8080
# 3. Frontend (otra terminal)
cd frontend\Web-Intiwatana
npm install
npm run dev                                                 # http://localhost:5173
```

---

## 11. Anexo — Fuentes de código verificadas

`config/SecurityConfig.java`, `config/DataInitializer.java`, `config/AuditConfig.java`,
`security/JwtUtil.java`, `service/impl/*` (Boleto, Encomienda, Reporte, Auth),
`controller/{Auth,Boleto,Encomienda,Reporte,Pago,Viaje,Cliente,Usuario,AdminTools,Sucursal,Ruta,Bus}Controller.java`,
`dto/request/BoletoRequest.java`, `exception/GlobalExceptionHandler.java`,
`resources/application.yml`, `pom.xml`, `frontend\Web-Intiwatana\src\App.jsx`,
`frontend\Web-Intiwatana\src\data\destinos.jsx`.

**Nota de trazabilidad:** la auditoría previa `AUDITORIA-FINAL-INTIWATANA.md`
(2026-01) queda como antecedente; las correcciones S-1 a S-6 y D-1 a D-5
informadas allí fueron verificadas como presentes en el código actual.