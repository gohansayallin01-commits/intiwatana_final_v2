# FLUJO SOLICITUD DE ENCOMIENDA -> RECEPCION -> TRACKING - VERIFICACION

Fecha: 2026-10-04
Alcance: **exclusivamente el modulo de ENCOMIENDAS**.
Base de pruebas: `intiwatana_recorrido_qa` (aislada). `transporte_bd` no fue modificada.
Migracion nueva: `V5__encomienda_solicitud_sol_y_gui.sql` (V1-V4 sin tocar).

---

## 1. Regla de negocio vigente

| Actor | Puede | No puede |
|---|---|---|
| Publico (cliente del portal) | Registrar una **solicitud previa**: origen, destino, remitente, destinatario (nombre y telefono) y descripcion del contenido | Declarar peso, dimensiones, volumen, tarifa, metodo de pago ni pago; confirmar recepcion; emitir guia; mover el estado |
| Personal autorizado (ADMIN / CAJERO / VENDEDOR / OPERADOR_ENCOMIENDAS) | Registrar el alta **presencial**, confirmar recepcion, medir peso y dimensiones reales, fijar tarifa, registrar el pago en agencia, emitir la guia oficial y mover el estado | Registrar la solicitud en nombre del cliente |

Identificadores:

- La solicitud previa se identifica con una **referencia `SOL-AAAA-NNNNNN`**, que **no es guia** y **no habilita tracking**.
- La **guia oficial `GUI-AAAAMMDD-NNNNNN`** se emite **unicamente** cuando el personal autorizado confirma la recepcion fisica, el peso, las dimensiones, la tarifa y el pago.

Estados por el camino visible publicamente: `SOLICITUD_PENDIENTE` -> `RECIBIDO` ->
`EN_ALMACEN` -> `CARGADO_EN_BUS` -> `EN_TRANSITO` -> `LLEGADO_A_DESTINO` ->
`EN_DESTINO` -> `LISTO_ENTREGA` -> `ENTREGADO`.

Mensaje exigido, textual en el portal:
> Esta es una solicitud previa. El peso, las dimensiones, la tarifa, el pago y la guía oficial serán confirmados por el personal autorizado cuando el paquete sea recibido físicamente en una agencia INTIWATANA.

---

## 2. Cambios aplicados

### 2.1 Backend: una sola operacion de oficializacion

| Archivo | Cambio |
|---|---|
| `service/impl/EncomiendaServiceImpl.java` | `confirmarRecepcionOperativa(...)` es la **unica** operacion que convierte una solicitud en encomienda oficial: calcula el volumen, crea **un solo pago `COMPLETADO`**, emite **una sola guia `GUI`** y registra **un solo movimiento inicial**. La comparten el registro presencial y la confirmacion de una solicitud web, de modo que ambos caminos dejan el mismo resultado. |
| idem | `solicitarEncomienda()` y `registrar()` generan la referencia `SOL` **antes** del `INSERT` (la constraint la exige desde que nace la fila). |
| idem | `solicitarEncomienda()` nace sin `numeroGuia`, `pesoKg`, medidas, `volumenM3`, `costo`, `metodoPago` ni `pago` (todo `NULL`). |
| idem | `confirmarRecepcion()` bloquea la fila (`findByIdForUpdate`) y es **idempotente**: si ya fue oficializada devuelve la guia existente sin crear otro pago, guia ni movimiento. |
| idem | `registrar()` resuelve la idempotencia en **dos pasos**: consulta previa por clave (doble clic o reintento) y, si dos agentes registran **de verdad a la vez**, captura el `DataIntegrityViolationException` del indice unico y devuelve la encomienda que gano la carrera. Sin ese manejo el perdedor recibia un `500` con el error SQL filtrado. Se ejecuta con `TransactionTemplate` para poder releer la clave en una transaccion nueva tras el rollback. |
| idem | `actualizarEstado()` (endpoint generico) **rechaza** oficializar una solicitud: solo `confirmarRecepcion` puede emitir la guia. |
| idem | `validarDatosOperativos()` valida en el backend peso, medidas, tarifa y metodo de pago, y **rechaza `CONTRA_ENTREGA`**. No hay tarifario automatico. |
| idem | `buscarSolicitudPorReferencia()` exige `SOL-\d{4}-\d{6,}` y, si la referencia ya fue oficializada, responde con su guia en lugar de devolverla como pendiente. |
| `repository/EncomiendaRepository.java` | `findByReferenciaSolicitudIgnoreCase`, `findByClaveIdempotencia` y `siguienteCorrelativoSolicitud()` (`SELECT nextval('seq_encomienda_sol')`). |
| `entity/Encomienda.java`, `dto/request/EncomiendaRequest.java` | Nueva columna y campo `claveIdempotencia` (idempotencia del alta presencial). |
| `service/impl/ClienteServiceImpl.java` | `crear()` ejecuta el insert en una transaccion aislada (`TransactionTemplate`) y recupera la colision por DNI/RUC en lugar de marcar la transaccion entera como fallida. |
| `controller/ClienteController.java` | `GET/POST /api/v1/clientes` admiten `VENDEDOR` y `OPERADOR_ENCOMIENDAS`, igual que ya permitia `EncomiendaController`, para que el alta presencial reutilice clientes sin Ampliar el radio de escritura. `PUT` y `DELETE` siguen restringidos. |

