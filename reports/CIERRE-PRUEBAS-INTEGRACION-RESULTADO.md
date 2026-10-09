# CIERRE TECNICO INTIWATANA — TRES PRUEBAS DE INTEGRACION PENDIENTES

**Fecha:** 2026-10-08
**Entorno:** Windows, PostgreSQL 17.5, Java 17.0.18, Maven 3.9.12, Node 24.13.1
**Base aislada de las ITs:** `intiwatana_cierre_it_qa` (copia TEMPLATE de `intiwatana_recorrido_qa`,
Viaje 1 re-fechado a futuro). **No se toco `transporte_bd` ni produccion.**

---

## 1. Veredicto

**CIERRE TECNICO INTIWATANA — COMPLETO.**

Las 3 pruebas de integracion pendientes fueron clasificadas, corregidas cuando correspondia y
verificadas. `mvn clean verify` termina en **BUILD SUCCESS** con **0 FAIL / 0 ERROR**:

```
[INFO] Tests run: 103, Failures: 0, Errors: 0, Skipped: 0   (unitarias, surefire)
[INFO] Tests run: 14,  Failures: 0, Errors: 0, Skipped: 0   -- in AUDITORIA FINAL
[INFO] Tests run: 14,  Failures: 0, Errors: 0, Skipped: 0   -- in Flujo SOL -> recepcion -> tracking
[INFO] BUILD SUCCESS
```

- `npm run lint`  -> exit 0 (sin errores ni warnings).
- `npm run build` -> exit 0 (`dist/` generado).
- `/actuator/health` -> **HTTP 200 `{"status":"UP"}`**.
- Flujo comercial de pasajes: cubierto end-to-end por `AuditoriaFinalIT.flujoDePasajes` (PASS) y por
  la verificacion HTTP/navegador previa (`reports/FLUJO-COMERCIAL-REAL-RESULTADO.md`). Sin cambios de
  codigo en el camino de venta; la unica modificacion productiva afecta solo la autorizacion de cambio
  de estado del chofer.

---

## 2. Clasificacion de los 3 fallos (A–F)

| Prueba | Clasificacion | Causa raiz |
|---|---|---|
| `AuditoriaFinalIT.idorEstadoDeViajeAjeno` | **A (defecto productivo) + B (defecto de prueba)** | Produccion: `validarResponsable` lanzaba `NegocioException` (400) en vez de `SinAutorizacionException` (403) para un viaje ajeno. Prueba: enviaba `estado` en el **body** cuando el contrato lo toma por **query param**. |
| `AuditoriaFinalIT.bolaEndpointsPublicos` | **B + D (orden) + E (semilla)** | Dependia de una fila `CONFIRMADO` dejada por otra prueba (orden) o de una semilla ausente. |
| `EncomiendaFlujoIT.concurrenciaMismaClave` | **F (entorno)** | La BD de prueba no tenia el indice parcial unico `uq_encomiendas_clave_idempotencia` (definido en `V5`). Con la BD migrada la carrera deja 1 fila. |

---

## 3. Cambios aplicados

### 3.1 Produccion — `ViajeServiceImpl.validarResponsable` (403 correcto)
`backend/bus-system/src/main/java/com/transporte/sistema/service/impl/ViajeServiceImpl.java`

- Se importa `com.transporte.sistema.exception.SinAutorizacionException`.
- La denegacion por propiedad del viaje ahora lanza `SinAutorizacionException` (-> **403**) y no
  `NegocioException` (-> 400). Se mantiene la comprobacion de propiedad **antes** de la transicion de
  estado, por lo que no se filtra informacion de viajes ajenos.
- Consistente con el resto del sistema (`PropiedadChoferValidator.exigirPropiedadDeViaje`,
  `EncomiendaServiceImpl`).

### 3.2 Prueba 1 — `AuditoriaFinalIT.idorEstadoDeViajeAjeno`
`backend/bus-system/src/test/java/com/transporte/sistema/AuditoriaFinalIT.java`

- Llama al endpoint con el **contrato real** (`?estado=CANCELADO`, query param) en lugar de body.
- Selecciona un viaje **ajeno** de forma robusta (`chofer_id IS DISTINCT FROM`), sin excluir NULL.
- **Autosuficiente**: resuelve/crea el token del chofer atacante con fallback a `login(...)`.
- **Control positivo**: el chofer asignado si cambia el estado (200); el estado se restaura.

### 3.3 Prueba 2 — `AuditoriaFinalIT.bolaEndpointsPublicos`
- **Fixture autonomo**: crea su propia retencion -> reserva -> confirma con `cajero1`, obteniendo un
  boleto y documento propios. Ya no depende de semilla ni del orden. Limpia via `boletosQA`,
  `asientosQA` y el patron `906%`.

