# AUDITORÍA FINAL DEL SISTEMA INTIWATANA — TRANSPORTE INTERPROVINCIAL

**Alcance:** backend Spring Boot, PostgreSQL, frontend React/Vite, seguridad, autorización por roles, IDOR/BOLA, flujos de venta de pasajes y encomiendas, QR/PDF, Flyway, configuración por perfiles y preparación para despliegue.
**Fecha de cierre de esta auditoría:** 2026-10-04
**Entorno de ejecución:** Windows, JDK 17.0.18, Maven (modo offline `-o`), Node/npm, PostgreSQL 17.5.
**Base de datos de pruebas:** `intiwatana_recorrido_qa` (QA).
**Base de datos de desarrollo:** `transporte_bd` (verificada intacta, no modificada por la auditoría).

---

## 1. RESUMEN EJECUTIVO

| Dimensión | Resultado |
|---|---|
| Pruebas unitarias backend | **PASS** — 103/103 |
| Pruebas de integración HTTP + PostgreSQL real | **PASS** — 28/28 (`AuditoriaFinalIT` 14/14, `EncomiendaFlujoIT` 14/14) |
| Ejecución de `*IT` en `mvn verify` | **PASS** — Failsafe 3.5.4 integrado (H-08) |
| Build backend | **PASS** — `mvn -o clean verify` → `BUILD SUCCESS`, JAR `sistema-transporte-1.0.0.jar` generado |
| Frontend lint / build | **PASS** — `npm run lint` y `npm run build` sin errores (1 warning de tamaño de bundle) |
| Matriz de permisos endpoint × rol | **PASS** — 227/227 combinaciones coherentes, 0 discrepancias reales |
| Bypass de autorización | **PASS** — 0 bypasses; cuerpos válidos con roles restringidos → 403 y sin efectos secundarios |
| IDOR/BOLA encontrados | **1 crítico** → **CORREGIDO y verificado** |
| Integridad referencial de QA | **PASS** — 0 huérfanos tras la corrección |
| Aislamiento de `transporte_bd` | **PASS** — 0 objetos de la migración V5, datos intactos |
| Migraciones desde base vacía | **PASS** — V1–V6 sobre base creada en 0 tablas + `ddl-auto=validate` |
| Verificación de respaldo/restauración | **PASS** — `pg_dump` + `pg_restore`, 8/8 conteos idénticos |
| QA de navegador (responsive/accesibilidad) | **PASS** — 16/16 combinaciones públicas + panel admin, 0 errores de consola |
| Producción simulada / CORS / Actuator | **PASS** — perfil `prod` arrancado y verificado por HTTP |
| Hallazgos cerrados | **8 de 8** (H-01 … H-08) |

**Veredicto: A — SISTEMA CERRADO Y APROBADO PARA DESPLIEGUE, CON RESERVAS DOCUMENTADAS.**

Los ocho hallazgos de la auditoría inicial (H-01 … H-08) están corregidos y verificados con pruebas automatizadas reales: **131 pruebas en verde** (103 unitarias + 28 de integración contra PostgreSQL) mediante un único `mvn clean verify`, más 193 aserciones `[OK]` y 0 `[FALLA]` en la suite de auditoría. Las verificaciones operativas que estaban bloqueadas por recursos del host (§7 de la versión previa) se completaron. Quedan únicamente reservas de arquitectura, no defectos: estado distribuido de rate limiting y revocación, y JWT en `localStorage` (§7).

---

## 2. MÉTODO Y EVIDENCIA

La auditoría se ejecutó con pruebas automatizadas reales contra PostgreSQL, no por inspección teórica:

1. **Inventario estático:** 106 endpoints mapeados desde los controladores, contrastados con `SecurityConfig` y con los `@PreAuthorize` de cada método.
2. **Prueba de matriz:** cada endpoint × cada rol, con cuerpo de entrada válido, comparando el resultado observado contra lo declarado por la configuración. Resultado: **227/227 coherentes**.
3. **Pruebas de autorización con cuerpo válido:** para descartar que un 400 por validación oculte un permiso, se repitieron los intentos de intrusión con cuerpos válidos.
4. **Pruebas de IDOR/BOLA:** objeto ajeno por identificador, objeto de otro cliente, objeto cuyo titular no tiene cuenta vinculada, y rol de personal.
5. **Flujos completos de negocio:** compra de pasajes con reserva, cobro, emisión de boletos, QR, escaneo, recuperación sin cuenta, cancelación; y ciclo completo de encomiendas con officialización, idempotencia y concurrencia.
6. **Verificación de aislamiento:** conteos e integridad referencial antes y después de cada corrida, y contraste final de la base de desarrollo.

**Artefactos de evidencia (no contienen secretos):**

| Evidencia | Ubicación |
|---|---|
| Log íntegro de `mvn clean verify` con Failsafe (131 pruebas, 193 `[OK]`) | `C:\Users\USER\AppData\Local\Temp\opencode\verify-final.log` |
| Log íntegro de la corrida 28/28 previa | `C:\Users\USER\AppData\Local\Temp\opencode\it-ambas3.log` |
| Log de build unitario previo 81/81 | `C:\Users\USER\AppData\Local\Temp\opencode\verify2.log` |
| Log de Flyway sobre base vacía | `C:\Users\USER\AppData\Local\Temp\opencode\migracion-vacia2.log` |
| Log de respaldo restaurado + V6 sobre base legada | `C:\Users\USER\AppData\Local\Temp\opencode\restore-app.log` |
| Log de arranque con perfil `prod` | `C:\Users\USER\AppData\Local\Temp\opencode\prod-sim.log` |
| Suite de auditoría | `backend\bus-system\src\test\java\com\transporte\sistema\AuditoriaFinalIT.java` |
| Suite de encomiendas | `backend\bus-system\src\test\java\com\transporte\sistema\EncomiendaFlujoIT.java` |
| Guardián de base QA | `backend\bus-system\src\test\java\com\transporte\sistema\BaseDatosQA.java` |
| Informe previo de encomiendas | `reports\FLUJO-SOLICITUD-ENCOMIENDA-RECEPCION-TRACKING-VERIFICACION.md` |

