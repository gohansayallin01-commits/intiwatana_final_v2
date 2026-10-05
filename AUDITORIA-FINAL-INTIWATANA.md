# Auditoría final — Sistema de Transportes INTIWATANA S.R.L.

**Fecha:** 2026-01
**Alcance acordado:** Reportes + todo lo crítico + refactor de datos
**Alcance excluido:** ninguno de los módulos funcionales existentes; se preservó el flujo de venta, asientos, QR, encomiendas y tracking.

---

## 1. Estado del entregable

| Elemento | Estado |
|---|---|
| Compilación del backend (`mvn compile`) | **VERIFICADO** — sin errores |
| Build del frontend (`npm run build`) | **VERIFICADO** — 1743 módulos, sin errores |
| Lint del frontend (`npm run lint`) | **VERIFICADO** — 0 errores y 0 avisos |
| Pruebas automatizadas del backend (`mvn clean verify`) | **VERIFICADO** — 38 pruebas, 0 fallos (ver §5.2) |
| Pruebas funcionales contra la base de datos | **NO VERIFICADO** — bloqueado por credencial de PostgreSQL |
| Respaldo `database/intiwatana_transporte_bd.backup` | **PENDIENTE** — no generado (ver §4) |
| Módulo de reportes | **IMPLEMENTADO** — lógica con pruebas unitarias; cifras no verificadas contra datos reales |
| Migración de base de datos | **PENDIENTE** — script escrito, sin ejecutar |

> **Advertencia importante:** nada de lo que aparece como «verificado» se ha
> probado ejecutando la aplicación. Solo se han comprobado compilación, build,
> lint y las pruebas unitarias de lógica pura. El comportamiento en ejecución
> —incluidas las consultas SQL— requiere el respaldo y una base de datos
> accesible.

---

## 2. Correcciones implementadas

### 2.1 Seguridad

| # | Hallazgo | Corrección | Archivo |
|---|---|---|---|
| S-1 | `/api/v1/viajes/**` era público para **todos** los verbos, incluidos escritura y borrado | Se restringió a lecturas anónimas; el resto exige sesión | `config/SecurityConfig.java` |
| S-2 | Cualquier usuario autenticado (incluso un cliente) podía crear registros de pago por importes arbitrarios en `POST /api/v1/pagos`, falseando los ingresos | Restringido a `ADMIN` y `CAJERO`. Verificado que solo el panel de administración lo consume | `controller/PagoController.java` |
| S-3 | El tracking público devolvía el DTO completo: remitente, documentos, teléfono y el personal responsable de cada movimiento | Nuevo DTO `TrackingEncomiendaResponse` con solo lo necesario para seguir el envío | `dto/response/TrackingEncomiendaResponse.java`, `controller/EncomiendaController.java` |
| S-4 | Si el usuario del token había sido eliminado o desactivado, el filtro lanzaba una excepción y devolvía HTTP 500 | Se captura `AuthenticationException`, se limpia el contexto y se continúa como anónimo | `security/JwtAuthenticationFilter.java` |
| S-5 | Contraseña de la base de datos y secreto JWT escritos en el código fuente | Movidos a variables de entorno (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `CORS_ALLOWED_ORIGINS`) con los valores locales como valor por defecto, para no romper el entorno de desarrollo | `resources/application.yml` |
| S-6 | El secreto JWT se decodificaba en Base64 y se derivaba la clave en **cada** llamada a `getSigningKey()` | Se deriva una sola vez en `@PostConstruct`. Además avisa en el log si se está usando el secreto de desarrollo | `security/JwtUtil.java` |

### 2.2 Corrección de datos y lógica de negocio

| # | Hallazgo | Corrección | Archivo |
|---|---|---|---|
| D-1 | `cancelarPorUsuario` comparaba `getRol().getNombre()` (un enum `RolNombre`) contra cadenas `"ROLE_ADMIN"`. La comparación **siempre** daba `false`, así que un administrador podía recibir «no tienes permiso» | Comparación contra el enum | `service/impl/BoletoServiceImpl.java` |
| D-2 | En la misma ruta, `boleto.getCliente().getUsuario().getUsername()` lanzaba NullPointerException cuando el cliente no tenía cuenta asociada (clientes creados por un cajero) | Comprobación de nulos antes de desreferenciar | `service/impl/BoletoServiceImpl.java` |
| D-3 | El escaneo de QR leía el boleto sin bloquear la fila: dos operarios podían validar el mismo boleto a la vez y ambos Cobraban | Nuevo `findByNumeroBoletoForUpdate` con bloqueo pesimista (`SELECT … FOR UPDATE`) | `repository/BoletoRepository.java`, `service/impl/BoletoServiceImpl.java` |
| D-4 | Las encomiendas del portal guardaban **al remitente** en el campo `destinatario` como marcador de posición, porque el destinatario real no es un Cliente del sistema. Todos los reportes mostraban al emisor como receptor | `destinatario_id` pasa a ser opcional y se añade `destinatario_nombre` con el nombre informado por el remitente | `entity/Encomienda.java`, `service/impl/EncomiendaServiceImpl.java` |
| D-5 | El pago de una encomienda «contra entrega» quedaba en `PENDIENTE` para siempre, y los otros métodos tampoco se marcaban como `COMPLETADO`, así que ningún pago se contabilizaba en los ingresos | El estado inicial depende del método: `PENDIENTE` para contra entrega, `COMPLETADO` en el resto | `service/impl/EncomiendaServiceImpl.java` |

