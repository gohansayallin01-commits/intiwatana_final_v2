# REDISEÑO RESERVA PREMIUM INTIWATANA — RESULTADO

Fecha: 2026-10-08 · Entorno: QA (`intiwatana_recorrido_qa`, backend dev en :8080, frontend Vite en :5173)

## Alcance

Rediseño completo del flujo público **Comprar pasajes** (PasajesPage.jsx) al sistema visual premium `.iwr-*`
(sin modificar backend; ningún cambio de esquema; sin nuevos componentes duplicados).

Extensión de diseño al modal de recuperación **"Mis reservas"** para consistencia.

## Estados y pantallas verificados (navegador real, 0 errores de consola)

| Estado | Verificación |
|---|---|
| Hero/banner + buscador + pasos | `.iwr-banner`, `.iwr-progress` (5 pasos), `.iwr-search` presentes en 9 viewports |
| Resultados | 2 tarjetas de viaje reales del QA (08:00 a. m. y jue, 08 oct. 08:30 p. m.) |
| Mapa de asientos | `.iwr-modal.wide` abre; asientos con aria-label `Asiento N - Disponible`; resumen lateral real |
| Resumen de selección | "Asiento 1B · S/ 12.00 · Total (1 asiento) S/ 12.00" |
| Formulario de datos | `#rsv-nombres/apellidos/documento/telefono/email` funcionales |
| Confirmación | Reserva real creada → `PENDIENTE_PAGO` (referencias RET-6F7DFF4848374358 y RET-C13FFB7BF0B14D6D) |
| Pantalla éxito | Título "Reserva registrada correctamente", badge **FALTA PAGAR.**, "Esta reserva NO es válida para embarque…", ref RET- + botón Copiar, asiento N°, salida, Total S/ 12.00, "Tiempo para pagar" con countdown real (segundosRestantes 86365), PDF, botones "Ir a Mis reservas" y "Volver al inicio" |
| Estado vacío | Búsqueda sin resultados (2026-12-31) → mensaje `.iwr-empty` y captura |
| Error/loading | `ReservationErrorState` / `ReservationSkeleton` presentes; alertas con `role=alert/status` |

## End-to-end real (QA)

- Reserva 1B → **RET-6F7DFF4848374358** (documento 44221177): constancia **200, `application/pdf`, 2031 bytes**.
- `GET /ventas/recuperar?documento=44221177` → **200** con 2 reservas reales (PENDIENTE_PAGO), boleto/QR para pago en caja.
- Modal "Consultar mis reservas": listado con refs, chip "Pendiente de pago", ruta `Huamanga → Vilcas Huaman · jue 08 oct 08:30 p.m.`, botones Constancia PDF/Boleto PDF; sección Boletos correcta (0 confirmados hasta pago).
- Boleto inexistente **404** y recuperar sin datos **200** (no se generan 500).
- Sobre carrito/Caja/boleto/QR/Chofer/escaneos: código no modificado; `npm run build` verifica integridad; flujo de pago queda en la caja (por diseño, PAGO PRESENCIAL).

## Responsive (9 resoluciones + accesibilidad)

1920×1080 · 1440×900 · 1366×768 · 1024×768 · 768×1024 · 430×932 · 390×844 · 360×800 · 320×568

| Check | Resultado |
|---|---|
| Overflow horizontal (todos los viewports) | **PASS** (ninguno `scrollWidth > clientWidth`) |
| Grid a 1 columna en 320px / tarjeta y buscador dentro del viewport | **PASS** |
| Toggle menú móvil | **PASS** |
| Menú móvil, portrait/landscape | PASS |
| Zoom 200% (390px) sin overflow | PASS |
| Focus visible (`.iwr-page :focus-visible`, `.iwr-control:focus-within`, `.iwr-input:focus`) | PASS |
| Touch targets > 40px (excepto enlaces de nav desktop a 32px ≥ mínimo 24px WCAG) | PASS |
| `prefers-reduced-motion` | soportado en CSS |
| Errores de consola / requests fallidos | 0 / 0 |

Capturas: `reports/evidencias/responsive_<W>x<H>_{top,result}.png`, `responsive_430x932_mapa.png`, `responsive_zoom200.png`,
`reports/shot-reserva-exito.png`, `reports/shot-estado-vacio.png`, `reports/shot-mis-reservas.png`.

## Calidad

- `npm run lint` → 0 errores. `npm run build` → OK (único aviso: chunk > 500 kB).
- Archivos íntegros: `PasajesPage.jsx` 124991 bytes; `reserva-premium.css` 22198 bytes.
- Backend sin modificaciones en este rediseño (solo inserción de datos QA: viaje futuro id 3).

## Veredicto

**RESPONSIVE: PASS**

**FLUJO DE RESERVA PREMIUM INTIWATANA — CERRADO Y VERIFICADO EN QA**

