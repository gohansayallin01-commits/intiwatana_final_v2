# REDISEÑO ADMIN PREMIUM INTIWATANA — MATRIZ DE SEGURIDAD (§2)

Fecha de creación: 2026-10-08. Documento de control previo a la implementación.

## Respaldo (§3)
- Carpeta: `reports/respaldo-rediseño-admin/` (copia íntegra + MANIFEST.txt con SHA-256).
- Nada de `git reset/checkout/clean`; sin `.env`, `.jwt`, contraseñas, `node_modules`, `target`, dump de BD.

| Archivo | SHA-256 |
|---|---|
| frontend/Web-Intiwatana/src/components/AdminDashboard.jsx | 30BEC8714E8E6BE9419B16B9DB18BF548F86CCF11FA823353374024EF92C2A0A |
| frontend/Web-Intiwatana/src/components/ReportesPage.jsx | 06FFCE88A7794C05CB0FDFA257ED8860B65CBFAF7F5BC2DED0E7CB97EDE41C32 |
| frontend/Web-Intiwatana/src/components/PortalGestion.jsx | B3E84E1504C9AFB9B4864102B0819D7C3D91CB0368EE80EE76C4ED752D2443D8 |

## Paleta oficial (§7)
Deep green `#0B3D38` · `#104A43` · `#127369` · `#1A8A7E` · Gold `#F5C518` · `#C99B00` · Fondos `#F4F7F6`/`#FFFFFF` · Texto `#17312E`/`#667875` · Bordes `#DDE7E4` · Semánticos `#158463`, `#D58B15`, `#C84C4C`, `#397CBF`.

> Los tokens CSS comparten nombres (`verde`, `oscuro`, `amarillo`, `medio`, `gris`, `grisClaro`, `fondo`, `blanco`, `rojo`, `naranja`) → se actualizan VALORES, no claves. «Aplicar la paleta a las mismas clases/CSS existentes, no a otro» (§4).