### 2.2 Privacidad

| Archivo | Cambio |
|---|---|
| `service/impl/EncomiendaServiceImpl.java` | `consultarTracking()` **rechaza** cualquier referencia `SOL-` (tambien en minusculas): una solicitud no tiene guia, peso, tarifa ni movimientos. |
| idem | `toTrackingResponse()` ya no expone el nombre del destinatario, y los movimientos usan el mensaje publico del estado (`observacionPublica`) en lugar del detalle interno (tarifa, medidas, personal). |
| `dto/response/TrackingEncomiendaResponse.java` | Eliminado `destinatarioNombre`. El DTO publico queda en estado, descripcion, peso, sucursal origen/destino, fecha de registro y movimientos publicos. |
| idem | El panel de pendientes enmascara documento y telefono, y `sinTelefono(...)` retira de las observaciones el segmento generado `Tel. contacto:`. |

### 2.3 Migracion `V5__encomienda_solicitud_sol_y_gui.sql`

1. Anade `referencia_solicitud`, `remitente_telefono`, `clave_idempotencia`, `largo_cm`, `ancho_cm`, `alto_cm` (todas las demas columnas ya existian en V1).
2. **Backfill** de las solicitudes `SOLICITUD_PENDIENTE` preexistentes: les asigna su referencia `SOL` y les **libera los datos oficiales que nunca lo fueron** (`numero_guia`, `peso_kg`, `volumen_m3`, `costo`, `metodo_pago`, `pago_id`). No toca filas ya recibidas ni elimina informacion.
3. Crea `seq_encomienda_sol` y la posiciona por encima de `MAX(id)` para no chocar con el backfill.
4. `uq_encomiendas_referencia_solicitud` y `uq_encomiendas_clave_idempotencia` (indices unicos parciales).
5. `chk_encomienda_solicitud_sin_datos_oficiales`: una solicitud pendiente jamas puede tener guia, peso, medidas, volumen, tarifa, metodo de pago ni pago.
6. `uq_clientes_dni_ruc` si los datos existentes lo permiten (no se destruye informacion si hubiera duplicados historicos).

**Aplicada de verdad.** `V5` no se quedo en revision estatica: se ejecuto con Flyway 10.10.0
contra `intiwatana_recorrido_qa` (PostgreSQL 17.5) mediante un runner aislado, con una fila de
fixture propia. Resultado **20/20 comprobaciones OK, 0 fallos**: existencia de las columnas nuevas,
secuencia creada y correctamente posicionada por encima del `MAX(id)` (para que no colisione con el
backfill), backfill de las solicitudes preexistentes liberando datos oficiales que nunca lo fueron,
`chk_encomienda_solicitud_sin_datos_oficiales` activa y verificada con una violacion real,
`uq_encomiendas_clave_idempotencia` verificada con un `INSERT` duplicado rechazado
(*"Ya existe la llave (clave_idempotencia)=..."*), y unicidad de la referencia `SOL`. La fila de
fixture y todos los datos de prueba se eliminaron al terminar.

