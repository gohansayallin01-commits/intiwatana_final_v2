# LOGIN PREMIUM — INTIWATANA — RESULTADO DE VERIFICACIÓN

**Proyecto:** intiwatana_final_v2
**Fecha de la corrida QA:** 2026-10-08
**Alcance:** Rediseño oficial del Login sin selector de roles, con backend como única fuente de identidad y rol + Gestión del Portal (sección LOGIN).
**Veredicto final:** **FUNCIONAL Y VERIFICADO EN QA — ALMACENAMIENTO PENDIENTE PARA PRODUCCIÓN**
**Etiqueta N.º 43:** `Almacenamiento local no persistente.`

---

## Resumen ejecutivo

El Login fue reconstruido a versión "premium" (sin selector de roles), el backend es la única
fuente de identidad/rol, y el rol dirige la redirección oficial:

- `CLIENTE → /pasajes`
- `CHOFER → /chofer`
- `ADMIN / CAJERO / VENDEDOR / OPERADOR → /admin`

`ROLE_ADMIN` administra el contenido visual del Login en **Gestión del Portal → LOGIN**
(borrador / publicar / historial / restaurar / archivar), y el endpoint público
`GET /api/v1/portal/publico` es la **única fuente** desde la que el Login renderiza.

Pruebas automatizadas (navegador real + API, harness con JS-click):

| Suite | Casos | OK | FAIL | Notas |
|-------|------:|---:|-----:|-------|
| L — Métricas / Layout / Responsive | 8 viewports (1440→375) | ✅ | 0 | Ver `01-metricas-layout.json` |
| V — Flujos de autenticación | 26 | **26** | **0** | `02-v-auth-flows.json` |
| A — Gestión del Portal (LOGIN) | 43 | **43** | **0** | `03-a-gestion.json` |
| Imagen del portal en el Login (visual) | 3 | **3** | **0** | `03c-imagen-visual.json` |
| P — Público / render final | 7 | 6 ⚠ | 1 | Defecto de DEV documentado (ver §4) |

Total: **75/78 casos OK + 3 visuales OK**. El único fallo (P07) corresponde a un defecto
**de desarrollo del backend** (servir imágenes), ya corregido en código y pendiente de
re-verificación en vivo por restricción del entorno (§4).

---

## 1. Suite L — Métricas de layout y responsive

Viewports evaluados: `1440` (desktop), `1280`, `1200`, `1024`, `860`, `768`, `420`, `375`.

Conclusiones verificadas en `01-metricas-layout.json`:

- Layout partido desktop/tablet con panel izquierdo (`frac ≈ 0.57`) e imagen/imagen
  codificada a colores institucionales; en mobile la imagen pasa a cabecera.
- Tarjeta de login con `border-radius ≈ 26px` (rango 22–28) y animación `login-card-in 0.35s`.
- Inputs con altura mínima **50px** y `font-size 16px` (evita zoom iOS).
- **Sin verificador de roles** y **sin "¿Olvidaste tu contraseña?"** en el login (decisiones premium).
- `prefers-reduced-motion` respetada (bloqueo de animaciones).
- Duración de animaciones dentro de 180–500 ms.
- Frase institucional exacta presente.
- Evidencia: `01a-login-desktop.png`, `01c-login-tablet-860.png`, `01e-login-mobile-420.png`, `01g-login-mobile-375.png`.

## 2. Suite V — Flujos de autenticación (26/26 OK)

Claves del `02-v-auth-flows.json`:

- V01–V04: login administrativo y por rol (ADMIN, CAJERO, VENDEDOR, OPERADOR) → redirección `/admin`.
- V05–V06: CLIENTE → `/pasajes`, CHOFER → `/chofer`.
- V07/V08: credenciales incorrectas → `401` + mensaje exacto
  "Usuario o contraseña incorrectos.".
- V09–V11: cierre de sesión / limpieza de estado `/login`.
- V12: toggle mostrar/ocultar contraseña.
- V14: sesión vencida → "Tu sesión venció. Inicia sesión nuevamente."
  (evidencia `02e-login-sesion-vencida.png`).
- V16: servicio caído → "No pudimos conectar con el servicio. Inténtalo nuevamente."
  (`02d-login-error-red.png`).
- V17: `/login` con sesión activa redirige al dashboard del rol.
- V21: mensajes de error en `role=alert` + `aria-live`.
- V24: ningún valor de contraseña queda en el DOM tras el error.

## 3. Suite A — Gestión del Portal / sección LOGIN (43/43 OK)

`03-a-gestion.json` (A00–A39, incluidas validaciones A11v–A23v y galería A37a–A37d):

