# MATRIZ FLUJO DE RESERVA Y PAGO SEGURO — INTIWATANA

> El contenido de este documento se basa **únicamente** en la estructura y los aplicativos
> existentes en INTIWATANA, y describe la implementación mínima compatible, inspirada en el
> enunciado "IMPLEMENTACIÓN EMPRESARIAL SEGURA: RESERVA TEMPORAL, PAGO EN CAJA, YAPE MANUAL,
> BOLETO, CIERRE POR VIAJE Y LIQUIDACIÓN AL CONDUCTOR".

Fecha: 2026-10-09 · Entorno QA: `backend 8081 (perfil dev, flyway desactivado)` · DB `intiwatana_flujo_qa`
(historial Flyway: baseline v6, v7, v8 aplicadas; **la próxima migración será V9**).
Gates baseline: `npm run lint` = 0, `npm run build` = 0, `mvn clean verify` = BUILD SUCCESS (110 unit + 28 ITs).

Este documento se completa **antes** de cualquier modificación. Nada se modifica hasta cerrar esta matriz.

---

## 1. Modelo comercial actual (reglas estables que NO deben romperse)

- `Retencion` (tabla `retenciones`): ciclo `RETENIDO → PENDIENTE_PAGO → CONFIRMADO`, o `VENCIDO`/`CANCELADO`.
  No existe `PENDIENTE_VERIFICACION` en `retenciones` ni en la CHECK `retenciones_estado_check`.
- `Boleto` y su QR de embarque **solo** se emiten en `confirmarPago` (estado `CONFIRMADO`). No existe boleto
  mientras la reserva esté `RETENIDA`/`PENDIENTE_PAGO` (documentado en `Retencion.java:22-24`).
- `Pago` (`pagos`): `estado PENDIENTE|COMPLETADO|FALLIDO`; se crea en `confirmarPago` (caja/autorizado).
  Los reportes y la caja suman únicamente `Pago COMPLETADO`; las tarjetas pendientes **no** son ingresos.
- `confirmarPago` es transaccional: lock pesimista, asiento `RESERVADO → VENDIDO`, boleto + QR, Pago `COMPLETADO`.
- Idempotencia: `retenciones.idempotency_key` + índice único parcial (replay devuelve la retención previa).
- `RecuperacionResponse` (`GET /ventas/recuperar?documento=`) devuelve reservas + boletos de un documento.
- Auditoría Haystack `AuditoriaService` registra las operaciones de venta.
- Enums verificados: `EstadoRetencion{RETENIDO,PENDIENTE_PAGO,CONFIRMADO,VENCIDO,CANCELADO}`,
  `MetodoPago{EFECTIVO,YAPE,PLIN,TARJETA_CREDITO,TARJETA_DEBITO,TRANSFERENCIA}`, `EstadoPago{PENDIENTE,COMPLETADO,FALLIDO}`.

## 2. Inventario verificado (inspección de código + DB QA)

