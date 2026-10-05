# Cierre comercial y preparacion para produccion — Modulo de Encomiendas y Tracking INTIWATANA

Fecha: 2026-10-02
Responsable: cierre de pendientes criticos del modulo de Encomiendas y Tracking
Base de datos auditada: `transporte_bd` (PostgreSQL 17)
Entorno de verificacion: backend `http://localhost:8080` (jar reconstruido), frontend `http://localhost:5173`

---

## 1. Resumen ejecutivo

Se cerro el pendiente comercial mas critico del modulo: **el cliente ya no puede
fijar el precio de una encomienda**. Una solicitud del portal ahora nace como
`SOLICITUD_PENDIENTE` con costo `0.00` y **sin pago**, y solo un usuario
autorizado (`ADMIN`, `CAJERO`, `VENDEDOR`, `OPERADOR_ENCOMIENDAS`) puede
confirmar la recepcion fisica fijando el **peso real** y la **tarifa oficial**.

La interfaz ya no presenta la tarifa estimada como precio final: se etiqueta
como **referencial** y se informa que el precio se confirma en agencia. El
tracking publico se mantiene, sin exponer costo, metodo de pago ni datos
privados.

Toda la verificacion se ejecuto contra el sistema real (API + base de datos +
navegador). Resultado: **12 de 12 pruebas de API/UI en verde**, lint y build de
frontend limpios, sin errores de consola ni peticiones fallidas.

## 2. Objetivo

Resolver los pendientes que impedian operar comercialmente en produccion:

1. Impedir que el cliente controle o fije el precio oficial.
2. Establecer un flujo seguro de confirmacion de peso, tarifa y recepcion por
   personal autorizado.
3. No presentar la tarifa estimada como precio final.
4. Identificar y separar datos QA de una futura base productiva.
5. Mantener intactos tracking, estados, movimientos, roles y seguridad.

## 3. Alcance

Modulo de Encomiendas y Tracking: solicitud desde portal, confirmacion de
recepcion, gestion administrativa, portal del cliente y tracking publico.
No se toco el panel del chofer, la venta de pasajes, boletos, QR de pasajes ni
los pagos no relacionados.

## 4. Restricciones respetadas

- No se reconstruyo el modulo ni se creo un formulario/tracking paralelo.
- No se modifico JWT, BCrypt, nombres de roles ni Spring Security.
- No se cambio la maquina de estados salvo conservar `SOLICITUD_PENDIENTE -> RECIBIDO`.
- No se inventaron tarifas ni reglas comerciales: los valores mostrados se
  rotularon como referenciales.
- No se ejecuto ningun DROP/TRUNCATE/DELETE ni migracion destructiva.
- No se declaro produccion sin evidencia: cada afirmacion tiene su prueba.

## 5. Situacion inicial (hallazgos)

- `EncomiendaClienteRequest` tenia un campo `costo` que el cliente enviaba.
- `solicitarEncomienda` usaba ese `costo` como precio oficial; si venia vacio,
  caia en `calcularCosto = max(peso * 5, 10)`.
- La solicitud creaba un `Pago` marcado `COMPLETADO` de inmediato.
- El frontend (`PasajesPage`) calculaba un "Costo estimado" con tarifas
  hardcodeadas (`NORMAL:10`, `EXPRESS:18`, `FRAGIL:22` + S/2 por kg extra) y lo
  enviaba como `costo`.
- Consecuencia: el cliente decidia cuanto costaba su envio y el sistema lo
  registraba como cobrado.

## 6. Vulnerabilidad cerrada

| Antes | Ahora |
|-------|-------|
| Cliente envia `costo` y se usa como precio | El campo se elimino del DTO; el backend descarta cualquier monto del cliente |
| Se crea `Pago` `COMPLETADO` al solicitar | La solicitud nace sin pago (`pago = null`, costo `0.00`) |
| Precio estimado mostrado como final | Etiquetado "referencial"; el precio final lo confirma agencia |
| `actualizarEstado` permitia `SOLICITUD_PENDIENTE -> RECIBIDO` | Ese endpoint rechaza la transicion; se exige `confirmar-recepcion` |

## 7. Decision de diseno: autoridad del precio en el backend

