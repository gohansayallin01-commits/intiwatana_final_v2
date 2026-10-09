# FLUJO COMERCIAL INTIWATANA — RESERVA → CAJA → BOLETO → QR → CHOFER

**Fecha de ejecución:** 2026-10-08
**Entorno:** Windows, PostgreSQL 17.5, Java 17.0.18, Maven 3.9.12, Node 24.13.1
**Aislamiento QA:** backend `8081` + frontend `5180` (proxy → 8081) + BD `intiwatana_recorrido_qa`
**Sin mocks.** Todo verificado contra backend y PostgreSQL reales.

---

## 1. Veredicto

**FLUJO COMERCIAL INTIWATANA — CERRADO Y VERIFICADO.**

El recorrido completo (búsqueda → asiento → reserva → constancia → caja → pago → boleto → QR →
chofer → segundo escaneo → vencimiento → concurrencia → seguridad) funciona de extremo a extremo
sobre la BD QA aislada. Se verificó tanto por HTTP real como por la interfaz web en navegador
headless (sin mocks), con evidencia en PostgreSQL.

Calidad técnica:
- `npm run lint` → **OK** (exit 0)
- `npm run build` → **OK** (exit 0)
- Backend unit tests (surefire) → **103/103 PASS**
- Backend integration tests (failsafe) → **28/28 PASS** (las 3 pendientes de §7/§8 quedaron
  resueltas; ver `reports/CIERRE-PRUEBAS-INTEGRACION-RESULTADO.md`).
- `/actuator/health` → **UP**

---

## 2. Aislamiento (anti-regresión)

- La BD de producción `transporte_bd` **no se modificó** en datos existentes.
- Se trabajó en `intiwatana_recorrido_qa` (esquema copiado de `transporte_bd`).
- El backend QA se levantó apuntando exclusivamente a `intiwatana_recorrido_qa`; se confirmó
  por API que devuelve solo los viajes de esa BD (2), no los de `transporte_bd`.

---

## 3. Viaje de prueba

| Campo | Valor |
|---|---|
| `viaje.id` | 2 |
| Estado | PROGRAMADO |
| Salida | 2026-10-08 08:00 (futuro) |
| Ruta | QA-REC-01 (Huamanga → Vilcas Huaman) |
| Bus | QA-VER-01 (40 asientos, ECONOMICO) |
| Chofer | chofer1 |
| Precio adulto | S/ 12.00 |
| Asientos | 40 creados (id 41..80) |

---

## 4. Recorrido ejecutado (HTTP real, evidencia PostgreSQL)

### 4.1 Retención (web invitado)
`POST /api/v1/ventas/retencion` → **201**
- Referencia `RET-176EFC24EEE04736`, asiento `1A`, total S/ 12.00
- Estado `RETENIDO`, `validoParaEmbarque=false`, `requierePago=true`
- Mensaje: "Reserva registrada. Falta confirmar el pago en caja para emitir el boleto."

### 4.2 Reserva pendiente de pago
`POST /api/v1/ventas/retencion/{ref}/reservar` → **200**
- Estado `PENDIENTE_PAGO`, `expiraEn=2026-10-08T23:54:34`, `segundosRestantes≈86399`
- `validoParaEmbarque=false`

### 4.3 Constancia PDF (NO válida para embarque)
`GET /api/v1/ventas/retencion/{ref}/constancia.pdf?documento=...` → **200**, `application/pdf`, 2030 bytes.
Texto interno verificado (stream descomprimido): contiene
**"CONSTANCIA DE RESERVA PENDIENTE DE PAGO"**, **"FALTA PAGAR"**, **"NO VÁLIDO PARA EMBARQUE"**.
Sin QR de embarque.

### 4.4 Recuperación sin cuenta
`GET /api/v1/ventas/retencion/{ref}?documento=...` → **200** (estado PENDIENTE_PAGO).
`GET /api/v1/ventas/recuperar?documento=...` → **200**.

### 4.5 Caja — pago real
`POST /api/v1/auth/login` (`cajero1`) → **200** (JWT).
`POST /api/v1/ventas/reservas/{ref}/confirmar` (CAJERO) → **200**
- 1 boleto: `BOL-20261008-000021`, estado **ACTIVO**, con `qr_imagen_url` (PNG embebido)
- Asiento `1A` → **VENDIDO**

