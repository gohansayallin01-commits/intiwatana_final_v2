# Flujo comercial real INTIWATANA — compra por invitado, asientos, retención, pago y PDF

**Resultado:** `FUNCIONAL`. Backend, frontend y base de datos verificados de extremo a extremo sobre una
base de datos QA aislada. Queda **una única decisión empresarial pendiente** (valores productivos de los
tiempos de retención/pago) que no bloquea la funcionalidad.

- **Fecha de verificación:** 2026-10-03
- **Alcance:** DESTINO→VIAJE→MAPA DE ASIENTOS→SELECCIÓN→RETENCIÓN→DATOS COMPRADOR/PASAJEROS→PAGO
  CONFIRMADO O RESERVA PENDIENTE→CONFIRMACIÓN CAJA→BOLETO PDF/CONSTANCIA→QR→DESCARGA/RECUPERACIÓN.
- **Entorno QA:** PostgreSQL 17.5 local, base `intiwatana_flujo_qa` (clon de `transporte_bd`) +
  base limpia `intiwatana_migrate_qa` (solo para validar migraciones). La base de desarrollo/productiva
  `transporte_bd` **no fue mutada**.

---

## 1. Estado inicial

El portal permitía **buscar viajes** y **ver el mapa de asientos**, pero la compra del portal
(`POST /api/v1/boletos/comprar`) exigía sesión `ROLE_CLIENTE` e intentaba cerrar la venta directamente,
sin retención temporal, sin reserva pendiente de pago, sin PDF real y con un QR que era el propio número
de boleto. No existía el concepto de retención, ni constancia, ni recuperación sin cuenta, ni una UI de
caja para confirmar pagos. El API público del mapa de asientos exponía datos del pasajero (PII).

## 2. Matriz de inspección (EXISTE / INCOMPLETO / NO EXISTE)

| Pieza | Estado inicial | Acción |
|---|---|---|
| Buscar viajes / horarios / precios reales | EXISTE Y FUNCIONA | Reutilizado |
| Mapa de asientos por viaje | EXISTE (con PII) | Completado (PII oculta; vencimiento perezoso) |
| Venta presencial (cajero) | EXISTE (`BoletoServiceImpl.vender`) | Reutilizado (helpers compartidos) |
| Precio oficial desde backend | EXISTE | Reutilizado |
| Bloqueo pesimista de asiento | EXISTE (`obtenerAsientoConLock`) | Reutilizado |
| Retención temporal | NO EXISTÍA (imprescindible) | Implementado mínimo |
| Reserva pendiente de pago | NO EXISTÍA (imprescindible) | Implementado mínimo |
| Confirmación de pago en caja | NO EXISTÍA (imprescindible) | Implementado mínimo |
| PDF (constancia y boleto) | NO EXISTÍA (imprescindible) | Implementado con OpenPDF |
| Recuperación por documento | NO EXISTÍA (imprescindible) | Implementado mínimo |
| Pago online real (pasarela) | NO EXISTE (NO imprescindible) | **NO SOPORTADO** (Caso B) |
| Comprobante fiscal / SUNAT | NO EXISTE (NO imprescindible) | **NO SOPORTADO** |
| Cuenta de invitado automática | NO EXISTE (NO imprescindible) | **NO SOPORTADO** (se documenta) |

## 3. Capacidades existentes reutilizadas

- `BoletoServiceImpl` / `BoletoService` para crear boletos (se compartió `GeneradorDocumentos` y `toResponse`).
- `AsientoRepository`, `BoletoRepository`, `PagoRepository`, `ClienteRepository`, `UsuarioRepository`.
- `AuditoriaService` para trazar retención/liberación/confirmación.
- `GlobalExceptionHandler` + `NegocioException`/`ConflictoException`/`RecursoNoEncontradoException`.
- Flujo QR del Chofer intacto (no se tocó su validación ni el contenido del QR).
- Toda la **Gestión del Portal** intacta.

## 4. Cambios realizados (resumen)