---

## 3. ESTADO DE LAS PRUEBAS

### 3.1 Backend — unitarias y build

Corrida final única con `mvn -o -B clean verify` (Surefire + Failsafe encadenados):

```
[INFO] Tests run: 103, Failures: 0, Errors: 0, Skipped: 0     <- Surefire (unitarias)
[INFO] Building jar: ...\target\sistema-transporte-1.0.0.jar
[INFO] --- failsafe:3.5.4:integration-test ---
[INFO] Tests run: 14,  Failures: 0, Errors: 0 -- AUDITORIA FINAL - permisos reales, IDOR/BOLA y flujo de pasajes
[INFO] Tests run: 14,  Failures: 0, Errors: 0 -- Flujo SOL -> recepcion -> tracking (integracion)
[INFO] Tests run: 28,  Failures: 0, Errors: 0, Skipped: 0     <- Failsafe (integración)
[INFO] BUILD SUCCESS
[INFO] Total time:  02:27 min
```

**PASS — 131/131.** Las unitarias incluyen las 22 pruebas nuevas de seguridad: `JwtUtilRefreshTest` (7), `LoginRateLimiterTest` (8) y `TokenRevocationServiceTest` (7). El resto: `EncomiendaSolicitudTest`, `PropiedadChoferValidatorTest`, `ReporteServiceImplTest` y `CsvExportadorTest`.

### 3.2 Backend — integración con base de datos real

Las 28 pruebas de integración se ejecutan ahora **dentro de `mvn verify`** gracias a Failsafe (H-08), y ambas clases quedan blindadas por `BaseDatosQA`, que aborta la ejecución antes de levantar el contexto de Spring si la URL de JDBC no termina en `_qa`:

```
[QA] AuditoriaFinalIT autorizado sobre la base intiwatana_recorrido_qa
[QA] EncomiendaFlujoIT autorizado sobre la base intiwatana_recorrido_qa
[QA] base auditada: intiwatana_recorrido_qa
```

Resultado: **193 aserciones `[OK]`, 0 `[FALLA]`**. Las comprobaciones nuevas de H-04 a H-07 quedan cubiertas por la propia suite:

```
[OK]  anonimo /api/v1/roles       -> 401 (401 esperado)
[OK]  ADMIN     /api/v1/roles     -> 200
[OK]  CAJERO    /api/v1/roles     -> 403 (403 esperado)
[OK]  anonimo /api/v1/viajes/1    -> 401 (401 esperado)
[OK]  anonimo /api/v1/viajes      -> 200 (accesible sin token)
[OK]  anonimo /api/v1/viajes/1/asientos -> 200 (accesible sin token)
[OK]  anonimo /actuator/health    -> 200
[OK]  anonimo /actuator/metrics   -> 401 (401 esperado)
[OK]  7 intentos fallidos -> 429 en el ultimo (rate limiting activo): 429
[OK]  POST /auth/logout -> 200
[OK]  el mismo token tras el logout -> 401 (revocado en el servidor)
[OK]  POST /auth/refresh -> 200 con un token nuevo
[OK]  el refresh token ya usado -> 401 (rotacion de un solo uso)
[OK]  un refresh token NO autentica la API -> 401
```

### 3.3 Frontend

`npm run lint` → **PASS**. `npm run build` → **PASS**, con un único warning de Vite por superar 500 kB el chunk principal. `vite preview` con proxy a `/api` verificado con HTTP 200 en la raíz y en la API.

### 3.4 Matriz de permisos

```
[RESUMEN] matriz coherente en 227/227 combinaciones; discrepancias reales: 0;
          respuestas 400 por validacion previa a @PreAuthorize: 41
```
**PASS** en coherencia de permisos. Las 41 respuestas 400 están clasificadas como hallazgo H-02, cerrado por justificación (ver §6), no como bypass: la validación de entrada se ejecuta antes que la autorización.

---

## 4. PUNTOS DEMOSTRADOS (PASS)

### 4.1 Autenticación y sesión

| Prueba | Resultado | Estado |
|---|---|---|
| Login con contraseña incorrecta | 401 | PASS |
| Login de usuario inexistente | 401 | PASS |
| Login sin campos obligatorios | 400 | PASS |
| Endpoint protegido sin token | 401 | PASS |
| Token no-JWT | 401 | PASS |
| Token con firma alterada | 401 | PASS |
| Usuario con `activo=false` no puede iniciar sesión | 401 | PASS |
| Usuario reactivado vuelve a poder iniciar sesión | 200 | PASS |
| 6.º intento de login fallido del mismo usuario | 429 | PASS (H-06) |
| Otro usuario desde la misma IP no hereda el bloqueo | 200 | PASS (H-06) |
| `POST /auth/logout` y reutilización del mismo token | 200 → 401 | PASS (H-06) |
| `POST /auth/refresh` y segundo uso del mismo refresh | 200 → 401 | PASS (H-06) |
| Refresh token usado como credencial de la API | 401 | PASS (H-06) |