| # | Función | Estado | Componente actual | Dónde |
|---|---|---|---|---|
| B1 | Crear retención (bloqueo temporal asientos) | EXISTE | `POST /ventas/retencion` `permitAll` | `VentaController.java:41-42`, `VentaServiceImpl.crearRetencion:207` |
| B2 | `expiresAt` calculado por el backend | EXISTE | `ahora + holdMinutos` (sin margen vs salida) | `VentaServiceImpl:285` |
| B3 | Reservar a PENDIENTE_PAGO | EXISTE | `POST /retencion/{ref}/reservar` `permitAll` | `VentaController.java:58-59`, `VentaServiceImpl:499` |
| B4 | Confirmar pago (caja) → boleto/QR/VENDIDO | EXISTE | `POST /reservas/{ref}/confirmar` (ADMIN/CAJERO/VENDEDOR) | `VentaController.java:104-105`, `VentaServiceImpl:565` |
| B5 | Vencimiento automático + liberación | EXISTE | `RetencionScheduler` cada 60 s + `expirar()`/`liberarAsientos()` | `VentaServiceImpl:973-1061` |
| B6 | Idempotencia | EXISTE | `idempotency_key` + `findByIdempotencyKey` | `VentaServiceImpl:209-221` |
| B7 | Desbloqueo manual (caja) | EXISTE | `POST /reservas/{ref}/liberar` → `CANCELADO` | `VentaController.java:124-125`, `VentaServiceImpl:771` |
| B8 | Venta presencial | EXISTE | `POST /ventas/presencial` (solo EFECTIVO) | `VentaController.java:114-115`, `VentaServiceImpl:429-451` |
| B9 | Constancia PDF | EXISTE | `GET /retencion/{ref}/constancia.pdf` | `VentaController.java:67-68` |
| B10 | Boleto PDF + QR embarque | EXISTE | `GET /boleto/{n}/pdf` | `VentaController.java:76-77`, `PdfVentaService` |
| B11 | Recuperar reservas de un documento (público) | EXISTE | `GET /ventas/recuperar?documento=` | `VentaController.java:85-86` |
| B12 | Bandeja de reservas por estado (caja/admin) | EXISTE | `GET /ventas/reservas?estado=` | `VentaController.java:96-97` |
| B13 | Límites anti "reserva falsa" (1 por doc+viaje, 2 por doc, 2 por teléfono) | **NO EXISTE** | — | — |
| B14 | Margen de cierre web antes de salida (R-de-…) | **NO EXISTE** | — | `VentaServiceImpl:285`, `:529` (sin margen) |
| B15 | Configuración admin de tiempos (retención/pendiente/cierre/tolerancia c/historial) | **NO EXISTE** | solo `app.ventas.*` vía env/`@Value` | `VentaServiceImpl:175-187` |
| B16 | Configuración admin del QR Yape (c/historial y validación de imagen) | **NO EXISTE** | `MetodoPago.YAPE` solo como enum | — |
| B17 | Declaración de pago Yape por el cliente (PENDIENTE_VERIFICACION) | **NO EXISTE** | el cliente solo elige YAPE en `reservar` y su asiento queda 24 h sin prueba | — |
| B18 | Bandeja cajero de verificación Yape: aprobar/rechazar con motivo | **NO EXISTE** | caja confirma directo con cualquier referencia | — |
| B19 | Operación Yape no reutilizable | **NO EXISTE** | sin guard de `pagos.referencia` | `Pago.java:40-41` |
| B20 | Cierre por viaje (Cajero) | **NO EXISTE** | reportes por viaje son ADMIN-only (`ReporteService`) | — |
| B21 | Cierre general por fecha (~Resumen de Caja) | EXISTE (derivado) | frontend `CierreCaja.jsx` sobre pagos/viajes/reservas | `components/caja/CierreCaja.jsx` |
| B22 | Liquidación al conductor (política/ciclo) | **NO EXISTE / NO APLICA** | sin rastro de política de entrega de dinero al chofer | ver §4 |
| B23 | Auditoría de ventas | EXISTE | `AuditoriaService` + `AuditoriaController` | — |
| B24 | Rate limiting | PARCIAL | solo login (`LoginRateLimiter`) | — |
| B25 | CAPTCHA / OTP | NO EXISTE (infra) | sin infraestructura previa | ver §4 |
| F1 | Flujo público Pasaje→reserva con datos pasajero | EXISTE | `PasajesPage.jsx` (paso1 datos, paso2 éxito) | `PasajesPage.jsx:1035-1113` |
| F2 | Selección de método de pago (incl. YAPE) + operación en `reservar` | EXISTE (PARCIAL) | radio EFECTIVO/YAPE/PLIN/…; `necesitaDatos` pide operación | `PasajesPage.jsx:680-816` |
| F3 | Modal de éxito con referencia/constancia/TimerReserva | EXISTE | `ModalReserva/TimerReserva/ReservaProgress` | — |
| F4 | "Mis reservas" del cliente con estados (pendiente/en verificación/aprobado/rechazado/vencida) | PARCIAL | solo `GET /boletos/mis-boletos` (CLIENTE) + `ModalRecuperar` por documento | `ClienteDashboard.jsx`, `ModalRecuperar` |
| F5 | Pantalla Yape (QR oficial + "YA REALICÉ EL PAGO" + captura) | **NO EXISTE** | — | — |
| F6 | Menú ADMIN (Configuración Reservas/Yape) | **NO EXISTE** | menú ADMIN tiene Gestión del Portal (patrón a reutilizar) | `AdminDashboard.jsx` |
| F7 | Bandeja cajero "Pagos Yape por verificar" | **NO EXISTE** | menú CAJERO tiene "Pagos"/"Operaciones" (patrón a reutilizar) | `AdminDashboard.jsx` |
| F8 | "Cierre del viaje" en caja | **NO EXISTE** | solo "Resumen de Caja" por fecha | `AdminDashboard.jsx`, `CierreCaja.jsx` |

