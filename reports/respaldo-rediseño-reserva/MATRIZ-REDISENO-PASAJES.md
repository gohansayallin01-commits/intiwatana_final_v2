# MATRIZ DE REDISEÑO — FLUJO PÚBLICO DE PASAJES (INTIWATANA)

Documento de análisis previo. **No se toca backend.** Todo cambio es de
presentación sobre el componente oficial `PasajesPage.jsx` y su hoja de estilos.

## Inventario base (oficial, no se duplica)

- `frontend/Web-Intiwatana/src/components/PasajesPage.jsx` (2021 líneas, 130 979 bytes)
  - `ErrorBoundary`, `fmtFecha`, `getImagenDestino`, `abrirDocumento`, `etiquetaEstadoReserva`, `MODELO_METODOS`
  - `TopDownSeatSVG`, `DriverSeatSVG`, `AsientoBtn`, `MapaAsientos`
  - `FormEncomienda`
  - `ModalPago`, `ModalBoardingPass`, `TimerReserva`, `ModalReserva`, `ModalRecuperar`
  - `CarritoSidebar`, `ViajeCard`
  - `PasajesPage` (componente principal) — única pantalla `/pasajes`
- `frontend/Web-Intiwatana/src/App.css` (1666 líneas): variables `:root`, `.nav`, `.modal*`, `.btn-reservar`, `.mapa-asientos`, `.hiace-*`
- `frontend/Web-Intiwatana/src/admin.css`: `.btn-admin-nav`, `.btn-cancelar`, `.btn-registrarse`
- `frontend/Web-Intiwatana/src/App.jsx`: ruta `/pasajes` → `<PasajesPage/>` (no se cambia)
- Recursos: `src/assets/Vilcas.jpeg` (paisaje andino), `src/components/LogoSVG.jsx`

## Endpoints usados (contrato real — INTOCABLE)

| Endpoint | Método | Uso |
|---|---|---|
| `/api/v1/sucursales` | GET | carga orígenes/destinos |
| `/api/v1/viajes?origenId&destinoId&fecha` | GET | buscar viajes |
| `/api/v1/viajes/{id}/asientos` | GET | mapa de asientos |
| `/api/v1/ventas/retencion` | POST | retener asientos |
| `/api/v1/ventas/retencion/{ref}/reservar?metodoPago=` | POST | fijar reserva PENDIENTE_PAGO |
| `/api/v1/ventas/retencion/{ref}/constancia.pdf?documento=` | GET | constancia PDF |
| `/api/v1/ventas/boleto/{num}/pdf?documento=` | GET | boleto PDF (tras pago en caja) |
| `/api/v1/ventas/recuperar?documento=` | GET | mis reservas |
| `/api/v1/boletos/comprar` | POST | carrito (usuario con sesión) |

Campos reales disponibles: `viaje.{id,ruta.origen.ciudad,ruta.destino.ciudad,fechaHoraSalida,precioAdulto,precioOficial,precio,bus.placa,bus.tipo,asientosDisponibles,totalAsientos}`; `reserva.{referencia,estado,asientos[].numeroAsiento,fechaHoraSalida,total,expiraEn,mensaje,validoParaEmbarque,boletos[]}`; `asiento.{id,numeroAsiento,estado,fila,columna}`.

## Matriz

