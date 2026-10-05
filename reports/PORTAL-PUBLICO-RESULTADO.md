# PORTAL PÚBLICO, RESERVA DE PASAJES Y SOLICITUD WEB DE ENCOMIENDA
## Corrección final comercial, visual y no destructiva

**Fecha:** 2026-10-02
**Alcance:** Portal público (landing), buscador público, reserva de pasajes con conservación de contexto, Carrito exclusivo de pasajes y solicitud web de encomienda (pre-registro).
**Restricción cumplida:** cambio NO destructivo. No se tocó backend, BD, entidades, repositorios, servicios, controladores, contratos API, JWT, Spring Security, BCrypt, Flyway, pagos, boletos, QR, disponibilidad de asientos, estados de viajes ni paneles (Admin/Cajero/Chofer/Cliente/Encomiendas internas/Tracking).

---

## 1. Objetivo

Pulir el Portal Público oficial, permitir la reserva de pasajes **sin login prematuro** (autenticación solo al continuar) conservando y revalidando el contexto, y **separar por completo** la solicitud web preliminar de encomienda del carrito de compra (que debe ser exclusivo de pasajes).

## 2. Referencia visual

El enunciado menciona una imagen de referencia que **no fue adjuntada** al proyecto. El trabajo se basó en la **especificación textual** (paleta, escalas, secciones A–H, componentes). No se realizó cotejo pixel a pixel; se documenta esta limitación.

## 3. Estado visual inicial (antes)

- Navbar sin "Encomiendas".
- Buscador del hero sin campo **Pasajeros**.
- Barra de indicadores con cifras no verificables: `10+ Destinos`, `20+ Años`, `25K+ Pasajeros/año`, `24/7 Atención`.
- Sección "Nosotros" con la afirmación "más de 20 años de experiencia".
- Botón de tarjeta de destino "Reservar pasaje".
- Carrito mezclaba pasajes y una solicitud preliminar de encomienda con tarifa referencial.
- Formulario de encomienda mostraba "tarifa estimada/referencial" con precios inventados.

## 4. Comparación textual (antes → después)

| Elemento | Antes | Después |
|---|---|---|
| Nav | Inicio, Destinos, Nosotros, Contacto | Inicio, Destinos, Nosotros, **Encomiendas**, Contacto |
| Hero subtítulo | "Conectando los Andes con seguridad y puntualidad" | "Conectamos destinos con seguridad, comodidad y atención cercana." |
| Buscador | Origen, Destino, Fecha | Origen, Destino, Fecha, **Pasajeros** |
| Indicadores | 10+/20+/25K+/24/7 | **Beneficios cualitativos** (4 tarjetas) |
| Nosotros | "más de 20 años…" | Sección **Por qué viajar** (oscura, cualitativa) + **Nosotros** (clara, datos reales) |
| Tarjeta destino | "Reservar pasaje" | "Ver horarios" |
| Carrito | Pasajes + pre-registro encomienda | **Solo pasajes** |
| Encomienda | Tarifa referencial inventada | Solo pre-registro; tarifa pendiente en agencia |

## 5. Recursos locales — verificación por metadatos y rutas

Inventario real del proyecto (dimensiones leídas con `System.Drawing`; existencia verificada con `GET` HTTP 200):

| Recurso | Archivo local | Dimensiones | Estado |
|---|---|---|---|
| Logo | `public/assets/logo.png` (usado por `LogoSVG.jsx`) | 604×413 | **Existe** |
| Vilcashuamán | `public/assets/img/rutas/vilcas.jpg` (+ `src/assets/Vilcas.jpeg`) | 544×360 | **Existe** |
| Paisaje regional | `public/assets/img/rutas/andabamba.jpeg` (fondo del hero) | 1400×1050 | **Existe** |
| Otras rutas | accomarca 1242×719, chiribamba 1220×1206, huarcas 1404×768, manallasacc 1407×768, pongococha 1490×704, vischongo 1488×717 | — | Existen |
| **Bus institucional** | — | — | **NO EXISTE** archivo local autorizado |
| **Laguna de Intiwatana** | — | — | **NO EXISTE** archivo local autorizado |