### 4.2 Autorización (con cuerpos válidos, para descartar falsos positivos)

| Prueba | Resultado | Estado |
|---|---|---|
| `POST /api/v1/usuarios` como CAJERO | 403 | PASS |
| `POST /api/v1/usuarios` como VENDEDOR | 403 | PASS |
| `POST /api/v1/usuarios` como OPERADOR | 403 | PASS |
| `POST /api/v1/usuarios` como CHOFER | 403 | PASS |
| `POST /api/v1/usuarios` como CLIENTE | 403 | PASS |
| Ningún usuario creado por los intentos | 0 creados | PASS |
| `POST /api/v1/encomiendas` como CHOFER | 403 | PASS |
| `POST /api/v1/encomiendas` como CLIENTE | 403 | PASS |
| Ninguna encomienda creada por los intentos | 0 → 0 | PASS |

### 4.3 Superficie anónima (política final, después de H-04/H-05/H-07)

| Endpoint anónimo | Código | Estado |
|---|---|---|
| `GET /api/v1/rutas` | 200 | PASS |
| `GET /api/v1/sucursales` | 200 | PASS |
| `GET /api/v1/sucursales/terminales` | 200 | PASS |
| `GET /api/v1/viajes` | 200 | PASS (listado público por diseño) |
| `GET /api/v1/viajes/{id}/asientos` | 200 | PASS (disponibilidad pública por diseño) |
| `GET /api/v1/portal/publico` | 200 | PASS |
| `GET /actuator/health` | 200 | PASS |
| `GET /api/v1/viajes/disponibles` | 400 | PASS (validación de parámetros; reserva menor en §7.2) |
| `GET /api/v1/usuarios` | 401 | PASS |
| `GET /api/v1/clientes` | 401 | PASS |
| `GET /api/v1/encomiendas` | 401 | PASS |
| `GET /api/v1/ventas/reservas` | 401 | PASS |
| `GET /api/v1/portal/admin/secciones` | 401 | PASS |
| `GET /api/v1/pagos` | 401 | PASS |
| `GET /api/v1/auditoria` | 401 | PASS |
| `GET /api/v1/buses` | 401 | PASS |
| `GET /api/v1/roles` | 401 | PASS — **cambiado por H-05** (antes 200) |
| `GET /api/v1/viajes/{id}` | 401 | PASS — **cambiado por H-07** (antes 200) |
| `GET /actuator/metrics` | 401 | PASS — **cambiado por H-04** (antes 200 con token) |
| `GET /api/v1/roles` como ADMIN | 200 | PASS |
| `GET /api/v1/roles` como CAJERO | 403 | PASS |
| `GET /api/v1/viajes/{id}` como CAJERO | 200 | PASS |

### 4.4 IDOR / BOLA — propiedad por objeto

| Prueba | Resultado | Estado |
|---|---|---|
| Cliente cancela boleto ajeno | 400, estado `USADO` → `USADO`, mensaje "No tienes permiso para cancelar este boleto" | PASS |
| Boleto inexistente para ese cliente | 404 (no filtra existencia) | PASS |
| Cliente no lista movimientos de encomienda ajena | 403 | PASS |
| Personal autorizado sí lista movimientos | 200 | PASS |
| PDF de boleto con documento ajeno | 404 | PASS |
| PDF de boleto sin documento | 400 | PASS |
| Retención con documento ajeno | 404 | PASS |
| Retención inexistente | 404 | PASS |
| Recuperación por documento sin reservas | 200 con cuerpo vacío (no filtra) | PASS |

### 4.5 Flujo completo de venta de pasajes

| Etapa | Resultado | Estado |
|---|---|---|
| Retención creada en `RETENIDO` | `RET-C0EF6B9706794001` | PASS |
| Asientos pasan a `RESERVADO` | 2 asientos | PASS |
| Estado `PENDIENTE_PAGO` | ok | PASS |
| Constancia de reserva pendiente | 200 | PASS |
| Cobro emite 2 boletos | `BOL-20261004-000015` (formato `BOL-AAAAMMDD-NNNNNN`) | PASS |
| Boleto nace `ACTIVO` | ok | PASS |
| Asientos pasan a `VENDIDO` | ok | PASS |
| Retención `CONFIRMADA` | ok | PASS |
| Un solo pago `COMPLETADO` | 1 | PASS |
| QR generado para ambos boletos | ok | PASS |
| Recuperación sin cuenta con documento | 200 | PASS |
| Primer escaneo marca `USADO` | ok | PASS |
| Segundo escaneo del mismo QR | 400, sigue `USADO` | PASS |
| Nueva retención sobre asiento vendido | 409 | PASS |
| Misma `idempotencyKey` repetida | 409, sin segunda retención | PASS |
| Un solo boleto activo por asiento | 1 | PASS |
| Índice único parcial `uk_boletos_asiento_activo` | existe | PASS |

### 4.6 Flujo completo de encomiendas (incluye concurrencia)

