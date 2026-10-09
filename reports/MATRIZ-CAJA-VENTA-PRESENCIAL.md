# MATRIZ DE INSPECCIÃ“N â€” MÃ“DULO EMPRESARIAL DE CAJA / VENTA PRESENCIAL

**Proyecto:** `C:\Users\USER\Downloads\intiwatana_final_v2`
**Fecha:** 2026-10-08
**Rama:** `main`
**Alcance:** verificar, ANTES de tocar cÃ³digo, quÃ© partes del flujo
`turno â†’ nueva venta â†’ viaje â†’ asiento â†’ pasajero â†’ pago â†’ confirmaciÃ³n â†’ boleto â†’ QR â†’ cierre`
ya existen, cuÃ¡les estÃ¡n a medias y cuÃ¡les faltan, para implementar la brecha mÃ­nima reutilizando
lo existente y sin duplicar componentes ni endpoints.

> MÃ©todo: lectura directa de controladores, servicios, entidades y del panel React
> (`AdminDashboard.jsx`, `PasajesPage.jsx`) mÃ¡s bÃºsqueda de `caja|turno|cierre|ROLE_CAJERO`.
> No se modificÃ³ ningÃºn archivo para elaborar esta matriz.

---

## 1. Resumen ejecutivo

| Bloque | Estado | Comentario |
|---|---|---|
| Venta presencial directa (1 asiento) | **PARCIAL** | `POST /api/v1/boletos/vender` existe y funciona (pago + boleto + QR + asiento VENDIDO) pero **no hay UI** que lo use y exige `clienteId` (no acepta comprador inline). |
| Venta presencial multi-asiento atÃ³mica | **NO EXISTE** | No hay endpoint que emita N boletos en una sola transacciÃ³n. |
| Reserva web â†’ caja â†’ boleto | **EXISTE** | `VentaService` (retenciÃ³n â†’ pendiente â†’ confirmar) + `ReservasVentas` UI. |
| MenÃº del rol CAJERO | **PARCIAL** | Existe `MENU_CAJERO` con Panel/Viajes/Reservas/Clientes/Pagos/Encomiendas; **faltan** Inicio de caja, Nueva venta, Boletos, Operaciones, Cierre. |
| SelecciÃ³n de asiento reutilizable | **PARCIAL** | `MapaAsientos` (interactivo) existe pero **no estÃ¡ exportado** ni importado en el panel; el panel sÃ³lo tiene `MapaAsientosGrafico` (solo lectura). |
| Cobro en efectivo (recibido/vuelto) | **NO EXISTE** | No hay captura de monto recibido ni validaciÃ³n de vuelto en el panel. |
| Boleto + QR + imprimir/descargar | **PARCIAL** | Se emite boleto con `codigoQr`/`qrImagenUrl` y hay PDF pÃºblico; **no** hay vista Boletos ni reimpresiÃ³n desde caja. |
| Operaciones de caja (registro del turno) | **PARCIAL** | `Pago`/`CrudPagos` registran pagos; no se asocian a un turno/caja. |
| Cierre de caja / arqueo | **NO EXISTE** | No hay entidad, tabla, servicio ni endpoint de Caja/Turno/Cierre. |
| Registro/consulta de cliente | **EXISTE** | `ClienteController` CRUD + `SelectorCliente` reutilizable. |
| Seguridad por rol | **EXISTE** | `SecurityConfig` + `@PreAuthorize`; CAJERO habilitado en ventas/boletos/pagos/clientes. |

---

## 2. Inventario backend (lo que existe)

### 2.1 Entidades / tablas (`entity/`)
`Viaje`, `Bus`, `Ruta`, `Sucursal`, `Usuario`, `Rol`, `Cliente`, `Asiento`, `Pago`, `Boleto`,
`Retencion`, `Encomienda`, `MovimientoEncomienda`, `ImagenPortal`, `ContenidoPortal(+Version)`, `BaseEntity`.

- **No existe** entidad `Venta`, `Caja`, `Turno`, `Cierre` ni `SesionCaja`.
- Modelo comercial real = **`Retencion`** (reserva) â†’ **`Pago`** â†’ **`Boleto`** (con `codigoQr`, `qrImagenUrl`).
- `Asiento` tiene `estado` (`DISPONIBLE|RESERVADO|VENDIDO|BLOQUEADO`), `retencion`, `retenidoHasta`, `boleto`.
- `Pago` tiene `cajero` (`Usuario`), `cliente`, `metodo`, `estado`, `referencia`, `fechaPago`.
- `Boleto` tiene `cajero`, `pago`, `asiento`, `cliente`, `numeroBoleto` (Ãºnico), `codigoQr`, `qrImagenUrl`.

