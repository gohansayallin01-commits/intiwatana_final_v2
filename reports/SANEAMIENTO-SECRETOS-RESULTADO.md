# SANEAMIENTO DE SECRETOS - RESULTADO

**Proyecto:** intiwatana_final_v2
**Fecha:** 2026-10-02
**Alcance:** eliminar credenciales y secretos en claro de código, configuración,
documentación, scripts y artefactos; externalizar la configuración sensible;
preparar perfiles de entorno para que producción no herede valores de desarrollo.

> Nota: en este documento los valores sensibles se muestran como `<REDACTADO>`.
> Ningún secreto real (contraseñas de base, secreto JWT) se imprime aquí.

---

## 1. Estado inicial (hallazgos)

| # | Hallazgo | Ubicación | Riesgo |
|---|---|---|---|
| H1 | Contraseña de base de datos en claro | `application.yml` (`password: <valor>`) | Alto |
| H2 | Secreto JWT de desarrollo en constante Java con valor por defecto | `JwtUtil.java` | Alto |
| H3 | Contraseñas de demostración en código | `DataInitializer.java` (`admin123`, `cajero123`, `chofer123`, `vendedor123`, `operador123`) | Medio (solo perfil no-prod) |
| H4 | Sin `.gitignore`: `.env`, `target/`, `node_modules/`, `dist/`, volcados | raíz del proyecto | Alto |
| H5 | Credenciales en claro en informes/README | `reports/*.md`, `AUDITORIA*.md`, `database/README-BACKUP.md`, `INFORME_FINAL_CORRECCION_INTIWATANA.md` | Alto (documentos distribuibles) |
| H6 | Hashes BCrypt de credenciales de demo | `schema.sql`, `database/transporte_bd.sql` | Medio |
| H7 | Sin Flyway: esquema dependiente de `ddl-auto: update` | backend | Alto (reproducibilidad) |
| H8 | `DataInitializer` y `/api/v1/admin/tools` activos en cualquier entorno | backend | Alto |

---

## 2. Acciones aplicadas

### 2.1 Externalización de la configuración
- `application.yml` (común): **sin credenciales ni secretos**. Perfil por defecto
  `dev`. Importación opcional de `.env` local (`spring.config.import:
  optional:file:.env[.properties]`).
- `application-dev.yml`: conexión por variables de entorno (`${DB_URL:...}`,
  `${DB_USERNAME:...}`, `${DB_PASSWORD}`), `JWT_SECRET` obligatorio, `ddl-auto:
  update`, Flyway **desactivado** (la base de desarrollo/QA se conserva intacta).
- `application-test.yml`: todas las credenciales obligatorias, Flyway **activado**,
  `ddl-auto: validate`.
