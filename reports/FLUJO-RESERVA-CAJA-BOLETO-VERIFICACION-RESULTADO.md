# Verificación integral del flujo RESERVA → CAJA → BOLETO → PASAJERO → CHOFER

**Proyecto:** Sistema de Transporte Interprovincial INTIWATANA
**Fecha de ejecución:** 2026-10-04 (09:5x – 11:2x)
**Alcance:** verificación de extremo a extremo **sin reconstruir** los módulos cerrados, con aplicación de **correcciones mínimas** solo ante fallas reales demostradas.
**Base de datos de la verificación:** `intiwatana_recorrido_qa` (QA aislada, creada para esta prueba). **Ninguna operación sobre `transporte_bd` ni producción.**

---

## 1. Resultado ejecutivo

| # | Etapa | Resultado | Evidencia clave |
|---|-------|-----------|-----------------|
| 1 | Viaje QA visible desde Portal Público | **PASS** | 1 viaje, S/ 55.00, bus QA-VER-01, 40/40 asientos |
| 2 | Reserva como invitado (retención → pendiente) | **PASS** | HTTP 201 + HTTP 200, estado `PENDIENTE_PAGO` |
| 3 | Constancia pendiente sin QR válido | **PASS** | `validoParaEmbarque:false`, PDF sin imagen QR |
| 4 | "Mis Reservas" antes de pagar | **PASS** | 2 reservas, **Boletos (0)**, "No hay boletos confirmados" |
| 5 | Cajero confirma el pago (transacción única) | **PASS** | 1 pago `COMPLETADO`, 1 boleto, 1 QR, 1 PDF |
| 6 | Idempotencia de la confirmación | **PASS** | 2ª y 3ª llamada → HTTP 200 mismo boleto, sin duplicados |
| 7 | Boleto PDF (descarga, protección) | **PASS** | A4 1 página, QR incrustado, doc. incorrecto → 404 |
| 8 | Pasajero recupera su boleto | **PASS (con 1 corrección)** | Boletos (1) + PDF; ruta corregida en la tarjeta |
| 9 | Chofer ve pasajero confirmado | **PASS** | Tabla de boletos del viaje, 1 pendiente de uso |
| 10 | Chofer valida QR (1er escaneo) | **PASS** | HTTP 200 → `USADO` |
| 11 | 2º escaneo del mismo QR | **PASS** | HTTP 400 "Boleto ya utilizado" |
| 12 | QR desconocido | **PASS** | HTTP 400 "Código no reconocido" |
| 13 | QR de otro viaje (otro chofer) | **PASS** | HTTP 403 "No puedes validar un boleto correspondiente a otro viaje" |
| 14 | Boleto cancelado | **PASS** | HTTP 400 "…o está cancelado. Estado: CANCELADO" |
| 15 | Vencimiento perezoso (sin barrido) | **PASS** | `VENCIDO` + asiento `DISPONIBLE`, confirmarlo → HTTP 409 |
| 16 | Barrido programado (`RetencionScheduler`) | **PASS** | Log: "Barrido de reservas vencidas: 1 liberadas" |
| 17 | Doble venta / doble clic / concurrencia | **PASS** | Ver matriz §7 |

**Correcciones aplicadas: 1** (frontend, 3 líneas efectivas). **Backend: 0 cambios.**

---

## 2. Entorno QA aislado (demostración previa obligatoria)

| Ítem | Valor |
|------|-------|
| Base QA | `intiwatana_recorrido_qa` (Flyway V1–V4, 17 tablas) |
| Backend QA | puerto **8081**, perfil `test` (`ddl-auto: validate`, Flyway activo) |
| Frontend QA | Vite puerto **5180** con `VITE_PROXY_TARGET=http://localhost:8081` |
| Backend dev (no tocado) | puerto 8080 → `transporte_bd` |
| Frontend dev (no tocado) | Vite puerto 5173 |

**Aislamiento verificado con `pg_stat_activity`:**

- Conexiones del backend QA (arrancado 10:28:50) → **3 y solo 3** hacia `intiwatana_recorrido_qa`.
- Las 3 conexiones hacia `transporte_bd` corresponden al backend **dev** del puerto 8080 (inicio 10:09:43), ajeno a esta verificación.
- Contenido de `transporte_bd` tras la verificación: `buses QA = 0`, `viajes QA = 0`, `retenciones = 0`, `boletos QA = 0`.