### 2.2 Servicios (`service/impl/`)
- `VentaServiceImpl` â€” flujo reserva: `crearRetencion` (bloquea asientos con lock pesimista,
  `CanalVenta.WEB`), `reservarPendientePago`, `confirmarPago` (crea `Pago` + N `Boleto`+QR, marca
  asiento `VENDIDO`, **idempotente**), `liberar`, `listarReservas`, `recuperar`, `constanciaPdf`,
  `boletoPdf`, `liberarVencidas`. Fechas: `now` del servidor, `expiraEn` calculado.
- `BoletoServiceImpl` â€” **venta directa por cajero** `vender(BoletoRequest)`: exige `clienteId`,
  lock pesimista, un solo `asientoId`, crea `Pago` COMPLETADO + `Boleto`+QR, marca `VENDIDO`,
  auditorÃ­a (`VENTA_BOLETO`). TambiÃ©n `comprarPasaje` (portal), `escanearQr`, `cancelar`.
- `PagoServiceImpl` â€” CRUD de pagos manuales (no ligados a boleto salvo por `pago_id`).
- `ClienteServiceImpl` â€” CRUD + bÃºsqueda `q` + `obtenerPorDniRuc`.
- `ViajeServiceImpl` â€” incluye `obtenerAsientos` (libera reservas vencidas perezosamente) y
  `ConfiguracionAsientosService` (config COMPLETA del bus).
- `AsientoServiceImpl` â€” lectura por id.

### 2.3 Controladores / endpoints
| Endpoint | Rol permitido | Uso |
|---|---|---|
| `POST /api/v1/ventas/retencion` | pÃºblico | crea reserva (canal WEB) |
| `POST /api/v1/ventas/retencion/{ref}/reservar` | pÃºblico | pendiente de pago |
| `POST /api/v1/ventas/reservas/{ref}/confirmar` | ADMIN/CAJERO/VENDEDOR | cobra y emite boletos (idempotente) |
| `POST /api/v1/ventas/reservas/{ref}/liberar` | ADMIN/CAJERO/VENDEDOR | libera reserva |
| `GET /api/v1/ventas/reservas` | ADMIN/CAJERO/VENDEDOR | lista reservas |
| `GET /api/v1/ventas/boleto/{n}/pdf?documento=` | pÃºblico | boleto PDF (control por documento) |
| `POST /api/v1/boletos/vender` | ADMIN/CAJERO/VENDEDOR | **venta directa 1 asiento** |
| `POST /api/v1/boletos/comprar` | CLIENTE | compra portal |
| `GET /api/v1/boletos/{id}` / `/numero/{n}` / `/cliente/{id}` / `/viaje/{id}` | ADMIN/CAJERO/... | consultas |
| `GET /api/v1/clientes?q=` / `POST /api/v1/clientes` | ADMIN/CAJERO/VENDEDOR/... | buscar/registrar pasajero |
| `GET /api/v1/viajes` / `GET /api/v1/viajes/{id}/asientos` | pÃºblico | viajes y mapa de asientos |
| `GET/POST /api/v1/pagos` | ADMIN/CAJERO | pagos |

### 2.4 Seguridad (`SecurityConfig`)
- `/api/v1/ventas/reservas/**` â†’ ADMIN/CAJERO/VENDEDOR.
- `/api/v1/boletos/**`, `/api/v1/pagos/**`, `/api/v1/clientes/**` â†’ autenticado + `@PreAuthorize`.
- Flujo pÃºblico de retenciÃ³n/PDF/recuperaciÃ³n â†’ `permitAll`.
- Sin regla especÃ­fica para una venta presencial: cae en `authenticated()` (+ `@PreAuthorize`).

---

## 3. Inventario frontend (lo que existe)

`frontend/Web-Intiwatana/src/components/AdminDashboard.jsx` (â‰ˆ3.3k lÃ­neas, estilos inline `adm-*`):

| FunciÃ³n | LÃ­neas aprox. | Estado |
|---|---|---|
| `MENU_ADMIN` / `MENU_CAJERO` | 2737-2771 | arrays `{group,items:[{key,icon,label}]}` |
| `DashboardHome` (cajero) | 236-555 | KPIs, prÃ³ximas salidas, reservas por confirmar |
| `CrudViajes` (readOnly para cajero) | 1050-1337 | lista viajes + modal `MapaAsientosGrafico` (solo lectura) |
| `MapaAsientosGrafico` | 945-1045 | mapa visual estÃ¡tico |
| `CrudClientes` + `SelectorCliente` | 1343-1645 | CRUD y selector inline (reutilizable) |
| `CrudPagos` | 2333-2581 | libro de pagos manual (no liga venta) |
| `ReservasVentas` | 2590-2735 | **confirma/libera reservas web** (Ãºnica vÃ­a actual a boleto) |
| `ModalForm` / `ConfirmModal` | 192-230 | primitivas de modal reutilizables |