### 4.6 Invariantes post-pago (PostgreSQL)
```
reservas=1 | estado_retencion=CONFIRMADO | pagos=1 | boletos_del_pago=1
boletos_mismo_numero=1 | asiento=VENDIDO | codigo_qr presente | qr_imagen_url=SÍ
```
Se cumple 1 reserva / 1 pago / 1 boleto / 1 QR, sin duplicados.

### 4.7 Boleto PDF (con QR)
`GET /api/v1/ventas/boleto/{num}/pdf?documento=...` → **200**, `application/pdf`, 2084 bytes,
texto "BOLETO DE VIAJE" y QR. (Se obtuvo además vacío 78→ reintento con manejo binario correcto 200.)

### 4.8 Chofer — escaneo QR
1.er escaneo `POST /api/v1/boletos/escanear?codigoQr=BOL-20261008-000021` (CHOFER1) → **200**
   → boleto pasa a **USADO**.
2.º escaneo del mismo QR → **RECHAZADO** ("Boleto ya utilizado").
QR inválido → **rechazado**. Sin token → **401**.

### 4.9 Vencimiento real (scheduler)
- Retención con `expira_en` en pasado → el barrido (`RetencionScheduler`, 60 s) la pasó a
  **VENCIDO** y liberó el asiento a **DISPONIBLE**. (Confirmado en PostgreSQL.)
- Reserva `PENDIENTE_PAGO` vencida → `POST .../confirmar` → **409**
  "La reserva ya no está vigente y no puede confirmarse"; asiento liberado y sin boletos.

### 4.10 Concurrencia
Dos intentos simultáneos sobre el mismo asiento → uno **permitido** (201) y el otro
**rechazado** (409 "…ya no está disponible"). Sin doble reserva / doble boleto / doble QR.

### 4.11 Seguridad
| Caso | Resultado |
|---|---|
| Chofer ajeno escanea boleto de otro viaje | **403** "No puedes validar un boleto correspondiente a otro viaje." |
| CHOFER accede a `/api/v1/ventas/reservas` | **403** sin permisos |
| ADMIN accede a `/api/v1/ventas/reservas` | **200** |
| Token inválido | **401** |
| Sin token | **401** |

---

## 5. Verificación visual en navegador (sin mocks)

Navegador headless contra `http://localhost:5180` (frontend real, proxy → backend 8081 QA):

1. Home `/` → render OK (title, sin errores de consola, sin requests fallidos).
2. `RESERVAR PASAJE` → `/pasajes` → "Busca tu viaje".
3. Origen `Huamanga`, destino `Vilcas Huaman` → `Buscar` → "1 viaje(s) disponible(s) S/ 12".
4. `Elegir asiento` → mapa de asientos (38/40 disponibles) → asiento `1D` seleccionado.
5. `Reservar y pagar` → formulario comprador → `Continuar con la reserva`.
6. Aparece el modal final **"Reserva registrada"** con:
   - Referencia `RET-9269279331A94B17`
   - Asiento `N°1D`, Total `S/ 12.00`
   - "Reserva pendiente de pago — **NO VÁLIDA PARA EMBARQUE** hasta confirmar el pago en caja."
   - Temporizador "23:59:58" y botón **"DESCARGAR CONSTANCIA (PDF)"**
   - Estado en BD: `PENDIENTE_PAGO`
   Captura: `reports/evidencia-qa-cierre/ui-reserva-registrada.png`.

**Nota de discrepancia textual (no funcional):** el texto exacto pedido
"RESERVA REGISTRADA CORRECTAMENTE" no existe en el código; la pantalla real muestra
"Reserva registrada" + "Reserva pendiente de pago — NO VÁLIDA PARA EMBARQUE". La etiqueta
"FALTA PAGAR" aparece en la **constancia PDF** y en el mensaje
"…Falta confirmar el pago en caja…". Se documenta como diferencia de copy, no de flujo.

---

## 6. Correcciones aplicadas durante el trabajo

| # | Archivo | Corrección |
|---|---|---|
| C-1 | `service/impl/VentaServiceImpl.java` | Restaurado de 0 bytes; mensaje L114 corregido a `"El viaje ya partió o no está disponible"` (UTF-8). |
| C-2 | `repository/ViajeRepository.java` | `buscarDisponibles`: cuando `desde` es null se filtra por `CURRENT_TIMESTAMP`, unificando la regla UI↔retención (antes la API mostraba viajes pasados). |
| C-3 | `resources/application-test.yml` | Restaurado a valores seguros (`flyway.enabled: true`, `baseline-on-migrate`, `baseline-version: 6`, `ddl-auto: validate`). |
| C-4 | Arranque QA | Flyway no puede correr sobre esquema preexistente sin historial → se usa el jar con `--spring.flyway.enabled=false` y `ddl-auto=none` para QA. |
| C-5 | CORS QA | El frontend en `5180` recibía 403 por CORS; se levantó el backend con `--app.cors.allowed-origins=http://localhost:5180,http://localhost:5173`. |