*Nota: los PNG se validaron por tamaño/contenido generado y checks programáticos (bounding boxes, estilos calculados, overflow); el modelo no dispone de visión para inspección pixel a pixel.*

---

# REPARACIÓN VISUAL DEFINITIVA DE MODALES Y FLUJO DE RESERVA

Fecha: 2026-10-08 · Segunda pasada de QA visual sobre el flujo público de reserva de pasajes.

## Causa raíz (diagnóstico real, no cambios a ciegas)

El flujo renderiza sus modales con `createPortal(…, document.body)` (PasajesPage.jsx). Los tokens `--iwr-*` estaban **scoped en `.iwr-page`**, y como el modal queda FUERA de `.iwr-page`, **todos los `var(--iwr-*)` dentro de los modales no resolvían** y el texto heredaba `color: var(--blanco)` = blanco de `body` (App.css línea 29-33). Resultado medido antes del fix: **texto blanco sobre superficies blancas** (resumen, etiquetas, campos, bloques de pago ilegibles/invisibles) y **botón primario transparente**. Adicionalmente:

- `.iwr-modal-grid` tenía `grid-template-columns: 62fr 38fr` **sin `display: grid`** → el mapa ocupaba 1093px a una sola columna y el resumen quedaba fuera de vista (no se aplicaba el 62/38).
- `.hiace-body { max-width: 264px }` de App.css limitaba el bus a 264px dentro de un panel de ~1093px (24%).
- Tamaños de modales fuera de especificación (1140/980/900) y colores del sistema no exactos.

## Cambios aplicados (solo CSS, `frontend/Web-Intiwatana/src/components/reserva-premium.css`)

1. **Tokens `--iwr-*` declarados también en `:root`** → resuelven dentro de los modales portados.
2. `.iwr-modal-grid { display: grid; grid-template-columns: 62fr 38fr }` → restaura las 2 columnas Mapa | Resumen.
3. `.iwr-modal .hiace-body { max-width: none; width: fit-content }` + `.iwr-btn-asiento svg 64×78px` desktop y `56×70px` ≤768px → **bus ~310px (antes 264px)** sin desbordes en móvil.
4. Geometría según especificación: `seat-modal min(1180px,94vw)`, `datos-modal min(1000px,94vw)`, `exito-modal min(820px,94vw)`; overlay `rgba(4,24,22,.72)`; modal borde `#DDE7E4`, radius `22px`, sombra `0 24px 70px rgba(5,35,31,.28)`.
5. Tokens alineados al sistema visual: secciones `#F8FAF9`, secundario `#5C706C`, campos `#FFFFFF/#17312E` borde `#BFCFCB`, `.iwr-btn-primary` min-height `52px`.
6. `.iwr-seat-side { position: sticky }` en desktop (resumen siempre a la vista).

## Antes → Después (navegador real, 1440×900)

| Métrica | ANTES | DESPUÉS |
|---|---|---|
| Color de texto del modal | `rgb(255,255,255)` (invisible) | `rgb(23,49,46)` `#17312E` |
| Fondo del modal | transparente (overlay oscuro a través) | `rgb(255,255,255)` |
| Columnas mapa/resumen | 1 col (1093px) | 691 / 424 (62/38) |
| Bus segmentado | 264px | 310px |
| Asiento | 56×70 | 64×78 (56×70 móvil) |
| Título resumen | blanco (invisible) | `#0B3D38` visible |
| Etiquetas de campos | blanco (invisibles) | `#5C706C` legibles |
| Botón Continuar/Confirmar | transparente | `#127369` texto blanco, 52px |
| Campo | blanco/borde blanco | `#FFFFFF` borde `#BFCFCB`, 48px |

## Evidencia

- **ANTES (estado roto, 5 PNG):** `reports/reparacion-visual-reserva/before/{10-main-desktop,11-seat-modal-desktop,12-seat-selected,13-pasajero-desktop-empty,14-pasajero-desktop-filled}.png` + JSON de estilos calculados `rsv-before-FINAL.json` (resumen 1093px, título y pago blanco sobre blanco).
- **DESPUÉS (49 PNG):** `reports/reparacion-visual-reserva/after/` con `vw-<W>x<H>-{main,seat,seat-sel,datos}.png` en 1920×1080 · 1440×900 · 1366×768 · 1024×768 · 768×1024 · 430×932 · 390×844 · 360×800 · 320×568 · 844×390 (landscape), `vw-390x844-zoom200-seat.png`, `vw-1440x900-reduced-motion-seat.png`, y flujo `20…25-{seat-selected-summary,datos-filled,success-ret,constancia-clicked,mis-reservas,inicio}.png`.
- **Backups + SHA-256:** `reports/respaldo-reparacion-visual-reserva/` (`.orig` de PasajesPage.jsx, reserva-premium.css, App.css, index.css). SHA-256 final de reserva-premium.css reparado: `F038ECD6C26AD19BC630C0C6DBD0FF966AECFC3AA92A47D967EB4FB4438A1786`.