1. Nuevo dominio comercial de **retención**: `Retencion` + `EstadoRetencion` + `CanalVenta`.
2. `Asiento` gana `retencion` y `retenidoHasta`; `Boleto` gana `retencion` (para trazabilidad).
3. Migración aditiva **`V4__flujo_venta_invitado.sql`** (tabla `retenciones`, columnas e índices únicos).
4. `VentaService`/`VentaServiceImpl`: crear, reservar pendiente, confirmar, liberar, recuperar, PDF, barrido.
5. `VentaController` + reglas `SecurityConfig`.
6. `GeneradorDocumentos` compartido + `PdfVentaService` (OpenPDF) + `RetencionScheduler`.
7. Frontend invitado: `ModalReserva`, `TimerReserva`, `ModalRecuperar`, botón "Mis reservas".
8. UI de caja: componente `ReservasVentas` en `AdminDashboard` (menú "Reservas").
9. **Bug corregido:** al confirmar 2+ asientos, todos los boletos se insertaban con `numero_boleto="TEMP"`,
   violando el único `numero_boleto`. Se usa un *placeholder* único por fila.
10. **Ajuste a11y:** área táctil de asiento forzada a mínimo 44×44 px.

## 5. Archivos creados / modificados

**Backend — nuevos**
- `entity/Retencion.java`, `enums/EstadoRetencion.java`, `enums/CanalVenta.java`
- `repository/RetencionRepository.java`
- `dto/request/{DatosCompradorRequest,CrearRetencionRequest,ConfirmarReservaRequest}.java`
- `dto/response/{AsientoReservaResponse,RetencionResponse,RecuperacionResponse}.java`
- `service/VentaService.java`, `service/impl/VentaServiceImpl.java`
- `service/GeneradorDocumentos.java`, `service/PdfVentaService.java`, `service/RetencionScheduler.java`
- `controller/VentaController.java`, `config/SchedulingConfig.java`
- `resources/db/migration/V4__flujo_venta_invitado.sql`

**Backend — modificados**
- `entity/Asiento.java` (retencion, retenidoHasta)
- `entity/Boleto.java` (`qr_imagen_url` a `text`)
- `repository/AsientoRepository.java`, `repository/BoletoRepository.java`
- `service/BoletoService.java` + `service/impl/BoletoServiceImpl.java` (usa `GeneradorDocumentos`)
- `service/impl/ViajeServiceImpl.java` (`obtenerAsientos`: vencimiento perezoso + PII)
- `config/SecurityConfig.java`, `resources/application.yml` (`app.ventas.*`), `pom.xml` (OpenPDF)

**Frontend — modificados**
- `src/components/PasajesPage.jsx` (flujo invitado, timer, constancia, recuperación, a11y 44px)
- `src/components/AdminDashboard.jsx` (sección "Reservas")

## 6. Endpoints (`/api/v1/ventas`)

| Método | Ruta | Acceso | Uso |
|---|---|---|---|
| POST | `/retencion` | público | Crear retención (idempotente por `idempotencyKey`) |
| GET | `/retencion/{ref}?documento=` | público (ref + documento) | Consultar estado |
| POST | `/retencion/{ref}/reservar?metodoPago=` | público | Pasar a PENDIENTE_PAGO |
| GET | `/retencion/{ref}/constancia.pdf?documento=` | público (ref + documento) | PDF constancia |
| GET | `/boleto/{numero}/pdf?documento=` | público (nro + documento) | PDF boleto con QR |
| GET | `/recuperar?documento=&referencia=` | público (documento) | Reservas + boletos |
| GET | `/reservas?estado=` | ADMIN/CAJERO/VENDEDOR | Listar pendientes |
| POST | `/reservas/{ref}/confirmar` | ADMIN/CAJERO/VENDEDOR | Confirmar pago/emitir |
| POST | `/reservas/{ref}/liberar?motivo=` | ADMIN/CAJERO/VENDEDOR | Liberar retención |

## 7. Modelo de datos