Aviso del tooling: Flyway 10.10.0 declara soporte hasta PostgreSQL 16 y advierte que la 17.5 es
superior; la migracion aplico sin errores.

### 2.4 Secuencia en perfiles sin Flyway

`config/SecuenciaSolicitudInitializer.java`: en `dev` el esquema lo construye Hibernate
(`ddl-auto: update`) y **Flyway esta desactivado**, por lo que `seq_encomienda_sol` no existia y
todo el flujo `SOL` fallaba con `relation "seq_encomienda_sol" does not exist`. Este componente
crea la secuencia **solo si falta** y **solo si Flyway esta desactivado** (`@Profile("!prod")`);
nunca reinicia una secuencia existente. La fuente de verdad sigue siendo `V5`.

### 2.5 Frontend

| Archivo | Cambio |
|---|---|
| `components/PasajesPage.jsx` | Solicitud publica sin peso ni dimensiones; mensaje exigido visible en el formulario y en "¿Como funciona?". Boton **"SOLICITAR ENVIO"**. |
| `components/AdminDashboard.jsx` | Panel de encomiendas unico: selector reutilizable de clientes, **"REGISTRAR ENCOMIENDA PRESENCIAL"** con **clave de idempotencia estable por apertura del modal**, **"RECUPERAR SOLICITUD WEB PENDIENTE"** por referencia `SOL`, **"CONFIRMAR RECEPCION Y REGISTRAR PAGO"** con peso, medidas, volumen calculado (solo lectura), tarifa y metodo de pago. Se elimino el modal obsoleto de confirmacion (`confirmar` / `abrirConfirmar`). Mientras la solicitud esta pendiente: **"PENDIENTE DE RECEPCION EN AGENCIA"**, "Se define en agencia", "Se mide en agencia". |
| `components/ClienteDashboard.jsx` | "Mis encomiendas" busca por `GUI`, `SOL` o contenido; muestra `GUI`, `SOL` o `#id`; las pendientes muestran **"Se mide en agencia" / "Se define en agencia"** y boton deshabilitado **"Pendiente de recepcion en agencia"**; no abren tracking de una `SOL`. `TrackingModal` excluye `SOLICITUD_PENDIENTE` y muestra el peso sin `null`. |
| `App.jsx` | Tracking publico: `GUIA_REGEX = /^GUI-\d{8}-\d{6}$/`, `TRACK_PASOS` arranca en `RECIBIDO`, sin nombre del destinatario y peso null-safe. |

---

## 3. Pruebas automatizadas: **81/81 unitarias + 14/14 de integracion HTTP**

### 3.1 Gate unitario: 81/81 PASS

`mvn clean verify` -> **BUILD SUCCESS**, 81 pruebas, 0 fallos (54 previas + 27 nuevas).

`backend/bus-system/src/test/java/com/transporte/sistema/service/impl/EncomiendaSolicitudTest.java`
(27 pruebas nuevas, con dobles de repositorio):

