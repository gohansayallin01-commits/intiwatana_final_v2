# Módulo de Encomiendas y Tracking — Resultado de implementación y verificación

Fecha: 2026-10-02
Autor: QA / Desarrollo asistido
Entorno: `C:\Users\USER\Downloads\intiwatana_final_v2`
Backend: Spring Boot 3.3 + PostgreSQL (`transporte_bd`), `http://localhost:8080`
Frontend: React + Vite, `http://localhost:5173`
Evidencias: `reports/capturas-encomiendas-tracking/`

---

## 1. Resumen ejecutivo

Se auditó y mejoró el módulo de encomiendas existente sin crear un sistema paralelo ni alterar
la arquitectura. Se implementaron las reglas de negocio y seguridad explícitas:

- La **solicitud del portal cliente** ya no se registra como `RECIBIDO`; ahora nace como
  `SOLICITUD_PENDIENTE` y el personal confirma la recepción física.
- El **tracking público** dejó de exponer datos de pago (`costo`, `metodoPago`) y valida el
  formato de la guía, además de contar con una página pública dedicada.
- El **módulo administrativo** de encomiendas pasó de un CRUD mínimo a una pantalla operativa
  con indicadores reales, búsqueda, filtro por estado, alta, detalle, movimientos y transiciones
  válidas.
- Se corrigió un bug real: el control de propiedad de movimientos bloqueaba también al personal
  autorizado.
- Se redujo una sobre-autorización: `ROLE_CHOFER` ya no puede cambiar el estado de encomiendas.

Resultado: **24/24 pruebas API (E/T)**, **12/12 capturas**, **0 errores de consola**, **0 peticiones
fallidas**, **54/54 tests backend**, **build de frontend correcto**.

---

## 2. Alcance y objetivo

Objetivo: que el ciclo real de una encomienda (registro oficial, solicitud del cliente, seguimiento
público, estados y evidencia) funcione sobre la API y la base de datos existentes, con seguridad,
privacidad y responsive, sin inventar endpoints ni datos.

Fuera de alcance (no modificado): esquema PostgreSQL, JWT/BCrypt, módulos de pasajes, viajes,
clientes, pagos, reportes y el panel del chofer.

---

## 3. Archivos revisados y modificados

Revisados:
- `backend/bus-system/.../controller/EncomiendaController.java`
- `backend/bus-system/.../service/EncomiendaService.java`
- `backend/bus-system/.../service/impl/EncomiendaServiceImpl.java`
- `backend/bus-system/.../entity/Encomienda.java`, `MovimientoEncomienda.java`
- `backend/bus-system/.../repository/EncomiendaRepository.java`, `MovimientoEncomiendaRepository.java`
- `backend/bus-system/.../dto/request/EncomiendaRequest.java`, `EncomiendaClienteRequest.java`, `ActualizarEstadoEncomiendaRequest.java`
- `backend/bus-system/.../dto/response/EncomiendaResponse.java`, `TrackingEncomiendaResponse.java`, `MovimientoEncomiendaResponse.java`
- `frontend/Web-Intiwatana/src/components/AdminDashboard.jsx`
- `frontend/Web-Intiwatana/src/components/ClienteDashboard.jsx`
- `frontend/Web-Intiwatana/src/App.jsx`, `App.css`, `admin.css`

Modificados:
- `backend/.../enums/EstadoEncomienda.java`
- `backend/.../service/impl/EncomiendaServiceImpl.java`
- `backend/.../dto/response/TrackingEncomiendaResponse.java`
- `backend/.../controller/EncomiendaController.java`
- `frontend/.../components/AdminDashboard.jsx`
- `frontend/.../components/ClienteDashboard.jsx`
- `frontend/.../App.jsx`
- `frontend/.../App.css`

---

## 4. Arquitectura del módulo

- Capa de presentación: `AdminDashboard.jsx` (operación interna) y `SeguimientoPage` en `App.jsx`
  (público). Reutiliza los componentes compartidos `PageHeader`, `SearchBar`, `TableWrap`,
  `StatCard`, `Badge`, `ModalForm`, `Field`.