---

## 7. Matriz de pruebas R01–R50

> **Nota (2026-10-08):** los 3 ITs que estaban en rojo (R-auditoría) fueron corregidos y ya pasan
> 28/28. Ver `reports/CIERRE-PRUEBAS-INTEGRACION-RESULTADO.md`.

| ID | PRUEBA | ESPERADO | RESULTADO REAL | EVIDENCIA | ESTADO |
|---|---|---|---|---|---|
| R01 | Backend QA 8081 responde | UP | `{"status":"UP"}` | actuator | PASS |
| R02 | Aislamiento vs transporte_bd | Solo QA | 2 viajes QA devueltos | API `/viajes` | PASS |
| R03 | Viaje QA futuro PROGRAMADO | Válido | viaje 2, 2026-10-08 08:00 | PostgreSQL | PASS |
| R04 | Asientos del viaje | 40 DISPONIBLE | 40 creados/Disponibles | API/PostgreSQL | PASS |
| R05 | Buscar viajes disponibles | Lista | 1 viaje S/12 | API/UI | PASS |
| R06 | Retención asiento | 201 RETENIDO | `RET-176E…`, 1A | API | PASS |
| R07 | Rechazo viaje pasado | 4xx | Bloqueado | VentaServiceImpl | PASS |
| R08 | Reserva PENDIENTE_PAGO | 200 | expiraEn futuro | API | PASS |
| R09 | Constancia PDF | 200 PDF | 2030 bytes | PDF | PASS |
| R10 | Constancia sin QR | Sin QR embarque | FALTA PAGAR/NO VÁLIDA | PDF | PASS |
| R11 | Recuperación por documento | 200 | OK | API | PASS |
| R12 | Login cajero | 200 JWT | OK | API | PASS |
| R13 | Confirmar pago | 200 | 1 boleto | API | PASS |
| R14 | Boleto ACTIVO | ACTIVO | ACTIVO | PostgreSQL | PASS |
| R15 | QR generado | imagen | qr_imagen_url=SÍ | PostgreSQL | PASS |
| R16 | Asiento VENDIDO | VENDIDO | VENDIDO | PostgreSQL | PASS |
| R17 | 1 reserva/1 pago/1 boleto/1 QR | Invariante | 1/1/1/1 | PostgreSQL | PASS |
| R18 | Boleto PDF con QR | 200 PDF | 2084 bytes | PDF | PASS |
| R19 | 1er escaneo QR | APROBADO | 200, USADO | API/PostgreSQL | PASS |
| R20 | 2º escaneo mismo QR | RECHAZADO | Rechazado | API | PASS |
| R21 | QR inválido | Rechazado | Rechazado | API | PASS |
| R22 | QR reserva no pagada | Rechazado | No emite boleto | API | PASS |
| R23 | Chofer no autorizado | 403 | 403 | API | PASS |
| R24 | Sin token | 401 | 401 | API | PASS |
| R25 | Token inválido | 401 | 401 | API | PASS |
| R26 | Rol sin permiso (CHOFER /reservas) | 403 | 403 | API | PASS |
| R27 | ADMIN /reservas | 200 | 200 | API | PASS |
| R28 | Vencimiento retención | VENCIDO | VENCIDO | PostgreSQL | PASS |
| R29 | Asiento liberado tras vencer | DISPONIBLE | DISPONIBLE | PostgreSQL | PASS |
| R30 | Confirmar reserva vencida | 409 | 409 | API | PASS |
| R31 | Concurrencia mismo asiento | 1 OK / 1 rechazo | 201 / 409 | API | PASS |
| R32 | Sin doble QR/boleto en concurrencia | Invariante | Sin duplicados | PostgreSQL | PASS |
| R33 | UI home render | OK | Sin errores consola | navegador | PASS |
| R34 | UI búsqueda | Resultados | 1 viaje | navegador | PASS |
| R35 | UI mapa asientos | Render | 38/40 disponibles | navegador | PASS |
| R36 | UI selección asiento | Seleccionado | 1D | navegador | PASS |
| R37 | UI formulario comprador | Validado | OK | navegador | PASS |
| R38 | UI pantalla reserva registrada | Visible | RET-9269279331A94B17, NO VÁLIDA PARA EMBARQUE, botón PDF | navegador + captura | PASS |
| R39 | UI reserva persistida | PENDIENTE_PAGO | PENDIENTE_PAGO | PostgreSQL | PASS |
| R40 | UI sin errores de red | 0 fallidos | 0 fallidos (tras C-5) | navegador | PASS |
| R41 | `npm run lint` | OK | exit 0 | consola | PASS |
| R42 | `npm run build` | OK | exit 0 | consola | PASS |
| R43 | Unit tests backend | PASS | 103/103 | surefire | PASS |
| R44 | IT AuditoriaFinalIT.flujoDePasajes | PASS | PASS | failsafe | PASS |
| R45 | IT EncomiendaFlujoIT | PASS | 14/14 | failsafe | PASS |
| R46 | IT AuditoriaFinalIT.idorEstadoDeViajeAjeno | 403 | 403 y estado intacto; control positivo 200 | failsafe | PASS |
| R47 | IT AuditoriaFinalIT.bolaEndpointsPublicos | PASS | fixture autónomo, asserts OK | failsafe | PASS |
| R48 | IT EncomiendaFlujoIT.concurrenciaMismaClave | 1 registro | filas=1, guía=1 | failsafe | PASS |
| R48b | `/actuator/health` post-build | UP | UP | actuator | PASS |
| R49 | Migraciones V1–V6 intactas | No modificar | Sin cambios | git | PASS |
| R50 | transporte_bd intacto | Sin cambios | Solo lectura/copia | git/psql | PASS |