| Grupo | Cobertura |
|---|---|
| Solicitud web | La referencia `SOL` se asigna **antes** del `INSERT` y con formato `SOL-AAAA-NNNNNN`; la fila nace sin guia, peso, medidas, volumen, tarifa, metodo de pago ni pago; rechaza origen = destino; exige perfil de cliente; sin correlativo falla de forma explicita en vez de inventar una referencia. |
| Confirmacion de recepcion | Oficializa en una sola operacion (un pago `COMPLETADO`, una guia `GUI`, un movimiento `SOLICITUD_PENDIENTE -> RECIBIDO`, volumen `0.0060` m3 calculado por el backend, auditoria `FIJAR_TARIFA_ENCOMIENDA`); la segunda confirmacion devuelve la misma guia sin duplicar; una encomienda no pendiente no se confirma; rechaza `CONTRA_ENTREGA` y tarifa cero. |
| Registro presencial | Nace con referencia `SOL` y termina oficializado por la misma operacion; la misma clave de idempotencia devuelve la encomienda ya creada sin volver a guardar; la clave se normaliza antes de buscar; sin clave no consulta el indice unico; rechaza remitente = destinatario. |
| **Carrera de idempotencia** | Si dos altas compiten a la vez, el `INSERT` perdedor choca con el indice unico y el servicio devuelve la encomienda ganadora (**no** un `500`), sin crear pago ni movimiento. Este caso fallo antes de la correccion descrita en 2.1. |
| Transiciones | Una `SOL` no se oficializa por `PATCH /{id}/estado`; `RECIBIDO -> EN_ALMACEN` sí; rechaza saltarse a `ENTREGADO`. |
| Panel de solicitudes | Enmascara documento y telefono, quita el telefono de las observaciones y conserva el resto; rechaza formatos invalidos; una referencia ya oficializada responde con su guia; el listado filtra por `SOLICITUD_PENDIENTE`. |
| Tracking publico | `SOL-` rechazada (tambien en minusculas) sin tocar la base; el JSON de la respuesta no contiene remitente, destinatario, telefono, documento, tarifa, costo, usuario ni auditoria; los movimientos usan el mensaje publico; el DTO no expone ninguna propiedad de persona o de personal. |

Suite previa preservada: `PropiedadChoferValidatorTest`, `ReporteServiceImplTest`, `CsvExportadorTest`.

### 3.2 Integracion HTTP contra PostgreSQL real: 14/14 PASS

`backend/bus-system/src/test/java/com/transporte/sistema/EncomiendaFlujoIT.java`

Arranca la aplicacion completa con perfil `test` (Flyway construye el esquema y Hibernate
**valida** el mapeo, asi que un desalineamiento entre entidad y migracion rompe el arranque),
sirve la API en un puerto aleatorio y la ejercita por HTTP con tokens JWT reales. El test **aborta
si la base no termina en `_qa`**.

La tabla siguiente lista los **16 escenarios** verificados; los **14 tests** agrupan algunos de
ellos en una misma prueba (por ejemplo, la guia, el volumen, el pago y el movimiento se comprueban
juntos dentro de la misma confirmacion).

| # | Escenario | Resultado |
|---|---|---|
| 1 | Login de `admin` y alta de un cliente publico con su propio usuario `CLIENTE` | PASS |
| 2 | `POST /solicitar` -> `201` con `SOL-2026-000017` y **todos** los datos oficiales en `null` (verificado tambien leyendo la fila cruda) | PASS |
| 3 | El cliente publico intenta `confirmar-recepcion` y `PATCH /estado` | `403` en ambos |
| 4 | `GET /tracking/{SOL-...}` | `400` "Formato de guia invalido" |
| 5 | Panel: recuperar la solicitud por referencia | PASS |
| 6 | Panel: documento del remitente `****1288`, telefono del destinatario `****0002`, sin telefono en observaciones, sin guia ni costo | PASS |
| 7 | `PATCH /estado` con una `GUI` como referencia y con `RECIBIDO` sobre una `SOL` | `400` en ambos; la solicitud sigue pendiente y sin guia |
| 8 | `confirmar-recepcion` con peso, medidas y tarifa | `GUI-20261004-000023`, `RECIBIDO`, volumen `0.006` calculado por el backend, peso `3.2`, tarifa `25.00`, referencia `SOL` conservada |
| 9 | Integridad de la oficializacion | **exactamente 1 pago** `COMPLETADO` y **1 movimiento** en toda la base |
| 10 | Repetir la confirmacion | misma guia, sigue habiendo 1 pago y 1 movimiento |
| 11 | Consultar la solicitud ya oficializada | `400` con el mensaje que **incluye la guia oficial** |
| 12 | Tracking publico de la guia | sin `destinatario`, `remitente`, `telefono`, `dniRuc`, `costo`, `tarifa`, `usuario`, `cajero` ni `auditoria`; expone estado y sucursal de origen |
| 13 | Alta presencial | nace con referencia `SOL` y queda oficializada en el mismo paso; reintento con la misma clave devuelve la misma encomienda y la misma guia; clave distinta crea otra encomienda con otra guia |
| 14 | **Carrera real con dos hilos** en el alta presencial, misma clave | ninguna respuesta `5xx`; **una sola encomienda** y **una sola guia** en BD; el perdedor devuelve la ganadora |
| 15 | **Carrera real con dos hilos** confirmando la misma solicitud | `200` y `200` con la **misma guia**, **un solo pago** y **un solo movimiento** |
| 16 | Limpieza e aislamiento | `restantes = 0` encomiendas, igual al conteo previo |