- API: `EncomiendaController` bajo `/api/v1/encomiendas`.
- Servicio: `EncomiendaServiceImpl` (registro, solicitud, consultas, estados, movimientos).
- Persistencia: JPA/Hibernate sobre PostgreSQL; estados almacenados como texto.
- No se creó ningún servicio, ruta ni store paralelo.

---

## 5. Base de datos y datos reales

Motor único: PostgreSQL (`jdbc:postgresql://localhost:5432/transporte_bd`). No se ejecutó ninguna
sentencia destructiva ni migración de esquema. El estado es una columna de texto, por lo que el
nuevo valor `SOLICITUD_PENDIENTE` no requirió cambio de esquema.

Estado real de `encomiendas` al cierre: **15 registros** (ENTREGADO=3, RECIBIDO=8, EN_ALMACEN=4).

Guías reales usadas como evidencia:
- `GUI-20261002-000032` — registro oficial admin, estado `EN_ALMACEN`.
- `GUI-20261002-000033` — solicitud del cliente convertida a `RECIBIDO` por personal.
- `GUI-20261002-000035` — alta realizada desde la UI (prueba de navegador).

---

## 6. Roles y permisos

Matriz aplicada en `EncomiendaController`:

| Operación | Endpoint | Roles autorizados |
|---|---|---|
| Listar | `GET /api/v1/encomiendas` | ADMIN, CAJERO, VENDEDOR, OPERADOR_ENCOMIENDAS |
| Detalle | `GET /api/v1/encomiendas/{id}` | ADMIN, CAJERO, VENDEDOR, OPERADOR_ENCOMIENDAS |
| Por remitente | `GET /api/v1/encomiendas/remitente/{id}` | ADMIN, CAJERO, VENDEDOR, OPERADOR_ENCOMIENDAS |
| Por sucursal | `GET /api/v1/encomiendas/sucursal/{id}` | ADMIN, CAJERO, VENDEDOR, OPERADOR_ENCOMIENDAS |
| Registro oficial | `POST /api/v1/encomiendas` | ADMIN, CAJERO, VENDEDOR, OPERADOR_ENCOMIENDAS |
| Solicitud cliente | `POST /api/v1/encomiendas/solicitar` | CLIENTE |
| Cambiar estado | `PATCH /api/v1/encomiendas/{id}/estado` | ADMIN, CAJERO, VENDEDOR, OPERADOR_ENCOMIENDAS |
| Movimientos | `GET /api/v1/encomiendas/{id}/movimientos` | ADMIN, CAJERO, VENDEDOR, OPERADOR_ENCOMIENDAS, CLIENTE (con control de propiedad) |
| Tracking | `GET /api/v1/encomiendas/tracking/{guia}` | Anónimo |

Cambio de seguridad: se retiró `CHOFER` de la actualización de estado (sobre-autorización). El
cliente nunca puede cambiar estados: no tiene el permiso del endpoint.

---

## 7. Endpoints usados (reales)

- `GET /api/v1/encomiendas`
- `GET /api/v1/encomiendas/{id}`
- `GET /api/v1/encomiendas/remitente/{id}`
- `GET /api/v1/encomiendas/{id}/movimientos`
- `POST /api/v1/encomiendas`
- `POST /api/v1/encomiendas/solicitar`
- `PATCH /api/v1/encomiendas/{id}/estado`
- `GET /api/v1/encomiendas/tracking/{numeroGuia}`
- Apoyo de formulario: `GET /api/v1/clientes`, `GET /api/v1/sucursales`.

---

## 8. Flujo de registro oficial

1. El operador abre **Nueva encomienda**.
2. Selecciona remitente, destinatario, sucursal origen y destino.
3. Ingresa descripción, peso, volumen, valor declarado, método de pago y observaciones.
4. El backend valida (ver sección 12), calcula el costo, crea el pago y guarda la encomienda en
   estado `RECIBIDO` con número de guía `GUI-AAAAMMDD-000001`.
5. Se registra el movimiento inicial y la auditoría.

