# GESTIÓN DEL PORTAL EN EL PANEL ADMINISTRADOR

**Fecha:** 2026-10-02
**Módulo:** Gestión editorial del Portal Público (CMS acotado) desde el Panel Administrador.
**Rama funcional:** backend Spring Boot 3.3.0 / Java 17 / PostgreSQL / Flyway + frontend React/Vite.

---

## 1. Objetivo y alcance

Permitir que un administrador autenticado (`ROLE_ADMIN`) administre el **contenido editorial** del Portal Público (Hero, Destinos, Beneficios, Nosotros, Contacto, Footer), con:

- estados **BORRADOR / PUBLICADO / ARCHIVADO**;
- **historial de versiones** con comparación, restauración y auditoría;
- **validación y sanitización en el backend**;
- **carga de imágenes con validación real** (firma/magic bytes, dimensiones, peso, nombre seguro, duplicados) y metadatos/licencia/autorización;
- **almacenamiento abstraído** (interfaz sustituible);
- Portal Público que consume **solo lo PUBLICADO**, con fallback que nunca rompe buscador, reserva, login, carrito, encomiendas ni tracking.

**Fuera de alcance (no modificado):** precios, rutas operativas, horarios, disponibilidad, asientos, viajes, pagos, boletos, QR, estados de encomiendas, tracking, roles, permisos, JWT, contraseñas, endpoints operativos, CORS, secretos, migraciones previas.

---

## 2. Restricciones cumplidas

| Restricción | Cumplimiento |
|---|---|
| No crear otro Portal Público | Se reutiliza el existente; solo se le agregó **consumo** de contenido publicado con fallback. |
| No reconstruir el Panel Admin | `AdminDashboard.jsx` solo recibió un ítem de menú y el render de la nueva vista. |
| No editar HTML/JS/CSS/SQL desde el panel | El contenido es **texto plano validado** (se rechaza `<`/`>` y caracteres de control); sin campos de código. |
| No imágenes sin validación | `PortalImageValidator` valida firma real, MIME, dimensiones, peso y checksum. |
| El contenido editorial no controla datos operativos | El JSON solo admite claves de una **whitelist por sección**; `precio`, `horario`, etc. son rechazados (G08). |
| Migraciones aditivas | V3 crea 3 tablas nuevas; no altera ni borra nada existente. |
| No borrar historial físicamente | `contenido_portal_version` es **append-only**; no hay endpoint de borrado. |
| No publicar imagen sin autorización | Se exige `autorizacionComercial=true`, `textoAlt`, `autor`, `fuente` y `licencia` (G17/G18). |
| No afirmar producción sin almacenamiento persistente | Se documenta el bloqueo exacto y el veredicto es condicionado (§13). |

---

## 3. Matriz CONTENIDO → UBICACIÓN ACTUAL → EDITABLE → FUENTE → VALIDACIÓN → PERMISO → IMPACTO

| Contenido | Ubicación actual (antes) | ¿Editable? | Fuente | Validación aplicación | Permiso | Impacto |
|---|---|---|---|---|---|---|
| Título/subtítulo del Hero | `App.jsx` (landing) / literal | **Sí** | Panel Portal › HERO | longitud, texto plano, sin etiquetas | `ROLE_ADMIN` (escritura) | Solo presentación |
| Imagen de fondo del Hero | `/assets/img/rutas/vilcas.jpg` (ruta local) | **Sí** (URL validada o imagen subida) | HERO / imágenes | URL `^(https?://|/)[^\s<>"']+$` o `/media/portal/...` | `ROLE_ADMIN` | Solo presentación |
| Imagen del bus / `mostrarBus` | no existía | **Sí** (URL + booleano) | HERO | URL segura + booleano | `ROLE_ADMIN` | Solo presentación |
| Alt / autor / fuente / licencia / atribución del Hero | no existía | **Sí** | HERO / imágenes | texto plano, longitudes | `ROLE_ADMIN` | Trazabilidad editorial |
| Destinos destacados (título, texto, imagen, alt, orden, visible) | `data/destinos.jsx` `DESTINOS` | **Sí** | DESTINOS | clave permitida por ítem; **rechaza `precio`** | `ROLE_ADMIN` | Solo listado editorial; **no** toca tarifas/horarios reales |
| Beneficios (título, descripción, icono, orden, visible) | `App.jsx` literales | **Sí** | BENEFICIOS | icono en whitelist; texto plano | `ROLE_ADMIN` | Solo presentación |
| Nosotros (título, descripción, imagen, alt, fuente, licencia) | `App.jsx` literales | **Sí** | NOSOTROS | texto plano, URL segura | `ROLE_ADMIN` | Solo presentación |
| Contacto (teléfono, correo, dirección, horario, redes, visible) | `App.jsx` literales | **Sí** | CONTACTO | texto plano, URL de red segura; nace `visible:false` | `ROLE_ADMIN` | Solo presentación |
| Footer (descripción, enlaces, info legal, créditos) | `App.jsx` literales | **Sí** | FOOTER | textos planos + URLs seguras | `ROLE_ADMIN` | Solo presentación |
| Precios, rutas, horarios, asientos, viajes, pagos, boletos, QR, encomiendas, tracking, roles, JWT, secretos, código | Servicios operativos reales | **No** | — | no expuestos al CMS; clave desconocida ⇒ `400` | — | Sin impacto; intactos |