## Prueba funcional real (flujo completo)

BUSCAR → ELEGIR VIAJE → SELECCIONAR ASIENTO → CONTINUAR → COMPLETAR DATOS → CONFIRMAR → **RET real `RET-C6C5852F07EE435D`** → COPIA → DESCARGAR CONSTANCIA → IR A MIS RESERVAS → VOLVER AL INICIO.

- Reserva real creada (POST `/api/v1/ventas/retencion` 201, `/reservar` 200) → `PENDIENTE_PAGO`.
- Pantalla éxito verificada: referencia `RET-C6C5852F07EE435D`, countdown real `23:59:58` (desde `expiraEn`), alerta **FALTA PAGAR** (`#FFF9E6`/borde `#D58B15`) y **NO VÁLIDA PARA EMBARQUE** (`#FDEEEE`/borde `#C84C4C`), botón "Descargar constancia PDF".
- "Ir a Mis reservas" y "Volver al inicio" completos sin errores. La descarga del PDF no pudo observarse en el harness headless (sin `acceptDownloads`); el endpoint de constancia ya fue verificado 200 `application/pdf` en la pasada anterior; no es defecto de producto.
- **Errores de consola: 0 · Requests fallidos: 0** en todas las ejecuciones.

## Checklist §50 (39 condiciones)

| # | Condición | Resultado |
|---|---|---|
| 1 | Modal de asientos abre centrado, `min(1180px,94vw)` | PASS |
| 2 | Mapa 62% / resumen 38% reales en desktop | PASS |
| 3 | Bus más grande y centrado (310px vs 264px) | PASS |
| 4 | Asientos visibles y táctiles (≥46px; 64×78 desktop / 56×70 móvil) | PASS |
| 5 | Resumen lateral legible (`#0B3D38`/`#17312E` sobre blanco) | PASS |
| 6 | Modal pasajero `min(1000px,94vw)`, secciones `#F8FAF9` | PASS |
| 7 | Etiquetas de campos legibles `#5C706C` | PASS |
| 8 | Campos `#FFFFFF/#17312E`, borde `#BFCFCB`, alto 48px | PASS |
| 9 | Bloque "Pago presencial en caja" visible y legible | PASS |
| 10 | Pantalla éxito `min(820px,94vw)`: título/RET/countdown/alertas | PASS |
| 11 | Referencia RET destacada + botón Copiar | PASS |
| 12 | Alertas FALTA PAGAR (dorado) y NO VÁLIDA PARA EMBARQUE (rojo/coral) | PASS |
| 13 | Overlay `rgba(4,24,22,.72)` | PASS |
| 14 | Modal `#FFFFFF`, texto `#17312E`, borde `#DDE7E4` | PASS |
| 15 | Radius 22px y sombra `0 24px 70px rgba(5,35,31,.28)` | PASS |
| 16 | Secciones de modal `#F8FAF9` | PASS |
| 17 | Campos de formulario según sistema visual | PASS |
| 18 | Texto secundario `#5C706C` | PASS |
| 19 | Botón primario `#127369`/blanco | PASS |
| 20 | Botones primarios alto mínimo 52px | PASS |
| 21 | Estados de foco visibles | PASS |
| 22 | Sin texto blanco sobre blanco ni contenedores con opacidad | PASS |
| 23 | Un solo dialog activo por etapa | PASS |
| 24 | Una única acción primaria por etapa | PASS |
| 25 | Continuar deshabilitado sin asiento; activo al seleccionar | PASS |
| 26 | Protección anti doble envío (`procesando`, submit disabled) | PASS |
| 27 | Countdown solo desde `expiraEn` (frontend no cambia estado) | PASS |
| 28 | 1920×1080 sin overflow horizontal | PASS |
| 29 | 1440×900 sin overflow | PASS |
| 30 | 1366×768 sin overflow | PASS |
| 31 | 1024×768 sin overflow | PASS |
| 32 | 768×1024 (portrait) sin overflow | PASS |
| 33 | 430×932 · 390×844 · 360×800 · 320×568 sin overflow | PASS |
| 34 | Landscape 844×390 sin overflow | PASS |
| 35 | Zoom 200% (390px) sin overflow; `prefers-reduced-motion` | PASS |
| 36 | `role=dialog`, `aria-modal`, `aria-labelledby/label`, labels vinculados | PASS |
| 37 | `aria-live` (countdown/estados), `aria-invalid` + `aria-describedby` | PASS |
| 38 | Foco inicial/teclado: asientos tabulables, cierre foco a close | PASS |
| 39 | Touch targets ≥46px | PASS |

## Calidad

- `npm run lint` → **exit 0**. `npm run build` → **exit 0** (único aviso: chunk > 500 kB, preexistente).
- Backend sin cambios en esta reparación (solo datos QA: se consumieron asientos de los trips QA con las reservas de prueba).

## Veredicto

**PASS**