El backend es la unica autoridad del precio. La solicitud solo transporta datos
descriptivos (origen, destino, destinatario, descripcion, peso declarado,
metodo de pago previsto). El monto oficial se fija en `confirmarRecepcion` con
el peso real y la tarifa validados por personal autorizado. El calculo
volumetrico automatico queda fuera de alcance hasta que INTIWATANA defina su
politica oficial.

## 8. Cambios backend — DTOs

- `EncomiendaClienteRequest`: **eliminado** el campo `costo`. Un cliente viejo
  que lo siga enviando no rompe nada (Jackson ignora propiedades desconocidas).
- `ConfirmarRecepcionEncomiendaRequest` (nuevo): `pesoKgReal` (`@NotNull`,
  `@DecimalMin 0.001`, `@Digits`), `precioOficial` (`@NotNull`, `@DecimalMin 0.01`),
  `metodoPago` (`@NotNull`), `observacion` (`@Size 500`) y
  `recepcionConfirmada` (`@NotNull @AssertTrue`).

## 9. Cambios backend — `solicitarEncomienda`

- `pago(null)`, `.costo(BigDecimal.ZERO)`.
- No crea `Pago`.
- Estado `SOLICITUD_PENDIENTE`, movimiento inicial "pendiente de recepcion
  fisica en sucursal".
- Ya no usa `calcularCosto` para fijar precio.

## 10. Cambios backend — `confirmarRecepcion`

`EncomiendaServiceImpl.confirmarRecepcion(Long id, request)`:

1. Exige estado `SOLICITUD_PENDIENTE`.
2. Exige `recepcionConfirmada == true`.
3. Revalida remitente, destinatario, origen/destino distintos y descripcion.
4. Exige peso real > 0 y precio oficial > 0 (normaliza a 2 decimales).
5. Crea/actualiza el `Pago` con el monto **oficial**, metodo, estado inicial
   (`COMPLETADO`; `CONTRA_ENTREGA` -> `PENDIENTE`) y cajero = operador.
6. Actualiza la encomienda (peso, costo, metodo, pago) y pasa a `RECIBIDO`.
7. Registra `MovimientoEncomienda` con `usuarioResponsable`.
8. Registra auditoria `CONFIRMAR_RECEPCION_ENCOMIENDA`.

## 11. Cambios backend — guarda en `actualizarEstado`

Se agrego una guarda al inicio de `actualizarEstado`: si el estado actual es
`SOLICITUD_PENDIENTE`, lanza `NegocioException` y obliga a usar
`confirmar-recepcion`. Esto cierra el atajo por el endpoint generico.

## 12. Cambios backend — endpoint

`POST /api/v1/encomiendas/{id}/confirmar-recepcion`
`@PreAuthorize("hasAnyRole('ADMIN','CAJERO','VENDEDOR','OPERADOR_ENCOMIENDAS')")`
con `@Valid @RequestBody ConfirmarRecepcionEncomiendaRequest`.

## 13. Modelo de datos y entidades

No se altero el esquema. Se reutilizaron:
- `Encomienda.pago` (nullable) / `costo` / `pesoKg` / `metodoPago`.
- `MovimientoEncomienda.usuarioResponsable` para trazabilidad del operador.
- `Pago` (monto, metodo, estado, cajero, cliente).
- `EncomiendaResponse` (incluye costo) y `TrackingEncomiendaResponse`
  (sin costo ni metodo de pago).

## 14. Seguridad y roles

- Solicitud del portal: `@PreAuthorize("hasRole('CLIENTE')")`.
- Confirmacion de recepcion: solo personal autorizado (ADMIN, CAJERO, VENDEDOR,
  OPERADOR_ENCOMIENDAS). **El CLIENTE y el CHOFER no pueden confirmar** (403
  verificado).
- Confirmacion sin token: 401.
- No se confia en roles enviados desde React: la autorizacion es del backend.

## 15. Frontend — solicitud (`PasajesPage`)

- Titulo "Solicita tu envio"; copy que aclara que la tarifa se confirma en agencia.
- Bloque "Tarifa estimada (referencial)" con `S/ ~` y leyenda "La tarifa final
  la confirma el personal autorizado en agencia".
- Barra lateral "Tarifas referenciales" con `S/ ~` y leyenda.
- Boton "Solicitar envio".
- Se elimino `costo` del payload enviado a `/solicitar`.

