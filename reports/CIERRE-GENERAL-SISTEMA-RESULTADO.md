# CIERRE FINAL DE PENDIENTES GENERALES — SISTEMA INTIWATANA

- **Proyecto:** `C:\Users\USER\Downloads\intiwatana_final_v2`
- **Fecha:** 2026-10-03
- **Objetivo:** cerrar únicamente los pendientes generales que impiden considerar el sistema listo para uso y despliegue controlado, sin reconstruir módulos ya cerrados.
- **Módulos cerrados respetados (no reconstruidos):** Portal Público, compra como invitado, reservas/retenciones, mapa de asientos, constancia PDF, confirmación presencial, boleto PDF/QR, protección anti doble venta, Panel del Cajero (6 módulos, **Reservas conservado**), Encomiendas, Tracking, Panel Chofer, Gestión editorial del Portal en QA.

---

## 1. Estado del arranque local

**Herramientas verificadas**
- JDK 17: `C:\Program Files\Eclipse Adoptium\jdk-17.0.18.8-hotspot` (runtime por defecto en `.vscode/settings.json`).
- Maven: `C:\apache-maven-3.9.12-bin\apache-maven-3.9.12\bin\mvn.cmd`.
- Node: v24.x · Vite v8.0.0 · PostgreSQL 17.5 (servicio `postgresql-x64-17` Running, puerto 5432).

**Configuración de VS Code (`.vscode/`)**
- `launch.json`: tipo `java`, mainClass `com.transporte.sistema.SistemaTransporteApplication`, `projectName=sistema-transporte`, `cwd=${workspaceFolder}/backend/bus-system`, `javaExecPath` JDK 17, `preLaunchTask` del F5, `env` con `DB_URL=…:5432/transporte_bd` y `CORS_ALLOWED_ORIGINS=5173/3000`. **Coherente y completo.**
- `tasks.json`: la `preLaunchTask` invoca `start-frontend-detached.ps1` y **termina de inmediato** (proceso desacoplado, sin `problemMatcher` de background) → no bloquea el Java Debugger.
- `start-frontend-detached.ps1`: si Vite ya responde en 5173 **no lanza una segunda instancia** (evita procesos/duplicados) y lanza el lanzador del navegador desacoplado.
- `settings.json`: runtime `JavaSE-17`, `java.import.maven.enabled=true`, autobuild, exclusiones de `node_modules/target/dist`.

**Verificación en vivo**
| Comprobación | Resultado |
|---|---|
| Backend responde en 8080 | ✅ `GET /api/v1/viajes` → 200 |
| Frontend responde en 5173 | ✅ 200 (instancia única, arrancada por el propio proyecto) |
| Proxy `/api` de Vite → 8080 | ✅ `GET :5173/api/v1/viajes` → 200 |
| Perfil/base usados | ✅ `dev` → `transporte_bd` (**no** backend QA) |
| Sin procesos duplicados | ✅ 1 solo java escuchando en 8080; 1 solo Vite en 5173 |
| Se puede detener | ✅ detenido en las pruebas (8080 liberado) |
| Se puede reiniciar | ✅ reinicio vía Maven → 8080 listo en 15 s |
| Alternativa segura con Maven | ✅ `mvn spring-boot:run` desde `backend/bus-system` (usa `.env` local) |

**“Importing projects” / F5 — CORREGIDO (investigación posterior del 2026-10-03):** ⚠️ el `Java Language Server` **NO** está operativo en la sesión de VS Code. El `client.log` del extension muestra una caída en bucle:
```
07:54:13 The Language Support for Java server crashed 5 times in the last 3 minutes. The server will not be restarted.
```
Causa raíz: **workspace JDT corrupto** (`ObjectNotFoundException: Tree element '/sistema-transporte/target/generated-sources/annotations/com/transporte/sistema/mapper/ClienteMapperImpl.java' not found`). Solo queda vivo el servidor de sintaxis (`ss_ws`); por eso el editor responde hover/definición pero la importación nunca concluye y F5 no arranca Spring Boot. Reparación aplicada (reversible): movidos `ss_ws` y `jdt_ws\.metadata` corruptos a `*.corrupt-<stamp>`; requiere **recargar la ventana** (o “Java: Clean Java Language Server Workspace”) para reimportar limpio. Detalle y pasos exactos: `reports/F5-ARRANQUE-VSCODE-RESULTADO.md`.