Evidencia: guía `GUI-20261002-000032`, estado inicial `RECIBIDO`, HTTP 201 verificado.

---

## 9. Flujo de solicitud del cliente

`POST /api/v1/encomiendas/solicitar` (rol CLIENTE) deduce el remitente del token. El destinatario
es texto (nombre/teléfono) porque en el portal no es un cliente registrado. La encomienda nace en
`SOLICITUD_PENDIENTE`, no en `RECIBIDO`.

Evidencia: guía `GUI-20261002-000033`, estado `SOLICITUD_PENDIENTE` al crear.

---

## 10. Separación solicitud vs recepción

- La solicitud web representa intención del cliente, no recepción física.
- `SOLICITUD_PENDIENTE` es el único estado inicial del portal cliente.
- Sólo el personal confirma la recepción física moviendo a `RECIBIDO`.
- `RECIBIDO` sólo aparece por registro oficial o por confirmación de recepción.

Esto corrige la regla "no confundir solicitud del cliente con recepción oficial".

---

## 11. Máquina de estados y transiciones

Estados reales: `SOLICITUD_PENDIENTE`, `RECIBIDO`, `EN_ALMACEN`, `CARGADO_EN_BUS`, `EN_TRANSITO`,
`LLEGADO_A_DESTINO`, `EN_DESTINO`, `LISTO_ENTREGA`, `ENTREGADO`, `DEVUELTO`, `PERDIDO`.

Transiciones válidas aplicadas (backend y UI):

```
SOLICITUD_PENDIENTE -> RECIBIDO
RECIBIDO            -> EN_ALMACEN | CARGADO_EN_BUS | EN_TRANSITO
EN_ALMACEN          -> CARGADO_EN_BUS | EN_TRANSITO
CARGADO_EN_BUS      -> EN_TRANSITO
EN_TRANSITO         -> EN_DESTINO | LLEGADO_A_DESTINO
EN_DESTINO          -> LISTO_ENTREGA | ENTREGADO
LLEGADO_A_DESTINO   -> LISTO_ENTREGA | ENTREGADO
LISTO_ENTREGA       -> ENTREGADO | DEVUELTO
ENTREGADO | DEVUELTO | PERDIDO -> (final)
```

El frontend muestra únicamente las transiciones válidas y bloquea estados finales. El backend
rechaza cualquier salto inválido con HTTP 400.

---

## 12. Validaciones backend

- Remitente distinto de destinatario.
- Sucursal origen distinta de destino.
- Campos obligatorios y rangos (`@NotNull`, `@NotBlank`, `@DecimalMin`) en `EncomiendaRequest`.
- Transición de estado permitida (`validarTransicionEstado`).
- Formato de guía en tracking: `^GUI-\d{8}-\d{6}$`; formato inválido → HTTP 400.
- Control de propiedad de movimientos sólo para rol `CLIENTE`.
- Pago contra entrega se completa al pasar a `ENTREGADO`.

---

## 13. Validaciones frontend

- Alta: obligatorios, remitente≠destinatario, origen≠destino, peso positivo.
- Tracking: campo vacío → "Ingresa el número de guía."; formato inválido → "Revisa el número de
  guía ingresado.".
- Estados: sólo se ofrecen transiciones válidas.
- Mensajes de error y estados de carga visibles.

---

## 14. Módulo administrativo de encomiendas (UI)

Reubicado dentro de `AdminDashboard.jsx` (sección Encomiendas). Incluye encabezado con acción
**Nueva encomienda**, indicadores, búsqueda, filtro, tabla real y modales de alta, detalle y estado.

---

## 15. Indicadores

Calculados sobre los datos reales cargados desde la API:
- Total registradas.
- En tránsito / destino.
- Entregadas.
- Pendientes de entrega.

Evidencia de navegador: el indicador "Total registradas" está presente.

---

## 16. Búsqueda y filtros

- Búsqueda por número de guía, remitente o destinatario.
- Filtro por estado con las 11 opciones reales.
- El estado vacío muestra "Sin encomiendas para los filtros seleccionados".

---

## 17. Formulario de alta