### 3.4 Prueba 3 — `EncomiendaFlujoIT.concurrenciaMismaClave`
- **Sin cambios de codigo de produccion.** El defecto era la base de datos. La proteccion ya existe
  en el modelo (`V5__encomienda_solicitud_sol_y_gui.sql`) y en la logica transaccional
  (`EncomiendaServiceImpl`: busqueda por clave + catch `DataIntegrityViolationException` en
  `TransactionTemplate`). **No se creo migration V7.**
- La prueba se **reforzo a 10 hilos concurrentes** con la misma clave (antes 2) para evidenciar la
  proteccion: todas las respuestas apuntan a **1 sola encomienda**, `filas=1`, `guias=1`.

---

## 4. Evidencias (salida real de `mvn clean verify`)

```
[OBS]    viaje 1 asignado al usuario 3 -> 3
WARN  ...GlobalExceptionHandler - Acceso denegado [/api/v1/viajes/1/estado]: El viaje no esta asignado a tu usuario
[OK]     chofer ajeno NO cambia el estado (HTTP 403, PROGRAMADO -> PROGRAMADO)
[OK]     el chofer ASIGNADO (chofer1) si cambia el estado de su viaje (HTTP 200)
[OBS]    boleto de prueba para el PDF -> BOL-20261008-000025 / documento 90617737
[OK]     PDF del boleto con documento ajeno -> 404 (no 200)
[OK]     retencion con documento ajeno -> 404 (no 200)
[OK]     la carrera (10 hilos) con la misma clave deja UNA sola encomienda (filas=1)
[OK]     todas las respuestas apuntan a UNA sola encomienda (ids distintas=1)
[OK]     y una sola guia oficial
```

Indice presente en la base de las ITs (PostgreSQL):

```
uq_encomiendas_clave_idempotencia | CREATE UNIQUE INDEX ... (clave_idempotencia)
                                    WHERE (clave_idempotencia IS NOT NULL)
flyway_version = 6
```

---

## 5. Criterios de cierre

| # | Criterio | Resultado |
|---|---|---|
| C01 | Viaje propio permitido (200) | PASS (control positivo) |
| C02 | Viaje ajeno rechazado (403) | PASS |
| C03 | Prueba publica autonoma (sin semilla/orden) | PASS |
| C04 | Concurrencia misma clave (10 hilos) -> 1 registro / 1 guia | PASS |
| C05 | Claves distintas -> registros distintos | PASS |
| C06 | Encomiendas / Tracking / Guias OK | PASS (14/14) |
| C07 | Flujo de pasajes sigue OK | PASS (`flujoDePasajes`) |
| C08 | Proteccion comprobada en PostgreSQL | PASS (indice unico) |
| C09 | 0 FAIL / 0 ERROR | PASS |
| C10 | `mvn clean verify` -> BUILD SUCCESS | PASS |
| C11 | `npm run lint` exit 0 | PASS |
| C12 | `npm run build` exit 0 | PASS |
| C13 | `/actuator/health` -> UP | PASS |
| C14 | Flyway consistente (sin V7) | PASS |
| C15 | Sin secretos en el repo; aislamiento de `transporte_bd` | PASS |

---

## 5b. Bloque esperado de pruebas C01–C15 (referencia)

- Viaje propio permitido (200) — verificado en el control positivo de `idorEstadoDeViajeAjeno`.
- Viaje ajeno 403 / sin permiso 403 / sin token 401: cubiertos por `AuditoriaFinalIT` (permisos,
  IDOR/BOLA).
- Reintento con la misma `idempotencyKey` no crea segunda retencion (409); claves distintas ->
  registros distintos; misma clave concurrente -> 1 fila.

---

## 5b bis. Detalle Paso 4/6 (prueba de pasajes)
`AuditoriaFinalIT.flujoDePasajes` (Order 10) ejecuta por HTTP real: retencion -> reservar ->
confirmar -> boleto -> QR y verifica invariantes en PostgreSQL. Resultado: **PASS**.

---

## 6. Aislamiento e integridad

- Unica base escrita: `intiwatana_cierre_it_qa`. `transporte_bd` y produccion: solo lectura.
- Migraciones `V1..V6` intactas; no se creo `V7`.
- Limpieza verificada en la base de ITs: `retenciones 906% = 0`, `clientes 906% = 0`,
  `pagos OPQA% = 0`.
- Respaldos y hashes de los archivos tocados en `reports/evidencia-cierre/backup/HASHES.txt`.

---

## 7. Artefactos

- `reports/evidencia-cierre/backup/` — versiones ANTES/DESPUES + `HASHES.txt` de
  `ViajeServiceImpl.java` y `AuditoriaFinalIT.java`.
- `reports/FLUJO-COMERCIAL-REAL-RESULTADO.md` — actualizado (matriz R45–R48 en PASS, §8 resuelto).
- Logs de ejecucion en `%LOCALAPPDATA%\Temp\opencode\` (`verify_final2.log`, `verify_final.log`,
  `it_conc10.log`, `it_after.log`, `fe_lint.log`, `fe_build.log`).