> ### ARRANQUE LOCAL — NO CERRADO (F5 PENDIENTE DE VERIFICACIÓN MANUAL)
> La configuración `.vscode/*` es correcta y la alternativa Maven funciona, pero **F5 dentro de VS Code no puede darse por aprobado**: el Java LS se deshabilitó tras un fallo en bucle por un workspace JDT corrupto. Requiere recarga manual de la ventana y una pulsación real de F5.

---

## 2. Gestión del Portal en el Administrador

Sesión `ROLE_ADMIN` (`admin`). **“Gestión del Portal”** visible en el sidebar y funcional en navegador:

| Comprobación | Resultado |
|---|---|
| Menú “Gestión del Portal” visible (solo Admin) | ✅ `R04_portalMenuVisible=true`; el **Cajero NO lo ve** |
| Pestañas | ✅ Hero, Destinos, Beneficios, Nosotros, Contacto, Footer |
| Guardar borrador | ✅ botón presente |
| Vista previa | ✅ “Vista previa (borrador)” desktop/tablet/móvil |
| Publicar / Archivar | ✅ presentes |
| Historial / Restaurar | ✅ botón Historial + modal con “Restaurar” |
| Acceso exclusivo del Administrador | ✅ cajero sin acceso; endpoints `/portal/admin/**` = `hasRole('ADMIN')` |

Verificación funcional profunda (G01–G30: publicar, versionar, restaurar, validaciones, imágenes) ya documentada en `reports/GESTION-PORTAL-ADMIN-RESULTADO.md` sobre base QA aislada.

> ### GESTIÓN DEL PORTAL — FUNCIONAL Y VERIFICADA EN QA

---

## 3. Almacenamiento permanente de imágenes

**Estado real (sin inventar proveedor):**
- Endpoint `GET /api/v1/portal/admin/almacenamiento` → `{"tipo":"local","disponible":true,"persistente":false}`.
- Config: `app.portal.storage.tipo=${PORTAL_STORAGE_TIPO:local}`, `directorio=${PORTAL_STORAGE_DIR:storage/portal}`. La subida funciona y valida (firma real, dimensiones, peso, checksum, duplicados, metadatos/licencia/autorización), pero **vive en el filesystem local** y se pierde al reiniciar/redeployar.
- **No existe** decisión de infraestructura ni credenciales para un proveedor de objetos (S3/R2/GCS) ni disco persistente. Implementar uno ahora **inventaría** un proveedor, lo que está prohibido.

**Defecto detectado y corregido (mínimo, sin reconstruir `PortalGestion.jsx`):**
- La UI solo mostraba el aviso cuando `!storage.disponible`; con `disponible=true, persistente=false` el bloqueo **no era visible**. Se añadió un aviso `role="alert"` que ahora sí muestra: *“Almacenamiento no persistente (tipo actual: local) … no debe declararse listo para producción…”*. La subida permanece habilitada (en un servidor con disco persistente, `local` es válido); el aviso es informativo, no bloquea la función editorial.
- Verificado en navegador: `warnNoPersistente=true`, `uploadEnabled=true`, 0 errores de consola.

**Bloqueo exacto:** `app.portal.storage.tipo=local` es la única opción y apunta a un filesystem efímero; falta un adaptador `PortalStorageService` respaldado por almacenamiento persistente y sus credenciales por variables de entorno.

> ### GESTIÓN DEL PORTAL — FUNCIONAL, NO LISTA PARA PRODUCCIÓN
> Bloqueo único: **almacenamiento de imágenes no persistente**.

---

## 4. Viajes reales futuros

- **No se insertaron viajes ficticios** ni en desarrollo ni en producción.
- Administrator → **Viajes → “Nuevo viaje”** presente y con formulario real de campos:
  `Ruta*`, `Bus*`, `Chofer asignado`, `Fecha y hora salida*`, `Fecha y hora llegada estimada`, `Precio adulto (S/)*`, `Precio niño (S/)`, `Estado` (incluye `PROGRAMADO`).
  → Permite registrar ruta, bus activo, chofer activo, fecha/hora futura, llegada estimada, precio autorizado y estado.
- **Cajero en solo lectura**: sin “Nuevo viaje”, sin Editar, sin Eliminar, sin Cancelar; conserva “Asientos” (11). Verificado `R07_nuevoViaje=0, editar=0, eliminar=0, asientos=11`.
- Portal → Destino → Ver horarios → viaje disponible → Elegir asiento → mapa numerado → reserva como invitado: ya verificado en QA (`FLUJO-COMPRA-INVITADO...md`).
- Si no hay viajes `PROGRAMADO` futuros, el mensaje **“No encontramos viajes disponibles”** es correcto y **no** se sustituye por datos ficticios (en `transporte_bd` el único `PROGRAMADO` tiene fecha pasada).

