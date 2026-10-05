# CORRECCIÓN DE ORTOGRAFÍA Y CODIFICACIÓN UTF-8 — FRONTEND INTIWATANA

**Fecha:** 2026-10-04
**Raíz:** C:\Users\USER\Downloads\intiwatana_final_v2
**Frontend:** `frontend/Web-Intiwatana` (único frontend del proyecto)
**Verificación:** escaneo estricto de bytes + ESLint + build Vite + navegador headless sobre `dist/`

## 1. Estado general

| Concepto | Resultado |
|---|---|
| Archivos fuente revisados | 43 |
| Archivos con UTF-8 inválido | 0 |
| Archivos con BOM UTF-8 | 0 |
| Archivos con mojibake | 0 |
| Archivos modificados | 5 |
| Palabras únicas en texto visible revisadas | 1 478 |
| Lint / Build | exit 0 / exit 0 |

## 2. Alcance respetado

Solo se corrigió **codificación y texto**. **No se modificó** lógica de negocio, componentes funcionales, backend, base de datos, seguridad, endpoints, enums, validaciones, modelos de datos ni ninguna funcionalidad.

## 3. Metodología (5 capas)

1. **Decodificación estricta por bytes** (`TextDecoder(fatal:true)`) → ningún archivo con bytes UTF-8 inválidos.
2. **Detección de BOM** → aucun BOM restante.
3. **Rastreo de marcadores de corrupción** (`U+FFFD`, `Ã`, `Â`, `Ø`, `â€`, CJK, cirílico) → 0 coincidencias.
4. **Inventario de caracteres no ASCII** (5 236 caracteres, 41 *code points* distintos) → todos legítimos (español, tipografía `═ ─`, flechas `→`, emoji, `ã ç` de `relação` ya corregida).
5. **Extracción del vocabulario visible completo** (1 478 palabras únicas) y contraste contra lista de palabras que exigen tilde → verificación manual palabra por palabra.

> **Advertencia metodológica:** la consola PowerShell de este host degrada UTF-8 y muestra `ú` como `ǧ`, `ó` como `ǭ`. Durante la revisión se descartó el appearance de consola y se/confió en la salida ASCII-escapada de Node (`\u00FA`, `\u00F3`). Varios falsos positivos se descartaron así.

## 4. Correcciones realizadas

| # | Archivo | Línea | Antes | Después | Tipo | Visible |
|---|---|---|---|---|---|---|
| 1 | `src/App.jsx` | 69 líneas | Texto corrupto por doble conversión UTF-8 → CP1252 → UTF-8 (`Iniciar sesion`, tildes ausentes, `¿ ¡` rotos, flechas/bullets/separadores dañados) | UTF-8 correcto | Codificación | Sí |
| 2 | `src/App.css` | (BOM) | BOM UTF-8 `EF BB BF` al inicio | Eliminado | Codificación | No |
| 3 | `src/components/PasajesPage.jsx` | 385 | `DOBLABLE` | `DOBLEABLE` | Consistencia de texto visible | **Sí** |
| 4 | `src/App.css` | 249 | `ACCESO RAPIDO` | `ACCESO RÁPIDO` | Ortografía (comentario) | No |
| 5 | `src/components/AdminDashboard.jsx` | 1311–1312 | `se envia` / `otra guia` | `se envía` / `otra guía` | Ortografía (JSDoc) | No |
| 6 | `src/components/chofer/PasajerosView.jsx` | 8 | `relação` (portugués) | `relación` | Idioma (JSDoc) | No |

**Detalle del caso `DOBLABLE`/`DOBLEABLE`:** la misma etiqueta del mapa de asientos se renderizaba de dos formas distintas en la misma pantalla (`DOBLEABLE` en las filas 2–3 y `DOBLABLE` en la fila 4). Se comprobó en el DLE de la RAE que **ninguna de las dos formas está en el diccionario** (`dle.rae.es/doblable` y `dle.rae.es/dobleable` → "no está en el Diccionario"), por lo que es jerga del sector y **no un error ortográfico**. Se normalizó a `DOBLEABLE` por ser la forma mayoritaria (2 de 3) y la que ya usan las clases CSS `.hiace-dobleable-label` / `.hiace-fold-gap`. Se modificó **solo el texto visible**; los nombres de clase quedaron intactos.

## 5. Elementos deliberadamente NO modificados

Estos tokens coinciden con palabras que llevan tilde en español, pero **son identificadores** y tocarlos rompería la aplicación:

| Token | Dónde | Por qué no se toca |
|---|---|---|
| `telefono`, `direccion`, `descripcion`, `observacion`, `metodo`, `titulo`, `subtitulo` | `PortalGestion.jsx`, `Login.jsx`, `PasajesPage.jsx` | Nombres de campo de formulario / `FormData` enviados a la API. La etiqueta visible ya lleva tilde (`Teléfono`, `Dirección`, `Descripción`, `Método`, `Título`, `Subtítulo`). |
| `guia` | `App.jsx:165,167,173` | Atributos `id`/`aria-describedby` (`guia-input`, `guia-error`). El `<label>` visible dice `Número de guía`. |
| `numero_guia`, `numero_boleto`, `ultima_actualizacion`, `referenciaPago` | varios | Nombres de campo de la API. |
| `ECONOMICO`, `SEMI_CAMA`, `EN_TRANSITO`, `EN_ALMACEN`, `CARGADO_EN_BUS`, `FRAGIL` | `AdminDashboard.jsx`, `ClienteDashboard.jsx` | Enums del backend. La etiqueta visible ya es `Económico`, `En tránsito`, `Frágil`. |
| `autorizacionComercial` | `PortalGestion.jsx:171` | Clave enviada en `FormData`. La casilla visible dice `Declaro autorización comercial`. |
| `publico` | `App.jsx:276` | Ruta de API `/api/v1/portal/publico`. |
| `.acceso-rapido`, `.hiace-dobleable-label`, `.hiace-fold-gap`, `.cd-vacio`, `.badge-transito`, `.nav-acciones` | CSS | Nombres de clase. Con ellos se rompería el estilo. |
| `ocupacion`, `seccion`, `subtitulo` como clave de objeto | varios | Claves de estado/identificador. |