## 16. Frontend — carrito y pago

- La encomienda entra al carrito con `precio: 0` y `precioEstimado` (referencial).
- El carrito muestra "Tarifa por confirmar" y "Estimado referencial", y aclara
  que las encomiendas no se cobran ahi.
- El modal de pago muestra "Total pasajes" y el aviso de que la encomienda no se
  cobra ahora; el metodo elegido es solo preferencia.
- Al confirmar, el banner dice "Solicitud de envio registrada. Estado: pendiente
  de recepcion en agencia..." en lugar de "Pago procesado con exito".

## 17. Frontend — cliente (`ClienteDashboard`)

En lista y en el modal de seguimiento, cuando el estado es `SOLICITUD_PENDIENTE`
el costo se muestra como **"Pendiente de confirmacion"**; una vez confirmado se
muestra el monto oficial. (Verificado en navegador.)

## 18. Frontend — admin (`CrudEncomiendas`)

- Se agrego la accion "Confirmar recepcion" (icono) para las solicitudes
  pendientes.
- Modal "Confirmar recepcion" con peso real, metodo de pago, tarifa oficial,
  observacion y casilla obligatoria de confirmacion fisica.
- La tabla y el detalle muestran "Sin tarifa" / "Pendiente de recepcion" mientras
  no haya confirmacion.
- Guarda adicional: `abrirEstado` redirige las solicitudes pendientes al flujo de
  confirmacion.

## 19. Tracking publico y privacidad

`GET /api/v1/encomiendas/tracking/{guia}` sigue siendo publico (sin token) y
**no** expone `costo`, `metodoPago`, `pago` ni `usuarioResponsable`. Verificado:
la respuesta solo contiene guia, estado, descripcion, peso, sucursales,
destinatario, fecha y movimientos (solo estado/observacion/sucursal/fecha).

## 20. Maquina de estados

Se conservo integra. El unico camino hacia `RECIBIDO` desde
`SOLICITUD_PENDIENTE` es la confirmacion de recepcion. El resto de transiciones
(`RECIBIDO -> EN_ALMACEN/CARGADO_EN_BUS/EN_TRANSITO`, etc.) no se modifico.

## 21. Matriz de pruebas ejecutadas

| ID | Caso | Resultado |
|----|------|-----------|
| P01 | Solicitar con `costo` inyectado (9999.99) | PASS: guardado con costo 0, ignorado |
| P02 | `PATCH /{id}/estado` RECIBIDO en pendiente | PASS: 400 con guarda |
| P03 | Confirmar sin casilla de recepcion | PASS: 400 validacion |
| P04 | Confirmar como CLIENTE | PASS: 403 |
| P05 | Confirmar como CHOFER | PASS: 403 |
| P06 | Confirmar como OPERADOR valido | PASS: 200, RECIBIDO |
| P07 | Tracking publico sin token | PASS: 200, sin costo/pago |
| P08 | Segunda confirmacion | PASS: 400 (ya no pendiente) |
| P09 | Confirmar en estado no pendiente | PASS (cubierto por P08) |
| P10 | Precio oficial <= 0 | PASS: 400 validacion |
| P11 | Peso real <= 0 | PASS: 400 validacion |
| P12 | Origen == destino | PASS: 400 |
| P13 | Solicitar sin token | PASS: 401 |
| P14 | Confirmar sin token | PASS: 401 |
| P15 | Pago con monto/estado/cajero correctos | PASS (BD: ids 41,42,43) |
| P16 | Movimiento con `usuarioResponsable` | PASS (mov. 38 y 40) |
| P17 | Auditoria de confirmacion | PASS (tabla `auditoria` ids 316/320/329) |
| P18 | Respuesta de solicitud sin `pago` | PASS |
| P19 | Formato de guia `GUI-YYYYMMDD-NNNNNN` | PASS |
| P20 | Cliente ve "Pendiente de confirmacion" | PASS (navegador) |
| P21 | Carrito no cobra la encomienda | PASS (precio 0 / Total pasajes) |
| P22 | Admin confirma por UI | PASS (navegador, id 38) |
| P23 | Cliente ve precio oficial tras confirmar | PASS (navegador) |
| P24 | Tracking publico accesible sin login | PASS |