---

## 5. DECISIONES COMERCIALES PENDIENTES

Valores leídos de `backend/bus-system/src/main/resources/application.yml` (perfil común; sobrescribibles por variable de entorno):

| Variable | Valor actual (dev, por defecto) | Valor QA | Valor productivo | Responsable de aprobación | Estado |
|---|---|---|---|---|---|
| `SEAT_HOLD_MINUTES` (retención de asiento) | 15 min | reducido durante pruebas (no fijo) | **sin definir** | Gerencia comercial / dueño | ⛔ PENDIENTE |
| `PENDING_PAYMENT_MINUTES` (vigencia reserva pendiente) | 1440 min (24 h) | reducido durante pruebas (no fijo) | **sin definir** | Gerencia comercial / dueño | ⛔ PENDIENTE |
| `ALLOW_PENDING_PAYMENT` (permitir pago en caja) | `true` | probado en `true` | **sin definir** | Gerencia comercial / dueño | ⛔ PENDIENTE |
| `RETENCION_LIMPIEZA_MS` (barrido de vencidos) | 60000 ms | — | sugerido 60000 | Operación técnica | informativo |
| `RECUPERACION_MAX_INTENTOS` | 10 | — | sugerido 10 | Operación técnica | informativo |

- **No se impusieron** los valores QA como productivos.
- **No bloquea** las funciones ya verificadas (retención, pago pendiente, confirmación, boleto, QR).
- **Sí bloquea el cierre productivo** mientras no exista una política comercial aprobada.

> ### POLÍTICAS COMERCIALES — PENDIENTES DE APROBACIÓN (no bloquean función; bloquean producción)

---

## 6. Base de datos y Flyway

Entorno productivo esperado (`application-prod.yml`): **Flyway habilitado**, `ddl-auto=validate`, `baseline-on-migrate=false`, `clean-disabled=true`, DataInitializer deshabilitado (`@Profile("!prod")`).

**Validación sobre base vacía** (`intiwatana_cierre_qa`, perfil `test`, Flyway ON + `ddl-auto=validate`):

```
Successfully validated 4 migrations
Migrating schema "public" to version "1 - esquema inicial"
Migrating schema "public" to version "2 - catalogos base"
Migrating schema "public" to version "3 - gestion portal"
Migrating schema "public" to version "4 - flujo venta invitado"
Successfully applied 4 migrations to schema "public", now at version v4   (0.537 s)
Started SistemaTransporteApplication in 17.684 seconds   (Hibernate validate OK)
```

`flyway_schema_history` (base limpia y `intiwatana_migrate_qa`): V1, V2, V3, V4 con `success = t`.

**Índices de V4 presentes** (protección anti doble venta en PostgreSQL):

| Índice | Propósito |
|---|---|
| `uk_boletos_asiento_activo` (único parcial `WHERE estado <> 'CANCELADO'`) | **1 solo boleto vigente por asiento** |
| `uk_retencion_idempotency` (único parcial `WHERE idempotency_key IS NOT NULL`) | idempotencia de reservas |
| `idx_retencion_estado_expira` | barrido de vencidos |
| `idx_retencion_documento` | recuperación por documento |
| `idx_contenido_portal_version_seccion`, `idx_imagenes_portal_checksum` | V3 (portal) |

Además, doble venta se protege en la capa de servicio (`PESSIMISTIC_WRITE` + verificación de estado en transacción). `ALTER TABLE asientos` incorpora `retencion_id` y `retenido_hasta` (2/2 columnas confirmadas).

**Sin migraciones destructivas:** grep de `DROP TABLE/COLUMN`, `TRUNCATE`, `DELETE FROM`, `DROP INDEX` en `db/migration` → **0 coincidencias**. V4 es aditiva.

> ### FLYWAY / MIGRACIONES — VALIDADAS (V1–V4, base vacía + QA, `ddl-auto=validate`)

---

## 7. Regresión final R01–R30

Verificación en vivo (navegador headless) sobre `:5173` → `:8080` (dev). **0 errores de consola, 0 peticiones fallidas.**