Notas:
- El hero usa **únicamente** `andabamba.jpeg` (recurso local) con degradado de marca y un SVG decorativo de montañas. **No se declara coincidencia con la referencia visual** porque no es posible visualizar los recursos.
- `public/assets/hero.png` (343×361) existe pero **no está referenciado** en el código y su contenido **no es verificable** por este entorno (no se pueden interpretar imágenes); por eso **no se usa ni se declara como bus**.
- **No se inventó ni descargó ningún recurso nuevo.**

### RECURSOS VISUALES PENDIENTES DE ENTREGA (archivos exactos)

Rutas solicitadas para el hero (verificadas con `Test-Path` en disco → **no existen**):

| Recurso | Ruta exacta solicitada | Estado |
|---|---|---|
| Bus institucional | `frontend/Web-Intiwatana/public/assets/img/bus/bus-institucional.webp` | **PENDIENTE DE ENTREGA** (no existe) |
| Laguna de Intiwatana | `frontend/Web-Intiwatana/public/assets/img/rutas/laguna-intiwatana.webp` | **PENDIENTE DE ENTREGA** (no existe) |

Aceptables como respaldo (misma decisión de no inventar): `bus-institucional.(jpg|png|webp)` y `laguna-intiwatana.(jpg|jpeg|png|webp)`.

Acción tomada mientras no se entreguen: **no se modificó el hero**; se mantiene el recurso local autorizado existente (`andabamba.jpeg`). No se inventaron ni descargaron archivos.

Mientras estos dos recursos no existan como archivos locales autorizados, **no puede declararse el cierre** (V02 no aprobado).

## 6. Archivos modificados

| Archivo | Cambio |
|---|---|
| `frontend/Web-Intiwatana/src/App.jsx` | Nav "Encomiendas"; campo Pasajeros + envío de `?pasajeros=`; subtítulo hero; barra de beneficios; sección "Por qué viajar" + "Nosotros"; botón "Ver horarios". |
| `frontend/Web-Intiwatana/src/App.css` | Estilos scoped `.landing-root` para beneficios, "Por qué viajar" (oscuro), "Nosotros" (claro), campo Pasajeros, offset `#porque`. |
| `frontend/Web-Intiwatana/src/components/PasajesPage.jsx` | Carrito solo pasajes; solicitud web de encomienda independiente (POST `/solicitar`, `SOLICITUD_PENDIENTE`, `CONTRA_ENTREGA`, sin precio); eliminación de tarifas referenciales inventadas; pasajeros; intención/returnTo; revalidación; cambiarTab. |
| `frontend/Web-Intiwatana/src/components/ClienteDashboard.jsx` | Soporte `?seccion=encomiendas` para "Ver mis solicitudes". |

**No modificados:** `Login.jsx` (ya tenía returnTo seguro de la ronda previa), `AuthContext.jsx`, todo el backend, BD, y paneles Admin/Cajero/Chofer.

## 7. Hero

- Título "Viaja con **Intiwatana**"; subtítulo según spec.
- Fondo con paisaje local autorizado + degradado/servicio SVG decorativo.
- Pestañas Pasajes / Encomiendas / Seguimiento con `role="tablist"` y `aria-selected`.
- Buscador apilable; campo Pasajeros 1–6.

## 8. Indicadores

Se **retiraron** `10+`, `20+`, `25K+`, `24/7` y "más de 20 años". En su lugar, 4 beneficios cualitativos reales: Compra organizada · Encomiendas con seguimiento · Atención en agencia · Información clara. **Verificado por DOM: 0 coincidencias** de las cifras prohibidas.

## 9. Reservar pasaje