---

## 4. Modelo de datos (migración V3)

`backend/bus-system/src/main/resources/db/migration/V3__gestion_portal.sql` — aditiva, estilo de V1/V2.

### 4.1 `contenidos_portal`
Estado vivo por sección. Columnas: `id`, `seccion` (UNIQUE), `estado`, `version`, `contenido_borrador` (jsonb), `contenido_publicado` (jsonb), `autor`, `actualizado_por`, `publicado_en`, timestamps + `deleted_at`, `activo`.
- Check `contenidos_portal_estado_check` ⇒ `BORRADOR | PUBLICADO | ARCHIVADO`.
- Check `contenidos_portal_seccion_check` ⇒ `HERO | DESTINOS | BENEFICIOS | NOSOTROS | CONTACTO | FOOTER`.

### 4.2 `contenido_portal_version` (append-only, auditoría)
`id`, `contenido_id` (FK), `seccion`, `version`, `estado`, `accion`, `contenido` (jsonb), `usuario_id`, `username`, `rol`, `observacion`, `fecha_hora`.
- Índice `idx_contenido_portal_version_seccion (seccion, version DESC)`.

### 4.3 `imagenes_portal`
`id`, `seccion`, `nombre_interno`, `url`, `miniatura_url`, `mime`, `peso_bytes`, `ancho`, `alto`, `checksum`, `texto_alt`, `autor`, `fuente`, `licencia`, `url_origen`, `autorizacion_comercial`, `subido_por`, timestamps.
- Índice `idx_imagenes_portal_checksum`.

### 4.4 Evidencia de migración (base QA aislada, desde vacío)

```
 version |   description   | success
---------+-----------------+---------
 1       | esquema inicial | t
 2       | catalogos base  | t
 3       | gestion portal  | t
```

Hibernate arrancó con `ddl-auto: validate` (perfil `test`) sin errores ⇒ el mapeo de entidades coincide con el esquema Flyway.

---

## 5. Endpoints y permisos

| Método | Ruta | Permiso | Descripción |
|---|---|---|---|
| GET | `/api/v1/portal/admin/secciones` | ADMIN | Lista las 6 secciones con estado/versión/borrador |
| GET | `/api/v1/portal/admin/secciones/{seccion}` | ADMIN | Detalle de una sección |
| PUT | `/api/v1/portal/admin/secciones/{seccion}/borrador` | ADMIN | Guardar borrador (valida y versiona) |
| POST | `/api/v1/portal/admin/secciones/{seccion}/publicar` | ADMIN | Publicar borrador (bloquea doble publicación) |
| POST | `/api/v1/portal/admin/secciones/{seccion}/archivar` | ADMIN | Archivar sección |
| POST | `/api/v1/portal/admin/secciones/{seccion}/restaurar?versionId=` | ADMIN | Restaurar versión del historial |
| GET | `/api/v1/portal/admin/secciones/{seccion}/historial` | ADMIN | Historial de versiones |
| GET | `/api/v1/portal/admin/imagenes` | ADMIN | Imágenes registradas |
| POST | `/api/v1/portal/admin/imagenes` (multipart) | ADMIN | Subir imagen con metadatos |
| GET | `/api/v1/portal/admin/almacenamiento` | ADMIN | Estado del almacenamiento |
| GET | `/api/v1/portal/publico` | **público** | Solo contenido PUBLICADO |
| GET | `/media/portal/**` | **público** | Sirve las imágenes subidas |