| # | Casilla | Resultado | Evidencia |
|---|---|---|---|
| R01 | Portal abre | ✅ PASS | landing 200 + hero/buscador |
| R02 | Login funciona | ✅ PASS | `POST /auth/login` 200 (admin) |
| R03 | Administrador entra | ✅ PASS | `/admin` |
| R04 | Gestión del Portal aparece | ✅ PASS | menú + 6 pestañas + acciones |
| R05 | Cajero entra | ✅ PASS | `/admin` (rol CAJERO) |
| R06 | Cajero muestra seis módulos | ✅ PASS | Panel, Viajes, Clientes, Pagos, Reservas, Encomiendas |
| R07 | Viajes solo lectura para Cajero | ✅ PASS | nuevo/edit/delete=0; Asientos=11 |
| R08 | Clientes funciona | ✅ PASS | Nuevo cliente + editar; sin eliminar |
| R09 | Pagos funciona | ✅ PASS | Registrar pago; sin eliminar |
| R10 | Reservas funciona | ✅ PASS | sección renderiza |
| R11 | Encomiendas funciona | ✅ PASS | Nueva encomienda |
| R12 | Chofer entra | ✅ PASS | `/chofer` |
| R13 | QR sigue funcionando | ✅ PASS | vista QR del chofer presente (política QR intacta) |
| R14 | Cliente entra | ✅ PASS | `/pasajes` (`pruebaa001`) |
| R15 | Tracking funciona | ✅ PASS | `/encomiendas/tracking/GUI-20260930-000021` → 200 |
| R16 | Compra como invitado funciona en QA | ✅ PASS (QA) | `FLUJO-…md` §40–§42 |
| R17 | Retención funciona | ✅ PASS (QA) | `FLUJO-…md` §10, §26 |
| R18 | Vencimiento libera | ✅ PASS (QA) | `FLUJO-…md` §11 |
| R19 | Doble venta se rechaza | ✅ PASS | índice `uk_boletos_asiento_activo` + 201/409 en QA |
| R20 | Constancia pendiente sin QR de embarque | ✅ PASS (QA) | `FLUJO-…md` §20 |
| R21 | Cajero confirma pago | ✅ PASS (QA) | `FLUJO-…md` §16, §26 |
| R22 | Boleto PDF se genera | ✅ PASS (QA) | `%PDF`, >1 KB |
| R23 | QR válido tras el pago | ✅ PASS (QA) | `FLUJO-…md` §21, §42 |
| R24 | Frontend sin errores críticos | ✅ PASS | 0 console errors |
| R25 | Backend sin errores críticos | ✅ PASS | arranque y logs limpios |
| R26 | Sin overflow | ✅ PASS | `scrollWidth-innerWidth=0` en 1440 y 390 |
| R27 | Teclado y foco funcionan | ✅ PASS | `Tab` enfoca botón; `:focus-visible` |
| R28 | No existen requests duplicados | ✅ PASS (con nota) | ver abajo |
| R29 | Datos de desarrollo intactos | ✅ PASS | `transporte_bd`: 11/9/43/18, retenciones 0 |
| R30 | Producción intacta | ✅ PASS | no hay BD productiva activa; `transporte_prod_validation` sin tocar |

**Nota R28:** se observaron reconsultas (`/rutas`, `/viajes`, `/buses`, `/clientes`, `/encomiendas`, `/sucursales`, `/pagos`, `/ventas/reservas`) con conteos 2–5. **No son duplicados dentro de una misma acción**: provienen de (a) el doble montaje de efectos de `React.StrictMode` en desarrollo y (b) las **cuatro reautenticaciones** de la prueba (cada login remonta el panel y reejecuta sus `fetch`). No hay llamadas duplicadas idénticas dentro de un mismo paso del usuario. En producción `StrictMode` no duplica.

---

## 8. Quality gates

| Gate | Comando | Resultado |
|---|---|---|
| Frontend lint | `npm run lint` | **EXIT=0** |
| Frontend build | `npm run build` | **EXIT=0** (`index-*.js` 595.26 kB; aviso de chunk >500 kB **preexistente**, no bloqueante) |
| Backend verify | `mvn clean verify` | **EXIT=0** — 14 suites, **54 tests, 0 fallos, 0 errores, 0 omitidos** |
| Migraciones base vacía | Flyway V1–V4 + `ddl-auto=validate` | **OK** |
| Migraciones base QA | `intiwatana_migrate_qa` V1–V4 | **OK** |

- Pruebas **no desactivadas**.
- **No se añadió** ningún `eslint-disable` en este trabajo. Los 8 comentarios `eslint-disable-next-line react-hooks/exhaustive-deps` existentes son preexistentes, acotados a dependencias de hooks (patrón aceptado), y **no ocultan errores** (lint = 0).

---

## 9. Evidencias