### 2.3 Frontend

| # | Hallazgo | Corrección | Archivo |
|---|---|---|---|
| F-1 | **Bucle infinito de peticiones.** `showToast` se creaba en cada render; los módulos hijos la usan dentro de `useCallback` y cargan con `useEffect([cargar])`. Ante cualquier fallo de red: error → `showToast` → re-render del padre → nueva `cargar` → otra petición, en bucle | `showToast` estabilizada con `useCallback` y temporizador limpiado al desmontar | `components/AdminDashboard.jsx` |
| F-2 | «Mi cuenta» navegaba a `/dashboard`, **ruta que no existe** en `App.jsx`; la ruta comodín `*` devolvía al inicio y perdía el contexto del panel | Se resuelve la ruta según el rol (`/admin`, `/chofer` o `/cliente`) | `components/PasajesPage.jsx` |
| F-3 | **Banner de éxito falso.** Al pagar el carrito, si **todos** los ítems fallaban, se vaciaba el carrito y se mostraba «¡Compra exitosa!» | Si hay errores no se vacía el carrito ni se muestra el éxito; se informa cuántos ítems fallaron y se conservan para reintentar | `components/PasajesPage.jsx` |
| F-4 | El modal de cancelación calculaba `Date.now()` durante el render (impureza) y además colocaba hooks después de un retorno temprano | Fecha límite calculada en un efecto, con los hooks antes de cualquier `return` | `components/ClienteDashboard.jsx` |
| F-5 | Al fallar la carga de movimientos de una encomienda se silenciaba el error (`catch {}`) y se mostraba «Sin movimientos registrados», que es un mensaje falso | Se distingue error de «aún no hay movimientos» y se muestra el motivo | `components/ClienteDashboard.jsx` |
| F-6 | La pantalla de tracking no contemplaba el nuevo `destinatarioNombre` de texto plano | Añadido a la cadena de respaldo de nombres | `App.jsx` |
| F-7 | Variables declaradas y nunca usadas | Eliminadas | `AdminDashboard.jsx`, `CrudAsientos.jsx` |

---

## 3. Módulo de reportes — IMPLEMENTADO

Consultas agregadas reales sobre PostgreSQL. No hay datos de ejemplo, ni valores
calculados en el frontend, ni atajos que devuelvan constantes.

### 3.1 Criterio de fechas

Se aplicó un criterio **consistente y documentado**, porque mezclar fechas
produce cifras que no cuadran entre pantallas:

| Concepto | Ancla temporal | Exclusiones |
|---|---|---|
| Ingresos | Fecha del pago | Solo pagos en estado `COMPLETADO` (el contra entrega cuenta al entregar) |
| Viajes y boletos | Fecha de **salida** del viaje | Viajes y boletos cancelados |
| Encomiendas | Fecha de registro | Registros dados de baja |

### 3.2 Endpoints (`/api/v1/reportes`, todos `ADMIN`)

| Endpoint | Devuelve |
|---|---|
| `GET /resumen` | Indicadores del período: viajes, boletos, ingresos, ocupación, encomiendas, peso, desglose por método |
| `GET /ingresos` | Total, desglose pasajes/encomiendas y por método de pago |
| `GET /ingresos/serie-diaria` | Serie temporal día a día |
| `GET /pasajes` | Boletos, ingresos, ticket promedio y ventas por ruta |
| `GET /ocupacion` | Ocupación por viaje con porcentaje |
| `GET /encomiendas` | Totales, desglose por estado y por corredor |
| `GET /sucursales` | Actividad consolidada por sucursal |

Cada uno tiene su equivalente `…/csv`.

### 3.3 Exportación CSV