- CTA superior "Reservar pasaje" navega a `/pasajes` sin forzar login (sin sesión) y a `/cliente` para `ROLE_CLIENTE`.
- No se crea ninguna reserva al pulsar el CTA.

## 10. Búsqueda pública

- `GET /api/v1/viajes/disponibles` con `origenId/destinoId/desde/hasta`, ejecutada sin token.
- Estados: buscando / sin resultados / error.
- **Limitación de datos:** la data semilla no tiene viajes futuros en estado `PROGRAMADO` (11 viajes, casi todos `FINALIZADO`); la consulta devuelve 0 resultados reales. Es una condición de datos, no un defecto de código.

## 11. Login en el momento correcto

- El login se solicita **solo** al pulsar "Pagar ahora" / "Agregar al carrito" / "Proceder al pago" sin sesión, no al buscar ni al seleccionar viaje.
- `irALogin()` guarda la intención en `sessionStorage["iwt_intencion"]` y navega a `/login?returnTo=<ruta+query>` **sin JWT ni datos sensibles**.

## 12. Conservación de contexto

`iwt_intencion = { origenId, destinoId, fecha, pasajeros, viajeId }`. Tras autenticarse, un efecto de restauración repuebla el estado y relanza la búsqueda.

## 13. Retorno y revalidación

Al volver con sesión, si el viaje sigue disponible se reabre (`seleccionarViaje` recarga asientos desde el backend); si ya no está, se informa que el viaje dejó de estar disponible. **Nunca se asume disponibilidad previa.**

## 14. Carrito (solo pasajes)

- Ítems exclusivamente de tipo pasaje (icono Ticket, precio `S/ …`).
- Vacío: **"Agrega un pasaje para continuar con tu compra."**
- Eliminados: `hayEncomiendas`, tarifa referencial y cualquier pre-registro de encomienda.
- **Verificado por DOM:** `encomiendaInCart = false`.

## 15. Solicitud web de encomienda

- Copy informativo: "Completa los datos iniciales del envío. El paquete debe entregarse en una agencia INTIWATANA, donde el personal autorizado confirmará el peso, la tarifa y la recepción."
- `POST /api/v1/encomiendas/solicitar` con `metodoPago: "CONTRA_ENTREGA"`; **no envía precio**.
- Confirmación independiente: "Solicitud registrada correctamente." / "Estado: Pendiente de recepción en agencia." / "Tarifa: Pendiente de confirmación." / "Presenta el paquete en una agencia para completar el registro." con acciones **Ver mis solicitudes / Registrar otra solicitud / Volver al portal**. Sin "Pagar/Comprar/Finalizar".
- **Eliminadas** todas las tarifas referenciales inventadas (bloque de tarifa del formulario y panel lateral "Tarifas referenciales"). **Verificado por DOM: 0 coincidencias de `S/ ~`.**

## 16. Registro oficial de encomienda

Intacto. Permanece en manos de OPERADOR_ENCOMIENDAS/CAJERO/VENDEDOR/ADMIN. No se alteró el flujo interno.

## 17. Tracking

`/encomiendas/seguimiento` sin login, sin costo, sin método de pago ni información interna. (Verificado en ronda previa.)

## 18. Destinos

Tarjetas con imagen local, región, duración, distancia y horarios existentes; acción "Ver horarios". Data de `src/data/destinos.jsx` (existente, no inventada).

## 19. Beneficios

Franja clara con 4 beneficios cualitativos (sin cifras). Solo `.landing-root`.

## 20. Nosotros

Sección clara con descripción factual (empresa ayacuchana, terminal en Av. Cusco 362, servicios del portal) **sin años, premios ni cifras no verificables**.

## 21. Contacto y footer

Footer `id="contacto"`; el enlace "Nosotros" apunta a la nueva sección `#nosotros`. `scroll-margin-top` para navbar fija.

## 22. Responsive

Sin overflow horizontal en 1366 / 1024 / 390 (landing y encomiendas). Buscador apilable, menú hamburguesa funcional.