> Durante la ejecución el equipo se reinició y los procesos QA (8081/5180) y los dev (8080/5173) se detuvieron. Se relanzaron; los datos de la base QA persistieron sin pérdidas y los resultados ya obtenidos antes del reinicio se volvieron a comprobar tras el relanzamiento.

---

## 3. Datos QA creados (mínimos y señalizados)

- Ruta `QA-REC-01`: Terminal Huamanga → Terminal Vilcas Huamán, 55 km, 3.0 h, precio base S/ 55.00.
- Bus `QA-VER-01`: ECONÓMICO, 40 asientos, activo.
- Viaje **#1**: ruta `QA-REC-01`, bus `QA-VER-01`, conductor **chofer1**, salida **2026-10-05 09:00** (futura), llegada estimada 12:00, precio adulto S/ 55.00 / niño S/ 38.50, estado `PROGRAMADO`, observación `QA-RECORRIDO`.
- 40 asientos `01`–`40` en 4 columnas (VENTANA en 1 y 4), todos `DISPONIBLE` al inicio.
- Pasajero invitado QA: documento `9000…0001`, teléfono `9000…0001`, correo QA.
- Usuarios existentes de la prueba: cajero QA (rol `ROLE_CAJERO`), choferes QA `chofer1`/`chofer2`/`chofer3`, administrador QA.

---

## 4. Flujo verificado, paso a paso

### 4.1 Búsqueda desde el Portal Público
`/pasajes?origen=Ayacucho&destino=Vilcas Huaman` → 1 viaje, S/ 55.00, bus QA-VER-01, "40/40", botón "ELEGIR ASIENTO". **0 errores de consola, 0 peticiones fallidas.**

### 4.2 Reserva como invitado
Selección del asiento 01 → formulario de invitado → "Continuar con la reserva":

- `POST /api/v1/ventas/retencion` → **201** (`RETENIDO`)
- `POST /api/v1/ventas/retencion/{ref}/reservar?metodoPago=EFECTIVO` → **200** (`PENDIENTE_PAGO`)

Pantalla: "Reserva pendiente de pago — **NO VÁLIDA PARA EMBARQUE**", número de referencia y "DESCARGAR CONSTANCIA (PDF)".
Estado verificado en base: `PENDIENTE_PAGO`, `requierePago: true`, `validoParaEmbarque: false`, **0 pagos y 0 boletos**, asiento 01 `RESERVADO`.

### 4.3 "Mis Reservas" antes de pagar
Documento del pasajero → "Reservas (2)" (la vigente + una cancelada de un intento previo) y **"Boletos (0) — No hay boletos confirmados con ese documento."** Correcto: una reserva pendiente **no** muestra boleto ni QR.

### 4.4 Cajero: confirmación del pago
Panel del Cajero (6 módulos) → **Reservas** → fila de la reserva `Pendiente de pago` → "Gestionar" → documento del comprador → "Confirmar pago y emitir boleto":

- `POST /api/v1/ventas/reservas/{ref}/confirmar` → **200**, mensaje "Pago confirmado. Boletos emitidos."
- Resultado visible en UI: **BOL-20261003-000001 · Asiento N°01** + botón PDF.

**Transacción única verificada en base de datos:**

| Entidad | Resultado |
|---------|-----------|
| Retención | `CONFIRMADO` |
| Pago | 1 fila — S/ 55.00, `EFECTIVO`, `COMPLETADO`, cajero QA |
| Boleto | 1 — `BOL-20261003-000001`, `ACTIVO`, pago asociado |
| Asiento 01 | `VENDIDO` |

Tras confirmar, la reserva **desaparece de la lista de pendientes** (el listado filtra `RETENIDO`/`PENDIENTE_PAGO`), por lo que el botón "Confirmar pago" queda fuera del alcance: no hay forma de volver a cobrar desde la UI.

### 4.5 Idempotencia de la confirmación
Se repitió el `POST .../confirmar` (equivalente a doble clic o reintento de red):

- HTTP **200** con el **mismo** boleto (`id 1`, `BOL-20261003-000001`).
- Base de datos sin cambios: `pagos = 1`, `boletos = 1`, `boletos activos = 1`, `ventas del asiento 01 = 1`.

### 4.6 Boleto PDF
`GET /api/v1/ventas/boleto/{numero}/pdf?documento=…`

