# CORRECCIÓN ORTOGRÁFICA Y UTF-8 — SISTEMA INTIWATANA

Fecha: 2026-10-09
Ámbito: `C:\Users\USER\Downloads\intiwatana_final_v2`
Alcance acordado con el usuario: **solo frontend** (no se modificaron servicios,
controladores, DTOs, entidades, repositorios, migraciones ni base de datos).

---

## 1. Resumen

El daño era **mojibake por doble codificación Windows‑1252 → UTF‑8**: los bytes
UTF‑8 originales fueron interpretados como Windows‑1252 y vueltos a guardar como
UTF‑8. Ejemplo: `ó` (bytes `C3 B3`) → `Ã³`.

El daño estaba concentrado en **un solo archivo de la interfaz**:

- `frontend/Web-Intiwatana/src/components/AdminDashboard.jsx`

Los otros 37 archivos con caracteres no ASCII del frontend ya estaban
correctamente codificados en UTF‑8 y **no se tocaron**.

Se corrigieron **306 líneas / 2 634 secuencias mojibake** en ese archivo, sin
cambiar ni un solo carácter ASCII (código, llaves, comillas, imports, hooks,
estados, lógica, endpoints o diseño).

---

## 2. Metodología de detección

1. Barrido recursivo de todo el repositorio (376 archivos de código, excluyendo
   `node_modules`, `target`, `dist`, `.git`, etc.) buscando los patrones
   `Ã.`, `Â.`, `â€.`, `â†.`, `â”`, `â•`, `ï»¿` y el carácter de reemplazo `U+FFFD`.
2. Para cada archivo con daño se probó la reversión `Windows‑1252 → UTF‑8`
   (`Buffer.from(texto, 'cp1252').toString('utf8')`).
3. Se detectaron dos sub‑casos:
   - Archivos “correctos” (cuyo texto ya era UTF‑8 válido): la reversión produce
     `U+FFFD` ⇒ **no se transforman** (falso positivo).
   - `AdminDashboard.jsx`: reversión limpia ⇒ **archivo realmente dañado**.
4. La corrección se aplicó únicamente a las secuencias mojibake reales,
   dejando intacto todo lo demás (se verificó que la “huella” ASCII de cada
   línea permaneciera idéntica).

---

## 3. Archivos revisados

### Frontend `frontend/Web-Intiwatana/src/` (40 archivos)
`App.jsx`, `App.css`, `admin.css`, `index.css`, `chofer.css`, `main.jsx`,
`data/destinos.jsx`, `context/*`, `components/AdminDashboard.jsx`,
`components/Login.jsx`, `components/PortalGestion.jsx`, `components/PasajesPage.jsx`,
`components/ReportesPage.jsx`, `components/ClienteDashboard.jsx`,
`components/ChoferDashboard.jsx`, `components/BoletoVirtual.jsx`,
`components/CrudUsuarios.jsx`, `components/CrudAsientos.jsx`,
`components/chofer/*`, `components/caja/*`, `components/portal-gestion.css`,
`components/reserva-premium.css`, etc.

Resultado: 2 archivos 100 % ASCII, 37 con UTF‑8 ya correcto y **1 dañado**.

### Resto del repositorio
Se barrió también `backend/`, `database/`, `frontend/Web-Intiwatana/*` y
`reports/`. Ver sección 8 (pendientes) para el backend.

---

## 4. Archivos modificados

| Archivo | Cambios | Descripción |
|---|---|---|
| `frontend/Web-Intiwatana/src/components/AdminDashboard.jsx` | 306 líneas / 2 634 secuencias | Reversión Windows‑1252 → UTF‑8 solo en textos dañados |

Copia de respaldo del original:
`C:\Users\USER\AppData\Local\Temp\opencode\backup\AdminDashboard.jsx.orig`

---

## 5. Textos anteriores → textos corregidos

Todas las sustituciones aplicadas (run dañado → carácter correcto, con conteo):

| Antes (mojibake) | Después | Unicode | Veces |
|---|---|---|---|
| `â•` | ═ | U+2550 | 1558 |
| `â”€` | ─ | U+2500 | 748 |
| `Ã³` | ó | U+00F3 | 100 |
| `â€”` | — | U+2014 | 74 |
| `Ã©` | é | U+00E9 | 26 |
| `Ã­` | í | U+00ED | 23 |
| `Ã¡` | á | U+00E1 | 21 |
| `Â·` | · | U+00B7 | 20 |
| `Ãº` | ú | U+00FA | 17 |
| `â†’` | → | U+2192 | 14 |
| `Ã“` | Ó | U+00D3 | 10 |
| `Ã±` | ñ | U+00F1 | 7 |
| `Â°` | ° | U+00B0 | 5 |
| `Â¿` | ¿ | U+00BF | 2 |
| `â€¢` | • | U+2022 | 2 |
| `âš ` | ⚠ | U+26A0 | 2 |
| `Ãš` | Ú | U+00DA | 1 |
| `Ã` (C3 8D) | Í | U+00CD | 1 |
| `Ã‰` | É | U+00C9 | 1 |
| `Ã` (C3 81) | Á | U+00C1 | 1 |
| `Â§` | § | U+00A7 | 1 |