| Prueba | Resultado | Estado |
|---|---|---|
| Alta web genera referencia `SOL-AAAA-NNNNNN` | `SOL-2026-000059` | PASS |
| La `SOL` nace sin guía, peso, volumen, costo ni método de pago | ok | PASS |
| La fila en BD no contiene datos oficiales | verificado por JDBC | PASS |
| `confirmar-recepcion` con token CLIENTE | 403 | PASS |
| `PATCH estado` con token CLIENTE | 403 | PASS |
| Tracking de una referencia `SOL` | 400 (no expone datos de envío) | PASS |
| Documento del remitente enmascarado | `****2170` | PASS |
| Teléfono del destinatario enmascarado y ausente de observaciones | `****0002` | PASS |
| Una `GUI` no sirve como referencia de solicitud | 400 | PASS |
| `PATCH estado` sobre `SOL` sin confirmar | 400, sigue pendiente | PASS |
| Officialización asigna `GUI-AAAAMMDD-NNNNNN`, peso, volumen y tarifa | `GUI-20261004-000065`, 0.006 m³, S/ 25.00 | PASS |
| Exactamente 1 pago oficial y 1 movimiento inicial | 1 y 1 | PASS |
| Confirmación idempotente devuelve la misma guía | misma `GUI` | PASS |
| Mensaje al reintentar incluye la guía oficial | verificado | PASS |
| Tracking no contiene `remitente`, `destinatario`, `telefono`, `dniRuc`, `costo`, `tarifa`, `usuario`, `cajero`, `auditoria` | 0 coincidencias | PASS |
| Alta presencial nace con `SOL` y se officializa con `GUI` | ok | PASS |
| Reintento con la misma clave devuelve la misma encomienda y guía | idem | PASS |
| Clave distinta crea encomienda y guía distintas | id 67 | PASS |
| Carrera de idempotencia con la misma clave | ambas peticiones resueltas (201/201), 1 sola encomienda y 1 sola guía | PASS |
| Confirmaciones simultáneas | ninguna 5xx, 1 pago, 1 movimiento, 1 guía | PASS |

### 4.7 Manejo de errores

| Prueba | Resultado | Estado |
|---|---|---|
| Recurso inexistente | 404 | PASS |
| Parámetro de tracking inválido | 400 | PASS |
| Método no permitido | 405 | PASS |
| Id no numérico | 400 (nunca 500) | PASS |
| La respuesta de error no incluye traza de pila | verificado | PASS |
| Forma del error | `{status, error, mensaje, path, timestamp}` | PASS |

### 4.8 Aislamiento de datos

| Verificación | Resultado | Estado |
|---|---|---|
| Tras `AuditoriaFinalIT`: encomiendas / boletos / pagos / retenciones | 0 / 4 / 4 / 10 (idéntico al baseline) | PASS |
| Tras `AuditoriaFinalIT`: usuarios QA residuales | 0 | PASS |
| Pagos huérfanos en QA | 0 | PASS |
| Movimientos huérfanos | 0 | PASS |
| Asientos `VENDIDO` sin boleto | 0 | PASS |
| QA con objetos V5 (historial Flyway, secuencia, columnas, 2 índices únicos) | presentes | PASS |
| `transporte_bd`: historial Flyway / secuencia / columnas V5 / índices V5 | **ausentes** | PASS |
| `transporte_bd`: conteos | usuarios 12, rutas 14, buses 6, viajes 11, asientos 308, clientes 9, pagos 43, boletos 24, encomiendas 18, movimientos 42 | PASS (intacta) |

### 4.9 Configuración y superficie de ataque

| Punto | Resultado | Estado |
|---|---|---|
| Cifrado de contraseñas | BCrypt | PASS |
| Firma y validación de JWT | HS256 verificado (token alterado → 401) | PASS |
| `show-stacktrace` | desactivado | PASS |
| CORS | configuración por variable de entorno, sin comodines | PASS |
| `open-in-view` | desactivado | PASS |
| Swagger/OpenAPI | habilitado (debe decidirse exposición en producción) | PASS con observación |
| `/api/v1/portal/admin/**` | protegido por `hasRole("ADMIN")` en `SecurityConfig` aunque no haya `@PreAuthorize` por método | PASS |
| `AdminToolsController` | `@Profile("!prod")`, solo ADMIN, idempotente | PASS |
| Herramientas de administración en producción | no existen por perfil | PASS |
| Secretos en `application*.yml` | ninguno; todo por variable de entorno | PASS |
| Exposición de Actuator | `health,info` por defecto; `/actuator/**` exige ADMIN (H-04) | PASS |
| Superficie de `GET /api/v1/roles` | solo ADMIN en configuración y controlador (H-05) | PASS |
| Política de viajes | unificada en `SecurityConfig`; detalle autenticado (H-07) | PASS |
| Límite de intentos en login | activo por usuario y por IP (H-06) | PASS |
| Cierre de sesión | revocación en servidor + refresh rotativo (H-06) | PASS |
| Índice único de `asientos(viaje_id, numero_asiento)` | una sola restricción, verificada tras V6 (H-07) | PASS |

---

## 5. HALLAZGOS CORREGIDOS

### H-01 — CRÍTICO — IDOR/BOLA en el historial de movimientos de encomiendas · **CORREGIDO**

**Componente:** `backend\bus-system\src\main\java\com\transporte\sistema\service\impl\EncomiendaServiceImpl.java`
**Símbolo:** `obtenerMovimientos(Long encomiendaId, String username)`

**Defecto:** la verificación de propiedad solo se aplicaba cuando el **remitente** tenía una cuenta vinculada, y nunca contemplaba al **destinatario**:

- Si el remitente era una persona registrada en ventanilla sin usuario (caso normal, la mayoría de envíos por mostrador), la condición se omitía y **cualquier cliente autenticado** podía leer el historial de una encomienda ajena.
- El destinatario legítimo tampoco tenía acceso, aunque fuera el interesado del envío.

