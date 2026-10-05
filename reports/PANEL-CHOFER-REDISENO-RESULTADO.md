# PANEL DEL CHOFER — REDISEÑO Y CORRECCIÓN FUNCIONAL · INFORME FINAL

**Fecha:** 2026-10-02  
**Alcance:** exclusivo `frontend/Web-Intiwatana` (panel del chofer)  
**Backend y PostgreSQL:** sin modificaciones de código; solo lectura y verificación  
**Estado final:** **PANEL DEL CHOFER — CERRADO** (funcionalidades reales verificadas en navegador + API + base de datos)

---

## 1. Mapa componente → ruta → API → datos → función real

| Vista / Componente | Ruta | API real | Origen de datos | Función real implementada |
|---|---|---|---|---|
| `MisViajesView` | `/chofer` | `GET /api/v1/viajes/mis-viajes` | `viajes` del chofer autenticado | Listar y agrupar viajes propios por estado |
| `ViajeCard` | `/chofer` | — | `ViajeResponse` | Ruta, fecha, hora, placa, llegada, ocupación, acciones según estado |
| `ChoferStats` | `/chofer` | — | derivado de los viajes reales | Métricas: asignados, programados, en curso, finalizados, cancelados, asientos libres |
| `AsientosModal` | `/chofer` | `GET /api/v1/viajes/{id}/asientos` | `asientos` del viaje | Mapa real de asientos (40) con leyenda de estados |
| `PasajerosView` | `/chofer/pasajeros` | `GET /api/v1/boletos/viaje/{id}` | `boletos → cliente → asiento` | Pasajeros del viaje con DNI/RUC, asiento, tipo, pago y estado |
| `QrView` | `/chofer/qr` | `POST /api/v1/boletos/escanear?codigoQr=` | `codigo_qr` del boleto | Validar QR real; el backend marca el boleto `USADO` |
| `PerfilView` | `/chofer/perfil` | `POST /api/v1/auth/login` (sesión) | token de sesión | Perfil de **solo lectura** (0 campos editables) |
| `ChoferSidebar` / `ChoferHeader` | todas | — | sesión | Navegación, usuario, recargar, salir |

**Endpoints que NO seaggregate al menú y por qué (verificado, no supuesto):**

- `GET /api/v1/reportes/**` → **HTTP 403** para `ROLE_CHOFER` (`@PreAuthorize` solo admite `ROLE_ADMIN`). Por eso **“Reporte de Viaje” no aparece** en el menú en lugar de mostrar una pantalla vacía.
- `GET /api/v1/usuarios/{id}` → **HTTP 403** para `ROLE_CHOFER`. Por eso **Perfil es solo lectura**.
- Lector de cámara/QR: no existe ninguna dependencia de escaneo en el proyecto. El `codigo_qr` real generado por `BoletoServiceImpl` **es el `numeroBoleto`** (verificado en BD: `codigo_qr == numero_boleto` en las 17 filas). Por eso la validación es **ingreso del número real del QR**, sin simular cámara ni resultados.

---

## 2. Archivos creados

```
frontend/Web-Intiwatana/src/chofer.css                       (diseño + responsive exclusivo del panel)
frontend/Web-Intiwatana/src/components/chofer/apiChofer.js    (única capa de acceso a la API)
frontend/Web-Intiwatana/src/components/chofer/choferFormato.js (fechas, estados, orden, métricas)
frontend/Web-Intiwatana/src/components/chofer/ChoferSidebar.jsx
frontend/Web-Intiwatana/src/components/chofer/ChoferHeader.jsx
frontend/Web-Intiwatana/src/components/chofer/ChoferStats.jsx
frontend/Web-Intiwatana/src/components/chofer/ViajeCard.jsx
frontend/Web-Intiwatana/src/components/chofer/AsientosModal.jsx
frontend/Web-Intiwatana/src/components/chofer/MisViajesView.jsx
frontend/Web-Intiwatana/src/components/chofer/SelectorViaje.jsx
frontend/Web-Intiwatana/src/components/chofer/PasajerosView.jsx
frontend/Web-Intiwatana/src/components/chofer/QrView.jsx
frontend/Web-Intiwatana/src/components/chofer/PerfilView.jsx
```

## 3. Archivos modificados

```
frontend/Web-Intiwatana/src/components/ChoferDashboard.jsx  (reescrito: carga real, error/reintento, rutas hijas, logout, menú móvil)
frontend/Web-Intiwatana/src/App.jsx                           (path="/chofer/*" con RutaProtegida roles=["ROLE_CHOFER"])
frontend/Web-Intiwatana/src/main.jsx                          (import "./chofer.css")
```