- A00: token ADMIN obtenido. A01–A04: editor LOGIN (aviso "solo se usa lo PUBLICADO",
  banner de almacenamiento, campos presentes, botón **Subir imagen habilitado**).
- A05–A09: editar borrador por UI → guardar → **publicar** → el público `(publico)` expone el
  contenido exactamente como lo publicó el admin (single source).
- A10–A24: validaciones reales del backend (400 con mensajes de negocio): HTML, scripts,
  iconos no permitidos, límite de servicios, rangos `focoX/focoY` "debe estar entre 0.0 y 100.0",
  `overlay` "0.0 y 1.0", URLs no permitidas, campo ajeno `password`.
- A25–A31: historial (CREAR_BORRADOR, ACTUALIZAR_BORRADOR, PUBLICAR, ARCHIVAR, RESTAURAR);
  el borrador NO altera el público; restaurar revierte el contenido publicado.
- A32–A35: RBAC — anónimo `401`; cajero/cliente (token válido) `403`; público anónimo `200` con LOGIN.
- A36: almacenamiento `{tipo:"local", disponible:true, persistente:false}`.
- A37a–A37d: galería — exige autorización comercial ("Debe declarar la autorización comercial"),
  rechaza dimensiones 160×120 ("Imagen demasiado pequeña: mínimo 200×200 px"),
  deduplica por checksum (409 "mismo checksum"), y la imagen del portal queda **asignada al
  LOGIN publicado** (`imagenUrl` en `publico`).
- A38–A39: archivar → LOGIN deja de servirse públicamente; republicar → vuelve. **Estado final: PUBLICADO.**

Evidencia UI: `03a-portalgestion-login.png`, `03b-login-publicado.png`,
`03c-login-imagen-portal.png` (+ `03c-imagen-visual.json` = la imagen pública se pinta en el
fondo del Login, `background-image: url(.../media/portal/73ffd049….png)`, `visible`).

## 4. Defecto detectado y corregido — servir imágenes del portal (DEV)

Durante P07 (sin errores de consola) se detectó un **500** en el backend al servir
`GET /media/portal/*.png` (`NoResourceFoundException`), con la imagen existente en disco.

- Causa raíz: el resource handler `/media/portal/**` se registra contra un directorio que se
  creaba recién en el primer upload (`Files.createDirectories`); al no existir en el arranque,
  Spring lo resolvía como ausente mientras el proceso vivía.
- Evidencia cruda: `curl http://localhost:8080/media/portal/73ffd049….png → HTTP 500`. Log:
  `Error inesperado en /media/portal/…: No static resource …png.`
- **Corrección aplicada** (backend, compila y empaqueta OK, jar reconstruido):
  1. `LocalPortalStorageService` crea el directorio **en el constructor** (garantiza que exista
     desde el arranque).
  2. `GlobalExceptionHandler` mapea `NoResourceFoundException → 404` (deja de reportar 500
     "inesperado" para rutas estáticas inexistentes).
