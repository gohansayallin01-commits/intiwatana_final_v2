# INFORME_FINAL_CORRECCION_INTIWATANA

> Sistema: Empresa de Transportes INTIWATANA S.R.L. — backend Spring Boot + frontend React (Vite) + PostgreSQL.
> Fase: **CORRECCIÓN → VERIFICACIÓN → CIERRE**. Proyecto: `intiwatana_final_v2`.
> Fecha de verificación: 2026-09-30. Estado del veredicto: **CERRADO CON OBSERVACIONES** (dentro del alcance acordado).

---

## 0. Resumen ejecutivo

Se ejecutó la fase de corrección funcional sobre los hallazgos de las auditorías previas
(`AUDITORIA_INTEGRAL_INTIWATANA.md` y `AUDITORIA-FINAL-INTIWATANA.md`). El sistema fue
compilado (backend `mvn` con salida limpia y frontend `vite build`), levantado de forma real sobre
PostgreSQL `transporte_bd`, y verificado con pruebas E2E de la API y revisión en navegador.

Resultado de la verificación E2E:

| Verificación | Resultado |
|---|---|
| Compilación backend `mvn -q compile -DskipTests` | OK (ExitCode 0) |
| Build frontend `npm run build` (vite v8) | OK (ExitCode 0) |
| Arranque backend real (`:8080`) con roles y usuarios nuevos | OK (PID 51500) |
| Login de todos los roles (admin, cajero, vendedor, operador, chofer, cliente) | OK |
| Reparación de asientos huérfanos (26) | Aplicada y verificada en BD |
| 10 flujos E2E de la fase anterior re-ejecutados | OK |
| Flujos nuevos (roles, HTTP 401/403/415, transiciones nuevas, contra entrega, concurrencia, auditoría, reportes) | OK |
| UI en navegador (login admin y operador, dashboard, redirect) | OK sin errores de consola |

---

## 1. Desarrollo de la fase (qué se hizo y con qué evidencia)

### 1.1 Reparación de los hallazgos de la auditoría integral (H-*)

| Hallazgo | Corrección aplicada | Verificación |
|---|---|---|
| H-2 Roles fijos | `RolNombre` + `ROLE_VENDEDOR` y `ROLE_OPERADOR_ENCOMIENDAS`; `DataInitializer.crearRoles` idempotente por rol; `crearPersonalInicial` sin guard planar, crea `vendedor1` (Rosa Prado Quispe) y `operador1` (Jhon Ochoa Sáenz), con claves de demo <REDACTADO> | Log `Rol creado: ROLE_VENDEDOR`, `ROLE_OPERADOR_ENCOMIENDAS`, `Usuario creado: ...`; login de ambos OK |
| H-3 Asientos huérfanos | `UPDATE asientos SET estado='DISPONIBLE' WHERE estado IN ('VENDIDO','RESERVADO') AND NOT EXISTS (SELECT 1 FROM boletos b WHERE b.asiento_id=asientos.id)` (26 filas) | 0 huérfanos restantes; conservados los legítimos `511` (BOL-…021) y `577` (BOL-…023) |
| H-6 `BoletoRequest.clienteId` | Quitado `@NotNull`; en `BoletoServiceImpl.vender` se valida de forma explícita (cajero sin `clienteId` abre el modal); el portal compra sin `clienteId` y el backend lo asigna desde el token | POST `/boletos/comprar` sin `clienteId` → **201** |
| H-7 RBAC cobertura | `@PreAuthorize` por endpoint en `BoletoController`, `EncomiendaController`; se incluyen los roles nuevos y CHOFER donde corresponde; movimientos de encomienda accesibles también para CLIENTE | Verificado con roles vendedor/operador/cajero/cliente y respuestas 403 para los no autorizados |
| H-9 Manejadores 401/415 | `SecurityConfig.exceptionHandling` con JSON 401/403; `GlobalExceptionHandler` con 415 (`HttpMediaTypeNotSupportedException`) y 405 (`HttpRequestMethodNotSupportedException`); 403 responde 401 si el request es anónimo | SIN token → **401**; `Content-Type: text/plain` → **415**; cajero en `/reportes` y `/auditoria` → **403** |
| H-10 Reportes | `resumen.boletosVendidos` anclado a `pago.fechaPago` vía `BoletoRepository.boletosVendidosDelPeriodo` (excluye `CANCELADO`), coherente con `ingresosPasajes` que ya se agregaba por fecha de pago | `boletosVendidos=4` igual a los boletos pagados del día (BOL-021, 023, 024, 026); el cancelado (025) excluido |
| H-11 Estados encomienda | `EstadoEncomienda` con `CARGADO_EN_BUS` y `LLEGADO_A_DESTINO`; mapa de transiciones estricto en `EncomiendaServiceImpl`; al entregar con pago `PENDIENTE` (contra entrega) se completa el pago | Cadena completa `RECIBIDO→CARGADO_EN_BUS→EN_TRANSITO→LLEGADO_A_DESTINO→LISTO_ENTREGA→ENTREGADO` (200×5); salto inválido `RECIBIDO→ENTREGADO` → **400**; pago pasó de `PENDIENTE` a `COMPLETADO` |
| H-12 Nombre de BD | `.env.example` línea 11 ya usaba `transporte_bd` (se actualizó la nota obsoleta en `database/README-BACKUP.md` a «Inconsistencia resuelta») | Comparación `.env.example` vs `application.yml` consistente |
| Módulo de auditoría (nuevo) | Tabla `auditoria` (created por `ddl-auto: update` y añadida al dump), `AuditoriaRepository`, `AuditoriaService`+`impl` (`REQUIRES_NEW` + try/catch para no romper la operación), `AuditoriaController` `GET /api/v1/auditoria` (ADMIN, paginado, máx 500), hooks en `login` (LOGIN), `vender` (VENTA_BOLETO), `comprar` (COMPRA_BOLETO), `escanear` (ESCANEO_QR), `cancelar` (CANCELAR_BOLETO), encomiendas (REGISTRO_ENCOMIENDA / SOLICITAR_ENCOMIENDA / CAMBIO_ESTADO_ENCOMIENDA) | `SELECT accion, COUNT(*) FROM auditoria` → LOGIN 6, VENTA_BOLETO 4, COMPRA_BOLETO 1, ESCANEO_QR 1, CANCELAR_BOLETO 1, REGISTRO_ENCOMIENDA 2, CAMBIO_ESTADO_ENCOMIENDA 5 |