Nada más fue tocado: no se modificaron backend, PostgreSQL, módulos Público/Admin/Encomiendas/Cliente, ni `App.css`.

---

## 4. Correcciones funcionales aplicadas

| # | Defecto encontrado | Corrección |
|---|---|---|
| 1 | El panel solo pintaba sidebar/header; **no cargaba ni mostraba viajes** | `ChoferDashboard` carga `mis-viajes` y renderiza tarjetas, métricas y estados |
| 2 | Sidebar **oculto sin posibilidad de navegar** | Sidebar fijo en escritorio + menú lateral con overlay en móvil |
| 3 | Menú con funciones **inexistentes** (Reporte de Viaje) | Retirado: la API responde 403 para chofer |
| 4 | `npm run lint` con **9 errores** | 0 errores |
| 5 | Sin estados de carga, error ni reintento | Spinner, mensaje de error del backend y botón **Reintentar** verificado |
| 6 | Perfil sin datos | Datos reales de la sesión, solo lectura |
| 7 | **Bug encontrado en QA**: entrar directo a `/chofer/qr` o `/chofer/pasajeros` (F5 o enlace directo) dejaba el viaje **sin seleccionar** porque `viajeId` se inicializaba antes de que llegaran los viajes | Efecto de selección por defecto en ambas vistas → verificado con URL directa y recarga |
| 8 | Horario con formato roto (`08:00a m`) por locale `es-PE` | Normalización a `08:00 A.M.` |
| 9 | Métricas con alturas desiguales por salto de línea del rótulo | Rótulo `Asientos libres` (+ `title` explicativo) → alturas uniformes medidas |
| 10 | Menú móvil sin cierre por teclado | Cierre con `Escape` y con clic en el overlay |

---

## 5. Evidencia funcional real en navegador

Servidor: `http://localhost:5173` · API: `http://localhost:8080` · BD: `transporte_bd` · Usuario: `chofer1`.

### 5.1 Login y navegación
- `POST /api/v1/auth/login` → **HTTP 200**; redirección a `/chofer`.
- Menú renderizado: `Mis Viajes · Pasajeros · Validar QR · Perfil · Salir`.

### 5.2 Mis Viajes (datos reales)
- `GET /api/v1/viajes/mis-viajes?size=200&sort=fechaHoraSalida,desc` → **HTTP 200**.
- Métricas: `Viajes asignados=5 · Programados=0 · En curso=0 · Finalizados=5 · Asientos libres=137`.
- Tarjetas con datos reales de BD, por ejemplo:
  - `Huamanga → Vilcas Huaman` · `vie, 15 ene · 08:00 A.M. · 2.5 h aprox.` · `MNO-345` · `Lleg. 10:30 A.M.` · `HUA-VIL-01` · `34 / 40 libres`
  - `Vilcas Huaman → Huamanga` · `dom, 27 set · 06:39 P.M.` · `MNO-345` · `0 / 40 libres`

### 5.3 Iniciar / Finalizar viaje (mutación real)
Ejecutado desde la interfaz con confirmación, recarga y contraste con PostgreSQL:

```
PATCH /api/v1/viajes/31/estado?estado=EN_CURSO    -> HTTP 200
   UI:  "Viaje #31: estado actualizado a EN_CURSO."  · badge "En curso"  · métricas 0/1/4
   tras F5: sigue "En curso" (persistencia real)
PATCH /api/v1/viajes/31/estado?estado=FINALIZADO -> HTTP 200
   UI:  "Viaje #31: estado actualizado a FINALIZADO." · métricas 0/0/5
   tras F5: sigue "Finalizado"
```
Segunda verificación literal del `PATCH` en el panel Network (chofer2, viaje 8, su propio viaje):
```
POST   /api/v1/auth/login                                        -> 200
GET    /api/v1/viajes/mis-viajes?size=200&sort=...desc           -> 200
PATCH  /api/v1/viajes/8/estado?estado=EN_CURSO                   -> 200
GET    /api/v1/viajes/mis-viajes?size=200&sort=...desc           -> 200
PATCH  /api/v1/viajes/8/estado?estado=FINALIZADO                -> 200
GET    /api/v1/viajes/mis-viajes?size=200&sort=...desc           -> 200
```
Contraste en PostgreSQL:
```sql
select v.id, v.estado, u.username, r.codigo from viajes v ... where v.id in (8,31);
 id |   estado   | username |   codigo
----+------------+----------+------------
 31 | FINALIZADO | chofer1  | HUA-VIL-01
  8 | FINALIZADO | chofer2  | HUA-HCA-01
```
Solo se ofrecen las transiciones que el backend acepta: en `PROGRAMADO` aparece **Iniciar viaje**; en `EN_CURSO`, **Finalizar viaje**; en `FINALIZADO`, ninguna acción de estado.