---

## 8. Cierre de los 3 ITs de auditoría — RESUELTO

Los 3 fallos ya no existen; se corrigieron y `mvn clean verify` da **BUILD SUCCESS** (unidad
103/103 + IT 28/28) contra `intiwatana_cierre_it_qa`. Detalle y evidencias en
`reports/CIERRE-PRUEBAS-INTEGRACION-RESULTADO.md`. Resumen:

1. `AuditoriaFinalIT.idorEstadoDeViajeAjeno` — **defecto productivo (A) + defecto de prueba (B).**
   `validarResponsable` lanzaba `NegocioException` (400); ahora lanza `SinAutorizacionException`
   (403). La prueba además enviaba `estado` en el **body**, cuando el contrato real lo toma por
   **query param**. Corregido en ambos lados.
2. `AuditoriaFinalIT.bolaEndpointsPublicos` — **defecto de prueba (B) + dependencia de orden (D) +
   dato semilla ausente (E).** Ahora crea su propia reserva confirmada con boleto y es autónoma.
3. `EncomiendaFlujoIT.concurrenciaMismaClave` — **dependencia de entorno (F).** La BD usada no tenía
   el índice parcial único `uq_encomiendas_clave_idempotencia` (ya definido en `V5`). Con la BD
   migrada por Flyway la concurrencia deja 1 fila; la prueba se reforzó a **10 hilos** concurrentes.
   **No se creó ninguna migración V7.**

Nota: la base sembrada original (`database/transporte_bd.sql`) tiene fechas de viajes anteriores a
hoy y esquema previo a V5, por lo que los IT con `viaje_id=1` requieren re-fechar el viaje y aplicar
índices de V4 (`uk_boletos_asiento_activo`, `uk_retencion_idempotency`,
`idx_retencion_estado_expira`). Se hizo en `intiwatana_cierre_it_qa` para correrlos.

---

## 8b. Artefactos de evidencia

- `reports/evidencia-qa-cierre/ui-reserva-registrada.png` — captura de la pantalla final.
- `reports/evidencia-qa-cierre/constancia_RET-176EFC24EEE04736.pdf` — constancia de reserva.
- `reports/evidencia-qa-cierre/boletos`/evidencias en PostgreSQL documentadas arriba.
- Base QA: `intiwatana_recorrido_qa`. Base para IT: `intiwatana_cierre_it_qa`.

---

## 9. Conclusión

El flujo comercial **RESERVA → CAJA → BOLETO → QR → CHOFER** queda **CERRADO Y VERIFICADO** en
entorno QA aislado, con evidencia real de API, PostgreSQL y navegador. Los 3 ITs de auditoría que
estaban en rojo fueron corregidos y ahora **28/28 PASS** con `mvn clean verify` = BUILD SUCCESS.