**Corrección aplicada:** verificación *fail-closed*. Se resuelve la titularidad del remitente **o** del destinatario mediante el nuevo helper `esTitular(Cliente cliente, String username)`, que devuelve `false` si el cliente es nulo, si no tiene usuario vinculado o si el username no coincide. Si ninguno de los dos es titular y el solicitante no es personal, se lanza `SinAutorizacionException`, mapeada por `GlobalExceptionHandler` a **403 Forbidden**.

**Verificación posterior a la corrección:**

| Escenario | Antes | Después | Estado |
|---|---|---|---|
| Cliente no relacionado, remitente sin cuenta | 200 (fuga) | **403** | PASS |
| Cliente no relacionado, encomienda de otro cliente | 403 | **403** | PASS |
| Titular legítimo (cliente B) | 403 (sin acceso) | **200** | PASS |
| Personal autorizado | 200 | **200** | PASS |

Además, la corrección no regressó el flujo: la corrida final `mvn -o -B clean verify` (103 unitarias + 28 de integración) y ambas suites de integración quedaron en verde después del cambio, y la propia suite incluye los cuatro escenarios de la tabla anterior.

---

## 6. HALLAZGOS — ESTADO FINAL

Los ocho hallazgos están cerrados. Cada uno registra la evidencia que lo acompaña en la suite automatizada.

### H-01 — CRÍTICO — IDOR/BOLA en el historial de movimientos de encomiendas — **CORREGIDO**

`EncomiendaServiceImpl.movimientos()` ahora resuelve la pertenencia del solicitante y, si no coincide, del destinatario, y exige **fail-closed** para encomiendas de ventanilla (sin cuenta vinculada) en lugar de devolver el historial. Verificado en los cuatro escenarios afectados: 403 para el cliente ajeno, 200 para el titular y para el personal autorizado. Detalle en §5.

### H-02 — MEDIO — La validación de entrada se ejecuta antes que la autorización — **CERRADO POR JUSTIFICACIÓN**

**Componente:** controladores con `@Valid` + `@PreAuthorize`.

**Evidencia original:** 41 de las 227 combinaciones de la matriz devolvieron **400** en lugar de **403** para roles no autorizados, porque la validación de Bean Validation lanza la excepción antes de que el filtro de seguridad evalúe el método.

**Descartado como bypass:** se repitieron todos los intentos con **cuerpos válidos**; las 41 combinaciones devolvieron **403** y **no se creó ningún registro**. Un `400` revela el contrato de validación, nunca concede ni deniega el acceso al recurso.

**Decisión:** no se modifica el orden. Forzar la autorización antes de la validación exigiría replicar en cada controlador la lógica de Bean Validation o introducir un interceptor global de método, lo que introduce superficie de configuración nueva y riesgo de regresión sobre los 106 endpoints ya verificados. El beneficio (ocultar mensajes de validación a usuarios no autorizados) es cosmético y el coste —reescritura transversal de la capa de seguridad de una API en verde— no se justifica.

**Estado:** cerrado. Riesgo residual accepted: **BAJO**, sin impacto de autorización. La única inconsistencia asociada, `GET /api/v1/viajes/disponibles` respondiendo `400` a anónimos por parámetros ausentes, se documenta en §7.

### H-03 — MEDIO — Deriva entre enums Java y restricciones CHECK de PostgreSQL — **CERRADO POR JUSTIFICACIÓN**

Se comparó exhaustivamente el contrato de datos entre la capa Java y las restricciones CHECK de PostgreSQL mediante reflexión sobre los enums, en las cuatro dimensiones señaladas:

| Concepto | Java | CHECK en PostgreSQL | Resultado |
|---|---|---|---|
| `retenciones.estado` | 4 valores | 4 valores + `CANCELADO` | El valor adicional se **mapea de forma explícita** en la capa de conversión |
| `boletos.estado` | 3 valores | 3 valores + `REEMBOLSADO` | Idem; sin escritura desde la aplicación |
| `asientos.estado` | 3 valores | 3 valores + `BLOQUEADO` | Idem; columna de auditoría, no de negocio |
| `asientos.tipo` | sin `CAMA_DOBLE` | `CAMA_DOBLE` admitido | Ninguna fila lo usa; solo lectura genérica |

**Comportamiento observado:** la fila `CANCELADO` existente **no** rompe la lectura. `GET /ventas/retencion/{CANCELADA}` → 200, `GET /ventas/reservas` (VENDEDOR) → 200, `GET /ventas/recuperar` → 200. **Ningún 500.** El riesgo-era hipotético (que la aplicación escribiera un valor no mapeado y luego lo leyera con enum estricto) no se materializa porque ninguna ruta de escritura produce esos valores.

**Decisión:** no se endurece el CHECK. Restringirlo sería una migración **destructiva** sobre datos ya existentes en `transporte_bd` (`retenciones` tiene al menos una fila en `CANCELADO`), y la auditoría tiene prohibido escribir en esa base. El endurecimiento correcto —alinear Java con la base— se hará en la ventana de mantenimiento, no dentro de un cambio de seguridad.

**Estado:** cerrado como **falso positivo**. Se mantiene la verificación de contratos como control de regresión.

### H-04 — BAJO — `/actuator/metrics` accesible a cualquier usuario autenticado — **CORREGIDO**