### 5.4 Mapa de asientos
- `GET /api/v1/viajes/31/asientos` → **HTTP 200** · `40` asientos renderizados.
- Leyenda real: `Disponible (34) · Vendido (6) · Reservado (0) · Bloqueado (0)`.
- Occupancy de la tarjeta (`34 / 40 libres`) coincide con la API y con la BD.

### 5.5 Pasajeros
- `GET /api/v1/boletos/viaje/31` → **HTTP 200** · `6` filas.
- Ejemplo real: `BOL-20261002-000032 · Prueba INTIWATANA-A-001 · INTIWATANA1 · 1A · VENTANA · S/ 15.00 · USADO`.
- URL directa y recarga (F5): sigue seleccionando el viaje y mostrando las 6 filas.

### 5.6 Validación de QR (real, con efectos en BD)
| Caso | Resultado |
|---|---|
| QR inexistente `BOL-00000000-000000` | `HTTP 400` → “Validación rechazada · QR inválido o boleto no encontrado” |
| Campo vacío | Validación en el formulario, **sin llamada a la API** → “Ingresa el código QR del boleto.” |
| QR válido escrito a mano `BOL-20261002-000035` | `HTTP 200` → “Boleto validado · Prueba INTIWATANA-A-001 · asiento 1D · INTIWATANA1 · **USADO**”; input limpiado y lista refrescada |
| Re-escanear el mismo boleto | `HTTP 400` → “El boleto ya fue usado o está cancelado. Estado: USADO” |

Efecto verificado en PostgreSQL (`codigo_qr == numero_boleto`):
```
 id |    numero_boleto     | estado | viaje_id | chofer
----+---------------------+--------+----------+---------
 27 | BOL-20260930-000027 | USADO  |        7 | chofer1
 32 | BOL-20261002-000032 | USADO  |       31 | chofer1
 33 | BOL-20261002-000033 | USADO  |       31 | chofer1
 34 | BOL-20261002-000034 | USADO  |       31 | chofer1
 35 | BOL-20261002-000035 | USADO  |       31 | chofer1
 36 | BOL-20261002-000036 | ACTIVO |       31 | chofer1
 37 | BOL-20261002-000037 | ACTIVO |       31 | chofer1
```

### 5.7 Perfil, sesión y cierre
- `/chofer/perfil` → datos reales de sesión: `USUARIO=chofer1 · NOMBRE= Carlos Flores · ROL=ROLE_CHOFER · SUCURSAL=Terminal Huamanga · ID USUARIO=3 · ID SUCURSAL=1`.
- **0 campos editables** (sin endpoints de actualización disponibles para el chofer).
- **Salir** → `http://localhost:5173/login` y `localStorage` vacío.
- `/chofer` sin sesión → redirigido a `/login`.

### 5.8 Manejo de errores y sesión inválida
| Prueba | Resultado |
|---|---|
| Ruta `mis-viajes` abortada a propósito | Pantalla “No se pudieron cargar tus viajes · No se pudo conectar con el servidor…” + botón **Reintentar**; al reintentar se recuperan las 5 tarjetas y las métricas |
| Token inválido en `/chofer` | `HTTP 401` → sesión eliminada (`localStorage = null`) y redirección a `/login` |

---

## 6. Seguridad y permisos (verificado)

| Prueba | Resultado |
|---|---|
| `chofer1` intenta `PATCH` sobre el viaje `5` (de `chofer2`) | **HTTP 400** “El viaje no está asignado a tu usuario” |
| El panel de `chofer1` lista viajes | Solo los 5 propios; el de `chofer2` lista solo los 3 suyos (aislamiento por UI y por API) |
| `admin` (sin `ROLE_CHOFER`) entra a `/chofer` | Redirigido a `/admin` (`RutaProtegida`) |
| Chofer sin sesión en `/chofer` | Redirigido a `/login` |
| `GET /api/v1/reportes/**` como chofer | **HTTP 403** → función omitida del menú |
| `GET /api/v1/usuarios/{id}` como chofer | **HTTP 403** → perfil solo lectura |
| JWT | Todas las llamadas del panel viajan con el token de sesión; ninguna llave o dato sensible en el código |