`util/CsvExportador.java` genera CSV con las alertas de Excel cubiertas:

- **UTF-8 con BOM**, para que los acentos («importe», número de guía) se vean bien.
- **Comillas dobles escapadas** según RFC 4180.
- **Neutralización de fórmulas**: los valores que empiezan por `=`, `+`, `-` o `@`
  se entrecomillan, evitando que Excel los ejecute al abrir el archivo.
- El `Content-Disposition` lleva el nombre sugerido, y el frontend lo respeta al
  descargar.

### 3.4 Pantalla

`components/ReportesPage.jsx`, integrada en el panel de administración con el
elemento de menú «Reportes». Incluye selector de período (hoy / 7 días / 30 días /
mes actual / fechas manuales), indicadores, cinco pestañas, gráfico de ingresos
diarios y descarga CSV de cada tabla.

**No se añadió ninguna librería de gráficos**: el gráfico es HTML y CSS, en línea
con el resto de la aplicación.

### 3.5 Nota sobre la agregación

La serie diaria se agrupa en Java, no en SQL. HQL no ofrece un truncamiento de
fecha portable, y la alternativa sería una consulta nativa con
`date_trunc(...)` atada a los nombres físicos de columnas, que no se han podido
verificar contra la base de datos real. Ante la duda se eligió la opción que
depende del mapeo de la entidad y no de nombres de columnas.

---

## 4. Estado del respaldo de base de datos — PENDIENTE

**No se ha generado ningún respaldo.** El bloqueo es exclusivamente de
credencial. Diagnóstico ejecutado sobre el servidor local:

| Comprobación | Resultado |
|---|---|
| Servicio `postgresql-x64-17` | Activo |
| Puerto 5432 | Escuchando en `0.0.0.0` y `::` |
| Herramientas `psql` / `pg_dump` / `pg_restore` | Presentes — `psql (PostgreSQL) 17.5` |
| Conexión IPv4 `127.0.0.1` con la clave de `application.yml` | **FALLA** — `FATAL: la autenticación password falló para el usuario «postgres»` |
| Conexión IPv6 `::1` con la misma clave | **FALLA** — mismo error |
| `pg_hba.conf` | Todas las líneas activas exigen `scram-sha-256`; **no hay ninguna regla `trust`** |
| `.pgpass` en `%APPDATA%\postgresql`, `%USERPROFILE%` y directorio de datos | No existe |
| Variables `PGPASSWORD` / `DB_PASSWORD` / `DB_URL` | No definidas |

`application.yml` declaraba `password: <REDACTADO>` como valor por defecto para
desarrollo, y es rechazada. Con `scram-sha-256` y sin regla `trust` no existe
ninguna vía alternativa de autenticación local: **hace falta la contraseña real
del rol `postgres`**. No se ha modificado `pg_hba.conf` ni se ha reseteado la
contraseña, porque ambas cosas requieren autorización expresa.

> Con `scram-sha-256` un rol inexistente produce el mismo error que una
> contraseña incorrecta. Este diagnóstico **no confirma** que el rol `postgres`
> exista ni que la base `transporte_bd` esté creada; solo constata que la
> autenticación falla.

El procedimiento completo paso a paso está en `database/README-BACKUP.md`, que
además incluye este mismo diagnóstico.

Comprobado también que **no existe ningún otro respaldo en el proyecto**: la
búsqueda de `*.backup`, `*.dump`, `*.sql`, `*.tar`, `*.bak` y `*.csv` fuera de
`node_modules`, `target` y `dist` solo devuelve los dos scripts SQL del backend.
La carpeta `database/` contiene únicamente el `README-BACKUP.md`.

Lo que falta para cerrar este punto:

1. Disponer de la credencial válida del usuario `postgres`.
2. Ejecutar `pg_dump -Fc` → `database/intiwatana_transporte_bd.backup`.
3. Validar con `pg_restore --list`.
4. Restaurar en `transporte_bd_restore_test` y **comparar el número de filas**
   tabla por tabla con el original.
5. Eliminar únicamente la base temporal.

**Debe existir un solo archivo de respaldo.** No se generarán SQL planos ni
copias adicionales.

---

## 5. Pendientes y riesgos abiertos

### 5.1 Bloqueos

| Tema | Detalle |
|---|---|
| Respaldo | Requiere la credencial de PostgreSQL |
| Migración de esquema | Requiere respaldo verificado |
| Verificación funcional | Requiere base de datos accesible |

### 5.2 Pruebas automatizadas

`mvn clean verify` ejecuta **38 pruebas, 0 fallos**. El proyecto sí tiene ahora
`src/test`, con dos clases:

