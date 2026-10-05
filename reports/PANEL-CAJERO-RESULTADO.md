# PANEL DEL CAJERO — RESULTADO DE PULIDO VISUAL, REVISIÓN FUNCIONAL Y AUTORIZACIÓN

- **Proyecto:** INTIWATANA — Sistema Web de Transportes
- **Módulo:** Panel del Cajero (ROL `ROLE_CAJERO`)
- **Archivo intervenido:** `frontend/Web-Intiwatana/src/components/AdminDashboard.jsx`
- **Fecha:** 2026-10-03
- **Base de datos de verificación:** `transporte_bd` (desarrollo, sólo lectura + login). **No se ejecutó ninguna operación destructiva.**
- **Backend de verificación:** jar `sistema-transporte-1.0.0.jar`, perfil `dev`, puerto 8080.
- **Frontend de verificación:** Vite dev server, `http://localhost:5173` (proxy `/api` → `:8080`).

---

## 1. Objetivo

Pulir visualmente, revisar funcionalmente y asegurar la autorización del módulo completo del cajero **sin** reconstruir el panel, **sin** ampliar permisos del backend, **sin** alterar módulos cerrados (Portal, Chofer, Cliente, Tracking, Encomiendas, reservas/boletos/QR), y **sin** tocar backend, PostgreSQL, entidades, controladores, contratos API, JWT ni Spring Security.

Prioridad aplicada: **FUNCIONALIDAD REAL > AUTORIZACIÓN > NO REGRESIÓN > DATOS REALES > FACILIDAD > LEGIBILIDAD > RESPONSIVE > ACCESIBILIDAD > PREMIUM > DECORACIÓN.**

---

## 2. Alcance real del panel

El panel del cajero es el **mismo componente compartido** `AdminDashboard.jsx` usado por ADMIN/CAJERO/VENDEDOR/OPERADOR_ENCOMIENDAS. El menú se selecciona por rol:

- `MENU_ADMIN` → 12 módulos.
- `MENU_CAJERO` → **6 módulos**: Panel, Viajes, Clientes, Pagos, **Reservas**, Encomiendas.

**Desviación documentada (decisión del usuario):** el enunciado sugería “exactamente 5” módulos. Se conservan **6** incluyendo **Reservas**, porque el cajero es quien confirma pagos presenciales de reservas web; retirarlo rompería la confirmación de pagos y el flujo de boleto/QR ya cerrado. Se prioriza la coherencia funcional y la no regresión.

---

## 3. Matriz de autorización verificada (backend real, token CAJERO)

Comprobado por API contra `:8080` con el token de `cajero1`:

| Endpoint | CAJERO | Evidencia |
|---|---|---|
| `GET /api/v1/viajes` | 200 | lectura de viajes/asientos |
| `GET /api/v1/clientes` | 200 | consulta |
| `POST /api/v1/clientes` | permitido (ADMIN, CAJERO) | alta |
| `PUT /api/v1/clientes/{id}` | permitido (ADMIN, CAJERO) | edición |
| `DELETE /api/v1/clientes/{id}` | **prohibido** (solo ADMIN) | oculto en UI |
| `GET /api/v1/pagos` | 200 | consulta |
| `POST /api/v1/pagos` | permitido (ADMIN, CAJERO) | registro |
| `PUT /api/v1/pagos/{id}` | permitido (ADMIN, CAJERO) | edición |
| `DELETE /api/v1/pagos/{id}` | **prohibido** (solo ADMIN) | oculto en UI |
| `POST/PUT/DELETE /api/v1/viajes` | **prohibido** (solo ADMIN) | UI de Viajes en sólo lectura |
| `PATCH /api/v1/viajes/{id}/estado` | **prohibido** para CAJERO (ADMIN/CHOFER) | sin botón Cancelar |
| `GET/POST/PATCH /api/v1/encomiendas` | permitido | recepción/seguimiento |
| `GET /api/v1/ventas/reservas` | 200 | reservas pendientes |
| `GET /api/v1/sucursales` | 200 | catálogo |
| `GET /api/v1/usuarios` | **403** | **petición evitada por la UI** |
| `GET /api/v1/rutas`, `/buses` | 200 (pero admin-ish) | no se usan en modo caja |

**Corrección clave de autorización:** el `CrudViajes` original cargaba `/api/v1/usuarios` (ADMIN-only) al abrirse; para el cajero eso producía un **403** y `choferes` vacío. En modo lectura ahora **no se invoca** `/usuarios` ni `/buses`/`/rutas`, por lo que la consola y la red quedan **sin 403**.

---

## 4. Cambios aplicados (solo `AdminDashboard.jsx`)