Ejemplos de textos visibles ya corregidos y verificados en el bundle:

- `ADMINISTRACIÃ“N` → **ADMINISTRACIÓN**
- `OPERACIÃ“N` → **OPERACIÓN**
- `CONFIGURACIÃ“N` → **CONFIGURACIÓN**
- `GESTIÃ“N` → **GESTIÓN**
- `GestiÃ³n del Portal` → **Gestión del Portal**
- `OcupaciÃ³n promedio` → **Ocupación promedio**
- `PrÃ³ximas salidas` → **Próximas salidas**
- `Ãšltimas encomiendas` → **Últimas encomiendas**
- `AdministraciÃ³n` → **Administración**
- `OperaciÃ³n` → **Operación**
- `Â·` → **·**
- `MÃ³dulos del panel` → **Módulos del panel**
- `Cerrar sesiÃ³n` → **Cerrar sesión**
- `TelÃ©fono` → **Teléfono**, `DirecciÃ³n` → **Dirección**, `AÃ±o` → **Año**,
  `MÃ©todo` → **Método**, `GuÃ­a` → **Guía**, `acciÃ³n` → **acción**

Nota: las secuencias `â•` / `â”€` aparecen **solo dentro de comentarios y
selectores CSS** (separadores decorativos); se comprobó que ninguna está dentro
de un literal de cadena.

---

## 6. Origen de cada problema

| Origen | Estado |
|---|---|
| F. Codificación incorrecta del archivo fuente frontend | **Corregido** (`AdminDashboard.jsx`) |
| A. Texto codificado directamente en frontend | **Corregido** (mismo archivo) |
| B. Respuesta del backend con mojibake | **Pendiente** (ver sección 8) |
| C. Base de datos | Sin hallazgos en `database/` (ver sección 8) |
| D. Archivo de configuración | Sin mojibake |
| E. Transformación incorrecta en JavaScript | No aplica (no hay `decode`/`encode` defectuoso) |

El frontend no contenía conversiones manuales de codificación; el daño era del
propio archivo fuente, por lo que la corrección se hizo a nivel de texto.

---

## 7. Codificación final y pruebas

### Codificación
- `AdminDashboard.jsx`: **UTF‑8 con BOM** (igual que el original; el BOM ya
  existía y se preservó). Sin `U+FFFD`, sin controles C1, sin mojibake.
- Resto del frontend: UTF‑8 sin cambios.

### Verificaciones ejecutadas
- Barrido de mojibake en `frontend/`: **0 archivos afectados** (de 46).
- Comprobación de que cada línea modificada conserva **idéntica huella ASCII**
  (0 diferencias de código): `changedLines=306, asciiChangedLines=0`.
- Bundle compilado: contiene `Administración`, `Operación`, `CONFIGURACIÓN`,
  `GESTIÓN`, `Gestión del Portal`, `Próximas salidas`, `Últimas encomiendas`,
  `Ocupación promedio`, `Módulos del panel`, `Cerrar sesión`, `Teléfono`,
  `Dirección`, `Año`, `Método`, `Guía`, `acción`; sin `Ã` ni `U+FFFD`.

### `npm run lint`
```
> eslint .
LINT_EXIT=0
```

### `npm run build`
```
vite v8.0.0 building client environment for production...
✓ 1765 modules transformed.
dist/index.html   0.47 kB
dist/assets/index-*.css  132.26 kB
dist/assets/index-*.js   772.28 kB
✓ built in 2.82s
BUILD_EXIT=0
```
(Único aviso: chunks > 500 kB, advertencia preexistente de Vite; no es error.)

### Revisión visual (regresión corta)
`vite preview` + navegador headless sobre `http://localhost:4173/`:

- Título: `web-intiwatana`
- `console errors/warnings (0)`
- `requests failed (0)`
- Sin overlay de Vite
- Texto renderizado correcto: *"Emp. de Transportes INTIWATANA S.R.L. …
  Iniciar sesión Registrarse … Conectamos destinos con seguridad, comodidad y
  atención cercana."*