**Observación de backend (no modificada, fuera del alcance acordado):** `POST /api/v1/boletos/escanear`, `GET /api/v1/boletos/viaje/{id}` y `GET /api/v1/boletos/{id}` solo exigen `hasAnyRole('ADMIN','CHOFER')` y **no validan que el viaje/boleto pertenezca al chofer autenticado** (`BoletoController`). El panel no expone esa fuga: solo consulta y escanea sobre viajes obtenidos de `mis-viajes`. Se deja constancia para que el backend lo evalúe.

---

## 7. Responsive (medido en el DOM, sin desbordes)

| Vista | Ancho | Columnas de tarjetas | Sidebar | Menú hamburguesa | Desborde horizontal | Alturas de métricas |
|---|---|---|---|---|---|---|
| Escritorio | 1440 px | 3 | visible | oculto | 0 px | 81/81/81/81/81 |
| Tablet | 820 px | 2 | oculta | visible | 0 px | 81/81/81/81/81 |
| Móvil | 390 px | 1 | oculta | visible | 0 px | 68/68/68/68/68 |

- Menú móvil: se abre, se cierra con clic en el overlay y con `Escape`; el sidebar pasa de `-264px` a `0px`.
- Modal de asientos a 390 px: `40` asientos, ancho `390 = ancho de ventana`, sin desborde.
- Tablas en móvil: `362 px` visibles con scroll **interno** (sin romper la página); 6 filas en Pasajeros y en QR.
- Capturas: `reports/capturas-panel-chofer/` (16 imágenes: escritorio, tablet, móvil, menú móvil, asientos, pasajeros, QR, QR validado, perfil y regresión de otros módulos).

---

## 8. Consola y red

- Recorrido completo del panel (login → viajes → asientos → pasajeros → QR válido e inválido → perfil → logout): **0 errores de consola y 0 peticiones fallidas**.
- Los únicos errores capturados en toda la verificación fueron **deliberados**: los `400` de los QR inválidos/re-escanados, el `404` de una guía de encomienda inexistente, el `401` del token inválido y la ruta abortada para probar el reintento.

---

## 9. Calidad técnica

```
frontend/Web-Intiwatana : npm run lint   -> 0 errores
frontend/Web-Intiwatana : npm run build  -> SUCCESS (1756 módulos, 1.29 s)
                          dist/index.html                   0.47 kB
                          dist/assets/index-*.css          62.14 kB (gzip 12.34)
                          dist/assets/index-*.js          504.35 kB (gzip 132.68)
                          warning: chunk > 500 kB (preexistente del proyecto, no introducido)

backend/bus-system : mvn clean verify -> BUILD SUCCESS
                     Tests run: 38, Failures: 0, Errors: 0, Skipped: 0
```

---

## 10. Regresión de los demás módulos

**Público**
- `/` → hero “Viaja con Intiwatana”, 8 tarjetas de destino, 4 accesos rápidos. La tipografía del landing sigue siendo la del proyecto (Playfair Display 51.2 px, blanco): `chofer.css` no filtra estilos.
- Tracking público de encomiendas → `GET /api/v1/encomiendas/tracking/GUI-20260930-000022` **200** con datos reales (`Paquete auditoria · 1.2 kg · S/ 10 · Accomarca → Huamanga · Juana Perez`) y guía inexistente → **404** con el mensaje esperado.
- `/registrar` → formulario con `nombres, apellidos, dniRuc, telefono, email` + `Continuar`.
- `/pasajes` → tablero de compra de pasajes operativo.

**Admin** (usuario `admin`, clave `<REDACTADO>`) — las 10 secciones cargan con `HTTP 200`:
`Panel · Sucursales · Rutas · Autobuses · Viajes · Clientes · Pagos · Encomiendas · Reportes · Personal`
APIs observadas: `/clientes`, `/encomiendas`, `/buses`, `/rutas`, `/viajes`, `/sucursales`, `/pagos`, `/usuarios`, `/roles` y las 7 de `/reportes/**` (solo para admin).

**Chofer**: cubierto en las secciones 5 y 6.

**Encomiendas**: sección del panel admin (200) + tracking público (200/404).

---

## 11. Efectos secundarios de la verificación sobre los datos