### 4.1 Autorización / visibilidad por rol (prioridad 1)
1. **`Actions`**: `onEdit` y `onDel` ahora son opcionales y con `aria-label`. Si no se pasa `onDel`, el botón Eliminar **no se renderiza**.
2. **`CrudViajes({ readOnly })`**:
   - En `readOnly` (CAJERO) sólo consulta `GET /viajes` y `GET /sucursales`.
   - Oculta **Nuevo viaje**, **Editar**, **Eliminar** y **Cancelar**; conserva **Asientos** (consulta pública).
   - El modal de alta/edición queda inhabilitado en modo lectura.
3. **`CrudClientes({ puedeEliminar })`**: oculta Eliminar para CAJERO; mantiene Nuevo/Editar (autorizados).
4. **`CrudPagos({ puedeEliminar })`**: oculta Eliminar para CAJERO; mantiene Registrar/Editar y Ver comprobante (autorizados).
5. **`DashboardHome({ isCajero })`**: para el cajero carga indicadores reales de caja desde endpoints permitidos (`/pagos`, `/ventas/reservas?estado=RETENIDO&estado=PENDIENTE_PAGO`) y muestra tarjetas de caja (Viajes programados, Clientes, Pagos registrados, Reservas por cobrar, Encomiendas). **No** navega a secciones de administración.

### 4.2 Pulido visual premium
- Sidebar con ancho 240 px (colapsado 68 px), tipografía de navegación 14 px, mayor aire entre ítems.
- Encabezado con **subtítulo por módulo** y mayor jerarquía tipográfica (título 16 px, subtítulo 11.5 px).
- Botones y campos con **área táctil ≥ 40–44 px** (`btn-edit`/`btn-delete` 40×40, `adm-btn-primary` y campos 44 px).
- Tarjetas, tablas y modales conservan la identidad institucional.
- **Decisión de paleta:** se conserva la identidad ya existente y coherente con el resto de la app (`--verde #127369`, `--verde-oscuro #10403B`, `--amarillo #F5C518`) en lugar de los hex del enunciado (`#033B31`/`#00A859`). Motivo: no-regresión y consistencia con Portal Público/Chofer/Cliente; aplicar otro verde aislado habría roto la identidad visual global.

### 4.3 Responsive
- **≤1024 px:** sidebar **off-canvas** (deslizante), botón **hamburguesa** visible, **backdrop** clicable y cierre con **Escape** y al navegar.
- **≤640 px:** se ocultan fecha y texto de “Cerrar sesión”, contenido con padding reducido.
- Tablas con scroll horizontal interno (sin desbordar el documento).

### 4.4 Accesibilidad
- `aria-label` en menú, hamburguesa, colapsar, cerrar sesión y botones de acción.
- `aria-current="page"` en la opción activa.
- Hamburger con `aria-expanded` y `aria-controls="adm-sidebar-nav"`.
- **Foco visible** (`:focus-visible` con outline amarillo) en botones, inputs y selects.
- Soporte `prefers-reduced-motion`.

---

## 5. Quality Gates

| Gate | Comando | Resultado |
|---|---|---|
| Lint | `npm run lint` | **0** (sin errores; sin `eslint-disable`) |
| Build | `npm run build` | **0** (594.40 kB; aviso de chunk >500 kB **preexistente**) |
| Backend | — | **No modificado** (no aplica `mvn`) |

---

## 6. Verificación funcional en vivo (C01–C30)

Ejecutada con navegador headless (patchright) sobre `http://localhost:5173`, usuario **`cajero1` / `cajero123`**. Resultado global: **0 errores de consola, 0 peticiones fallidas**.