## 23. Accesibilidad

`role="tablist"`/`role="tab"`, `aria-selected`, `aria-label` en campos e iconos, `role="alert"` en errores, `aria-busy` en estados de carga, foco visible, áreas táctiles ≥44px, `prefers-reduced-motion`.

## 24. Verificación no destructiva — V01–V26

| # | Verificación | Resultado |
|---|---|---|
| V01 | Hero con recurso local autorizado (`andabamba.jpeg`) | OK |
| V02 | Bus institucional + Laguna de Intiwatana + paisaje regional | **NO APROBADO**: faltan archivos locales `bus-institucional.*` y `laguna-intiwatana.*` (paisaje sí: `andabamba.jpeg`) |
| V03 | Sin cifras inventadas (10+/20+/25K+/24/7/años) | OK (DOM 0 coincidencias) |
| V04 | CTA superior "Reservar pasaje" sin login prematuro | OK |
| V05 | Destino preseleccionado desde el portal | **APROBADO** (navegador, BD QA aislada): tarjeta "Vilcas Huaman" → `/pasajes?destino=Vilcas%20Huaman`; Origen="Huamanga", Destino="Vilcas Huaman" |
| V06 | Búsqueda pública ejecuta GET sin token | OK (0 errores) |
| V07 | Resultados reales de viajes | **APROBADO** (navegador, BD QA aislada): 2 resultados reales, `GET /api/v1/viajes/disponibles` sin token |
| V08 | Selección de viaje / reapertura | **APROBADO** (navegador, BD QA aislada): selección de asiento anónima → login al "Pagar ahora" → retorno al **mismo viaje** (id 1→1), asientos 15, sin compra |
| V09 | Login solo al continuar (Pagar/Agregar) | OK (código + gate) |
| V10 | Conservación de contexto (intención) | OK (código) |
| V11 | Retorno + revalidación de disponibilidad | OK (código) |
| V12 | Asientos no confirman compra hasta pagar | OK (sin mutación) |
| V13 | Encomienda **no** entra al carrito | OK (DOM) |
| V14 | La solicitud web no crea pago | OK |
| V15 | Estado `SOLICITUD_PENDIENTE` | Verificado en contrato backend (no ejecutado para no mutar) |
| V16 | Registro oficial de encomienda intacto | OK (no modificado) |
| V17 | Tracking público | OK (ronda previa) |
| V18 | Escritorio/tablet/móvil sin overflow | OK |
| V19 | Teclado/foco/ARIA | OK (código) |
| V20 | Consola sin errores | OK (0) |
| V21 | Sin requests duplicadas/fallidas | OK (0 fallidas) |
| V22 | Panel Admin sin modificar | OK |
| V23 | Panel Cajero sin modificar | OK |
| V24 | Panel Chofer sin modificar | OK |
| V25 | Conteos PostgreSQL antes/después | Idénticos |
| V26 | `npm run lint` + `npm run build` | OK |

## 24-A. Validación aislada V05/V07/V08 (base de pruebas independiente)

Para ejercitar V05/V07/V08 sin tocar `transporte_bd` ni producción se levantó una **instancia QA aislada**:

- **Base de datos:** `intiwatana_portal_qa` (independiente; `transporte_bd` intacta).
- **Perfil:** `test` (`application-test.yml`), puerto `8090`, Flyway construye el esquema y `DataInitializer` siembra roles/sucursales/usuarios base.
- **Fixture marcado como QA** (creado vía API oficial, nunca migrado a producción):
  - Ruta `QA-PORTAL-R1` (origen real "Huamanga" → destino real "Vilcas Huaman", ids 7→8).
  - Bus `QA-0001` (`QA-FIXTURE`, 15 asientos).
  - Viaje futuro `PROGRAMADO` (`observaciones=QA-PORTAL-FIXTURE`).
  - Cliente `qa_cliente_portal` / `QA Portal`.