| # | PANTALLA (estado real) | ESTILOS ACTUALES | FUNCIÓN / HANDLER | ENDPOINT | PROBLEMA | CAMBIO PROPUESTO | RIESGO |
|---|---|---|---|---|---|---|---|
| 1 | Navbar público (invitado y con sesión) | `.nav`, `.nav-links`, `.btn-admin-nav`, `.btn-registrarse` | `navigate`, `cambiarTab`, `irALogin`, `setCarritoAbierto`, `setModalRecuperar` | — | Navbar sin identidad de marca consistente, botón "Mis reservas" compite con "Carrito"; no agrupa bien en móvil | Reordenar a marca + enlaces (Destinos, Horarios, Mis reservas, Encomiendas, Nosotros, Contacto) + Iniciar sesión; menú compacto en móvil; no mostrar cuenta falsa | Bajo (solo presentación; handlers intactos) |
| 2 | Banner/Hero de pasajes (tabs Compra/Envío) | inline + `linear-gradient` | `cambiarTab`, `buscar` | `/sucursales` | Banner usa degradado plano; sin imagen andina ni altura controlada; título genérico | Banner institucional con `Vilcas.jpeg` + overlay verde; altura 130–190/110–160/90–135px; título "MÁS QUE UN DESTINO, ES TU HISTORIA"; fallback si la imagen falla | Medio (fallback obligatorio para no romper si falta imagen) |
| 3 | Barra de progreso del flujo | (no existe) | derivada de estado | — | El usuario no sabe en qué paso está | Nuevo `ReservaProgress` (5 pasos): Buscar → Asiento → Datos → Confirmar → Listo; `aria-current`; no introduce lógica de negocio | Bajo (componente de solo lectura) |
| 4 | Buscador (origen, destino, fecha, pasajeros) | pill inline | `buscar` | `/sucursales`, `/viajes` | Campos sin labels visibles/iconos; min-height <48px en móvil; select nativos | Formulario con labels accesibles, iconos, min 48px, layout responsivo; mismo submit | Bajo |
| 5 | Resultados: lista de viajes | `ViajeCard` inline | `seleccionarViaje` | `/viajes` | Tarjeta oscura poco jerarquizada; datos apretados en móvil | Nuevo `ViajeSummaryCard` (ruta, hora, placa, precio, asientos con barra), reutiliza `ViajeCard` mejorado, no nuevo componente duplicado | Bajo |
| 6 | Estado "Buscando…" / "Sin viajes" | inline spinner/texto | `buscando`, `buscado`, `viajes` | — | Sin skeleton; empty state débil | `ReservationSkeleton` para carga; `ReservationErrorState`/vacío con CTA "probar otra fecha" | Bajo |
| 7 | Modal mapa de asientos | `.modal-overlay/.modal`, `.mapa-asientos`, `.hiace-*` | `MapaAsientos`, `toggleAsiento`, `agregarPasajesAlCarrito`, `setModalReserva` | `/viajes/{id}/asientos` | Modal max-width 800; en móvil el grid fijo `minmax(300px,1fr) 300px` no colapsa → scroll horizontal; asiento base 54×68 | Modal amplio 2 columnas en escritorio (mapa izq, resumen der) y 1 columna en móvil; asientos ≥44 escritorio / ≥48 móvil; nuevo `SeatLegend` y `SeatSelectionSummary` (si no hay equivalente) | Alto (no romper selección ni layout Hiace; probar 320px) |
| 8 | ModalReserva paso 1 (datos comprador) | `.modal pago-modal`, `inputStyle` | `enviar`, `setForm`, `setMetodo` | `/ventas/retencion` | Formulario de 2 columnas fijas; en móvil apretado; mezcla "método de pago" (son preferencias, no pago real) | `PassengerFormSection` en 1 columna móvil; seccionado; aclarar "Pago presencial en caja" como bloque informativo no seleccionable; conservar campos y body exactos | Alto (el body del POST debe permanecer idéntico) |
| 9 | ModalReserva paso 2 (reserva registrada) | inline | `documentos`, `TimerReserva`, `onReservaCreada`, `onVencida` | `/constancia.pdf` | Reserva REF poco destacada; alertas de estado en texto pequeño | `ReservationSuccessCard`: REF grande + copiar, alerta amarilla "FALTA PAGAR" / roja "NO VÁLIDA PARA EMBARQUE" según `estado`/`validoParaEmbarque`; countdown desde `expiraEn`; botones PDF / Mis reservas / Inicio | Medio (estado y countdown vienen del backend; no calcular expiración) |
| 10 | ModalBoardingPass (boleto) | inline | `ModalBoardingPass` | `/boleto/.../pdf` | Card de boleto funcional pero no sigue nueva paleta | Alinear colores/tipografía al sistema premium; sin cambiar datos | Bajo |
| 11 | ModalRecuperar "Mis reservas" | inline | `buscar`, `abrirDocumento`, `rutaDeBoleto` | `/ventas/recuperar` | Enlaces PDF y estados poco legibles | Reestilizar con badges de estado y botones claros; misma lógica | Bajo |
| 12 | CarritoSidebar | inline | `onRemove`, `onCheckout` | — | Panel lateral oscuro; en móvil 100% ancho | Mantener; ajustar a 100% ancho ≤480px | Bajo |
| 13 | ModalPago (carrito) | `.modal pago-modal` | `confirmarPagoCarrito`, `confirmarPagoCarrito` | `/boletos/comprar` | Idem | Restilizar; conservar lógica | Bajo |
| 14 | Tab Encomiendas / FormEncomienda | inline | `FormEncomienda` | `/encomiendas/solicitud` | Fuera del alcance estricto de pasajes, pero comparte pantalla | Solo adaptar contenedor a nueva paleta; NO tocar lógica | Bajo |
| 15 | Responsive global | `@media` en App.css | — | — | Faltan breakpoints 1366, 768, 430, 390, 360, 320; grids fijos | Añadir bloque `@media` dedicado; sin scroll horizontal; focus visible; contraste | Alto (validar en todos los puntos) |

## Componentes nuevos permitidos (solo si no existe equivalente)

`ReservaProgress`, `ViajeSummaryCard`, `SeatLegend`, `SeatSelectionSummary`,
`PassengerFormSection`, `ReservationSuccessCard`, `ReservationAlert`,
`ReservationSkeleton`, `ReservationErrorState`.

Todos viven **dentro** de `PasajesPage.jsx` o un archivo de apoyo de estilos; no se crea
otra pantalla, otro mapa, otro PDF ni endpoints paralelos.

## Reglas de seguridad del rediseño

1. No modificar cuerpo de requests, endpoints, estados ni flujo de reserva/emisión.
2. No inventar `expiresAt`, `referencia`, `estado` ni importes: todo del backend.
3. "Pago presencial en caja" informativo; el botón dice *Confirmar reserva para pagar en caja*.
4. No mostrar sesión falsa como "Invitado".
5. Evidencia con capturas reales por breakpoint; veredicto final RESPONSIVE.

## Respaldo

Carpeta `reports/respaldo-rediseño-reserva/`:
`PasajesPage.jsx.bak`, `App.css.bak`, `App.jsx.bak`, `index.css.bak` + `HASHES.txt`.