| Clase | Pruebas | Qué cubre |
|---|---|---|
| `util/CsvExportadorTest` | 10 | BOM UTF-8, cabeceras legibles, comillas RFC 4180, neutralización de fórmulas (`=`, `+`, `-`, `@`), notación decimal plana, importes nulos, formato de fecha `dd/MM/yyyy HH:mm` |
| `service/impl/ReporteServiceImplTest` | 28 | Período por defecto de 30 días, rechazo de rango invertido, separación de ingresos por origen, agrupación por día y orden cronológico, ticket promedio con redondeo, división por cero, disponibles negativos ante sobreventa, nulos en agregados, orden del ranking de sucursales, cabecera y nombre de archivo de cada CSV |

Estas pruebas **no** sustituyen la verificación contra PostgreSQL: los
repositorios se sustituyen por dobles, así que el JPQL de los reportes sigue
sin ejecutarse. Ver §4.

Durante la escritura de estas pruebas se detectaron y corrigieron tres defectos
reales de `CsvExportador`:

| Defecto | Por qué importaba |
|---|---|
| `formatear()` nunca recibía un `LocalDateTime` | El round-trip por Jackson convertía la fecha a texto ISO, así que la rama de formato no se ejecutaba y en producción las fechas saldrían como `2026-01-05T08:00:00`. Además fallaba con cualquier `ObjectMapper` sin `JavaTimeModule`. Se resolvió leyendo las propiedades por getters |
| `etiqueta()` no capitalizaba la primera letra | Las cabeceras salían como `metodo,total,momento` en vez de `Metodo,Total,Momento` |
| `Introspector.getPropertyDescriptors()` devuelto como lista | Error de compilación al migrar la lectura de propiedades |

### 5.3 Lint del frontend

`npm run lint` termina **sin errores ni avisos**, sin desactivar ninguna regla.
Los seis errores y cinco avisos que existían se corrigieron de raíz:

| Archivo | Corrección |
|---|---|
| `App.jsx` | El catálogo `DESTINOS` se movió a `src/data/destinos.jsx`; el archivo ya solo exporta componentes |
| `context/AuthContext.jsx` | Separado en `auth-context.js` (contexto), `AuthContext.jsx` (provider) y `useAuth.js` (hook), sin reglas desactivadas |
| `AdminDashboard.jsx` | `useCountUp` agenda siempre el primer fotograma, añade `cancelAnimationFrame` al desmontar y deja de actualizar estado de forma síncrona en el efecto |
| `AdminDashboard.jsx` | `DonutChart` acumula los arcos con un bucle en el cuerpo del render en vez de reasignar una variable desde el callback de `map` |
| `ClienteDashboard.jsx` | El plazo de cancelación se recalcula con el patrón oficial de ajuste de estado en render y ahora **desciende cada segundo**; antes se calculaba una sola vez y la cuenta regresiva quedaba congelada |
| `ClienteDashboard.jsx` | La carga inicial se hace dentro del efecto con un guardia `vigente`, evitando actualizar estado tras desmontar el componente |
| `ChoferDashboard.jsx`, `CrudAsientos.jsx`, `PasajesPage.jsx` | `headers` memorizado con `useMemo` y dependencias de hooks completas en `useCallback`/`useEffect` |

### 5.4 Hallazgos revisados que resultaron ser incorrectos o ya resueltos

Se registran para que no se vuelvan a investigar:

| Reportado | Realidad verificada |
|---|---|
| `ViajeRepository` consulta `bus.sucursal` inexistente | **Falso.** No hay ninguna referencia a `bus.sucursal` en el código actual. El error del log `logs/sistema-transporte.log` (2026-04-13) es histórico y anterior a la corrección actual |
| El frontend cancela boletos con un método incorrecto | **Falso.** `PasajesPage.jsx` ya usa `PATCH` y `BoletoController` expone `@PatchMapping` |
| `Pago.cliente_id` y `Asiento.precio` no coinciden con `schema.sql` | **No verificable** sin acceso a la base de datos. Con `ddl-auto: update` es probable que Hibernate ya haya creado esas columnas. **No se tocó nada** hasta poder inspeccionar el esquema real |

### 5.5 Hallazgo nuevo detectado al revisar la configuración

| Reportado | Realidad verificada |
|---|---|
| `backend/bus-system/.env.example` apunta a `transporte_db` mientras `application.yml` usa `transporte_bd` | **Cierto.** Los dos ficheros declaran nombres de base distintos. Si alguien copia el `.env.example` a `.env` sin corregirlo, la aplicación se conectará a `transporte_db` y no a la base respaldada. **No se modificó** el `.env.example` porque es una plantilla del proyecto y el alcance acordado es reportes, crítico y datos; queda como aviso documentado |