Todo cambio proviene de operaciones reales del negocio ejecutadas desde la interfaz; **no se modificó esquema ni datos desde SQL**.

- `viajes.id = 31` (chofer1): `PROGRAMADO → EN_CURSO → FINALIZADO`.
- `viajes.id = 8` (chofer2): `PROGRAMADO → EN_CURSO → FINALIZADO` (para capturar el `PATCH` en el panel Network).
- Boletos que quedaron `USADO` por validaciones reales: `BOL-20260930-000027`, `BOL-20261002-000032`, `-000033`, `-000034`, `-000035`.
- Estado final de la base: viajes `10 FINALIZADO / 1 PROGRAMADO` (sin viajes abandonados en `EN_CURSO`); boletos `8 ACTIVO / 2 CANCELADO / 7 USADO`.

---

## 12. Veredicto

- Las 4 funciones del panel están conectadas a endpoints reales, se ven en pantalla y **cambian datos en PostgreSQL**.
- Sin funciones simuladas, sin campos muertos y sin pantallas que simulen datos.
- Seguridad y aislamiento por chofer verificados en API, sesión y UI.
- Consola limpia, responsive sin desbordes, lint y build correctos, backend con 38/38 tests.
- Regresión de Público, Admin y Encomiendas correcta.

**PANEL DEL CHOFER — CERRADO**

---

# PULIDO VISUAL FINAL

**Fecha:** 2026-10-02
**Alcance:** exclusivamente presentación (UI/UX) del módulo del chofer.
**Funcionalidad, rutas, endpoints, datos y seguridad:** intactos (A01–A11 y F01–F17 siguen vigentes como evidencia histórica).

## 1. Estado visual anterior

- `chofer.css` era funcional pero plano: tokens repetidos, tarjetas con sombra uniforme, jerarquía débil entre encabezado, métricas y viajes.
- El “viaje en curso” solo se distinguía por un borde verde, sin línea de ruta ni rótulo.
- No existía sección de **acciones rápidas** ni un **estado vacío** explícito para “sin viaje activo”.
- Las tablas de Pasajeros y QR usaban scroll horizontal en móvil (legibles, pero no óptimas).
- La vista QR mostraba un único rótulo “Validación rechazada” para cualquier error.

## 2. Problemas visuales corregidos

| # | Antes | Ahora |
|---|---|---|
| 1 | Métricas con iconos pequeños y sin acento | Tarjetas de altura uniforme (108 px), número 30 px, barra de acento lateral por estado, rótulo + texto secundario |
| 2 | Sin acciones rápidas | Sección **Acciones rápidas** con 4 accesos reales (Actualizar, Ver asientos, Pasajeros, Validar QR) |
| 3 | Sin estado para “sin viaje activo” | Estado vacío elegante: “No tienes un viaje activo en este momento.” + botón **Ver mis viajes** |
| 4 | Tarjeta de viaje sin composición de ruta | Franja de ruta origen → bus → destino con puntos y tramo |
| 5 | Sidebar activo solo con fondo | Opción activa con fondo, borde e **indicador lateral** + `aria-current` de `NavLink` |
| 6 | Tablas incómodas en móvil | En ≤640 px cada fila se convierte en tarjeta con etiquetas (`data-label`) |
| 7 | QR con un único rótulo de error | Diferenciación visual: utilizado / cancelado / viaje no asignado / sin conexión / código no reconocido / rechazada |
| 8 | Encabezado y tarjetas sin profundidad | Encabezado translúcido sticky, sombras discretas, radios 16 px, foco visible global |

## 3. Archivos frontend modificados

```
src/chofer.css                                  (reescrito: sistema visual premium + responsive)
src/components/chofer/ChoferStats.jsx           (acento por métrica + texto secundario)
src/components/chofer/ViajeCard.jsx             (franja de ruta decorativa)
src/components/chofer/MisViajesView.jsx         (acciones rápidas + estado vacío + ids de sección)
src/components/chofer/PasajerosView.jsx         (data-label para tarjetas móviles)
src/components/chofer/QrView.jsx                (clasificación visual de resultado + data-label)
```

No se modificó backend, PostgreSQL, `apiChofer.js`, `AuthContext`, `App.jsx`, rutas ni contratos.
No se crearon componentes, rutas ni archivos paralelos.

## 4. Mejoras de jerarquía

Orden del dashboard: **encabezado → métricas → viaje en curso → acciones rápidas → próximos viajes → historial**.
Títulos de sección con barra de acento, subtítulos aclaratorios y “Viaje en curso” con rótulo propio.