- `application-prod.yml`: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` y
  `CORS_ALLOWED_ORIGINS` **sin valor por defecto → la aplicación no arranca si
  falta alguna** (fail-fast). Flyway activado, `ddl-auto: validate`,
  `clean-disabled: true`, sin stack traces, `show-details: never`.

### 2.2 Secreto JWT
- `JwtUtil.java`: eliminada la constante `SECRETO_DESARROLLO` y el bloque de aviso.
  El secreto se resuelve exclusivamente desde `${app.jwt.secret}`.

### 2.3 Datos sembrados / herramientas de desarrollo
- `DataInitializer` y `AdminToolsController` anotados con `@Profile("!prod")`:
  no se ejecutan ni se exponen en producción.
- Los logs de `DataInitializer` ya no imprimen contraseñas.

### 2.4 Control de versiones
- `.gitignore` creado en la raíz: ignora `.env`, `.env.*`, `*.pem`, `*.key`,
  `target/`, `logs/`, `*.log`, `node_modules/`, `dist/`, `database/*.backup`,
  `database/*.dump`, `database/*.sql` y `*.sql.gz`.
- Se mantiene `.env.example` (solo nombres/plantillas).
- **Motivo de ignorar `database/*.sql`:** los volcados pueden contener hashes
  BCrypt de credenciales de demostración.

### 2.5 Migraciones
- Flyway añadido (`flyway-core`, `flyway-database-postgresql`, versión gestionada
  10.10.0).
- `V1__esquema_inicial.sql`: estructura de las 13 tablas (derivada del esquema
  real).
- `V2__catalogos_base.sql`: 6 roles (`ON CONFLICT DO NOTHING`), sin usuarios.

### 2.6 Frontend
- `vite.config.js`: URL del backend externalizada por `VITE_PROXY_TARGET`
  (default `http://localhost:8080`). El bundle de producción usa rutas relativas
  `/api`, por lo que **no compila `localhost`**.
- `frontend/Web-Intiwatana/.env.example` creado (con advertencia: las variables
  `VITE_*` son públicas y no deben contener secretos).
- `eslint.config.js`: se añadió un bloque con `globals.node` para los archivos de
  configuración (`vite.config.js`, `eslint.config.js`), en lugar de silenciar la
  regla con `eslint-disable`.

---

## 3. Saneamiento documental

Se reemplazaron las credenciales en claro por marcadores `<REDACTADO>`,
conservando el contexto, en:

| Archivo | Elementos saneados |
|---|---|
| `reports/BLOQUES-C-D-RESULTADO.md` | `postgres/2007`, `chofer1/chofer123` |
| `reports/LOGIN-INTIWATANA.md` | `admin/admin123`, `pruebaa001/PruebaA001` |
| `reports/PANEL-CHOFER-REDISENO-RESULTADO.md` | `admin / admin123` |
| `AUDITORIA-FINAL-INTIWATANA.md` | `2007` (clave de `application.yml`) |
| `AUDITORIA_INTEGRAL_INTIWATANA.md` | `postgres/2007`, credenciales de prueba |
| `database/README-BACKUP.md` | `2007` (motivo y tabla de variables) |
| `INFORME_FINAL_CORRECCION_INTIWATANA.md` | `vendedor1/vendedor123`, `operador1/operador123` |
| `backend/bus-system/src/main/resources/schema.sql` | comentarios con contraseñas y hash BCrypt compartido |

---

## 4. Verificaciones

Escaneo de secretos tras el saneamiento (excluyendo artefactos de build):

| Patrón buscado | `.md` | `.sql` | `.java` | `.yml` |
|---|---|---|---|---|
| `admin123` / `chofer123` / `cajero123` / `operador123` / `vendedor123` | 0 | 0 | 7 (ver §5, H3) | 0 |
| `PruebaA001` | 0 | 0 | 0 | 0 |
| `postgres/2007` | 0 | 0 | 0 | 0 |
| `T1QOp0xFD...` (secreto JWT dev) | 0 | 0 | 0 | 0 |

- **Bundle de producción** (`dist/assets/*.js`, `*.css`, `index.html`): sin
  `localhost:8080`, `localhost:5173`, `2007`, `intiwatana_app`, `JWT_SECRET`, ni
  credenciales de demo.
- **Logs de producción** (`app.log`, `stdout.log`, `stderr.log`): 0 ocurrencias de
  la contraseña de base, el secreto JWT, `admin123` o cabeceras `Bearer`.

---

## 5. Hallazgos residuales (aceptados / pendientes)

| # | Hallazgo | Estado | Justificación / acción |
|---|---|---|---|
| R1 | `DataInitializer.java` contiene contraseñas de demo (`admin123`, `chofer123`, …) | **Aceptado, documentado** | Solo se ejecuta con perfil distinto de `prod` (`@Profile("!prod")`). La base de producción se creó con **0 usuarios**. Recomendación: externalizar a `app.seed.*` si la base de desarrollo pasa a ser compartida. |
| R2 | `database/transporte_bd.sql` contiene hashes BCrypt de credenciales de demo | **Mitigado** | Añadido a `.gitignore`. No se debe distribuir ni versionar. |
| R3 | Contraseñas de desarrollo y secreto JWT de desarrollo **no rotados** | **Pendiente para producción** | Se externalizaron, no se rotaron. No se declara rotación sin evidencia (ver §6). |
| R4 | Artefacto `backend/bus-system/target/classes/schema.sql` con hash antiguo | Transitorio | Es salida de build; se regenera desde `src`. |

---

## 6. Plan de rotación recomendado (producción)

1. Generar un `JWT_SECRET` nuevo y aleatorio (≥ 32 bytes, Base64). **No reutilizar**
   el secreto de desarrollo.
2. Crear la contraseña del usuario de base productiva (rol de mínimo privilegio) y
   custodiarla en un gestor de secretos (no en `.env` versionado).
3. Definir `CORS_ALLOWED_ORIGINS` con el dominio real (HTTPS).
4. Rotar/eliminar las credenciales de demostración cuando la base de desarrollo
   deje de ser de uso interno.
5. Inyectar los secretos por variables de entorno del orquestador/secret manager,
   **nunca** en el repositorio.

> Evidencia de no-rotación: los valores de desarrollo (`admin/admin…`,
> `postgres/<clave>`, secreto JWT) siguen siendo los mismos. No se afirma rotación
> alguna.

---

## 7. Veredicto de esta actividad

**SANEAMIENTO COMPLETADO en código, configuración y documentación.**
Los secretos de producción **no existen en el repositorio** y la aplicación
productiva falla de forma segura si no se le entregan por variables de entorno.
Queda **pendiente** la rotación de credenciales de desarrollo y la custodia
externa de los secretos productivos (§6), ya prevista en el plan de despliegue.