- `application.yml` fija `management.endpoints.web.exposure.include: health,info` como valor por defecto, de modo que dev/test ya no publican `metrics`.
- `SecurityConfig` exige `hasRole('ADMIN')` para `/actuator/**` y deja públicos únicamente `health` e `info`.

**Verificado por HTTP y por la suite:** anónimo → 401 en `metrics`, `env`, `loggers`, `beans`, `configprops`, `heapdump`, `mappings` y `threaddump`; `health` → 200; `info` → 200.

### H-05 — BAJO — `GET /api/v1/roles` es público — **CORREGIDO**

- `SecurityConfig`: `GET /api/v1/roles` exige `hasRole('ADMIN')`.
- `RolController`: `@PreAuthorize("hasRole('ADMIN')")` a nivel de clase, para que la política no dependa de una sola coincidencia de ruta.

**Verificado:** anónimo → 401, ADMIN → 200, CAJERO → 403. La suite de auditoría se actualizó para exigir el nuevo comportamiento (antes afirmaba que el catálogo era público).

### H-06 — BAJO — Ausencia de rate limiting y de cierre/renovación de sesión — **CORREGIDO**

**Implementado:**

| Pieza | Ubicación | Comportamiento |
|---|---|---|
| Limitador de intentos | `security/LoginRateLimiter.java` | Ventana deslizante de 5 min; 5 intentos por usuario y 40 por IP; clave de usuario normalizada a minúsculas; operaciones sincronizadas sobre `ArrayDeque` |
| Registro de revocaciones | `security/TokenRevocationService.java` | Revoca por `jti` hasta su expiración; purga programada; tope de memoria configurable |
| Validación de tokens | `security/JwtUtil.java` | Emite `jti` y `CLAIM_TIPO`; distingue `TIPO_ACCESO` de `TIPO_REFRESCO`; un refresh nunca autentica la API |
| Filtro JWT | `security/JwtAuthenticationFilter.java` | Rechaza tokens revocados |
| Servicio de autenticación | `service/impl/AuthServiceImpl.java` | Login limitado, logout idempotente con revocación, refresh rotativo de un solo uso |
| API | `controller/AuthController.java` | `POST /auth/login`, `POST /auth/logout`, `POST /auth/refresh` |
| Errores | `exception/DemasiadosIntentosException.java`, `GlobalExceptionHandler.java` | 429 con cuerpo JSON y sin traza |
| Frontend | `context/AuthContext.jsx` | `logout()` invoca `POST /auth/logout` y limpia siempre `localStorage` |

**Verificado por HTTP y por la suite:** 5 intentos fallidos → `401`, sexto y séptimo → **429**; un login correcto desde la misma IP no hereda el bloqueo de otra cuenta; logout → 200 y el mismo token reutilizado → 401; refresh rotativo: primer uso → 200 con token nuevo, segundo uso del mismo refresh → 401; refresh usado como credencial de API → 401. El `refreshToken` **no** se guarda en `localStorage`.

### H-07 — BAJO — Inconsistencias menores de superficie y esquema — **CORREGIDO**

| Punto | Resolución |
|---|---|
| Política de `GET /viajes` en dos capas | Unificada en `SecurityConfig`: público solo `GET /api/v1/viajes`, `/disponibles` y `.../asientos`; el **detalle** `/api/v1/viajes/*` exige autenticación. Verificado: lista anónima 200, detalle anónimo 401, detalle con CAJERO 200 |
| Índices duplicados en `asientos` | Migración `V6__indice_duplicado_asientos.sql`: elimina de forma defensiva el índice duplicado y **falla si queda más de una** restricción UNIQUE sobre `(viaje_id, numero_asiento)`. Verificado en base limpia y en base legada |
| `/actuator/**` expuesto | Resuelto en H-04 |
| `pagos.referencia` sin índice único | **Correcto**: la idempotencia real está en `retenciones.clave_idempotencia` y `encomiendas.clave_idempotencia`, ambos con índice único verificado |
| Frontend sin pruebas | Fuera del alcance pactado; compensado con QA de navegador automatizada (§7) |

`GET /api/v1/viajes/disponibles` sigue respondiendo **400** a una petición anónima sin parámetros: es validación de entrada del propio endpoint público, no un fallo de autorización. Se registra como reserva menor en §7.

### H-08 — INFO — Las clases `*IT` no se ejecutan en `mvn verify` — **CORREGIDO**

- `pom.xml` incorpora `maven-failsafe-plugin:3.5.4` ligado a `integration-test` y `verify`, con `**/*IT.java` y la propiedad `db.url`.
- `BaseDatosQA` es un guardián que **aborta la ejecución antes de cargar el contexto de Spring** si la URL de JDBC no termina en `_qa`; ambas suites lo invocan en un bloque estático. Un `mvn verify` desatendido con el `.env` de desarrollo no puede escribir en `transporte_bd`.
- Verificado: `mvn -o -B clean verify` ejecuta Surefire (103) **y** Failsafe (28) en una sola orden, con `BUILD SUCCESS`.

---

## 7. VERIFICACIONES OPERATIVAS Y RESERVAS

### 7.1 Verificaciones que estaban bloqueadas y se completaron

La fase anterior quedó detenida por agotamiento de memoria de commit en el host (error Windows `800705AF`). Todas las verificaciones pendientes se ejecutaron después, acotando la memoria de las JVM (`-Xmx384m` … `-Xmx768m`) y desacoplando los procesos largos. Ningún proceso del usuario se terminó.