- **Proxy de prueba:** todas las llamadas `/api/**` de la página se redirigen (intercepción de red del navegador) al backend QA `:8090`. El frontend en `:5173` **no** se modificó.

Evidencia (salida del runner headless):

| Verificación | Resultado |
|---|---|
| V05 tarjeta destino | `cardName="Vilcas Huaman"`; Origen="Huamanga"; Destino="Vilcas Huaman"; `destinoPreseleccionadoOk=true` |
| V07 resultados | `resultados=2`; `peticionDisponibles=true` (sin token) |
| V07 fecha | fecha enviada `2026-10-04` = valor del campo (`fechaCoincide=true`) |
| V08 login en momento correcto | modal de asientos visible antes del login; "Pagar ahora" → `/login?returnTo=...`; `sinCompraAntesDeLogin=true` |
| Retorno (1) Origen | conservado ("Huamanga") |
| Retorno (2) Destino | conservado ("Vilcas Huaman") |
| Retorno (3) Fecha | conservada ("2026-10-04") |
| Retorno (4) Pasajeros | `pasajeros=2`, modal "hasta 2 asiento(s)" |
| Retorno (5) ID viaje | antes `1` = después `1` |
| Retorno (6) mismo viaje | `mismoViaje=true`, modal reabierto |
| Retorno (7) reconsulta backend | `GET /viajes/disponibles` repetido tras login |
| Retorno (8) revalida disponibilidad | `GET /viajes/{id}/asientos` repetido tras login |
| Retorno (9) asientos visibles | 15 asientos mostrados |
| Retorno (10–14) sin venta/pago/boleto/reserva/estado | `POST /boletos/comprar=0`, `/pagos=0`, `/encomiendas=0`; conteos QA sin cambios comerciales |
| Retorno (15) producción intacta | backend QA en `:8090`; `transporte_bd` no usada por la prueba |
| Consola / red | 0 errores de consola, 0 requests fallidas |
| `npm run lint` / `npm run build` | 0 errores / OK (533 kB js, 79.85 kB css) |

Conteos en la BD QA aislada (antes → después): `boletos 0→0`, `pagos 0→0`, `encomiendas 0→0`, `vendidos 0→0`; solo aumentó el fixture QA (`viajes`, `asientos`). **Sin mutación comercial.**

## 25. Conteos PostgreSQL antes / después (desarrollo `transporte_bd`)

Conteos tomados en la base de **desarrollo** (la prueba QA aislada usa otra base, ver §24-A):


| Tabla | Antes | Después |
|---|---|---|
| viajes | 11 | 11 |
| boletos | 24 | 24 |
| encomiendas | 18 | 18 |
| pagos | 43 | 43 |
| usuarios | 12 | 12 |
| clientes | 9 | 9 |

Sin operaciones mutantes (no solicitudes, ventas, pagos, boletos ni cambios de estado).

## 26. Lint / Build

- `npm run lint` → 0 errores.
- `npm run build` → OK (533.29 kB js / 80.03 kB css). Aviso informativo de chunk >500 kB (no error).

## 27. Capturas

Capturas reales del sistema en ejecución (`:5173`), guardadas en `reports/capturas/` (ver inventario y detalle en §31, punto 13). No son mockups: las genera el navegador headless sobre la app real. Este entorno no permite interpretar PNG; la verificación de layout se sustenta en métricas DOM (`getBoundingClientRect`, `scrollWidth`), estilos computados y el reporte de consola/red.

## 28. Módulos no modificados

Backend completo, PostgreSQL, seguridad/JWT, pagos, boletos, QR, disponibilidad de asientos, estados de viajes, flyway, paneles Admin/Cajero/Chofer, flujo interno oficial de encomiendas y tracking.

## 29. Limitaciones

- La imagen de referencia no se adjuntó → sin cotejo pixel a pixel.
- V05/V07/V08 **ya no son limitación de datos**: se validaron en navegador con la BD QA aislada `intiwatana_portal_qa` (ver §24-A), sin tocar `transporte_bd`.
- V15 (POST real de solicitud) no se ejecutó para no mutar la BD; el contrato se verificó por lectura del backend.