### 3.3 Aislamiento verificado

Inventario de `intiwatana_recorrido_qa` **despues** de las tres ejecuciones de la integracion:
`encomiendas = 0`, `movimientos_encomienda = 0`, `clientes = 4`, `pagos = 4`, `usuarios = 7`
(`admin`, `cajero1`, `vendedor1`, `operador1`), idéntico al estado previo. Solo permanece aplicado
`V5`, que es intencional.

`transporte_bd` **no fue modificada** (verificado en solo lectura): 18 encomiendas, **sin**
`flyway_schema_history`, sin `clave_idempotencia`, sin `seq_encomienda_sol` y sin la constraint.
Se espera que en `dev` el esquema lo construya Hibernate al arrancar, pero la fuente de verdad
sigue siendo `V5`.

### 3.4 Gates

| Gate | Resultado |
|---|---|
| `mvn clean verify` | **BUILD SUCCESS** - 81/81 tests |
| `mvn test -Dtest=EncomiendaFlujoIT` (solo `_qa`) | **BUILD SUCCESS** - 14/14 |
| `npm run lint` | sin errores |
| `npm run build` | correcto (`dist/assets/index-BBRG5cWu.js`); unico aviso: chunk > 500 kB |

---

## 4. QA de API: revalidada. QA de navegador: pendiente

La QA de 31/31 PASS documentada antes validaba la variante **anterior** del flujo (solicitud con
peso estimado y `costo = 0.00`) y **no cubria** la separacion `SOL` / `GUI`.

Los nueve escenarios de aquella lista se han revalidado **por HTTP contra la base real**, con los
tokens y los estados que se pedian. Los ocho primeros quedaron cubiertos por
`EncomiendaFlujoIT` (seccion 3.2); el noveno, el aislamiento, quedo demostrado en 3.3.

Resultado: **14/14 PASS**, tres ejecuciones consecutivas sin residuos en la base.

**Lo que sigue pendiente: la QA en navegador** (responsive, navegacion por teclado, foco visible,
`label`/asociaciones `aria` a cada control y objetivos tactiles de 42-44 px). En esta sesion no se
pudo levantar ni el frontend ni un segundo backend en segundo plano: el puerto `5173` esta libre y
el arranque en background del runner no es viable en este entorno. Hay que repetirla a mano con
`npm run dev` y el backend en `dev`.

Nota de alcance relevante: la **solicitud publica** (`POST /solicitar`) no recibe clave de
idempotencia; su proteccion contra el doble envio es del lado del portal (boton bloqueado mientras
la peticion esta en curso). La clave de idempotencia del backend protege el **alta presencial**, que
es donde un doble clic duplicaria una guia y un pago. Las dos confirmaciones simultaneas de una
solicitud si estan protegidas en el servidor, por el bloqueo pesimista de `confirmarRecepcion`.

---

## 5. Fuera de alcance (no modificado)

Reservas, pagos de pasajes, boletos, QR, Mis Reservas, Panel del Cajero, Panel del Chofer, doble
venta y backend de pasajes. No se agrego `Encomienda2`, `Solicitud2`, `Tracking2`, `Guia2` ni
ningun panel o endpoint duplicado. No se modificaron las entidades compartidas `Pago`, `Boleto`,
`Asiento`, `Viaje` ni `Retencion`. V1-V4 no se editaron y el enum `MetodoPago` no se toco.