`PasajesPage.jsx`:
- `function MapaAsientos({ asientos, seleccionados, onSeleccionar, configuracion })` (lÃ­nea 228),
  **no exportada**. Descarga asientos del padre vÃ­a `GET /api/v1/viajes/{id}/asientos`.
- Estilos del sistema Premium en `reserva-premium.css` (`.iwr-*`, `.mapa-asientos`, stepper `.iwr-step*`).

**No existe** en el panel: wizard de venta, selecciÃ³n de asiento interactiva, captura de efectivo,
vista de Boletos, registro de operaciones por turno ni cierre de caja.

---

## 4. Brechas y cambio mÃ­nimo propuesto

| # | FunciÃ³n faltante | Cambio mÃ­nimo propuesto | Reutiliza |
|---|---|---|---|
| 1 | Venta presencial multi-asiento atÃ³mica | `VentaService.ventaPresencial(...)` + `POST /api/v1/ventas/presencial`: crea retenciÃ³n con `canal=CAJERO` y confirma en **una transacciÃ³n** (mÃ©todo privado compartido con `crearRetencion`). | `VentaServiceImpl.crearRetencion/confirmarPago` (lÃ³gica de locks, pago, boleto, QR, auditorÃ­a) |
| 2 | SelecciÃ³n interactiva de asiento en caja | Exportar `MapaAsientos` desde `PasajesPage.jsx` e importarlo en el panel. | `MapaAsientos`, `reserva-premium.css` |
| 3 | MenÃº CAJERO | AÃ±adir grupo **OPERACIÃ“N DE CAJA**: Inicio, Nueva venta, Reservas pendientes, Pagos, Boletos; **CONTROL**: Operaciones, Cierre de Caja (sin cambiar permisos). | `MENU_CAJERO`, `irA/seccion` |
| 4 | Wizard Nueva Venta (6 pasos) | Componente `VentaPresencial` que usa viajeâ†’mapaâ†’`SelectorCliente`â†’efectivoâ†’confirmaciÃ³nâ†’boletos. | `MapaAsientos`, `SelectorCliente`, `ModalForm` |
| 5 | Vista Boletos (ver/reimprimir) | Componente `BoletosCaja`: buscar por nÃºmero/cliente/viaje y abrir PDF existente. | `GET /api/v1/boletos/...`, `/api/v1/ventas/boleto/{n}/pdf` |
| 6 | Cierre de caja | **No existe entidad Turno/Caja**: se implementa un **resumen operativo del dÃ­a** (pagos + boletos emitidos) a partir de datos existentes y se documenta la limitaciÃ³n (no se inventa la entidad). | `GET /api/v1/pagos`, `GET /api/v1/boletos/viaje/{id}` |

**No se harÃ¡:** nuevos paneles duplicados, endpoints `v2/nuevo/final/corregido`, datos simulados,
pasarelas de pago, SUNAT/facturaciÃ³n, GPS, `DROP/TRUNCATE`, modificar migraciones aplicadas.

---

## 5. Decisiones de diseÃ±o

1. **Una sola fuente de verdad de disponibilidad:** la venta presencial reutiliza el mecanismo de
   `Retencion` (bloqueo por `viaje+asiento` con lock pesimista) en lugar de crear un camino paralelo.
2. **Canal `CAJERO`:** se parametriza `crearRetencion` con `CanalVenta` (WEB por defecto) para no
   falsear la auditorÃ­a del canal presencial.
3. **AtÃ³mico:** retenciÃ³n + confirmaciÃ³n dentro de la misma transacciÃ³n de `ventaPresencial`.
4. **Idempotencia:** se admite `idempotencyKey` como en el flujo web.
5. **Pago:** solo `EFECTIVO` (el resto de mÃ©todos existe en el enum pero no se ofrece en caja segÃºn
   las prohibiciones de la tarea).
6. **Cierre:** sin entidad de turno, se entrega un resumen derivado y se declara la limitaciÃ³n.

---

## 6. Evidencia de inspecciÃ³n (referencias)

- `backend/.../service/impl/VentaServiceImpl.java:195` (`crearRetencion`), `:445` (`confirmarPago`).
- `backend/.../service/impl/BoletoServiceImpl.java:65` (`vender`), `:315` (`crearYGuardarBoleto`).
- `backend/.../controller/VentaController.java:103` (confirmar), `BoletoController.java:64` (`/vender`).
- `backend/.../entity/Retencion.java`, `Pago.java`, `Boleto.java`, `Asiento.java`.
- `backend/.../config/SecurityConfig.java:95-114`.
- `frontend/.../AdminDashboard.jsx:2760` (`MENU_CAJERO`), `:2590` (`ReservasVentas`).
- `frontend/.../PasajesPage.jsx:228` (`MapaAsientos`, sin exportar).