Reglas en `SecurityConfig`: `GET /api/v1/portal/publico` y `GET /media/portal/**` son `permitAll`; `/api/v1/portal/admin/**` exige `hasRole('ADMIN')`. Sin sesión ⇒ `401`; autenticado sin rol ⇒ `403`.

---

## 6. Validación y sanitización (backend)

`ContenidoPortalValidator` aplica, por sección:

- **whitelist de claves** (una clave no permitida ⇒ `400`; p. ej. `precio` en DESTINOS);
- **texto plano**: rechaza `<`, `>` y caracteres de control;
- **longitudes máximas** por campo;
- **URLs** con patrón `^(https?://|/)[^\s<>"']+$` (rechaza `javascript:`, `data:`, etc.);
- **iconos** de una whitelist (`ticket, package, mappin, shield, clock, user, bus, route, heart, star, check, info, phone, mail`).

La validación es **de servidor**; React solo mejora la experiencia (no es la única barrera).

---

## 7. Validación de imágenes

`PortalImageValidator`:

1. **Firma real (magic bytes):** JPEG `FF D8 FF`, PNG `89 50 4E 47 0D 0A 1A 0A`, WebP `RIFF....WEBP`. Rechaza cualquier otro formato aunque cambie la extensión.
2. **Prohibidos:** SVG, HTML/JS, ejecutables, documentos, doble extensión y rutas con `..`.
3. **Dimensiones:** mínimo 200×200, máximo 6000×6000 (JPEG/PNG vía ImageIO; WebP con parser propio VP8/VP8L/VP8X).
4. **Peso:** máx. 5 MB configurables.
5. **Nombre seguro y miniatura única**; se calcula **checksum SHA-256** y se rechaza duplicado idéntico.
6. **Metadatos obligatorios:** `textoAlt`, `autor`, `fuente`, `licencia` y `autorizacionComercial=true`.
7. **Miniatura** generada solo para JPEG/PNG (máx. 400 px de lado).

---

## 8. Almacenamiento (abstracción y bloqueo)

- Interfaz `PortalStorageService` con implementación `LocalPortalStorageService` (disco) y modo `none` que **deshabilita la subida**.
- Configuración `app.portal.*`: `media-prefix` (`/media/portal`), `storage.tipo` (`local|none`), `storage.directorio`, límites de imagen; multipart `5MB/8MB`.
- `PortalWebConfig` publica `/media/portal/**` **solo si** el almacenamiento local está activo.

**Bloqueo exacto (NO resuelto):** no existe almacenamiento de objetos (S3/R2/GCS) ni disco persistente configurado para Render. Con `storage.tipo=local` las cargas viven en el **filesystem efímero** y se pierden al reiniciar el contenedor. El endpoint de estado lo declara explícitamente: `{ "disponible": true, "tipo": "local", "persistente": false }` (G22).

---

## 9. Frontend

### 9.1 Panel Administrador
- `PortalGestion.jsx`: 6 pestañas con **badge de estado** (PUBLICADO/BORRADOR/ARCHIVADO), editor por sección, arrays editables (destinos, beneficios, enlaces, redes), acciones **Guardar borrador / Publicar / Archivar / Historial**, **vista previa** desktop/tablet/móvil, subida de imagen con metadatos y **aviso de almacenamiento no persistente**.
- `AdminDashboard.jsx`: nuevo ítem de menú **“Gestión del Portal”** (icono `LayoutTemplate`), render bajo `isAdmin` y envuelto en `ErrorBoundary`.

### 9.2 Portal Público
- `App.jsx` consume `/api/v1/portal/publico` con **fallback** a los valores locales si el endpoint falla o devuelve vacío. Datos operativos (rutas, horarios, precios reales) siguen viniendo de los servicios reales.
- `App.css`: el hero usa la variable `--hero-image` (recurso local `vilcas.jpg`).