10 campos: remitente, destinatario, sucursal origen, sucursal destino, descripción, peso, volumen,
valor declarado, método de pago y observaciones. Usa `POST /api/v1/encomiendas` y refresca la lista.

Evidencia UI: 14 → 15 filas tras el alta.

---

## 18. Detalle de encomienda

Modal con estado, remitente, destinatario, origen, destino, contenido, peso, costo, método de pago,
fecha de registro e historial de movimientos.

---

## 19. Historial de movimientos

Proviene de `movimientos` del `EncomiendaResponse` (y del endpoint dedicado). Se muestra en orden
cronológico con estado, fecha, sucursal y observación.

---

## 20. Tracking público (página)

Nueva ruta pública `/encomiendas/seguimiento` (sin login):
- Título: "Seguimiento de encomiendas".
- Descripción: "Consulta en línea el estado y la ubicación actual de tu encomienda."
- Campo "Número de guía", placeholder "Ej. GUI-2026-001234".
- Botón "Consultar estado".
- Carga: "Consultando estado de la encomienda...".
- Secciones: estado actual, progreso, datos generales, historial cronológico, última actualización.

Accesible desde los accesos rápidos y el footer del portal (ya no es un modal oculto).

---

## 21. Privacidad del tracking

El DTO público `TrackingEncomiendaResponse` ya **no** incluye `costo` ni `metodoPago`, y nunca
incluyó remitente, documentos, teléfonos ni personal responsable. Verificado: el tracking contiene
sólo `numeroGuia, estado, descripcionContenido, pesoKg, sucursalOrigen, sucursalDestino,
destinatarioNombre, fechaRegistro, movimientos`.

---

## 22. Validación de formato de guía

Backend y frontend comparten la regla `GUI-AAAAMMDD-000001`. Una cadena con formato inválido
retorna HTTP 400 sin consultar datos inexistentes; el frontend muestra el mensaje antes de llamar.

---

## 23. Responsive

Verificado sin desbordamiento horizontal (`scrollWidth - clientWidth = 0`) en:
- 1440 px: lista, formulario, detalle, tracking inicial y resultado.
- 820 px: lista, formulario, tracking resultado.
- 390 px: lista, formulario, tracking inicial y resultado.

12 capturas en `reports/capturas-encomiendas-tracking/`.

---

## 24. Accesibilidad

- Input de guía con `label` asociado, `aria-invalid` y `aria-describedby` del error.
- Errores con `role="alert"`, carga con `role="status"` y `aria-live="polite"`.
- Modales con `role="dialog"`, `aria-modal` y botones de cierre con `aria-label`.
- Filtro con `aria-label` y botones de fila con nombre accesible.

---

## 25. Pruebas API E/T

Script: `C:/Users/USER/AppData/Local/Temp/opencode/qa-enc-api.ps1`
Resultado: **24/24 PASA**.

| ID | Caso | Resultado |
|---|---|---|
| E01 | Listar como ADMIN | PASA |
| E02 | Listar sin token → 401 | PASA |
| E03 | Alta válida → 201 `RECIBIDO` + guía | PASA |
| E04 | Remitente = destinatario → 400 | PASA |
| E05 | Sucursal origen = destino → 400 | PASA |
| E06 | Falta peso → 400 | PASA |
| E07 | `RECIBIDO → EN_ALMACEN` | PASA |
| E08 | `EN_ALMACEN → ENTREGADO` inválido → 400 | PASA |
| E09 | CHOFER cambia estado → 403 | PASA |
| E10 | Cliente solicita → `SOLICITUD_PENDIENTE` | PASA |
| E11 | `SOLICITUD_PENDIENTE → EN_TRANSITO` → 400 | PASA |
| E12 | Personal confirma `SOLICITUD_PENDIENTE → RECIBIDO` | PASA |
| E13 | Movimientos registran solicitud y recepción | PASA |
| E14 | Listar por remitente (paginado) | PASA |
| E15 | Detalle incluye movimientos | PASA |
| E16 | Cliente dueño ve sus movimientos | PASA |
| T01 | Tracking guía válida sin token → 200 | PASA |
| T02 | Tracking formato inválido → 400 | PASA |
| T03 | Tracking guía inexistente → 404 | PASA |
| T04 | Tracking no expone remitente | PASA |
| T05 | Tracking no expone costo/metodoPago | PASA |
| T06 | Tracking incluye movimientos | PASA |
| T07 | Tracking anónimo | PASA |
| T08 | Tracking normaliza minúsculas | PASA |