- Estado: **corrección en código, pendiente re-verificación en vivo**; el entorno dejó de
  permitir lanzar el backend en segundo plano al final de la corrida (este backend fue el mismo
  en que se validaron L/V/A). En producción (PaaS con disco persistente o proveedor de objetos,
  ver etiqueta #43) es obligatorio confirmar el volumen/servicio de almacenamiento.

## 5. Seguridad / nociones verificadas

- `GET /api/v1/portal/admin/secciones` sin token → **401**.
- Con token de cajero/cliente → **403** (rol insuficiente).
- Login sin selector de roles: identidad y rol provienen **solo del backend**.
- JWT con expiración y revocación de tokens (sesión vencida central).

## 6. Archivos de evidencia

`reports\evidencia-login-premium\`:

```
01-metricas-layout.json         métricas y conclusiones de layout (8 viewports)
01a/01c/01e/01g-*.png           captures desktop / tablet-860 / mobile 420 / 375
02-v-auth-flows.json            flujos V 26/26
02b/02d/02e-*.png               error incorrecto / servicio caído / sesión vencida
03-a-gestion.json               suite A 43/43
03a-portalgestion-login.png     editor LOGIN (Gestión del Portal)
03b-login-publicado.png         login renderizando lo publicado
03c-imagen-visual.json          imagen del portal renderizada (computed style)
03c-login-imagen-portal.png     login con la imagen publicada en el fondo
04-p-publico.json               suite P 6/7 (P07 = defecto de DEV §4)
04b-login-publico-final.png     estado visual final
```

## 7. Notas de ejecución y estado final

- Estado final de LOGIN en Gestión del Portal: **PUBLICADO**, con `imagenUrl` asignada.
- Credenciales QA: `admin/admin123`, `cajero1/cajero123`, `vendedor1/vendedor123`,
  `operador1/operador123`, `chofer1/chofer123`, `qaloginpremium/CliQa2026!` (ROLE_CLIENTE).
- Harness conocido: los eventos CDP teclado/ratón mueren tras ~2 interacciones por sesión;
  se mitigó con *JS-click* (`document.querySelector('button[type=submit]').click()`), `page.fill`
  y `page.evaluate`. No es defecto de la aplicación.
- **Módulo de modales (Tarea #44) NO se retomó** en esta entrega, por alcance acordado.

---

## 8. Corrección visual final del Login (2026-10-08)

> Corrección acotada **solo a presentación**: no se tocaron autenticación, roles,
> redirecciones, endpoints ni contratos.

### 8.1 Causa raíz del "bloque verde plano"

La imagen **administrable** publicada para LOGIN (`/media/portal/73ffd049….png`) **existe y
responde HTTP 200**, pero su contenido es un **placeholder de un solo color**
(`#127369` en todo el bitmap, 640×360). No era un fallo de ruta ni de servidor: los 4
recursos del almacén local son igualmente planos. El Login renderizaba correctamente esa
imagen, por eso el panel izquierdo se veía como un bloque verde sin fotografía.

### 8.2 Correcciones aplicadas (LOGIN)

**A. Imagen**
1. Se verificó físicamente el archivo y `GET /media/portal/73ffd049….png` → **200**;
   inexistente → **404** (no 500). El `500` histórico quedó corregido (§4) y re-verificado en vivo.
2. No se hardcodearon rutas absolutas ni se exponen rutas físicas (el backend usa
   `storage/portal` relativo).
3. `Login.jsx`: la imagen administrable ahora se **valida en el cliente** (se dibuja en un
   canvas y se rechaza si es un placeholder: pocos colores distintos o un color dominante
   ≥ 60 %). Si falta, falla o es plana, se usa el **respaldo institucional local**
   `src/assets/Vilcas.jpeg` (fotografía real 1440×960), de modo que el Login **nunca** queda
   en verde plano. El degradado verde queda solo como último recurso si el respaldo local fallara.
4. La gestión administrable de la imagen del Login queda **documentada como pendiente**
   (hay que reemplazar el placeholder del almacén por una fotografía real desde
   Gestión del Portal → LOGIN).

**B. Composición**
5. Se neutralizaron los pseudo-elementos decorativos heredados de `admin.css`
   (`::before/::after`) que competían con el degradado oficial; se eliminó el recorte
   del pie de panel (texto cortado en la esquina inferior izquierda).
6. División desktop **60 % / 40 %** conservada; se amplió la tarjeta (`max-width` 500 → 540,
   en 1366 px: 442 → 464 px reales) y se redujo el espacio exterior
   (`padding` del panel derecho 2.4rem → clamp(1.4–1.9rem)).
7. Logo institucional ampliado (58 → 78 px en la tarjeta; 60 → 78 px en el panel).
8. Se eliminó el separador "o" (`.login-divider`) que aparecía bajo **INGRESAR**.
9. Se eliminó el **"Volver al inicio" duplicado** (se conserva uno solo, en el formulario).
10. Se conservan **Consultar mis reservas** y **Regístrate gratis**; los textos siguen
    legibles sobre el overlay verde.

### 8.3 Validación mínima ejecutada

| Comprobación | Resultado |
|---|---|
| Captura desktop 1366×768 | `05-login-corregido-desktop-1366.png` |
| Captura móvil 390×844 | `06-login-corregido-mobile-390.png` |
| Imagen visible (computed `background-image`) | `url("…/src/assets/Vilcas.jpeg")` ✅ |
| Fallback (placeholder plano rechazado) | ✅ (sin `--login-foto`) |
| Pie de panel sin recorte (`footClipped`) | `false` ✅ |
| "Volver al inicio" en el DOM | `1` ✅ |
| Separador `.login-divider` | ausente ✅ |
| Login ADMIN (`admin/admin123`) | aterriza en `/admin`, `.adm-shell` montado, sin error ✅ |
| Overflow horizontal móvil | `false` ✅ |
| `npm run lint` | **exit 0** ✅ |
| `npm run build` | **√ built** (emite `dist/assets/Vilcas-*.jpeg`) ✅ |
| Backend | **no modificado** en esta corrección |

**Veredicto de esta corrección:** **CORRECCIÓN VISUAL DEL LOGIN APLICADA Y VERIFICADA EN QA**
(almacenamiento administrable de la imagen de Login documentado como pendiente).