## 3. Alcance mínimo compatible (implementar)

### 3.1 Backend
1. **Migración V9 (aditiva, no toca V1..V8 ya aplicadas)**: tablas `parametro_venta`,
   `parametro_venta_historial`, `configuracion_yape`, `configuracion_yape_historial`,
   `declaracion_pago` (estado `PENDIENTE_VERIFICACION|APROBADO|RECHAZADO`, FK única a retención);
   columna `retenciones.tolerancia_usada boolean NOT NULL DEFAULT false`; seed de parámetros
   (retención 5, PENDIENTE_PAGO 30, cierre web 30, tolerancia Yape 5, todos con rango);
   índice único parcial idempotente en `pagos.referencia` **solo si hoy no existen duplicados**
   (si hay duplicados, queda guarda en código y se documenta).
2. **Tiempos configurables**: `ConfiguracionVentaService` (lee `parametro_venta`, fallback
   `app.ventas`/default). `expiresAt` = `min(ahora+d, salida−margenCierreWeb)` para canal WEB;
   salida−margen ≤ ahora → rechazo orientando a atención presencial (canal CAJERO/Presencial sin margen).
   Endpoints ADMIN: `GET/PUT /ventas/configurar/tiempos`, `GET …/historial` con validaciones de rango,
   motivo obligatorio y auditoría.
3. **Límites anti reserva falsa** en `crearRetencion` (después del cortocircuito de idempotencia):
   ≥1 activa (`RETENIDO|PENDIENTE_PAGO` no vencida) para mismo documento+viaje → 409;
   ≥2 activas por documento → 409; ≥2 por teléfono → 409. Nuevas consultas en `RetencionRepository`.
4. **Yape administrado**: `POST /ventas/configurar/yape` (admin, multipart; validación MIME real +
   magic bytes + dimensiones + peso + rechazo de SVG/ejecutables; almacena imagen con el servicio
   oficial de imágenes del portal y guarda referencia segura + sha256, **nunca Base64 en BD**);
   `GET /ventas/configurar/yape` (admin, histórico), `GET /ventas/configurar/yape/activa` (público,
   solo datos presentables + número enmascarado 912****34).
5. **Declaración Yape (cliente)**: `POST /ventas/retencion/{ref}/pago-yape` (público, protegido por
   documento igual que `/recuperar`): exige retención activa + `ahora ≤ expiresAt` + sin declaración
   previa pendiente/aprobada; valida operación e importe; captura opcional como evidencia auxiliar;
   si se declara en los últimos `tolerancia` min → **una única** extensión `expiraEn = ahora+tolerancia`
   con `tolerancia_usada=true` (no renovable); la declaración NO genera boleto ni asiento vendido.
6. **Bandeja cajero**: `GET /ventas/pagos-yape/pendientes` (CAJERO/ADMIN/VENDEDOR, con tiempo restante);
   `POST /ventas/pagos-yape/{id}/aprobar` → reutiliza `confirmarPago` (metodo YAPE, referenciaPago=operación)
   + marca declaración `APROBADO`/verificador; `POST /ventas/pagos-yape/{id}/rechazar` (motivo obligatorio)
   → `RECHAZADO`, no libera asiento si aún hay tiempo (el cliente puede re-declarar); si la reserva ya
   venció → `RECHAZADO` automático con motivo del sistema.
7. **Operación no reutilizable**: en `confirmarPago` con metodo YAPE y `referenciaPago`, rechazo si ya
   existe un `Pago` u otra declaración aprobada con esa operación (guarda transaccional + índice si aplica).
8. **Cierre por viaje**: `GET /ventas/cierre/viaje/{viajeId}` (CAJERO/ADMIN/VENDEDOR): agregación
   derivada read-only (asientos vendidos, boletos, `Pago COMPLETADO` por método, pendientes,
   vencidas/rechazadas, total contable vs deficitario). **Sin tabla nueva** (evita doble conteo y no
   inventa un modelo contable). El "Resumen de Caja" por fecha (existente) queda como único consolidado financiero.
9. **`RecuperacionResponse` extendido**: por reserva se incluye declaración Yape (estado/motivo/
   operación enmascarada) y la configuración Yape activa, para que "Mis reservas" muestre los 5 estados.