### 9.3 Proxy de desarrollo
- `vite.config.js` ahora proxya también **`/media`** (además de `/api`). Sin esto, las imágenes `/media/portal/**` no resolverían en desarrollo (las vistas previas y el portal romperían). No afecta al bundle de producción.

---

## 10. Pruebas funcionales G01–G30

Ejecutadas contra el backend con perfil `test`, base **QA aislada `intiwatana_portal_qa`** (creada desde vacío) en `:8081`, y el frontend en `:5173` apuntando al backend QA.

| # | Prueba | Esperado | Obtenido | Estado |
|---|---|---|---|---|
| G01 | GET admin sin sesión | 401 | 401 | OK |
| G02 | GET admin con token CLIENTE | 403 | 403 | OK |
| G03 | GET admin con token ADMIN | 200 | 200 | OK |
| G04 | Publicar HERO | 200 | 200 | OK |
| G05 | `/publico` incluye HERO tras publicar | SÍ | SÍ | OK |
| G06 | Republicar sin cambios | 409 | 409 | OK |
| G07 | Borrador con `<script>` | 400 | 400 | OK |
| G08 | DESTINOS con clave `precio` | 400 | 400 | OK |
| G09 | HERO con URL `javascript:` | 400 | 400 | OK |
| G10 | Guardar borrador válido | 200 | 200 | OK |
| G11 | Publicar v2 | 200 | 200 | OK |
| G12 | Historial HERO | 200 / ≥2 versiones | 200 / 4 versiones | OK |
| G13 | `/publico` refleja título v2 | título v2 | `Viaja con INTIWATANA (v2)` | OK |
| G14 | Falso `.png` (texto) | 400 | 400 | OK |
| G15 | SVG con script | 400 | 400 | OK |
| G16 | PNG 50×50 bajo mínimo | 400 | 400 | OK |
| G17 | PNG sin licencia | 400 | 400 | OK |
| G18 | PNG sin autorización comercial | 400 | 400 | OK |
| G19 | PNG válido con metadatos | 201 | 201 (url, miniatura, checksum) | OK |
| G20 | Imagen duplicada (mismo checksum) | 409 | 409 | OK |
| G21 | JPEG válido con metadatos | 201 | 201 | OK |
| G22 | Estado de almacenamiento | — | `disponible:true, tipo:local, persistente:false` | OK (declara no persistente) |
| G23 | Listar imágenes | 200 | 200 (2 registros) | OK |
| G24 | GET `/media/portal/**` anónimo | 200 | 200 | OK |
| G25 | Historial con versiones | ≥2 | 4 (más antigua v0/BORRADOR) | OK |
| G26 | Restaurar versión | 200 | 200 | OK |
| G27 | Archivar sección | 200 | 200 | OK |
| G28 | `/publico` tras archivar | sin HERO | sin HERO | OK |
| G29 | `/api/v1/rutas` operativo | 200 | 200 | OK |
| G30 | `/publico` sin claves internas | sin usuario/rol/historial | `{}` sin esos campos | OK |

### 10.1 UI (navegador headless, `:5173`)
- Menú **“Gestión del Portal”** presente; al abrirlo se listan las 6 pestañas: **Hero (PUBLICADO)**, Destinos, Beneficios, Nosotros, Contacto, Footer (BORRADOR), con botones **Guardar borrador, Publicar, Archivar, Historial, Subir imagen**.
- **0 errores de consola**, **0 peticiones fallidas**.
- **Sin desbordamiento horizontal** en 1440 / 834 / 390 px (`scrollWidth - innerWidth = 0`).
- Portal público renderiza el hero **PUBLICADO** (`Viaja con INTIWATANA`) con el buscador intacto.

### 10.2 Quality gates

| Gate | Resultado |
|---|---|
| `npm run lint` | 0 errores |
| `npm run build` | OK (vite v8.0.0; `index-DgJ4aMDl.js` 560.36 kB, css 80.07 kB; aviso de chunk >500 kB **preexistente**, no bloqueante) |
| `mvn verify` | **BUILD SUCCESS** — 54 tests, 0 fallos, 0 errores |
| Flyway V1→V3 + `ddl-auto=validate` | OK desde base vacía |