- Documento correcto → **200**, 3 523 bytes, `%PDF-1.5`, `MediaBox 595×842` (**A4**), **1 página**, imagen QR incrustada (200×200).
- Documento incorrecto → **404** "Boleto no encontrado" (el PDF está protegido).
- Sin autenticación también responde, **por diseño**: `SecurityConfig` declara `GET /api/v1/ventas/boleto/**` como `permitAll` para la recuperación por documento (comentado explícitamente en el código). El acceso real está protegido por la combinación *número de boleto + documento del comprador*.

### 4.7 Pasajero recupera su boleto
"Mis Reservas" con el documento del pasajero → la reserva aparece `CONFIRMADO`, "Pago confirmado. Boleto válido para embarque." y **Boletos (1)** con el número de boleto, el asiento y el botón PDF. La API además entrega `codigoQr` y la imagen QR.

### 4.8 Chofer: vista y validación
`/chofer/qr` (chofer1):

- "1 viaje asignado", "#1 · Huamanga → Vilcas Huaman · lun, 05 oct 09:00 · PROGRAMADO".
- Tabla "Boletos del viaje seleccionado — 1 pendiente(s) de uso": `BOL-20261003-000001 · Pasajero QA · Asiento 01 · ACTIVO`.

**Matriz de validación de QR (todas contra el backend, sin simulación):**

| Escenario | HTTP | Resultado |
|-----------|------|-----------|
| Primer escaneo del QR | 200 | "Boleto validado", estado `USADO`, fecha/hora de uso |
| Segundo escaneo del mismo QR | 400 | "Boleto ya utilizado — El boleto ya fue usado o está cancelado. Estado: USADO" |
| Código inexistente | 400 | "Código no reconocido — QR inválido o boleto no encontrado" |
| Boleto de otro viaje (chofer2) | **403** | "No puedes validar un boleto correspondiente a otro viaje" |
| Boleto cancelado | 400 | "…o está cancelado. Estado: CANCELADO" |
| Boleto de su propio viaje (chofer1) | 200 | Validado correctamente |

> Los errores de consola observados durante las pruebas son las respuestas 400/403 **esperadas**; no hubo errores de JavaScript.

---

## 5. Vencimiento de la reserva (solo QA)

Configuración aplicada **únicamente al backend QA**: `PENDING_PAYMENT_MINUTES=1` y `RETENCION_LIMPIEZA_MS` alterado según el caso.

1. Reserva creada en el asiento 02 → `PENDIENTE_PAGO`, expira a los 60 s; asiento 02 `RESERVADO`.
2. **Vencimiento perezoso** (con `RETENCION_LIMPIEZA_MS=3600000`, es decir, sin barrido posible en la ventana): al consultar la reserva, el backend larecognized como `VENCIDO` y liberó el asiento 02 → `DISPONIBLE`, **sin pago y sin boleto**.
3. El cajero intentó confirmarla → **409** "La reserva ya no está vigente y no puede confirmarse".
4. **Barrido programado** (con `RETENCION_LIMPIEZA_MS=10000`): reserva en el asiento 08, sin tocar la API, y a los 2 s de la expiración el log registró
   `VentaServiceImpl - Barrido de reservas vencidas: 1 liberadas`; retención `VENCIDO` y asiento 08 `DISPONIBLE`.

Al finalizar se restauró la configuración QA por defecto.

---

## 6. Reglas de negocio comprobadas

- El precio siempre lo decide el backend (el frontend solo muestra; el total de la reserva y del boleto provino de la API).
- La constancia pendiente **no** contiene QR de embarque ni se presenta como boleto confirmado.
- El teléfono del pasajero se usó solo como dato operativo de la reserva; no se integró ningún canal comercial, WhatsApp, llamadas automáticas, correo ni pasarela de pago.
- No se creó ningún módulo, endpoint ni componente paralelo a los cerrados.
- No se insertó ningún viaje ficticio en `transporte_bd`.

---

## 7. Doble venta (matriz de casos, solo QA)

| Caso | Técnica | Resultado |
|------|---------|-----------|
| Web retiene y **otra** reserva intenta el mismo asiento | 2 POST `/ventas/retencion` | 1 → **201**, 1 → **409** "El asiento ya no está disponible" |
| Dos navegadores, mismo asiento, **simultáneos** | 2 procesos en paralelo | 1 → **201**, 1 → **409** |
| Cajero procesa y la web intenta retener el mismo asiento | crear → pagar → reintento web | Confirmación **200**; reintento web **409** |
| Doble clic / reintento de red en caja | POST `/confirmar` repetido | HTTP 200, **mismo boleto**, sin segundo pago |
| Dos confirmaciones **simultáneas** de la misma reserva | 2 procesos en paralelo | **1 solo boleto y 1 solo pago** en base de datos |