## 30. Veredicto

# PORTAL PÚBLICO — CERRADO FUNCIONAL Y VISUALMENTE
# RECURSOS FOTOGRÁFICOS OFICIALES — SUSTITUCIÓN EDITORIAL PENDIENTE NO BLOQUEANTE

### Justificación

Los dos archivos oficiales solicitados **no fueron entregados** (verificados ausentes en §31 punto 1). Conforme a la decisión de cierre (caso B), **el cierre funcional no se bloquea por un activo fotográfico no entregado**: el hero quedó profesional usando el **mejor paisaje local autorizado ya disponible en el proyecto** (`vilcas.jpg`), sin falsear su identidad (no se declara como Laguna de Intiwatana) y sin bus falso.

- Las **funciones están cerradas**: navegación pública, buscador, reserva, carrito de pasajes, solicitud de encomienda, seguimiento y registro interno.
- La **reserva** está cerrada (V05/V07/V08 aprobadas, §24-A).
- El **carrito** está cerrado (exclusivo de pasajes).
- La **solicitud de encomienda** está cerrada (independiente, sin carrito ni pago, `SOLICITUD_PENDIENTE`).
- El **responsive** está cerrado (H08–H15: 0 overflow en 8 viewports).
- La **accesibilidad** está cerrada (H16–H18; foco visible, menú móvil, textos alternativos).
- Solo queda una **sustitución editorial de fotografía oficial**, sin cambios funcionales.

**No se declara `PORTAL PÚBLICO — NO CERRADO`** porque: el hero no presenta errores, no hay overflow, el buscador no queda oculto, `lint` no falla, `build` no falla, no hay errores de consola, no se modificó PostgreSQL ni ningún módulo cerrado, y el recurso visual **no se falsea como oficial**.

## 31. CIERRE VISUAL DEL HERO

### 1. Estado de los recursos visuales solicitados

| Recurso solicitado | Ruta esperada | Estado |
|---|---|---|
| Bus institucional | `frontend/Web-Intiwatana/public/assets/img/bus/bus-institucional.webp` | **NO ENTREGADO** (no existe) |
| Laguna de Intiwatana | `frontend/Web-Intiwatana/public/assets/img/rutas/laguna-intiwatana.webp` | **NO ENTREGADO** (no existe) |

### 2. Archivos realmente disponibles (verificados con `Test-Path`)

| Archivo | Tamaño | Uso |
|---|---|---|
| `public/assets/img/rutas/vilcas.jpg` | 65.9 KB | **elegido** (prioridad 1) como fondo del hero |
| `src/assets/Vilcas.jpeg` | 294 KB | alternativa (no usada) |
| `public/assets/img/rutas/andabamba.jpeg` | 626.8 KB | ex-fondo del hero (no usado) |

### 3. Recurso local utilizado

`/assets/img/rutas/vilcas.jpg` — validado en navegador: **HTTP 200**, `image/jpeg`, 67 508 bytes. `background-image` del hero contiene `url(.../assets/img/rutas/vilcas.jpg)`.

### 4. Confirmación de que no se falsificó la identidad del recurso

- El texto/atributo accesible describe el fondo como **"Paisaje andino de la región de Ayacucho"**.
- `heroHTMLHasLagunaClaim = false` (el hero no afirma ser la Laguna de Intiwatana).
- `heroImgCount = 0` → **no se muestra bus falso** ni fotografía simulada de la unidad.
- No se descargaron imágenes externas ni se usaron recursos temporales.

### 5. Composición aplicada