- `hasMojibake: false` en el `innerText` del `body`.

No se crearon operaciones nuevas. No se modificó navegación, formularios,
peticiones HTTP, estados, temporizadores, PDF de constancia, boleto, QR ni mapa
de asientos.

> Nota: los paneles autenticados (Admin/Cajero/Chofer/Cliente) no pudieron
> recorrerse en vivo porque requieren backend activo; su texto fue validado en
> el bundle compilado y en el fuente corregido.

---

## 8. Pendientes (no corregidos en esta tarea)

Por decisión explícita del alcance (**solo frontend**), quedan sin modificar los
mensajes generados por el backend. Están dañados con el mismo mojibake y son
**visibles** en la interfaz cuando ocurre el error. Se documentan para una
corrección controlada posterior (requeriría `mvn clean verify`).

| Archivo | Línea | Texto dañado (origen) | Tipo |
|---|---|---|---|
| `backend/.../controller/RutaController.java` | 61 | `"Ya existe una ruta con cÃ³digo: "` | Visible (error) |
| `backend/.../controller/RutaController.java` | 90 | `"Ya existe una ruta con cÃ³digo: "` | Visible (error) |
| `backend/.../service/impl/VentaServiceImpl.java` | 285 | `"El viaje ya partiÃ³ o no estÃ¡ disponible"` | Visible (error) |
| `backend/.../service/impl/VentaServiceImpl.java` | 319 | `"El asiento ... ya no estÃ¡ disponible"` | Visible (error) |
| `backend/.../service/impl/VentaServiceImpl.java` | 577 | `"La reserva ya no estÃ¡ vigente"` | Visible (error) |
| `backend/.../service/impl/VentaServiceImpl.java` | 583 | `"El pago posterior en caja no estÃ¡ habilitado"` | Visible (error) |
| `backend/.../service/impl/VentaServiceImpl.java` | 659 | `"La reserva ya no estÃ¡ vigente y no puede confirmarse"` | Visible (error) |
| `backend/.../service/impl/VentaServiceImpl.java` | 671 | `"La reserva venciÃ³; los asientos fueron liberados"` | Visible (error) |
| `backend/.../service/impl/VentaServiceImpl.java` | 701 | `" ya no estÃ¡ retenido por esta reserva"` | Visible (error) |
| `backend/.../service/impl/VentaServiceImpl.java` | 994 | `"La reserva ya estÃ¡ confirmada; descargue el boleto de viaje"` | Visible (error) |
| `backend/.../service/impl/VentaServiceImpl.java` | 1024 | `"El boleto no estÃ¡ vigente"` | Visible (error) |
| `backend/.../service/impl/VentaServiceImpl.java` | 1106 | `r.setMotivoLiberacion("Vencimiento automÃ¡tico")` | Dato persistido / visible |
| `backend/.../service/impl/VentaServiceImpl.java` | 1380 | `"Pago confirmado. Boleto vÃ¡lido para embarque."` | Visible (mensaje) |

Además, el mismo mojibake aparece en **comentarios** de
`RutaController.java` (23, 88) y `VentaServiceImpl.java`
(135, 143, 145, 147, 151, 211, 633, 649, 677, 829, 879, 1230), y en los
separadores decorativos `â”€`/`â•` de
`backend/.../test/AuditoriaFinalIT.java` (623 ocurrencias, solo comentarios,
no visible). No se modificaron por no ser texto de interfaz y por la restricción
de no alterar backend.

Respecto a **base de datos**: el barrido de `database/` no encontró mojibake en
scripts ni migraciones. No se ejecutó ningún `UPDATE` ni se tocaron registros
históricos. Si en tiempo de ejecución apareciera un nombre/valor con mojibake,
habría que identificar tabla y columna y tratarlo individualmente (no se
modificó nada por suposición).

---

## 9. Veredicto

**CORRECCIÓN ORTOGRÁFICA Y UTF‑8 INTIWATANA — FRONTEND: COMPLETADA Y VERIFICADA**

- No quedan caracteres dañados visibles en el frontend.
- `npm run lint` → **EXIT 0**.
- `npm run build` → **EXIT 0**, 0 errores de sintaxis, 0 *Invalid Unicode escape
  sequence*, 0 imports rotos, 0 `ReferenceError`, sin overlay de Vite.
- Navegación principal (portal público y login) renderiza correctamente.

Pendiente declarado: los mensajes de error del backend enumerados en la
sección 8 (fuera del alcance autorizado para esta tarea).
