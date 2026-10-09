# EVIDENCIA FINAL — CAJA EMPRESARIAL INTIWATANA (QA)

> Fecha: 2026-10-09 · Entorno: `intiwatana_flujo_qa` + API QA `:8081` + Vite QA `:5174`
> Alcance: verificación de cierre del módulo **CAJA EMPRESARIAL** (venta presencial, boletos, QR, PDF, resumen, idempotencia).

## 1. Veredicto

**CAJA EMPRESARIAL INTIWATANA SITIO COMPLETO, PREMIUM Y VERIFICADO EN QA**

Gates verdes: `npm run lint` EXIT 0 · `npm run build` EXIT 0 · `mvn clean verify` BUILD SUCCESS (110 unit + 28 ITs, 0 fallos).

## 2. Venta presencial transaccional (prueba real end-to-end)

### 2.1 Venta por la UI (paso a paso, frontend React)
- Viaje 33: Huamanga → Vilcas Huamán, 09/10/2026 20:00, S/ 20.00.
- Paso 1: elegir asiento **2A** → "Seleccionado 1" → Continuar.
- Paso 2: búsqueda de cliente con **12345678** → fila **Ana García** → seleccionar.
- Paso 3: **Ir al cobro** → Cobro en efectivo → monto **20** → "Vuelto 0.00".
- Paso 4: Confirmar.
- Paso 5: **"Venta registrada · boletos emitidos"** → boleto **BOL-20261009-000055**, N° asiento 2A, Pagado S/ 20.00, con **QR de embarque** (imagen) y acciones PDF / Imprimir / Iniciar otra venta.
- Resultado: 0 errores de consola, 0 peticiones fallidas.

### 2.2 Venta por API (idem, canal directo)
`POST /api/v1/ventas/presencial` con `viajeId=33`, `asientoIds=[849]`, `clienteId=1`, `EFECTIVO`, `montoRecibido=20.00` → **HTTP 201**, boleto **BOL-20261009-000054** (asiento 1A), cajero `cajero1`. Cuerpo guardado en `venta-api-boleto-54.json` (fase previa).

## 3. Verificación en base de datos (`intiwatana_flujo_qa`)

| Boleto | N° | Asiento | Precio | Estado | Pago | QR |
|---|---|---|---|---|---|---|
| 54 | BOL-20261009-000054 | 1A (id 849) | S/ 20.00 | ACTIVO | único id 49 (EFECTIVO COMPLETADO) | data:image base64 |
| 55 | BOL-20261009-000055 | 2A (id 853) | S/ 20.00 | ACTIVO | único id 50 (EFECTIVO COMPLETADO) | data:image base64 |

- Asientos 849 y 853 → **VENDIDO**.
- Totales tras la venta: 31 boletos · 48 pagos.

## 4. Idempotencia

- Replay del **mismo payload** (misma `idempotencyKey` `qa-itinerario-…`) → **HTTP 201** devolviendo el **mismo boleto** (BOL-20261009-000054), sin crear filas nuevas (contador de boletos sin cambios). Mecánica: `VentaServiceImpl.confirmarPago` → si la retención ya está CONFIRMADO, devuelve los boletos existentes.
- Evidencia: `idempotencia-replay.json`.

## 5. Conflicto sobre asiento vendido

- Segundo intento con clave nueva sobre asiento 853 ya vendido → **HTTP 409** `{"mensaje":"El asiento 2A ya no está disponible"}` en 29 ms.
- Evidencia: `conflicto-asiento-vendido.json`.

## 6. PDF del boleto

- `GET /api/v1/ventas/boleto/BOL-20261009-000055/pdf?documento=12345678` (endpoint público, el mismo que llama la UI) → **HTTP 200 `application/pdf`**, 3 477 bytes, firma `%PDF-1.5` válida.
- Archivo: `boleto-55-bol-20261009-000055.pdf`.
- Contra-verificación del IT (AuditoriaFinalIT): PDF con documento correcto → 200 OK y firma %PDF · documento ajeno → NO 200 · sin documento → NO 200.

## 7. Secciones de la UI verificadas (0 errores)

- **Resumen de Caja**: EFECTIVO CONFIRMADO S/ 40.00 · N° OPERACIONES 2 · VENTAS PRESENCIALES 2 · RESERVAS COBRADAS 2 · BOLETOS EMITIDOS 2 · periodo 2026-10-09 → 2026-10-09.
- **Operaciones**: EFECTIVO CONFIRMADO S/ 504.25 · INGRESO S/ 591.25 · 48 pagos (la propia UI documenta que `Boletos emitidos` muestra "—" porque `/api/v1/pagos` no expone el cajero).
- **Boletos**: búsqueda BOL-20261009-000055 → Ana García · DNI 12345678 · N° 2A · S/ 20.00 · ACTIVO.
- **Encomiendas**: carga sin errores (módulo vecino tras el fix del 500).

## 8. Gates de calidad

| Gate | Resultado |
|---|---|
| `npm run lint` | EXIT 0 |
| `npm run build` (Vite 8) | EXIT 0 (warning chunk >500 kB preexistente) |
| `mvn clean verify` | BUILD SUCCESS — 110 unit + 28 ITs (AuditoriaFinalIT 14/14, EncomiendaFlujoIT 14/14) |

## 9. Ajustes aplicados para lograr el verde (evidenciados)

Todos sobre la base QA desechable `intiwatana_flujo_qa` (no afectan `transporte_bd` ni producción) y sin tocar datos de las ventas de evidencia (54/55, viaje 33):

1. **V7__login_seccion_portal.sql** (único cambio de fuente): `DROP CONSTRAINT IF EXISTS contenidos_portal_seccion_check` — ninguna base tiene V7 registrada ni dicha constraint; las migraciones V5/V6 ya usaban `IF EXISTS`. Idempotente y sin riesgo de checksum.
2. Migraciones faltantes por el baseline de QA aplicadas a mano (equivalentes a V4 y V5):
   - `uk_boletos_asiento_activo` (único parcial: 1 boleto vigente por asiento).
   - `uk_retencion_idempotency`, `idx_retencion_estado_expira`, `idx_retencion_documento`; `boletos.qr_imagen_url` como TEXT.
   - `uq_encomiendas_clave_idempotencia` (único parcial sobre `clave_idempotencia`), requerido por el manejo de carrera de idempotencia de `EncomiendaServiceImpl` (DOS pasos + captura de `DataIntegrityViolationException`).
3. Datos QA del viaje 1 (reciclado): asientos limpios (0 boletos, 40 DISPONIBLE) y viaje PROGRAMADO/futuro, para que los ITs re-vendan asientos sin boletos históricos.

## 10. Artefactos

- `venta-api-boleto-54.json` — respuesta 201 de la venta API.
- `idempotencia-replay.json` — replay con la misma clave (mismo boleto).
- `conflicto-asiento-vendido.json` — HTTP 409 sobre asiento ya vendido.
- `boleto-55-bol-20261009-000055.pdf` — PDF del boleto 55 (3 477 B, válido).
- Complementarias en `../evidencia-caja/`: capturas de pantalla (nueva venta, boletos, operaciones, pagos, reservas, cierre de caja, clientes, encomiendas, inicio).