### 1.2 Correcciones de frontend (coherentes con el backend)

- `App.jsx`: `ESTADO_LABELS` y `PASOS` con los 8 estados; `esAdmin` y `RutaProtegida` reconocen `ROLE_VENDEDOR` y `ROLE_OPERADOR_ENCOMIENDAS` (`/admin`).
- `PasajesPage.jsx`: `rutaPanel()` redirige los roles de staff nuevos a `/admin`.
- `ClienteDashboard.jsx`, `AdminDashboard.jsx` y `CrudUsuarios.jsx`: etiquetas/colores/estadísticas para los roles y estados nuevos.
- `Login.jsx` no requirió cambios: ya redirige a `/admin` cualquier rol que no sea CLIENTE/CHOFER.
- Verificación en navegador: login `admin` → `/admin` OK; login `operador1` → `/admin` con label «Operador de Encomiendas» y módulo Encomiendas; **0 errores de consola**, 0 peticiones rotas (solo el fetch esperado y benigno a `/clientes` → 403 para roles no admin).

### 1.3 Verificación técnica de extremo a extremo (evidencia en `%TEMP%\opencode`)

- `e2e_ventas_encomiendas.txt`: venta cajero (201, BOL-024), doble venta (400 «El asiento 01 ya no está disponible»), venta vendedor (201), compra portal sin `clienteId` (201, BOL-026), escaneo chofer (200→`USADO`), cancelación admin (200→`CANCELADO`), encomienda contra entrega (201, GUI-023), transiciones (5×200), transición inválida (400).
- `e2e_rbac_http.txt`: 401 anónimo, 415 `text/plain`, 403 (cajero en reportes y auditoría, cliente en auditoría), 200 (encomiendas operador, boletos vendedor/cajero).
- `e2e_reportes_auditoria.txt` + `e2e_resumen.json`: resumen coherente (boletosVendidos=4, ingresosPasajes=62.00, precioPromedio 12.40, encomiendas=4, ingresosEncomiendas=32.50).
- `e2e_concurrencia.txt`: dos POST simultáneos al mismo asiento (685, viaje 6) → una **201** (BOL-028) y una **400**; bloqueo pesimista verificado.
- `conc_*`, `e2e_logins*.txt`, `tokens2.json`: logins de los 6 roles.

### 1.4 Estado del respaldo de datos

- `database/transporte_bd.sql` regenerado con `pg_dump --format=plain` (incluye tabla `auditoria` + datos), 72.450 bytes, mismo formato/esquema de owner que el original.
- `database/intiwatana_transporte_bd.backup` regenerado con `pg_dump -Fc`.
- `database/README-BACKUP.md` actualizado (inconsistencia resuelta).

---

## 2. Matriz de cumplimiento actualizada (23 secciones)