---

## 11. Datos operativos y aislamiento

- Todas las pruebas de escritura se ejecutaron sobre la base **QA aislada `intiwatana_portal_qa`** montada desde cero; **no** se tocó la base de desarrollo ni producción.
- El contenido editorial inicial son **valores legítimos actuales** del portal (destinos reales, datos de empresa), cargados como **BORRADOR**; nada se publica automáticamente.
- Contacto nace con `visible:false` y sin teléfonos/correos inventados (solo la dirección legítima).
- `GET /api/v1/rutas` (dato operativo) respondió 200 con el sistema portal activo ⇒ sin interferencia (G29).

---

## 12. Archivos principales

**Backend (nuevos):** `entity/{ContenidoPortal, ContenidoPortalVersion, ImagenPortal}.java`; `repository/{ContenidoPortalRepository, ContenidoPortalVersionRepository, ImagenPortalRepository}.java`; `dto/request/{ContenidoPortalRequest, PublicacionPortalRequest}.java`; `dto/response/{ContenidoPortalResponse, ContenidoPortalVersionResponse, ImagenPortalResponse}.java`; `service/{PortalStorageService, PortalImageValidator, PortalActor, PortalActorResolver, ContenidoPortalValidator, ContenidoPortalService, PortalImagenService}.java`; `service/impl/{LocalPortalStorageService, ContenidoPortalServiceImpl, PortalImagenServiceImpl}.java`; `config/PortalWebConfig.java`; `controller/{PortalAdminController, PortalPublicoController}.java`; `db/migration/V3__gestion_portal.sql`.

**Backend (modificados):** `config/SecurityConfig.java`, `src/main/resources/application.yml`.

**Frontend:** nuevo `src/components/PortalGestion.jsx`; modificados `src/components/AdminDashboard.jsx`, `src/App.jsx`, `src/App.css`, `vite.config.js`.

---

## 13. Limitaciones y veredicto

**Limitaciones:**
1. **Almacenamiento no persistente** (bloqueo principal) — `local` sobre FS efímero; falta proveedor de objetos o disco persistente. El sistema lo declara y no se afirma producción.
2. La interpretación de imágenes por parte del entorno de QA es limitada (el modelo no lee PNG), por lo que la verificación visual se sustituyó por aserciones de DOM/estado y capturas almacenadas.
3. El contenido inicial queda como **BORRADOR**; publicar cada sección es una decisión del administrador.

**Veredicto:**

> ### GESTIÓN DEL PORTAL — FUNCIONAL, NO LISTA PARA PRODUCCIÓN
> La funcionalidad editorial está **completa, validada y verificada** (G01–G30, `mvn verify` 54 tests, lint/build, UI sin errores). La razón de no declararla lista para producción es **exclusivamente el almacenamiento de imágenes no persistente** (§8), que debe resolverse con un proveedor de objetos o disco persistente antes del cierre definitivo.

---

## 14. Trabajo pendiente para el cierre

1. Implementar almacenamiento persistente (p. ej. S3/R2/GCS o volumen persistente) tras `PortalStorageService`, sin tocar el resto del módulo; re-ejecutar G19–G24.
2. (No bloqueante) Sustitución editorial del hero por los recursos oficiales `bus-institucional.webp` y `laguna-intiwatana.webp` cuando se entreguen.

---

## 15. Matriz de pruebas ↔ requisito

| Requisito | Pruebas |
|---|---|
| Permisos admin/público | G01–G03, G30 |
| Flujo borrador/publicar/archivar/restaurar | G04–G06, G10–G13, G25–G28 |
| Validación y sanitización | G07–G09 |
| Validación real de imágenes | G14–G21 |
| Auditoría/versiones | G12, G25, G26 |
| Solo PUBLICADO en portal público | G05, G13, G28, G30 |
| Sin impacto operativo | G29 |
| Almacenamiento declarado | G22 |
| Frontend/UX | §10.1, lint/build |
| Migraciones aditivas | §4.4, `mvn verify` |