- Fondo `center/cover` (sin deformar) con **overlay verde pino oscuro** (`rgba(3,59,49,.92)` → `.74` → `rgba(2,40,33,.94)`).
- Título **"Viaja con INTIWATANA"** (Playfair Display 900, blanco, 64 px en escritorio).
- Subtítulo **"Conectamos destinos con seguridad, comodidad y atención cercana."** (`rgba(255,255,255,.9)`).
- Pestañas **Pasajes / Encomiendas / Seguimiento** (alto 44 px, activa en amarillo).
- Buscador en **tarjeta blanca** con Origen, Destino, Fecha y Pasajeros; botón principal **"Buscar viajes"**.
- CTA superior **"Reservar pasaje"** (alto 44 px).
- Sin estadísticas, testimonios, teléfono, correo o dirección inventados.

### 6. Escritorio

| Viewport | Overflow X | Altura hero | Título px | Buscador en viewport | CTA |
|---|---|---|---|---|---|
| 1920×1080 | 0 | 720 | 64 | sí | visible 44 px |
| 1440×900 | 0 | 684 | 64 | sí | visible 44 px |
| 1366×768 | 0 | 584 | 64 | sí | visible 44 px |

### 7. Tablet

| Viewport | Overflow X | Altura hero | Título px | Buscador en viewport |
|---|---|---|---|---|
| 1024×768 | 0 | 584 | 51.2 | sí |
| 768×1024 | 0 | 400 | 38.4 | sí |

### 8. Móvil

| Viewport | Overflow X | Altura hero | Título px | Buscador (alto) | Menú hamburguesa |
|---|---|---|---|---|---|
| 430×932 | 0 | 647 | 36.8 | 1 columna (297) | abre/cierra ok |
| 390×844 | 0 | 647 | 36.8 | 1 columna (297) | abre/cierra ok |
| 375×812 | 0 | 647 | 36.8 | 1 columna (297) | — |

Menú móvil verificado: `#menu-principal` pasa de `display:none` (cerrado) a `display:flex` con clase `open` (abierto) y 5 enlaces; vuelve a ocultarse al cerrar.

### 9. Accesibilidad

- Fondo con `role="img"` + `aria-label="Paisaje andino de la región de Ayacucho"` (sin afirmar identidad oficial).
- SVG decorativo con `aria-hidden="true"` y `focusable="false"`.
- Pestañas con `role="tab"` / `aria-selected`; campos con `aria-label` (Origen, Destino, Fecha de ida, Pasajeros).
- Botones/controles con alto mínimo 44 px; foco visible `outline: 2px solid #1D63ED` en `a/button/input/select`.
- Recorrido por teclado: `INICIO → DESTINOS → NOSOTROS → ENCOMIENDAS → CONTACTO → RESERVAR PASAJE`, todos con foco visible.

### 10. Resultados H01–H27