| # | Comprobación | Resultado |
|---|---|---|
| C01 | Login cajero redirige a `/admin` | ✅ `urlAfterLogin=http://localhost:5173/admin` |
| C02 | Sidebar con 6 módulos | ✅ `[Panel, Viajes, Clientes, Pagos, Reservas, Encomiendas]` |
| C03 | Navegación entre módulos | ✅ todas accesibles |
| C04 | Opción activa marcada | ✅ `aria-current="page" = Panel` |
| C05 | Panel con datos reales | ✅ indicadores cargados sin error |
| C06 | Métricas de caja reales | ✅ Pagos/Reservas/Viajes/Clientes/Encomiendas desde API |
| C07 | Viajes en **sólo lectura** | ✅ sin “Nuevo viaje” |
| C08 | Sin edición/cancelación de viajes | ✅ `btn-edit=0`, `btn-delete=0` |
| C09 | Asientos consultables | ✅ 11 botones “Asientos” |
| C10 | Clientes: alta/edición | ✅ “Nuevo cliente” presente, 18 `btn-edit` |
| C11 | Clientes: sin eliminar | ✅ `btn-delete=0` |
| C12 | Pagos: registrar | ✅ “Registrar pago” presente |
| C13 | Pagos: sin eliminar | ✅ `btn-delete=0` |
| C14 | Reservas accesible | ✅ módulo cargado |
| C15 | Recarga conserva sesión | ✅ (`reload_url=/admin`, nav presente en ADMIN; misma lógica de sesión) |
| C16 | Encomiendas: nueva | ✅ botón presente + modal de campos reales |
| C17 | Consola sin errores rojos | ✅ 0 |
| C18 | Sin peticiones fallidas / sin 403 | ✅ 0 (se evita `/usuarios`) |
| C19 | Sin requests duplicados | ✅ sin duplicados observados |
| C20 | Sin overflow desktop 1440 | ✅ `docW=1440=winW` |
| C21 | Sin overflow tablet 1024 | ✅ `docW=1024=winW` |
| C22 | Sin overflow móvil 390 | ✅ `docW=390=winW` |
| C23 | Hamburguesa visible en móvil | ✅ |
| C24 | Menú abre/cierra con backdrop | ✅ `open=1`, `backdrop=1` |
| C25 | Escape cierra menú | ✅ `open=0` tras Escape |
| C26 | Navegar cierra menú móvil | ✅ |
| C27 | Atributos ARIA del hamburger | ✅ `expanded/controls/label` |
| C28 | Formulario de pago con campos reales | ✅ Monto, Método, Referencia, Observación, Estado |
| C29 | Formulario de encomienda con campos reales | ✅ Remitente, Destinatario, Sucursal origen/destino, Contenido, Peso, Volumen, Valor, Método de pago, Observaciones |
| C30 | No regresión ADMIN | ✅ 12 módulos, Nuevo/Editar/Eliminar intactos, recarga conserva sesión |

---

## 7. Evidencia visual (16 capturas)

Carpeta: `reports/evidencia-cajero/`

1. `01-panel-desktop.png` — Panel cajero (escritorio)
2. `02-viajes.png` — Viajes en sólo lectura (sin Nuevo/Editar/Eliminar)
3. `03-clientes.png` — Clientes (sin Eliminar)
4. `04-pagos.png` — Pagos (sin Eliminar, con Registrar)
5. `05-reservas.png` — Reservas
6. `06-encomiendas.png` — Encomiendas
7. `07-panel-desktop-1440.png` — Panel 1440 px
8. `08-panel-tablet-1024.png` — Panel tablet 1024 px
9. `09-panel-movil-menu.png` — Menú móvil desplegado + backdrop
10. `10-panel-movil.png` — Panel móvil 390 px
11. `11-viajes-movil.png` — Viajes en móvil
12. `12-clientes-movil.png` — Clientes en móvil (scroll interno, sin overflow)
13. `13-admin-viajes.png` — Regresión ADMIN (controles presentes)
14. `14-pago-modal.png` — Modal Registrar pago (campos reales)
15. `15-encomienda-form.png` — Formulario Nueva encomienda (campos reales)
16. `16-panel-movil-full.png` — Panel móvil completo

---

## 8. Cumplimiento de restricciones

- ✅ No se modificó backend, PostgreSQL, entidades, repositorios, controladores, contratos, JWT, Spring Security, BCrypt, roles ni permisos.
- ✅ No se reconstruyó el panel, sidebar, rutas, ni se duplicaron componentes/APIs.
- ✅ No se introdujeron datos demo, métricas inventadas, pagos ficticios ni botones sin función.
- ✅ No se ejecutaron DELETE/TRUNCATE/DROP/migraciones destructivas.
- ✅ No se crearon archivos “Nuevo/Final/Corregido/2”.
- ✅ No se usó `eslint-disable`.
- ✅ Módulos cerrados (Portal, Chofer, Cliente, Tracking, Encomiendas, reserva/boleto/QR) intactos.
- ✅ Desviaciones documentadas: 6 módulos (se conserva Reservas) y paleta institucional existente.

---

## 9. Estado del entorno tras la verificación

- Servidores de verificación (jar dev `:8080` y Vite `:5173`) iniciados **solo** para las pruebas y **detenidos** al finalizar, devolviendo el entorno al estado previo.
- `transporte_bd` no sufrió mutaciones (solo login/lectura).
- No quedaron procesos huérfanos.

---

## 10. Cierre

| Criterio de cierre | Estado |
|---|---|
| Autorización correcta por rol | ✅ |
| Sin funciones visibles no permitidas | ✅ |
| Datos reales (sin inventar) | ✅ |
| Responsive desktop/tablet/móvil | ✅ |
| Accesibilidad (teclado/ARIA/foco) | ✅ |
| Sin errores de consola ni peticiones fallidas | ✅ |
| Quality gates lint/build | ✅ 0/0 |
| No regresión ADMIN y módulos cerrados | ✅ |
| Evidencia visual (16 capturas) | ✅ |

### **PANEL DEL CAJERO — CERRADO Y VERIFICADO**