| Módulo | Componente oficial | CSS | Endpoints | Funciones | Problema visual | Cambio | Riesgo | Prueba | Evidencia |
|---|---|---|---|---|---|---|---|---|---|
| Panel (Inicio) | `DashboardHome` (AdminDashboard.jsx, l.227) | `<style>` inline adm (l.2572+) | GET /api/v1/rutas, /viajes, /buses, /clientes, /encomiendas, /sucursales, /pagos, /ventas/reservas | carga KPIs, ocupación, donut, tabla paginada | Sin identidad institucional; KPIs genéricos; sin próximas salidas | Banner institucional SVG (paisaje/bus, sin foto), KPIs premium, próximas salidas reales, reservas pendientes reales, últimas encomiendas reales | Bajo: solo JSX/CSS de presentación con datos ya cargados | F01-F04, V01-V06 | reports/rediseno-admin-premium/*.png |
| Sucursales | `CrudSucursales` (l.389) | `<style>` inline | GET/POST/PUT/DELETE /api/v1/sucursales(/id) | CRUD, búsqueda | Tabla plana, botones pequeños | Tabla premium + badges + clase responsiva; acciones 40px; header de módulo | Bajo | F05-F08, V07-V10 | png |
| Rutas | `CrudRutas` (l.449) | `<style>` inline | GET/POST/PUT/DELETE /api/v1/rutas(/id), GET sucursales | CRUD, búsqueda, select origen→destino | Ídem | Ídem + chip ruta ORIGEN→DESTINO premium | Bajo | F09-F11, V11-V14 | png |
| Autobuses | `CrudBuses` (l.511) | `<style>` inline | GET/POST/PUT/DELETE /api/v1/buses(/id) | CRUD, búsqueda | Ídem | Ídem + tipo de bus badge premium | Bajo | F12-F14, V15-V18 | png |
| Viajes | `CrudViajes` (l.679) + `MapaAsientosGrafico` (l.574) | `<style>` inline | GET viajes/rutas/buses/usuarios/sucursales, GET viajes/{id}/asientos, PUT/POST/DELETE viajes(/id), PATCH viajes/{id}/estado?estado=CANCELADO | CRUD, cancelación, mapa asientos, filtros | Filtros sueltos; mini-stats básicos | Filtros unificados premium, mini-stat chips premium, tabla + mapa de asientos consistente | Medio (no tocar lógica de estados/transiciones) | F15-F18, V19-V23 | png |
| Clientes | `CrudClientes` (l.972) | `<style>` inline | GET/POST/PUT/DELETE /api/v1/clientes(/id), GET boletos/cliente/{id}, GET encomiendas/remitente/{id} | CRUD, historial (modal) | Historial modal denso | Modal premium, tabla premium, respeto a `puedeEliminar` | Bajo | F19-F21, V24-V26 | png |
| Pagos | `CrudPagos` (l.1962) | `<style>` inline | GET/POST/PUT/DELETE /api/v1/pagos(/id) | CRUD, boleta imprimible | Boleta/courier válida; tabla plana | Tabla premium + modal boleta conservado; no tocar bloque @media print | Bajo | F22-F24, V27 | png |
| Reservas / Ventas | `ReservasVentas` (l.2219) | `<style>` inline | GET /api/v1/ventas/reservas?estado=RETENIDO&estado=PENDIENTE_PAGO, POST /reservas/{ref}/confirmar, POST /reservas/{ref}/liberar, GET /boleto/{num}/pdf | Confirmar/liberar reservas, emitir PDF | Cabecera/ tabla ad-hoc (sin componentes compartidos) | Header modular premium + tabla premium + panel de pago premium; lógica y mensajes intactos | Medio (módulo transaccional; NO tocar confirmar/liberar) | F25-F28, V28-V30 | png |
| Encomiendas | `CrudEncomiendas` (l.1320) | `<style>` inline | GET/POST /api/v1/encomiendas, GET solicitudes-pendientes, GET solicitudes-pendientes/{ref}, POST /{id}/confirmar-recepcion, PATCH /{id}/estado, GET sucursales | Registro presencial, recuperar SOL, flujo de estados (ENC_TRANSICIONES), detalle | Panel SOL amarillo viejo; tabs con `#12736a` hardcoded; grids medios | Sección SOL premium (gold), tabs premium con token, tabla premium (2 tablas), respetar estados/transiciones y protección de datos | Alto (lógica de estados frágil) → SOLO presentación | F29-F32, V31-V33 | png |
| Reportes | `ReportesPage.jsx` (archivo oficial) | inline styles (paleta propia) | (propios del módulo) | KPIs, tablas, barras, CSV | Paleta antigua | Solo alinear valores de paleta `C` a la oficial; sin cambios de consultas | Bajo | F33, V34 | png |
| Gestión del Portal | `PortalGestion.jsx` (archivo oficial) | inline styles | (propios) | CRUD contenido portal | Paleta antigua | Solo alinear paleta `C` | Bajo | F34 | png |
| Personal | `CrudPersonal` (l.1840) | `<style>` inline | GET/POST/PUT/DELETE /api/v1/usuarios(/id) | CRUD usuarios, filtro rol | Filtros sueltos, tabla plana | Header/estadística rol premium, tabla premium; sin contraseñas/hashes ni claves en DOM | Medio (cuentas) → solo presentación | F35-F36, V17-V20 | png |
| Layout | `AdminDashboard` (l.2415) | `<style>` inline | — | sidebar, topbar, toast, responsive | Sidebar sin agrupación; topbar básica | Sidebar premium agrupado (OPERACIÓN/CONFIGURACIÓN/CLIENTES Y SERVICIOS/GESTIÓN) + colapso + drawer/Escape + tooltips; topbar ejecutiva (fecha, avatar, rol, logout); toast premium | Bajo (no cambiar roles/permisos por rol) | V01-V06, V34 | png |

## Riesgos transversales
1. Cache de Vite en CI/colegas: se documenta; no afecta runtime del usuario.
2. `@import` de Google Fonts (ya presente) puede fallar offline → fallback `sans-serif` definido.
3. Módulos transaccionales (Reservas, Encomiendas) y de cuentas (Personal): SOLO cambios de presentación; no se alteran handlers ni payloads.
4. No se reescribe la lógica de estados (ENC_TRANSICIONES, transiciones de viaje, confirmar/liberar reserva).
5. No se crean archivos nuevos de componente ni un segundo CSS admin: el sistema se aplica al mismo `<style>` oficial y a los valores de token.

## Regla de detención
Al terminar implementación + pruebas + informe no se inicia otro módulo; se cierra con el veredicto definido en §47.