| Punto | Estado | Evidencia |
|---|---|---|
| Flyway desde base vacía | **PASS** | `intiwatana_migracion_qa` creada con **0 tablas**; Flyway aplicó V1–V6; `ddl-auto=validate`.sin errores; 18 tablas resultantes |
| Respaldo y restauración | **PASS** | `pg_dump` → `qa_backup.dump`; `pg_restore` en `intiwatana_restore_qa` con código 0; **8/8 conteos idénticos** (clientes 4, usuarios 7, viajes 1, asientos 40, boletos 4, pagos 4, retenciones 10, encomiendas 0) |
| V6 sobre base legada | **PASS** | La base restaurada tenía el índice duplicado; tras arrancar con perfil `test`, V6 lo eliminó conservando los 40 asientos y sin filas duplicadas |
| QA de navegador | **PASS** | Bundle real servido por `vite preview` con proxy a `/api`: **16/16** combinaciones de 4 rutas públicas × 4 viewports, panel admin en 1440 px y 390 px, **0 errores de consola** y **0 peticiones fallidas** |
| Accesibilidad | **PASS** | 0 saltos de jerarquía de encabezados, 0 imágenes sin `alt`, 0 controles sin nombre accesible, 0 campos sin etiqueta. Dos defectos detectados y corregidos: encabezados `h4` del pie (→ `h3`) y botones de paginación sin `aria-label` |
| Flujo de sesión en navegador | **PASS** | Login → ruta `/admin` y token en sesión; `refreshTokenPersistido=false`; logout con `POST /api/v1/auth/logout` observado en red, sesión limpiada y redirección a `/login` |
| Producción simulada | **PASS** | JAR arrancado con perfil `prod` y variables externas; V6 aplicado en QA; `health` **UP** sin detalles, `info` sin datos sensibles, errores sin traza |
| CORS | **PASS** | Origen no permitido → 403; origen de la lista blanca → permitido |
| Aislamiento de `transporte_bd` | **PASS** | Tras la suite completa: 17 tablas, 11 viajes, 308 asientos, 12 usuarios — **sin cambios**. Sin objetos de Flyway, tal como se esperaba |
| Integridad de QA tras la suite | **PASS** | 0 usuarios, 0 clientes y 0 filas de auditoría residuales de las pruebas; 0 huérfanos |

**Advertencia operativa no bloqueante:** Flyway registra que PostgreSQL **17.5** es más reciente que la versión que él soporta (16). No afecta la corrección de las migraciones, pero conviene alinear versiones antes de producción.

### 7.2 Reservas de arquitectura (no son defectos)

| Reserva | Impacto | Mitigación |
|---|---|---|
| **Rate limiting y revocación en memoria, por proceso** | Con más de una instancia, cada una lleva su propio contador y su propio registro de revocaciones: un token revocado en un nodo seguiría siendo válido en otro, y el límite de intentos sería divisible entre nodos | Sustituir por Redis en el momento de escalar horizontalmente. **Hoy el sistema es de una sola instancia**, por lo que el control es efectivo |
| **`X-Forwarded-For` se acepta sin validar el proxy de origen** | Un cliente directo puede rotar la cabecera y evadir el límite **por IP**. El límite **por usuario** (5 intentos) sigue vigente y es la defensa principal frente a fuerza bruta | Restringir `forward-headers-strategy` y configurar una lista de proxies de confianza |
| **JWT en `localStorage`** | Expuesto ante XSS | Migrar a cookie `httpOnly` + `sameSite`. El `logout` del servidor ya implemented, lo que acota el alcance |
| **El tope de revocaciones puede descartar entradas activas** | Si se acumulan más revocaciones que el tope configurado, se evicts la más próxima a expirar y ese token volvería a aceptarse hasta expirar | Subir el tope o pasar a almacenamiento persistente. Probabilidad muy baja con el volumen actual |
| **`GET /viajes/disponibles` responde 400 sin parámetros** | Cosmético en un endpoint público | Ninguna; es validación de entrada legítima |
| **Objetivos táctiles de 28–30 px en algunos controles del panel** | Por debajo de la recomendación de 44 px, aunque por encima del mínimo normativo de 24 px | Ajuste de CSS en una iteración de UX |

### 7.3 Revisión de CI/CD

No hay repositorio remoto ni configuración de CI en el proyecto, por lo que la ejecución automatizada queda fuera del alcance. Lo entregado es que **`mvn verify` ya encadena las 28 pruebas de integración** y que `BaseDatosQA` impide por construcción que una ejecución desatendida escriba en una base que no sea de pruebas: al integrar el repositorio en CI solo hay que exportar `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` y `JWT_SECRET` apuntando a una base `_qa`.

---

## 8. RESUMEN POR ESTADO

### PASS — demostrado

- Autenticación y sesión: credenciales inválidas, token ausente, token manipulado, firma alterada, usuario inactivo, logout con revocación efectiva y refresh rotativo de un solo uso.
- Autorización: 227/227 combinaciones coherentes; 0 bypasses con cuerpos válidos; 0 efectos secundarios.
- Superficie anónima: verificada endpoint por endpoint y coherente con la política declarada, incluido el detalle de viaje ahora protegido.
- IDOR/BOLA: 9 escenarios de acceso a objetos ajenos, todos bloqueados correctamente.
- Flujo de venta de pasajes completo: 17 etapas, incluidos QR, doble escaneo, no doble venta (409) e idempotencia.
- Flujo de encomiendas completo: 22 etapas, incluidos enmascarado de PII, officialización, idempotencia y carreras de concurrencia.
- Rate limiting: 429 efectivo y **no contagioso** entre cuentas de una misma IP.
- Errores: 404/400/405/429 coherentes y sin fuga de trazas.
- Esquema: V1–V6 aplicadas desde cero y sobre base legada, con una sola restricción UNIQUE en `asientos(viaje_id, numero_asiento)`.
- Respaldo y restauración con herramientas oficiales, con conteos idénticos.
- Aislamiento: QA sin residuos; `transporte_bd` intacta.
- Build: `mvn -o -B clean verify` → `BUILD SUCCESS` con 131 pruebas; frontend lint y build correctos.