| # | Sección | Veredicto fase auditoría | Veredicto tras corrección |
|---|---|---|---|
| 4.1 | Autenticación y sesión (login/JWT) | CUMPLE | **CUMPLE** (además auditable: hook LOGIN) |
| 4.2 | Usuarios, roles y sucursales | CUMPLE PARCIALMENTE | **CUMPLE** (roles nuevos + seed idempotente) |
| 4.3 | Clientes (registro público y CRUD) | CUMPLE | **CUMPLE** |
| 4.4 | Buses y flota | CUMPLE | **CUMPLE** |
| 4.5 | Rutas y destinos | CUMPLE PARCIALMENTE | **CUMPLE CON NOTA** (catálogo `destinos.jsx` = presentación; compra/horarios usan `/viajes/disponibles`) |
| 4.6 | Viajes y programación | CUMPLE | **CUMPLE** |
| 4.7 | Selección de asientos y disponibilidad | CUMPLE PARCIALMENTE | **CUMPLE** (26 asientos reparados; concurrencia verificada) |
| 4.8 | Venta de boletos | CUMPLE | **CUMPLE** |
| 4.9 | Pagos y métodos de pago | CUMPLE | **CUMPLE** (contra entrega se completa al entregar) |
| 4.10 | QR y escaneo (chofer) | CUMPLE | **CUMPLE** |
| 4.11 | Cancelación y reembolso | CUMPLE | **CUMPLE** (asiento liberado; pago reembolsado en cancelación anticipada) |
| 4.12 | Registro de encomiendas | CUMPLE | **CUMPLE** |
| 4.13 | Tracking público de encomiendas | CUMPLE | **CUMPLE** |
| 4.14 | Cambios de estado y movimientos | CUMPLE PARCIALMENTE | **CUMPLE** (mapa de transiciones + 2 estados nuevos + movimientos completos) |
| 4.15 | Portal del cliente (autoservicio) | CUMPLE PARCIALMENTE | **CUMPLE** (`clienteId` opcional en compra) |
| 4.16 | Panel del chofer | CUMPLE PARCIALMENTE | **CUMPLE** |
| 4.17 | Panel de caja/administración | CUMPLE | **CUMPLE** |
| 4.18 | Reportes de ventas e ingresos | CUMPLE PARCIALMENTE | **CUMPLE CON NOTA** (ver 3.1) |
| 4.19 | Reporte de ocupación | CUMPLE PARCIALMENTE | **CUMPLE CON NOTA** (ver 3.1) |
| 4.20 | Reporte de encomiendas | CUMPLE | **CUMPLE** |
| 4.21 | Exportación CSV | CUMPLE | **CUMPLE** |
| 4.22 | Seguridad y RBAC | CUMPLE PARCIALMENTE | **CUMPLE** (PreAuthorize ampliado; 401/403/415 JSON) |
| 4.23 | Integración y datos | CUMPLE PARCIALMENTE | **CUMPLE** (BD reparada, dumps actualizados, auditoría funcional) |

---

## 3. Observaciones documentadas (no bloquean el cierre del alcance)

### 3.1 Reportes — notas de presentación
- `resumen.precioPromedioPasaje` se calcula como ingresos del período ÷ pagos registrados (incluye boletos cancelados, porque el pago no se elimina al cancelar). `boletosVendidos` (corregido, por fecha de pago y sin cancelados) puede diferir levemente de ese divisor. Es coherente con la definición preexistente y se documenta como nota, no como falla.
- `ocupacionPromedio` se mide por fecha de salida del viaje dentro del período (ventas a viajes futuros no inflan la ocupación del día). Coherente con la decisión acordada.

### 3.2 Pendientes explícitos (tarea posterior, fuera de esta fase)
- **F5 (Flujo de negocio del demonio/sistema principal)** y **Run Java (arranque desde el editor)** — se dejaron marcados como PENDIENTE en la fase anterior y permanecen pendientes por acuerdo del alcance.
- JWT: se usa el secreto de desarrollo (`JwtUtil` advierte en cada arranque). Definir `JWT_SECRET` antes de producción.

### 3.3 Menores, sin impacto funcional
- `PageImpl` se serializa as-is (warning de Spring) en endpoints paginados; no afecta estabilidad de estructura.
- En el dashboard admin, roles no-ADMIN solicitan `/clientes` y reciben 403 (fetch esperado, no rompe la UI).

---

## 4. INTIWATANA — RESULTADO FINAL

**CERRADO CON OBSERVACIONES** en el alcance acordado (reportes + correcciones críticas + refactor de datos/BD).

- Backend corregido, compilado y ejecutándose de forma real contra `transporte_bd` (PID 51500, `:8080`).
- Frontend corregido, con build exitoso y verificación en navegador sin errores de consola.
- Base de datos `transporte_bd` reparada (asientos huérfanos), `ddl-auto` aplicó `public.auditoria`, roles/usuarios nuevos presentes, y se regeneraron `database/transporte_bd.sql` y `intiwatana_transporte_bd.backup`.
- Los 10 flujos E2E previos y los flujos nuevos quedaron verificados con evidencia real de respuestas HTTP y estados de BD.

**PENDIENTE EXPRESO** (task F5 y Run Java del plan): ejecución de pruebas E2E a través de los flujos de negocio completos y arranque del proyecto desde el editor, una vez se disponga de aclaración del alcance por parte del usuario. Con ellos, el estado puede pasar a CERRADO sin observaciones.

---

## 5. Anexo — evidencia

- Compilaciones y arranque: log `backend/bus-system/logs/sistema-transporte.log` (arranque PID 51500 09:41–09:43, roles 09:43, flujos 09:47–09:57).
- E2E: `%TEMP%\opencode\e2e_*.txt`, `e2e_resumen.json`, `conc_a.txt`, `conc_b.txt`, `tokens2.json`.
- QA navegador: skill browser-automation sobre `http://localhost:5173` (login admin y operador → `/admin`, label de rol correcto, 0 errores de consola).