**Invariante final comprobada en la base QA:** ningún asiento tiene más de un boleto; los asientos 01, 03 y 07 (vendidos) tienen **exactamente un boleto y un pago `COMPLETADO`** cada uno, y el asiento 09 quedó libre tras la cancelación de prueba. Es decir, **un viaje + un asiento = como máximo una venta confirmada**.

---

## 8. Corrección mínima aplicada (única)

**Archivo:** `frontend/Web-Intiwatana/src/components/PasajesPage.jsx` — tarjeta de boleto de "Consultar mis reservas".

- **Falla demostrada:** la tarjeta renderizaba `{b.viaje?.ruta?.origen?.ciudad} → {b.viaje?.ruta?.destino?.ciudad}`, pero el endpoint `GET /api/v1/ventas/recuperar` devuelve en cada boleto un resumen del viaje **sin la ruta anidada**. Resultado en pantalla: `→ · Asiento N°01` (itinerario vacío).
- **Corrección:** se obtiene el itinerario de la reserva que emitió el boleto (misma respuesta, sin endpoint nuevo) y, si no existe, **no se inventa texto**.
- **Resultado tras la corrección:** `Huamanga → Vilcas Huaman · Asiento N°01`. Sin errores de consola.
- **Alcance:** solo presentación; no cambia DTOs, endpoints ni reglas de negocio. No se modificó código backend.

---

## 9. Observaciones (no bloqueantes, sin cambios aplicados)

1. `GET /api/v1/ventas/boleto/**` es `permitAll` por diseño; la protección real depende de *número de boleto + documento*. Se deja constancia para la revisión de seguridad.
2. La ficha de una reserva `VENCIDO` devuelve un `mensaje` genérico de "falta confirmar el pago" en lugar de indicar que venció. El estado y el asiento libre sí son correctos.
3. "Mis Reservas" no muestra el estado del boleto (`ACTIVO`/`USADO`) ni la imagen QR; el pasajero debe abrir el PDF para ver el QR.
4. `SEAT_HOLD_MINUTES`, `PENDING_PAYMENT_MINUTES` y `ALLOW_PENDING_PAYMENT` siguen siendo **decisiones comerciales pendientes**; los valores cortos usados aquí existieron solo en el proceso QA.
5. Al cancelar un boleto, el **asiento se libera** pero el pago permanece `COMPLETADO`. No es una doble venta (no se generó un segundo boleto), pero la **política de anulación/reembolso no está definida**; conviene decidirla antes de producción.
6. El arranque local con F5 sigue **pendiente de verificación manual** (ver `F5-ARRANQUE-VSCODE-RESULTADO.md`); la vía Maven funciona.

---

## 10. Gates de calidad

| Gate | Resultado |
|------|-----------|
| `npm run lint` (frontend) | **0 errores, 0 advertencias** |
| `npm run build` (frontend) | **Correcto** (1757 módulos) |
| `mvn clean verify` | **No ejecutado en esta ronda**: no hubo cambios en código backend. Gates previos vigentes en `CIERRE-GENERAL-SISTEMA-RESULTADO.md`. |
| Migraciones Flyway | V1–V4 aplicadas automáticamente en la base QA nueva, con `ddl-auto=validate` |

---

## 11. Limpieza y estado final

- Se detuvieron los servicios QA (backend 8081 y frontend 5180) al finalizar.
- Los servicios de desarrollo (8080 y 5173) **no se modificaron**.
- `transporte_bd` quedó **sin cambios**, verificado por consulta.
- La base `intiwatana_recorrido_qa` se conserva como evidencia; es independiente y puede eliminarse sin afectar desarrollo ni producción.

---

**Conclusión:** el flujo completo **RESERVA → CAJA → BOLETO → PASAJERO → CHOFER** funciona de extremo a extremo, con la regla de pago en caja (Caso B), idempotencia, protección contra doble venta, vencimiento y validación de QR correctamente implementados. Se detectaron y corrigieron **una** falla de presentación; el resto de hallazgos son observaciones menores documentadas.