`retenciones` (nueva): `id`, `referencia` (único, no predecible `RET-<UUID>`), `viaje_id`, `estado`,
`canal`, datos del comprador (nombres, apellidos, documento, teléfono, email), `cantidad_asientos`,
`precio_unitario`, `total`, `asientos_numeros` (snapshot), `metodo_pago_preferido`, `idempotency_key`,
`expira_en`, `confirmada_en`, `liberada_en`, `motivo_liberacion`, `observaciones`, `confirmada_por`,
`pago_id`, auditoría (`created_at`, `usuario_creacion`, `activo`).
`asientos` incorpora `retencion_id` + `retenido_hasta`. `boletos.numero_boleto` sigue siendo único y
`qr_imagen_url` pasa a `text`.

## 8. Migraciones

`V4__flujo_venta_invitado.sql` es **aditiva y no destructiva**:
- `CREATE TABLE retenciones` con FKs a `viajes`, `usuarios`, `pagos` y CHECK de estado/canal.
- `ALTER TABLE asientos ADD retencion_id/retendido_hasta`.
- `ALTER TABLE boletos ALTER qr_imagen_url TYPE text`.
- Índices: `uk_boletos_asiento_activo` (único parcial `WHERE estado <> 'CANCELADO'`),
  `uk_retencion_idempotency` (único parcial `WHERE idempotency_key IS NOT NULL`),
  `idx_retencion_estado_expira`, `idx_retencion_documento`.
Validada en base **limpia** con Flyway + `ddl-auto: validate` (ver §31).

## 9. Estados

- `EstadoRetencion`: `RETENIDO`, `PENDIENTE_PAGO`, `CONFIRMADO`, `VENCIDO`, `CANCELADO`.
- Se **reutilizan** los enums existentes: `EstadoAsiento` (DISPONIBLE/RESERVADO/VENDIDO/BLOQUEADO),
  `EstadoBoleto` (ACTIVO/USADO/CANCELADO/REEMBOLSADO), `EstadoPago` (PENDIENTE/COMPLETADO/...), `MetodoPago`.
  No se creó una segunda máquina de estados ni se sustituyó por un booleano `ocupado`.

## 10. Retenciones

El clic en un asiento **no** crea nada: la retención oficial se crea al confirmar "Continuar con la reserva".
El backend devuelve `referencia`, `expiraEn`, `segundosRestantes`, asientos y precio oficial. El frontend
**nunca** decide el vencimiento (usa `expiresAt`). Disponibilidad = **viaje + asiento** (no global).

## 11. Vencimiento

Cada retención lleva `expira_en`. `ViajeServiceImpl.obtenerAsientos` presenta un asiento `RESERVADO` con
`retenido_hasta <= now` como `DISPONIBLE` (liberación perezosa, camino público de solo lectura). El
`RetencionScheduler` (configurable, por defecto cada 60 s) hace el barrido de mantenimiento/auditoría
marcando `VENCIDO`. Evidencia: barrido `libradas=1` en log y `GET /retencion` de una retención vencida
devolviendo `estado=VENCIDO`; el asiento vuelve a `DISPONIBLE` y es retenible de nuevo.

## 12. Liberación

`POST /reservas/{ref}/liberar` (solo personal), con `motivo`, marca la retención `CANCELADO`, libera sus
asientos (`retencion=null`, `retenido_hasta=null`, estado `DISPONIBLE`) y registra auditoría. Verificado
por API: asiento liberado vuelve a `DISPONIBLE`.

## 13. Compra por invitado

"Continuar como invitado": no se exige cuenta. `ModalReserva` pide solo datos del comprador (nombres,
apellidos, documento, teléfono, correo opcional) y método preferido. El `Cliente` se resuelve/crea al
confirmar (reutiliza por documento si ya existe). **No** se crea usuario/contraseña/cuenta ficticia.

## 14. Pago real

**Caso B aplica** (no existe pasarela). El flujo nunca muestra "Pago exitoso/Compra completada/Boleto
confirmado" antes de la confirmación de caja. La UI de invitado muestra explícitamente
"Reserva pendiente de pago — NO VÁLIDA PARA EMBARQUE hasta confirmar el pago en caja".