## 22. Evidencia de API (extractos reales)

Solicitud (P01) — costo inyectado descartado:
```
POST /api/v1/encomiendas/solicitar -> 201
{"id":36,"numeroGuia":"GUI-20261002-000036","costo":0,
 "estado":"SOLICITUD_PENDIENTE","movimientos":[]}
```

Guarda (P02):
```
PATCH /api/v1/encomiendas/36/estado -> 400
"La recepcion de una solicitud pendiente debe confirmarse con peso real y tarifa oficial"
```

Permisos (P04/P05): `403 No tiene permisos para esta accion`.

Confirmacion (P06):
```
POST /api/v1/encomiendas/36/confirmar-recepcion -> 200
{"estado":"RECIBIDO","pesoKg":4.5,"costo":25.00,
 "movimientos":[{...,"estadoNuevo":"RECIBIDO","usuarioResponsable":"Jhon"}]}
```

Tracking (P07): respuesta sin `costo` ni `metodoPago`.

## 23. Evidencia de base de datos

```
36 | RECIBIDO | 25.00 | 4.500 | pago 41 | 25.00 COMPLETADO EFECTIVO | cajero 25
37 | RECIBIDO | 15.00 | 2.100 | pago 42 | 15.00 COMPLETADO YAPE     | cajero 25
38 | RECIBIDO | 20.00 | 3.400 | pago 43 | 20.00 COMPLETADO EFECTIVO | cajero 1
```

Los pagos se crean solo en la confirmacion, con el monto oficial y el cajero
responsable.

## 24. Lint, build y compilacion

- Frontend `npm run lint`: sin errores.
- Frontend `npm run build`: OK (assets generados; warning de chunk > 500 kB ya
  existente).
- Backend `mvn -o clean verify`: **BUILD SUCCESS**, `Tests run: 54, Failures: 0,
  Errors: 0, Skipped: 0`. Jar reconstruido y reiniciado.
- Navegador: 0 errores de consola, 0 peticiones fallidas en las tres pantallas
  verificadas (solicitud, panel cliente, panel admin).

## 25. Datos QA y separacion

Ver `reports/DATOS-QA-ENCOMIENDAS.md` para el inventario completo.

- QA claro detectado: ids **23, 25, 26, 27, 28, 29, 31, 33, 35, 36, 37, 38**.
- Datos ambiguos/demo (no tocar sin decision del negocio): **21, 22, 24, 30, 32, 34**.
- Se entrega `reports/sql/LIMPIEZA-QA-ENCOMIENDAS-PROPUESTA.sql`, transaccional,
  con respaldo en tablas de archivo, marca reversible `es_qa` y **ROLLBACK por
  defecto**. No se ejecuto.

## 26. Riesgos y limitaciones

1. Las tarifas por tipo/ruta siguen siendo referenciales sin tabla de tarifas
   oficial; recomendable que INTIWATANA defina una tabla de precios por ruta/peso.
2. El calculo volumetrico automatico no esta implementado (fuera de alcance).
3. Los ids 21/22/24/30/32/34 requieren decision del negocio antes de separarse.
4. No se configuro un `es_qa` en el modelo JPA: la separacion definitiva debe
   coordinarse con una migracion autorizada.
5. La auditoria de confirmacion se verifico en la tabla `auditoria`; no se
   agrego una prueba automatizada que la cubra en el pipeline.

## 27. Veredicto y recomendaciones

**Veredicto: A — Apto para produccion en el flujo comercial de encomiendas,
con una salvaguarda operativa.**

El precio deja de estar bajo control del cliente, existe un flujo de
confirmacion por personal autorizado y la comunicacion deja claro que la tarifa
es referencial hasta la recepcion en agencia. El tracking, los estados, los
movimientos, los roles y la seguridad se mantienen intactos y verificados.

Salvaguardas antes de salir a produccion:
1. Definir y cargar la tabla oficial de tarifas por ruta/peso.
2. Ejecutar (con respaldo y autorizacion) la separacion de datos QA propuesta.
3. Capacitar al personal de agencia en el uso del boton "Confirmar recepcion".
4. Agregar pruebas automatizadas del modulo (unitarias de `confirmarRecepcion` e
   integracion del endpoint) al pipeline.