### 3.2 Frontend (patrones existentes)
- `PasajesPage.jsx`: tras reservar con metodo YAPE, mostrar paso "PAGAR CON YAPE" (QR oficial activa +
  receptor/importe/monto) con botón "YA REALICÉ EL PAGO" → formulario operación/monto/fecha-hora + captura
  opcional → estado "PAGO EN VERIFICACIÓN" (cabina cajero). Los textos/avisos usan el estilo `iwr-*` actual.
- `ModalRecuperar`/"Mis reservas": listar reservas con su estado; para pendiente: contador, opción Yape y
  aviso de pago en caja; en verificación: mensaje esperando caja; aprobado: botón boleto/PDF + QR;
  rechazado: motivo + re-declarar si sigue vigente; vencida: sin acciones.
- `AdminDashboard.jsx`: sección ADMIN "CONFIGURACIÓN → Reservas y pagos" con pestañas TIEMPOS (campo+guardar
  con motivo + historial) y YAPE (previsualización + formulario imagen/receptor/número/instrucciones +
  historial). Sección CAJERO "Pagos Yape por verificar" (aprobar/rechazar con confirmación y motivo) y
  "Cierre del viaje" (generar/ver por viaje). Sin nuevos componentes pesados: reutilizar estilos `adm-*`.

### 3.3 No se implementa (decisiones explícitas)
- **Liquidación al conductor**: no existe política real de entrega de dinero al chofer (0 rastros). El
  enunciado condiciona su implementación a que exista esa política y sus checks a "si aplica". Se documenta
  como **NO APLICABLE** (fase futura propuesta) y como riesgo del flujo de Yape pendiente no liquidado.
- **CAPTCHA/OTP**: no existe infraestructura previa en el aplicativo; se documenta como **fase futura**.
  Las protecciones que sí se implementan: límites por documento/teléfono/sesión, idempotencia, margen de
  cierre, ventana única de verificación, operación no reutilizable y auditoría.
- **Datos de facturación/vendedor/impuestos**: fuera del alcance del enunciado.
- **Doble conteo caja-viaje**: cierre por viaje es derivado read-only; el financiero es solo el Resumen por fecha.

## 4. Dependencias / riesgos observados
- **Dependencia**: la DB QA usa flyway en el contexto de pruebas (aplica V9 automáticamente); el backend
  QA dev (8081) NO usa flyway → se aplicará el DDL de V9 manualmente sobre `flujo_qa` (igual que se hizo
  con los índices de V4/V5) antes de la evidencia en vivo.
- **Riesgo alto**: cambio de duraciones (retención 15→5 min, pendiente 1440→30 min) afecta reservas nuevas
  únicamente; reservas ya creadas conservan su vencimiento original. Se documenta en el informe final.
- **Riesgo**: `pagos.referencia` puede contener duplicados heredados (EFECTIVO "OPQA-…"); el índice único
  se crea solo si no hay duplicados y la guarda en código siempre está activa.
- **Riesgo**: aprobar = `confirmarPago` reutilizado; cualquier excepción (vencida/asiento no disponible)
  se propaga y la declaración queda pendiente para corrección o se rechaza con motivo automático.

## 5. Plan de verificación (enunciado)
- Unidad: `mvn clean verify`; aceptación: `npm run lint`, `npm run build` (EXIT 0, sin skipTests).
- Nuevas pruebas (clase tipo `AuditoriaFinalIT` sobre DB QA): límites R06-R08 documentados, margen de
  cierre web, tiempos configurables con historial, declaración→PENDIENTE_VERIFICACION, aprobación→boleto/QR/
  VENDIDO/operación única, rechazo→no boleto, vencida→no aprobable, cierre por viaje coherente con la
  caja por fecha (sin doble conteo), permisos 403 a roles indebidos y a ROLE_CAJERO sobre configuración.
- Evidencias y veredicto: `reports/FLUJO-PAGO-SEGURO-RESULTADO.md` + carpeta de evidencia.

**Conclusión de la matriz**: con el alcance mínimo de §3 se cubren todas las funciones del enunciado
(fuera de las declaradas NO APLICABLES en §3.3). Se procede a respaldos (tamaño + SHA-256 en
`reports/respaldo-flujo-pago-seguro/`) y luego a la implementación.