## 6. Verificación posterior al build

### 6.1 ESLint
```
> eslint .
(sin salida)  →  exit 0
```

### 6.2 Build de producción
```
vite v8.0.0 building for production...
✓ 1757 modules transformed.
dist/index.html                   0.47 kB │ gzip:   0.30 kB
dist/assets/index-C7YpclSP.css   80.08 kB │ gzip:  15.84 kB
dist/assets/index-CQU2as8k.js   616.81 kB │ gzip: 157.02 kB
✓ built in 1.12s                 →  exit 0
```

### 6.3 Codificación de los artefactos generados (`dist/`)
| Artefactos | BOM | Mojibake | `charset` |
|---|---|---|---|
| 17 archivos | 0 | 0 | `UTF-8` (meta presente) |

Confirmación de que los textos corregidos llegaron al bundle:

| Texto | Ocurrencias en `dist/` |
|---|---|
| `Iniciar sesión` | 2 |
| `Número de guía` | 2 |
| `DOBLEABLE` | 3 |
| `DOBLABLE` (debe ser 0) | **0** |
| `según su estado` | 1 |

Reglas CSS tras minificar: `.acceso-rapido` ×2, `.hiace-dobleable-label` ×1, `.hiace-fold-gap` ×1 — **todas intactas**. (Vite elimina los comentarios CSS, por eso `ACCESO RÁPIDO` no aparece en `dist`: es el comportamiento esperado del minificador, no una pérdida.)

### 6.4 Navegador headless sobre `dist/` — 5 rutas públicas

| Ruta | Caracteres | Corruptos | Texto truncado |
|---|---|---|---|
| `/` | 2 269 | ninguno | ninguno |
| `/login` | 503 | ninguno | ninguno |
| `/registrar` | 548 | ninguno | ninguno |
| `/pasajes` | 318 | ninguno | ninguno |
| `/encomiendas/seguimiento` | 212 | ninguno | ninguno |

- `document.characterSet` = `UTF-8`, `<meta charset="UTF-8">` presente.
- **0 errores de consola**, **0 requests fallidos** en las 5 rutas.
- `DOBLABLE` residual = 0 en el DOM renderizado.
- Sin encabezados (`h1–h4`, `label`, `th`, `button`, `a`) corruptos.
- Sin elementos con texto recortado por CSS (`scrollWidth > clientWidth` con `overflow:hidden`).

## 7. Observaciones fuera de alcance (NO modificadas)

1. **CSV de reportes** — `ReportesPage.jsx:228` obtiene el archivo con `await r.blob()`; el contenido lo genera el backend (`/api/v1/reportes/...`). El BOM UTF-8 que Excel necesita para mostrar bien las tildes depende del servidor. **Requiere cambio en backend**, excluido por directiva.
2. **PDF de constancia de retención** — `PasajesPage.jsx:1120,1329` descarga `/api/v1/ventas/retencion/.../constancia.pdf`, generado por el backend. No aplica el alcance de frontend.
3. **PDF/boleta de venta** — `AdminDashboard.jsx:2045` y `BoletoVirtual.jsx:220` usan `window.print()` sobre el DOM: son documentos **generados en el frontend**. Su texto se corrigió/escribió en UTF-8 dentro del alcance; el PDF resultante depende de la fuente instalada en el equipo del usuario.
4. **Datos de ejemplo de la boleta** — `AdminDashboard.jsx:2158-2159` muestran `Av. El Sol 123, Cusco - Perú` y `Telf: (084) 234567`, mientras la empresa opera en Ayacucho (el 084 es código de Cusco). Son datos de relleno, no un error ortográfico; cambiarlos sería alterar contenido.
5. **Finales de línea CRLF** en 10 archivos (`App.jsx`, `App.css`, `AdminDashboard.jsx`, `PasajesPage.jsx`, `destinos.jsx`, `index.html`, `eslint.config.js`, `README.md`, `vite.svg`, `icons.svg`). Es la convención de Windows y **no afecta la codificación**; no se normalizaron para evitar ruido en los archivos.

## 8. Veredicto

**CERRADO** — El frontend queda íntegramente en UTF-8 sin BOM y sin mojibake, con la etiqueta inconsistente del mapa de asientos normalizada y las erratas de documentación corregidas. `eslint` y `vite build` terminan con exit 0, los 17 artefactos de `dist/` están limpios y las 5 rutas públicas se renderizan sin caracteres corruptos, sin palabras incorrectas y sin texto truncado. No se modificó ninguna lógica, funcionalidad, dato, endpoint ni componente funcional.