### 5.6 Deuda técnica conocida, fuera del alcance acordado

| Tema | Detalle |
|---|---|
| Catálogo `DESTINOS` en `src/data/destinos.jsx` | Lista de rutas, horarios y precios escrita a mano en el frontend, con valores como `"S/ 10–15"`. Se usa en la pantalla de compra. Es el dato más parecido a «mock» que queda en el sistema y merece un rediseño propio |
| `ddl-auto: update` | Spring modifica el esquema al arrancar. Es cómodo en desarrollo pero arriesgado en producción; conviene migrar a una estrategia versionada |
| Cobertura de pruebas | Las 38 pruebas cubren la lógica de agregación de reportes y el exportador CSV. No hay pruebas de integración, ni de los controladores, ni de los servicios críticos de venta, cancelación y encomiendas |
| Gestión de errores de red | La mayoría de `fetch` no distingue «sin conexión» de «respuesta vacía» |

---

## 6. Cambios de esquema pendientes

`backend/bus-system/src/main/resources/db/migracion-2026-01-destinatario-encomienda.sql`

Script **idempotente, no destructivo y con consultas de verificación antes y
después**. Hace tres cosas:

1. Añade `encomiendas.destinatario_nombre VARCHAR(200)`.
2. Relaja el `NOT NULL` de `encomiendas.destinatario_id`.
3. Rellena `destinatario_nombre` con el nombre real cuando el destinatario **sí**
   es un Cliente.

Incluye además, **desactivado y solo como comentario**, un bloque opt-in para
reparar las encomiendas históricas cuyo destinatario quedó guardado como el
remitente, extrayendo el nombre real del campo `observaciones`. Se dejó
desactivado porque implica analizar texto libre: hay que ejecutar antes la
consulta de previsualización que trae el propio script.

**Condición previa obligatoria: el respaldo debe existir y estar verificado.**

### Efecto sobre la aplicación

`spring.jpa.hibernate.ddl-auto: update` añadirá la columna y relajará la
restricción por su cuenta al arrancar. Aun así, conviene ejecutar la migración de
forma explícita y controlada, en lugar de confiar en que lo haga el arranque.

---

## 7. Cómo ejecutar el sistema

```powershell
# 1. Base de datos (PostgreSQL 17 debe estar en ejecución)
$env:DB_PASSWORD='<CONTRASENA>'

# 2. Backend
cd backend/bus-system
mvn spring-boot:run          # http://localhost:8080

# 3. Frontend (en otra terminal)
cd frontend/Web-Intiwatana
npm install
npm run dev                  # http://localhost:5173
```

El frontend consume `/api/v1` a través del proxy de Vite.

**Antes de levantar el backend contra la base de datos real, tener el respaldo
verificado**: el arranque modifica el esquema.

---

## 8. Lo que falta para poder declarar CERRADO Y VERIFICADO

Todo lo de esta lista depende de la credencial de PostgreSQL. En el alcance
acordado (**reportes + crítico + refactor de datos**) queda así:

| # | Paso | Estado |
|---|---|---|
| 1 | Conexión válida a `transporte_bd` | **BLOQUEADO** — falta la contraseña del rol `postgres` |
| 2 | `pg_dump -Fc` → `database/intiwatana_transporte_bd.backup` | Pendiente del paso 1 |
| 3 | `pg_restore --list` sobre el dump | Pendiente del paso 2 |
| 4 | Restaurar en `transporte_bd_restore_test` y comparar tablas y filas | Pendiente del paso 3 |
| 5 | Aplicar y validar la migración de destinatario | Pendiente del paso 4 |
| 6 | Probar los 7 reportes contra datos reales | Pendiente del paso 1 |
| 7 | Regresión de los flujos principales | Pendiente del paso 1 |
| 8 | Verificación final de backend, frontend y base de datos | Backend y frontend **verificados**; base de datos pendiente |
| 9 | Confirmar que el único dump es `intiwatana_transporte_bd.backup` | **VERIFICADO** — no existe ningún otro |
| 10 | Documentación con evidencia real | Actualizada con el estado y el diagnóstico del bloqueo |

Los pasos 2 a 8 **no se pueden simular ni sustituir**: la auditoría exige
 PostgreSQL real. Hasta que se ejecuten, el estado es
**IMPLEMENTADO / NO VERIFICADO CONTRA LA BASE DE DATOS**.