| # | Verificación | Resultado |
|---|---|---|
| H01 | El hero usa un recurso local válido | **APROBADO** — `vilcas.jpg` HTTP 200, `image/jpeg`, referenciado en `background-image` |
| H02 | No se afirma que sea la Laguna si no está verificado | **APROBADO** — `aria-label="Paisaje andino…"`; `heroHTMLHasLagunaClaim=false` |
| H03 | No se muestra un bus falso | **APROBADO** — `heroImgCount=0`; sin imagen de bus |
| H04 | Título y subtítulo legibles | **APROBADO** — h1 64/51.2/38.4/36.8 px blanco; subtítulo 17.6 px `rgba(255,255,255,.9)` |
| H05 | Pestañas legibles | **APROBADO** — 3 pestañas 14 px, alto 44 px, activa amarilla |
| H06 | Buscador completo visible | **APROBADO** — visible e íntegro en viewport en todos los tamaños |
| H07 | CTA "Reservar pasaje" visible | **APROBADO** — texto "RESERVAR PASAJE", 44 px |
| H08 | Escritorio 1920×1080 sin overflow | **APROBADO** — overflowX 0 |
| H09 | Escritorio 1440×900 sin overflow | **APROBADO** — overflowX 0 |
| H10 | Escritorio 1366×768 sin overflow | **APROBADO** — overflowX 0 |
| H11 | Tablet 1024×768 sin overflow | **APROBADO** — overflowX 0 |
| H12 | Tablet 768×1024 sin overflow | **APROBADO** — overflowX 0 |
| H13 | Móvil 430×932 sin overflow | **APROBADO** — overflowX 0 |
| H14 | Móvil 390×844 sin overflow | **APROBADO** — overflowX 0 |
| H15 | Móvil 375×812 sin overflow | **APROBADO** — overflowX 0 |
| H16 | Menú móvil funciona | **APROBADO** — abre (`display:flex`, `.open`, 5 enlaces) y cierra (`display:none`) |
| H17 | Navegación por teclado funciona | **APROBADO** — 6 elementos enfocados en orden vía Tab |
| H18 | Foco visible | **APROBADO** — `outline solid 2px rgb(29,99,237)` |
| H19 | Cero errores rojos de consola | **APROBADO** — 0 errores/warnings |
| H20 | Cero solicitudes inesperadamente fallidas | **APROBADO** — 0 requests fallidas |
| H21 | PostgreSQL conserva los conteos anteriores | **APROBADO** — viajes 11, boletos 24, encomiendas 18, pagos 43, usuarios 12, clientes 9 |
| H22 | No se creó venta | **APROBADO** — sin POST de venta |
| H23 | No se creó pago | **APROBADO** — `pagos` 43→43 |
| H24 | No se creó boleto | **APROBADO** — `boletos` 24→24 |
| H25 | No se creó encomienda | **APROBADO** — `encomiendas` 18→18 |
| H26 | No se modificó estado de viaje | **APROBADO** — `viajes` 11→11; solo se hicieron lecturas (`GET`) |
| H27 | No se modificaron módulos cerrados | **APROBADO** — solo `App.jsx`/`App.css` (hero) y este informe |

### 11. Lint

`npm run lint` → **0 errores**.

### 12. Build

`npm run build` → **OK** (533.29 kB js / 80.03 kB css). Aviso informativo de chunk >500 kB (no bloqueante).

### 13. Capturas (reales, `reports/capturas/`)

`hero-desktop-1920.png`, `hero-desktop-1440.png`, `portal-desktop-1920.png` (página completa), `hero-tablet-1024.png`, `portal-tablet.png`, `hero-mobile-430.png`, `hero-mobile-390.png`, `portal-mobile-completo.png`, `buscador-pasajes.png`, `pestana-encomiendas.png`, `pestana-seguimiento.png`, `destinos.png`, `por-que-viajar.png`, `nosotros.png`, `footer.png` (15 capturas).

### 14. Conteos antes / después (`transporte_bd`)

| Tabla | Antes | Después |
|---|---|---|
| viajes | 11 | 11 |
| boletos | 24 | 24 |
| encomiendas | 18 | 18 |
| pagos | 43 | 43 |
| usuarios | 12 | 12 |
| clientes | 9 | 9 |

### 15. Confirmación de cero mutaciones

Durante la verificación del hero solo se ejecutaron `GET` (páginas, assets, endpoints públicos). No se crearon ventas, pagos, boletos ni encomiendas; no cambió el estado de ningún viaje ni la base `transporte_bd`. La BD QA aislada permanece en `viajes=2, boletos=0, pagos=0, encomiendas=0`.

### 16. Limitación editorial

**PENDIENTE EDITORIAL NO BLOQUEANTE:** sustituir el recurso visual provisional (`vilcas.jpg`) cuando la empresa entregue las fotografías oficiales del **bus institucional** y de la **Laguna de Intiwatana**. Esta sustitución **no se clasifica como falla funcional** y no requiere cambios de lógica.

### 17. Veredicto

**PORTAL PÚBLICO — CERRADO FUNCIONAL Y VISUALMENTE. RECURSOS FOTOGRÁFICOS OFICIALES — SUSTITUCIÓN EDITORIAL PENDIENTE NO BLOQUEANTE.**