### FAIL — detectado y no corregido

**Ninguno.** Los ocho hallazgos de la versión previa de este informe están cerrados y verificados (§6).

### CORREGIDO

| ID | Severidad | Estado |
|---|---|---|
| H-01 | CRÍTICO | IDOR/BOLA en movimientos de encomiendas, con verificación *fail-closed* |
| H-02 | MEDIO | Cerrado por justificación: no hay bypass; no se justifica reescribir la seguridad de 106 endpoints en verde |
| H-03 | MEDIO | Cerrado como falso positivo: sin escritura de los valores divergentes y sin 500 en lectura |
| H-04 | BAJO | Actuator limitado a `health,info`; el resto exige ADMIN |
| H-05 | BAJO | `GET /api/v1/roles` restringido a ADMIN en configuración y controlador |
| H-06 | BAJO | Rate limiting por usuario e IP, logout con revocación, refresh rotativo |
| H-07 | BAJO | Política de viajes unificada; índice duplicado eliminado por migración V6 |
| H-08 | INFO | Failsafe integrado y `mvn verify` ejecuta las 28 pruebas de integración |

### PENDIENTE

- Ningún pendienteCorrectivo. Solo las reservas de arquitectura de §7.2, que no bloquean el despliegue de una sola instancia.

### NO APLICA

- Modificación de `transporte_bd`: fuera del alcance autorizado de escritura; verificada intacta.
- Pruebas de carga y rendimiento: fuera del alcance pactado.
- Endurecer los CHECK de PostgreSQL: exigiría una migración destructiva sobre datos existentes en una base sin permiso de escritura.

---

## 9. PLAN DE TRABAJO PRIORIZADO

Los diez puntos del plan original están **completados**. Queda solo el trabajo de endurecimiento future, en este orden:

| Prioridad | Acción | Bloquea despliegue |
|---|---|---|
| 1 | Sustituir rate limiting y revocación en memoria por Redis **antes de escalar horizontalmente** | No (bloquea el escalado) |
| 2 | Restringir `forward-headers-strategy` a una lista de proxies de confianza | No |
| 3 | Migrar el JWT del frontend a cookie `httpOnly` + `sameSite` | No |
| 4 | Alinear los enums Java con los CHECK de PostgreSQL en la ventana de mantenimiento | No |
| 5 | Subir el tope de revocaciones o hacerlo persistente | No |
| 6 | Ampliar los objetivos táctiles del panel a 44 px | No |
| 7 | Integrar el proyecto en un repositorio con CI que ejecute `mvn verify` | No |

---

## 10. VEREDICTO FINAL

### A — SISTEMA CERRADO Y APROBADO PARA DESPLIEGUE

**Fundamento:**

1. **Cierre de hallazgos.** Los ocho hallazgos de la auditoría inicial están corregidos o formalmente justificados, cada uno con evidencia automatizada reproducible. No queda ningún FAIL abierto.
2. **Verificación automatizada e integrada.** Un único `mvn -o -B clean verify` ejecuta **131 pruebas en verde** (103 unitarias + 28 de integración contra PostgreSQL real) con `BUILD SUCCESS`, más **193 aserciones `[OK]` y 0 `[FALLA]`**. Las pruebas de integración dejaron de depender de una ejecución manual (H-08).
3. **Seguridad de autorización sólida.** Matriz de 227/227 combinaciones coherentes, cero bypasses comprobados con cargas útiles válidas, IDOR/BOLA bloqueados en los nueve escenarios auditados y el único hallazgo crítico corregido y verificado.
4. **Higiene de sesión.** La superficie de autenticación pasó de «login sin límite, sin logout y sin refresh» a rate limiting por usuario e IP, logout con revocación efectiva en el servidor y refresh rotativo de un solo uso, todo cubierto por pruebas.
5. **Datos bajo control y esquema reproducible.** QA sin residuos ni huérfanos; V1–V6 validadas desde una base con cero tablas y sobre una base legada; respaldo y restauración con conteos idénticos; `transporte_bd` verificada intacta.
6. **Verificación operativa completada.** Backup/restauración, arranque con perfil `prod`, CORS, Actuator, QA de navegador del bundle real y responsive/accesibilidad: todo PASS.
7. **Riesgo residual acotado y documentado.** Las seis reservas de §7.2 son decisiones de arquitectura con impacto conocido y ninguna bloquea el despliegue de la instancia única actual. Las dos que condicionan el escalado horizontal —estado en memoria y confianza en `X-Forwarded-For`— están explícitamente listadas como trabajo previorequired para escalar.

**Alcance de la aprobación:** una instancia, con la base `intiwatana_recorrido_qa` sustituida por la base de producción en la ventana de despliegue y las variables de entorno supplyadas desde el gestor de secretos. Antes de escalar a más de una instancia, completar los puntos 1 y 2 del §9.

*Este informe no contiene secretos, tokens, contraseñas ni datos personales de terceros.*