Bug detectado y corregido durante E13: el endpoint de movimientos aplicaba el control de propiedad
a todo usuario autenticado, impidiendo al personal ver movimientos de encomiendas de clientes.
Ahora el control sólo aplica a `ROLE_CLIENTE`.

---

## 26. Pruebas de navegador y capturas

Scripts:
- `C:/Users/USER/AppData/Local/Temp/opencode/qa-enc-browser.mjs`
- `C:/Users/USER/AppData/Local/Temp/opencode/qa-enc-ui.mjs`

Resultados:
- Textos de tracking correctos (título, descripción, placeholder, botón, vacío, formato inválido).
- Resultado real de `GUI-20261002-000032`: estado "En almacén", 6 datos, 2 movimientos, última
  actualización visible.
- `sinPago = true`, `sinRemit = true`.
- Admin: 14 filas, indicador, filtro, 10 campos de formulario, detalle con movimientos.
- UI alta: 14 → 15 filas.
- `console errors/warnings (0)`, `requests failed (0)`.
- 12 capturas generadas.

---

## 27. Regresión y build

- Backend: `mvn -o test` → **Tests run: 54, Failures: 0, Errors: 0**.
- Backend empaquetado: `mvn -o package -DskipTests` → BUILD SUCCESS.
- Frontend: `npx eslint .` → sin hallazgos.
- Frontend: `npx vite build` → `✓ built`, sólo advertencia de tamaño de chunk (pre-existente).
- No se modificaron módulos ajenos (pasajes, viajes, clientes, pagos, reportes, chofer).

Regresión general (`C:/Users/USER/AppData/Local/Temp/opencode/qa-regresion.mjs`, reescrito para la UI
actual): `fails: []`.
- Landing: hero y 8 destinos, 4 accesos rápidos, "Rastrear encomienda" navega a la página pública.
- Tracking: guía válida → "En almacén", guía inexistente → mensaje correcto; sin pago ni remitente.
- Registrar: 5 campos.
- Pasajes: búsqueda real `GET /api/v1/viajes/disponibles?... → 200`.
- Admin: 10 secciones recorridas (Panel, Sucursales, Rutas, Autobuses, Viajes, Clientes, Pagos,
  Encomiendas, Reportes, Personal), todas OK; APIs de reportes y catálogos → 200.
- Login cliente → redirige a `/pasajes`.
- Única alerta de consola/red: el 404 intencional de la guía inexistente.

---

## 28. Riesgos, limitaciones y cierre

Riesgos/limitaciones conocidos (documentados, no ocultados):
- `solicitarEncomienda` acepta un `costo` enviado por el frontend del portal. Es un valor
  manipulable por el cliente; no se cambió para no alterar el flujo de pasajes, pero se recomienda
  calcularlo siempre en backend.
- La solicitud del cliente genera un `Pago` al momento de solicitar, antes de la recepción física.
- `calcularCosto` usa el peso y no el volumen.
- Estados históricos duplicados semánticamente (`EN_TRANSITO` vs `CARGADO_EN_BUS`) se conservan por
  compatibilidad.
- Los datos de prueba (guías `000030`–`000035`) quedan registrados como datos reales no
  destructivos.

Criterio de cierre:
- Registro oficial real: cumplido.
- Solicitud del cliente diferenciada de recepción: cumplido.
- Estados y transiciones válidas: cumplido.
- Tracking público seguro y con formato validado: cumplido.
- Roles y permisos: cumplido.
- Pruebas E/T, responsive, accesibilidad y capturas: cumplido.
- Sin cambios destructivos de PostgreSQL: cumplido.

**Estado: cerrado con observaciones registradas en la sección 28.**