- Capturas de regresión: `reports/capturas-cierre/` — `R01-portal-publico`, `R04-gestion-portal`, `R04b-portal-warning`, `R07-cajero-viajes`, `R08-cajero-clientes`, `R09-cajero-pagos`, `R10-cajero-reservas`, `R11-cajero-encomiendas`, `R12-chofer`, `R14-cliente`, `R15-tracking`, `S04-admin-nuevo-viaje`.
- Log de arranque dev: `%TEMP%\opencode\boot-closure.log` (Tomcat 8080, Hikari `transporte_bd`, `Started … 17.69 s`, 11 viajes).
- Log migración: `%TEMP%\opencode\migrate-test.log` (Flyway V1–V4, `validate` OK).
- Informes relacionados: `GESTION-PORTAL-ADMIN-RESULTADO.md`, `FLUJO-COMPRA-INVITADO-ASIENTOS-PDF-RESULTADO.md`, `DIAGNOSTICO-ARRANQUE-VSCODE-RESULTADO.md`, `PANEL-CAJERO-RESULTADO.md`.

---

## 10. Restricciones cumplidas

- ✅ No se tocó backend, PostgreSQL de datos, entidades, controladores, contratos API, JWT, Spring Security, roles ni permisos.
- ✅ No se reconstruyó ningún módulo cerrado; **Reservas** se conservó en el Panel del Cajero (6 módulos). No se re-modificó `AdminDashboard.jsx`.
- ✅ Único cambio de código: **aviso de almacenamiento no persistente** en `PortalGestion.jsx` (aditivo, mínimo).
- ✅ No se inventaron proveedores, métricas ni datos ficticios; no se insertaron viajes.
- ✅ No se ejecutaron `DROP`/`TRUNCATE`/`DELETE` masivos ni migraciones destructivas.
- ✅ Pruebas mutantes (retención/doble venta/boleto/QR) solo en bases **QA aisladas**; `transporte_bd` no fue mutada.

---

## 11. Restauración del entorno

- Backend dev `:8080` y Vite `:5173` quedan **en ejecución** (estado de trabajo normal del proyecto) para el dueño.
- Base de validación `intiwatana_cierre_qa` creada (aislada) para probar migraciones; no interfiere con dev/producción.
- `transporte_bd` intacta (11 viajes / 9 clientes / 43 pagos / 18 encomiendas / 0 retenciones nuevas).

---

## 12. Veredicto final

| Área | Estado |
|---|---|
| 1. Arranque local | ⚠️ **NO CERRADO — F5 PENDIENTE DE VERIFICACIÓN MANUAL** (Maven sí funciona) |
| 2. Gestión del Portal (visible/funcional) | ✅ **FUNCIONAL Y VERIFICADA EN QA** |
| 3. Almacenamiento de imágenes | ⛔ **NO LISTA PARA PRODUCCIÓN** (bloqueo: storage no persistente) |
| 4. Viajes reales futuros | ✅ Admin con alta real; Cajero solo lectura; sin datos ficticios |
| 5. Políticas comerciales | ⛔ **PENDIENTES DE APROBACIÓN** (no bloquean función; bloquean producción) |
| 6. Flyway / migraciones | ✅ **VALIDADAS (V1–V4)** |
| 7. Regresión R01–R30 | ✅ **R01–R28 PASS** (R16–R23 verificados en QA aislada; R28 con nota), **R29–R30 intactos** |
| 8. Quality gates | ✅ **lint 0, build 0, mvn verify 0 (54 tests)** |

> ### SISTEMA INTIWATANA — FUNCIONALMENTE CERRADO; ARRANQUE POR F5 PENDIENTE DE VERIFICACIÓN MANUAL
> Función, autorización, migraciones y regresión están **cerrados y verificados**. Quedan tres pendientes:
> 1. **F5 / “Importing projects”** — ⚠️ NO cerrado: el Java Language Server se deshabilitó tras una caída en bucle por un workspace JDT corrupto; requiere recarga manual de la ventana y pulsación real de F5 (ver `reports/F5-ARRANQUE-VSCODE-RESULTADO.md`). La alternativa Maven **sí** funciona.
> 2. **Almacenamiento persistente de imágenes** del Portal (`tipo=local` sobre FS efímero) → implementar adaptador de `PortalStorageService` con proveedor de objetos o disco persistente.
> 3. **Políticas comerciales** (`SEAT_HOLD_MINUTES`, `PENDING_PAYMENT_MINUTES`, `ALLOW_PENDING_PAYMENT`) sin valor productivo aprobado.
> El pendiente 1 es de entorno/estado local; 2 y 3 son decisiones para producción.