## 5. Mejoras de espaciado

- Escala coherente: secciones 30 px, tarjetas 18–20 px, grids de 16–18 px.
- Contenido con `max-width` 1320 px (1440 px en pantallas ≥1600) y centrado, sin pegarse al sidebar.
- Métricas y tarjetas con separación uniforme y sin alturas dispares.

## 6. Mejoras responsive

Verificado sin desbordamiento horizontal (`scrollWidth − clientWidth = 0`):

| Ancho | Escritorio | Grid de métricas | Sidebar | Contenido |
|---|---|---|---|---|
| 1920 · 1440 · 1366 | 3 columnas | auto-fit | visible 264 px | máx. 1320–1440 px |
| 1024 · 768 | 2 columnas | 2 columnas | contraíble (burger) | una columna |
| 430 · 390 · 375 | 1 columna | 2 columnas compactas | menú desplegable | tarjetas a ancho completo |

- Modal de asientos a 390 px: 40 asientos, sin overflow.
- Pasajeros a 390 px: 6 tarjetas, overflow 0.
- Menú móvil: abre con burger, cierra con `Escape` y con el overlay.

## 7. Mejoras accesibles

- `:focus-visible` global con anillo verde.
- `aria-current` en la opción activa (`NavLink`), `aria-label` en el menú y en controles.
- `role="status"`/`aria-live` conservados; `role="alert"` en el rechazo de QR.
- Estados de viaje con texto + icono (no solo color); `prefers-reduced-motion` respetado.
- Tablas móviles con etiqueta textual por celda (`data-label`).

## 8. Resultado de lint

```
frontend/Web-Intiwatana : npm run lint -> 0 errores
```

## 9. Resultado de build

```
frontend/Web-Intiwatana : npm run build -> SUCCESS
  dist/assets/index-*.css   72.25 kB (gzip 14.45)
  dist/assets/index-*.js   521.54 kB (gzip 136.59)
  warning: chunk > 500 kB (preexistente, no introducido)
```

## 10. Capturas actualizadas

`reports/capturas-panel-chofer-pulido/` — **16 imágenes**:
`1920/1440/1366/1024/768/430/390/375-dashboard`, `390-menu-movil`,
`1440/390-asientos`, `1440/390-pasajeros`, `1440/390-qr`, `1440-perfil`.
No se repitieron capturas innecesarias y no se usó ningún mockup como evidencia.

## 11. Comprobación no destructiva

- Solo navegación, lectura de datos, cambio de resolución, teclado/foco y apertura del modal de asientos (GET).
- **No** se ejecutaron transiciones de estado, **no** se validaron QR, **no** se crearon boletos ni datos, **no** se tocó PostgreSQL.
- Métricas de navegador: título “Panel del Chofer”, 5 métricas, 4 accesos rápidos, 5 tarjetas, 5 franjas de ruta, estado vacío de viaje activo, sidebar 264 px, 40 asientos, 4 leyendas, 6 filas de Pasajeros y 6 de QR.
- **0 errores de consola, 0 peticiones fallidas.**
- Alturas de métricas uniformes (108 px), `--cd-stat-acento` aplicado por tarjeta, radio de tarjeta 16 px, franja de ruta visible.

## 12. Limitaciones visuales

- La captura de “Viaje actual” muestra el **estado vacío** porque el chofer `chofer1` no tiene viajes `EN_CURSO`; crear uno implicaría cambiar estados de viaje, prohibido en esta tarea. El diseño del viaje en curso se validó por DOM/CSS (`is-actual`, franja de ruta, badge).
- No se inspeccionaron las imágenes por parte de la herramienta (sin soporte de entrada de imagen); la verificación visual se sustentó en métricas computadas del DOM, consola y red.
- `calcularCosto`, lógica de negocio y cualquier defecto de backend quedan fuera de alcance (solo UI).

## 13. Veredicto

- Funcionalidad existente conservada; backend y PostgreSQL sin tocar.
- Sin boletos consumidos, sin estados de viaje cambiados, sin datos QA creados.
- Dashboard más legible, viaje actual con jerarquía clara, métricas equilibradas, espacios reducidos, botones y tablas legibles, QR visualmente claro, perfil equilibrado.
- Escritorio, tablet y móvil verificados sin desbordes; lint y build correctos.

**PANEL DEL CHOFER — PULIDO VISUAL CERRADO Y VERIFICADO**