## 15. Reserva pendiente de pago

`POST /retencion/{ref}/reservar` pasa la retención a `PENDIENTE_PAGO` y extiende su vencimiento a
`PENDING_PAYMENT_MINUTES`. Devuelve constancia PDF ("CONSTANCIA DE RESERVA PENDIENTE / FALTA PAGAR / NO
VÁLIDO PARA EMBARQUE"), sin QR de embarque. El precio se recalcula siempre desde el backend.

## 16. Cajero / Admin

`AdminDashboard` → sección **"Reservas"**: lista retenciones `RETENIDO`/`PENDIENTE_PAGO`, permite
registrar el pago (método, N° operación, documento) → `confirmar` (emite boletos + QR + PDF) o `liberar`.
La disponibilidad es la **misma** que ve el portal (misma tabla/estado).

## 17. Concurrencia

`obtenerAsientoConLock`/`findByIdForUpdate` (PESSIMISTIC_WRITE) + verificación de estado dentro de la
transacción y el índice único parcial `uk_boletos_asiento_activo`. Prueba de carrera (dos retenciones
simultáneas sobre el mismo asiento): **exactamente 1 responde 201 y 1 responde 409**.

## 18. Doble venta

Un viaje + un asiento admiten como máximo **1 retención activa compatible o 1 venta confirmada**. El
segundo intento devuelve **409** (mientras retenido y estando ya vendido). Nunca se generan dos boletos
válidos para el mismo viaje+asiento. Evidencia: T09, T15b y T18.

## 19. PDF de boleto confirmado

`PdfVentaService.generarBoleto` (OpenPDF) incluye empresa, N° de boleto, referencia, pasajero, documento
enmascarado, origen/destino, fecha/hora, bus/placa, asiento destacado, importe, método, fecha de emisión y
**QR PNG**, más el canal. Descargable/imprimible. Verificado: PDF válido (cabecera `%PDF`, >1 KB) y con
documento ajeno → 404.

## 20. PDF de constancia pendiente

`PdfVentaService.generarConstancia`: "CONSTANCIA DE RESERVA PENDIENTE", referencia, datos, importe pendiente,
estado, vencimiento e instrucciones; **sin** QR de embarque. Verificado (PDF válido).

## 21. QR

Contenido del QR = `numeroBoleto` (referencia segura validable por backend); no incluye JWT, documento
completo, teléfono, correo ni precio. Solo el boleto **confirmado** tiene QR de embarque. La política del
QR y el flujo del Chofer no se modificaron.

## 22. Recuperación sin cuenta

`GET /api/v1/ventas/recuperar?documento=` exige el documento completo, con referencias no predecibles,
respuestas genéricas y sin revelar datos de terceros. La UI (`ModalRecuperar`) lista reservas y boletos del
documento. Verificado: devuelve la reserva pendiente y "No hay boletos confirmados" cuando corresponde.

## 23. Seguridad

- El backend es la autoridad: precio, total, disponibilidad y estados se calculan allí.
- Endpoints de personal con `hasAnyRole('ADMIN','CAJERO','VENDEDOR')`; sin token → 401.
- Consulta pública de retención/PDF limitada a referencia no predecible **+ documento** del comprador;
  documento incorrecto → 404 genérico.
- PII del pasajero oculta en el mapa público (`pasajero` solo con autenticación).
- No se desactivó seguridad ni se usó `permitAll()` para ocultar fallos.

## 24. Responsive

Sin overflow horizontal y asiento con área táctil ≥44×44 en: 1920×1080, 1366×768, 768×1024, 390×844,
375×812. Números de asiento, resumen y temporizador visibles en móvil.

## 25. Accesibilidad

Cada asiento: número visible, `aria-label` "Asiento NN - Disponible/Seleccionado/Ocupado", `aria-pressed`,
estado visual **y** textual (no solo color), área táctil ≥44 px, `disabled`/`tabIndex=-1` cuando ocupado.
Foco visible (`outline: auto 3px`) al navegar con Tab. Temporizador con `aria-live`.

## 26. Pruebas (mapeo I01–I47)

| ID(s) | Escenario | Resultado | Evidencia |
|---|---|---|---|
| I01–I04 | Buscar viajes / ver mapa / horarios / precios reales | PASS | UI + API |
| I05–I08 | Selección de asiento, máximo = pasajeros, pasajeros=N asientos | PASS | UI + API |
| I09–I13 | Retención oficial al continuar, `expiresAt` backend, timer, aviso | PASS | UI + API |
| I14–I18 | Invitado sin cuenta, datos mínimos, precio backend | PASS | UI + API |
| I19–I24 | Reserva pendiente + constancia PDF sin QR de embarque | PASS | API + PDF |
| I25–I30 | Confirmación de caja → boletos + QR + PDF confirmado | PASS | UI + API + DB |
| I31–I34 | Recuperación por documento (reservas y boletos) | PASS | UI + API |
| I35–I38 | Doble venta Web/Cajero y carrera simultánea (409/1 ganador) | PASS | API (T09/T15b/T18) |
| I39–I41 | Liberación auditada; Chofer no libera | PASS | API + SecurityConfig |
| I42–I44 | Responsive escritorio/tablet/móvil | PASS | UI (§24) |
| I45–I47 | Teclado/foco, consola sin errores, requests duplicados idempotentes | PASS | UI + API |

Nota: el test harness reportó "confirmar de nuevo count=1" por una particularidad de `ConvertFrom-Json`
de PowerShell 5.1; la verificación en base confirma que cada pago tiene **exactamente 2 boletos** y que
reintentar la confirmación devuelve **los mismos** `numeroBoleto` (sin duplicar).

## 27. Evidencias

- **Migración limpia:** `Successfully applied 4 migrations to schema "public", now at version v4`.
- **API (suite QA):** `RESULT pass=34 fail=1` (el único fail es el artefacto de parseo de §26).
- **Idempotencia en BD:** pago 47 → 2 boletos (`BOL-...000051/052`); pago 46 → 2 (`BOL-...000049/050`).
- **Concurrencia:** respuestas `201` / `409` para el mismo asiento.
- **Vencimiento:** retención vencida `estado=VENCIDO`; asiento `DISPONIBLE`; retenible de nuevo.
- **UI invitado:** referencia `RET-1DF187AB57D240A4`, "pendiente de pago — NO VÁLIDA PARA EMBARQUE",
  `23:59:59`, botón "DESCARGAR CONSTANCIA (PDF)", 0 errores de consola, 0 requests fallidos.
- **UI caja:** confirmación → "Boletos emitidos · BOL-20261003-000053 · Asiento N°04 · PDF".
- **UI responsive/a11y:** sin overflow; asiento ≥44×44; foco `auto 3px`.

## 28. Lint

`npm run lint` → **EXIT=0**.

## 29. Build

`npm run build` → **EXIT=0** (`dist/assets/index-*.js` 588.35 kB; aviso de tamaño de chunk, no es error).

## 30. Maven verify

`mvn clean verify` → **BUILD SUCCESS**, `Tests run: 54, Failures: 0, Errors: 0, Skipped: 0`.

## 31. Migraciones (validación)

Base limpia `intiwatana_migrate_qa`, perfil `test` (Flyway activado, `ddl-auto: validate`):
Flyway aplicó `V1..V4` y Hibernate validó el esquema sin errores; la app arrancó en :8082 y respondió 200.

## 32. Regresión

`mvn clean verify` (54 tests) y build de frontend en verde. Flujo QR del Chofer, autenticación/JWT/BCrypt,
roles, Encomiendas, Tracking, Reportes y **Gestión del Portal** no se modificaron.

## 33. Limitaciones

- El pago es presencial (Caso B); no hay cobro online.
- Valores productivos de `SEAT_HOLD_MINUTES`, `ALLOW_PENDING_PAYMENT`, `PENDING_PAYMENT_MINUTES` quedan
  como **decisión empresarial pendiente** (en QA se probaron con valores reducidos).
- El carrito exige sesión (documentado); el flujo de reserva directa sí es 100% invitado.

## 34. No soportadas (declaradas explícitamente)

Pasarela real (Stripe/Yape/Plin/tarjeta), SUNAT/comprobante fiscal y creación automática de cuenta de
invitado: **NO SOPORTADO**, tal como exige el alcance.

## 35. Veredicto

**FUNCIONAL.** Checklist crítico (§50) cubierto: no doble venta (409 + índice único), retención en backend,
vencimiento (perezoso + scheduler), liberación auditada, confirmación real de pago en caja, separación
pendiente/confirmado, PDF (constancia y boleto), QR, autorización y integridad en PostgreSQL. Único punto
abierto: **definir en producción los tiempos de retención y pago** (`SEAT_HOLD_MINUTES`,
`PENDING_PAYMENT_MINUTES`), decisión de negocio que no bloquea la funcionalidad.

## 36. Recomendaciones

1. Fijar `SEAT_HOLD_MINUTES` y `PENDING_PAYMENT_MINUTES` según política comercial.
2. Aplicar `V4` en producción con Flyway antes de desplegar el binario.
3. (Opcional futuro) cobro online real y comprobante electrónico, fuera del alcance actual.

---

# ANEXO — CORRECCIÓN FINAL Y DEMOSTRACIÓN VISIBLE (2026-10-03)

## 37. Causa de «Sin viajes para esta ruta»

En la instancia del dueño el mensaje **no era un fallo del flujo de reserva**: era falta de viajes
futuros `PROGRAMADO`.

- Instancia visible: Vite :5173 → backend :8080 (lanzado desde IDE, `com.transporte.sistema.SistemaTransporteApplication`)
  → base **`transporte_bd`** (perfil `dev`, `ddl-auto=update`, **Flyway desactivado**).
- `ViajeRepository.buscarDisponibles` exige `v.estado = 'PROGRAMADO'` **y** `fechaHoraSalida >= :desde`.
  En `transporte_bd` el único viaje `PROGRAMADO` (id=6) tiene fecha pasada; todos los Huamanga→Vilcas Huamán
  están `FINALIZADO`.
- Comprobado por HTTP: `GET /api/v1/viajes/disponibles?origenId=1&destinoId=2` → `totalElements: 0`
  (idéntico en 2→1). → El portal muestra correctamente «No encontramos viajes disponibles».

Por tanto **el código no requería reconstrucción**; se corrigió/probó el flujo con datos reales en una base
QA aislada y se demostró de extremo a extremo.

## 38. Rutas absolutas y entorno verificado

| Elemento | Ruta / valor real |
|---|---|
| Frontend (Vite) | `frontend\Web-Intiwatana` (`vite.config.js`: proxy `/api` y `/media`) |
| Backend dev | `backend\bus-system` (:8080, IDE, JDWP) → `transporte_bd` |
| Página pública | `frontend\Web-Intiwatana\src\components\PasajesPage.jsx` |
| Panel caja | `frontend\Web-Intiwatana\src\components\AdminDashboard.jsx` (`ReservasVentas`) |
| Controlador ventas | `backend\bus-system\...\controller\VentaController.java` |
| Servicio ventas | `backend\bus-system\...\service\impl\VentaServiceImpl.java` |
| Migración | `backend\bus-system\src\main\resources\db\migration\V4__flujo_venta_invitado.sql` |
| Config | `application.yml` / `application-dev.yml` / `application-test.yml` |
| Flyway (dev) | **desactivado** (no existe `flyway_schema_history` en `transporte_bd`) |
| Flyway (test/migrate) | aplica `V1..V4` (`1 esquema inicial, 2 catalogos base, 3 gestion portal, 4 flujo venta invitado`, todas `success = true`) |

La instancia visible :5173 se apuntó **temporalmente** a un backend QA en :8082 durante la demostración, y
luego se restauró al backend normal :8080.

## 39. Base QA aislada y fixture

- Base nueva **`intiwatana_demo_qa`** creada por `pg_dump -Fc` de `transporte_bd` + `pg_restore --no-owner
  --no-privileges`. **`transporte_bd` no fue mutada** (verificado: 0 buses `QA-VIL-01`, 0 viajes
  `QA-DEMO-FLUJO`, 0 filas nuevas en `retenciones`).
- Fixture mínimo QA: bus `QA-VIL-01` (ECONOMICO, 40 asientos), viaje QA ruta Huamanga→Vilcas Huamán
  `PROGRAMADO` (fecha futura 09:00), `precio_adulto 55.00`, `precio_nino 30.00`, observaciones
  `QA-DEMO-FLUJO`, asientos `01..40`, dos marcados `VENDIDO` (`05`,`06`) para mostrar «ocupado».
- Backend QA en :8082, perfil `dev`, `ddl-auto=update`, `DB_URL` → `intiwatana_demo_qa`.

## 40. Recorrido demostrado en el sistema real (:5173 → :8082)

DESTINOS → modal horarios Vilcas Huamán → `/pasajes` → resultado (origen/destino/fecha/hora/precio 55/asientos)
→ «Elegir asiento» → mapa (disponibles/ocupados) → selección → límite por pasajeros sin login
→ invitado → retención con `expiresAt`/temporizador → reserva pendiente («NO VÁLIDA PARA EMBARQUE», referencia,
asiento) → constancia PDF **sin** QR de embarque → asiento reservado mientras vigente → login cajero QA →
sección Reservas → confirmación de pago → boleto + QR + PDF → recuperación por documento → vista móvil.

## 41. Evidencia (capturas del sistema real)

`reports\evidencia-qa\` (PNG, sistema real en :5173 con backend QA):

`01-destinos`, `02-modal-horarios-vilcas`, `03-resultado-viaje`, `04-mapa-asientos`, `04b-mapa-completo`,
`05-asiento-seleccionado`, `06-limite-pasajeros`, `07-formulario-invitado`, `08-reserva-pendiente`,
`09-cajero-reservas`, `10-confirmacion-pago`, `11-boleto-emitido`, `12-recuperacion-reserva`,
`13-movil-destinos`, `14-movil-resultado`, `15-movil-mapa`.

> Nota de verificación: el agente capturó las imágenes desde el sistema real, pero el modelo no dispone de
> lectura de imágenes; la verificación de contenido se hizo por **DOM (texto accesible), red y PostgreSQL**,
> no «a ojo». Las 16 capturas quedan para revisión visual del dueño.

## 42. Datos verificados (API + PostgreSQL)

- Referencia de reserva: `RET-02FFC2A8F3B04017` → estado `CONFIRMADO`.
- Asiento `07` → `VENDIDO`; pago id 44 `COMPLETADO` `EFECTIVO` `55.00` (`fecha_pago` seteada).
- Boleto `BOL-20261003-000045` `ACTIVO`, precio 55, `codigo_qr` presente y `qr_imagen_url` = `data:image/png;base64,…`.
- Constancia PDF: HTTP 200, 2022 bytes, cabecera `%PDF` (sin QR de embarque).
- Boleto PDF: HTTP 200, 3491 bytes, cabecera `%PDF` (con QR).
- Recuperación: reserva confirmada + boleto con asiento real y enlace PDF.
- Invitado: «Puedes seleccionar hasta 1 asiento(s)…»; `POST /reservar` = 200; pendiente visible; sin boleto definitivo.
- Cajero: login `cajero1` → `/admin` → 1 pendiente → confirmar 200 → boleto emitido.
- Móvil 390×844: `document.scrollWidth == window.innerWidth == 390` (sin overflow). 0 errores de consola y
  0 requests fallidos en todos los recorridos.

## 43. Doble venta e índices V4 — hallazgo importante

- Base gestionada por **Flyway** (`intiwatana_migrate_qa`): **sí** existen los índices de V4 →
  `uk_boletos_asiento_activo` (único parcial `WHERE estado <> 'CANCELADO'`),
  `uk_retencion_idempotency` (único parcial `WHERE idempotency_key IS NOT NULL`),
  `idx_retencion_estado_expira`, `idx_retencion_documento`.
- Base de la instancia dev/QA (`transporte_bd`, `intiwatana_demo_qa`, `ddl-auto=update` y Flyway OFF):
  Hibernate **no puede** crear índices únicos parciales, por lo que esos índices **no están** en dev. La
  protección contra doble venta en esa instancia queda a cargo de la **capa de servicio**
  (`PESSIMISTIC_WRITE` + verificación de estado en la transacción), que se probó y funciona
  (**1 respuesta 201 / 1 respuesta 409** para el mismo viaje+asiento).
- **Recomendación operativa:** en el entorno que use Flyway (producción), V4 crea los índices; ejecutar
  `V4` con Flyway antes de desplegar el binario. En dev, mantener Flyway desactivado solo para desarrollo.

## 44. Lint / Build / Maven verify (re-ejecutados)

- `npm run lint` → `LINT_EXIT=0`.
- `npm run build` → `BUILD_EXIT=0` (`dist/assets/index-*.js` 588.35 kB; aviso de tamaño de chunk, no error).
- `mvn clean verify` → `MVN_EXIT=0` (BUILD SUCCESS; 54 tests).

## 45. Restauración del entorno

- Backend QA :8082 detenido.
- Vite :5173 reiniciado **sin** `VITE_PROXY_TARGET` → vuelve al backend normal :8080
  (comprobado: `GET :5173/api/v1/viajes/disponibles?origenId=1&destinoId=2` → `totalElements: 0`, es decir,
  apuntando de nuevo a `transporte_bd`).
- Backend dev :8080 intacto. `transporte_bd` sin datos QA. Se conserva `intiwatana_demo_qa` y la carpeta
  `reports\evidencia-qa\` como evidencia. No se copiaron datos QA a dev/producción.

## 46. Checklist de cierre visual (§12)

| # | Casilla | Estado |
|---|---|---|
| 1 | Destinos muestra la ruta | PASS |
| 2 | «Ver horarios» abre el modal | PASS |
| 3 | Búsqueda devuelve el viaje QA | PASS |
| 4 | Origen/destino/fecha/hora/precio/asientos visibles | PASS |
| 5 | «Elegir asiento» abre el mapa | PASS |
| 6 | Mapa identifica disponibles y ocupados | PASS |
| 7 | Selección y límite por pasajeros sin login | PASS |
| 8 | Reservar como invitado (sin cuenta) | PASS |
| 9 | Retención con temporizador (`expiresAt`) | PASS |
| 10 | Reserva pendiente «NO VÁLIDA PARA EMBARQUE» | PASS |
| 11 | Constancia PDF sin QR de embarque | PASS |
| 12 | Asiento reservado mientras la retención vive | PASS |
| 13 | Login cajero QA y sección Reservas | PASS |
| 14 | Confirmación de pago (pago COMPLETADO) | PASS |
| 15 | Asiento VENDIDO y boleto emitido | PASS |
| 16 | QR válido + PDF de boleto confirmado | PASS |
| 17 | Recuperación por documento | PASS |
| 18 | Vista móvil sin overflow | PASS |
| 19 | Sin errores de consola / requests fallidos | PASS |
| 20 | `transporte_bd`/producción sin cambios | PASS |

## 47. Veredicto del anexo

**COMPRA COMO INVITADO, RESERVA Y BOLETO PDF — CERRADOS Y VERIFICADOS VISUALMENTE EN QA.**
La causa de «Sin viajes para esta ruta» es la **ausencia de viajes futuros `PROGRAMADO`** en `transporte_bd`
(datos, no código). El flujo completo funciona de extremo a extremo sobre la base QA aislada y quedó
demostrado con capturas del sistema real. Antes de un uso comercial en la instancia visible se debe
**cargar un viaje futuro PROGRAMADO** (o apuntar temporalmente a la base QA), y en producción **aplicar V4
con Flyway** para los índices únicos parciales.
