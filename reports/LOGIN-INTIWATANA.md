LOGIN INTIWATANA
================

Fecha: 2026-09-30
Raíz: C:\Users\USER\Downloads\intiwatana_final_v2

ARCHIVOS INSPECCIONADOS:
- frontend/Web-Intiwatana/src/components/Login.jsx (componente oficial: LeftPanel, RegisterForm y Login)
- frontend/Web-Intiwatana/src/App.jsx (rutas reales: /, /login, /registrar, /pasajes, /admin, /chofer; RutaProtegida)
- frontend/Web-Intiwatana/src/App.css (variables de identidad y bloque "Login & Auth")
- frontend/Web-Intiwatana/src/admin.css (reglas auth compartidas; se confirmó que App.css se carga después y prevalece)
- frontend/Web-Intiwatana/src/components/LogoSVG.jsx (logo oficial -> /assets/logo.png)
- frontend/Web-Intiwatana/src/context/AuthContext.jsx y src/context/useAuth.js (login/logout, sesión en localStorage)
- frontend/Web-Intiwatana/src/main.jsx (orden de importación de CSS)
- frontend/Web-Intiwatana/package.json (lucide-react ya instalado; no se añadió ninguna dependencia)
- Inventario de imágenes locales: public/assets/logo.png, public/assets/hero.png (sin uso en código), public/assets/img/rutas/*.jpeg, src/assets/*

ARCHIVOS MODIFICADOS:
- frontend/Web-Intiwatana/src/components/Login.jsx
- frontend/Web-Intiwatana/src/App.css
- frontend/Web-Intiwatana/src/components/LogoSVG.jsx (único cambio: prop opcional `alt`, valor por defecto intacto)

CAMBIOS VISUALES:
- Panel izquierdo institucional: 40% del ancho, verde oscuro #0D3330 -> #127369, con imagen local de paisaje andino (/assets/img/rutas/andabamba.jpeg) en capa dedicada (.login-left-bg, opacity 0.5) + degradado verde (.login-left-overlay) + patrón sutil. Sin URL externa.
- Contenedor de imagen identificado: data-image-slot="foto-oficial-bus" con role="img" y aria-label. La imagen se cambia mediante la variable CSS --login-foto-panel sin tocar JSX, para colocar la foto oficial cuando se entregue.
- Botón "Volver al inicio" translúcido, borde blanco, esquinas redondeadas, hover y foco visible.
- Título INTIWATANA, texto institucional y 4 beneficios con icono (Ticket, Armchair, Package, Bus) en círculo amarillo, cada uno con su subtítulo.
- Pie: "Conectando destinos, uniendo personas" + "© 2026 INTIWATANA S.R.L. Todos los derechos reservados."
- Tarjeta blanca centrada vertical y horizontalmente, max-width 520px, radio 20px, sombra suave, padding equilibrado.
- Formulario: etiquetas USUARIO y CONTRASEÑA en mayúsculas, iconos dentro del campo, borde 1.5px, foco con halo verde.
- Campo contraseña: botón de ojo (Eye/EyeOff) que alterna type="password"/"text".
- Error: caja roja suave con icono de alerta y role="alert".
- Botón Ingresar: ancho completo, verde institucional, flecha a la derecha, estado "Ingresando...".
- Enlace de registro: "Regístrate gratis" en amarillo institucional legible y subrayado.
- Iconografía Lucide reutilizada; cero dependencias nuevas.

BOTÓN VOLVER AL INICIO:
- Ruta utilizada: "/" (ruta pública real confirmada en App.jsx; no inventada). Implementado con <Link to="/"> de React Router, sin recarga completa de la aplicación.
- Estado: FUNCIONAL.
- Evidencia: 1 enlace en el árbol de accesibilidad, href="/"; clic real navega a http://localhost:5173/ con el .hero público renderizado. También presente en /registrar (comparte LeftPanel).

AUTENTICACIÓN:
- Endpoint conservado: POST /api/v1/auth/login (sin cambios, sin proxy nuevo, sin simulación).
- JWT conservado: sí. AuthContext intacto; la sesión guarda token y rol en localStorage "iwt_session".
- Redirección conservada: sí. ROLE_CLIENTE -> /pasajes, ROLE_CHOFER -> /chofer, resto -> /admin. Esa lógica no se modificó.
- Estado: FUNCIONAL contra el backend real.
  - Credenciales válidas verificadas (usuario / clave <REDACTADO> por saneamiento): admin -> /admin (ROLE_ADMIN); pruebaa001 -> /pasajes (ROLE_CLIENTE). Token presente en la sesión.
  - Credenciales incorrectas: permanece en /login, muestra "Usuario o contraseña incorrectos" (401 real del backend), conserva el usuario y limpia la contraseña.
  - Envío vacío: el navegador bloquea por validación HTML (required); no se realizó ninguna petición.
  - Sin credenciales hardcodeadas en el frontend.

RESPONSIVE (medido en navegador real; sin overflow horizontal en ningún tamaño):
- Escritorio (1600x900 y 1366x768): dos columnas 40%/60%, beneficios visibles, tarjeta de 520px centrada (desviación vertical 0px).
- Tablet (900x1200): dos columnas ajustadas 45%/55%, tarjeta 431px, beneficios visibles, textos sin cortes.
- Tablet vertical (768x1024): una sola columna; botón Volver, logo, pie y formulario visibles; solo se ocultan los beneficios.
- Móvil (390x844 y 320x640): una sola columna; "Volver al inicio", logo INTIWATANA, formulario, error y enlace de registro visibles; beneficios ocultos; botones de 47px de alto; sin scroll horizontal.

ACCESIBILIDAD:
- Etiquetas asociadas por htmlFor (login-usuario, login-password); iconos decorativos con aria-hidden.
- Orden de tabulación verificado: usuario -> contraseña -> ojo -> Ingresar.
- Foco visible: contorno de 2-3px en el enlace Volver, el botón del ojo y el enlace de registro.
- role="alert" en el error; aria-busy en el formulario y en el botón durante el envío.
- aria-label dinámico del ojo: "Mostrar contraseña" / "Ocultar contraseña", con aria-pressed sincronizado y type="button" para no enviar el formulario.
- autocomplete="username" y autocomplete="current-password".
- Contraste calculado contra WCAG: subtítulo 5.01:1, enlace de registro 5.07:1, etiquetas 7.3:1 (antes 2.92:1 y 1.63:1, corregidos).
- prefers-reduced-motion: reduce -> transiciones a 0s y sin backdrop-filter (verificado).
- Logo con alt explícito "Logo de Transportes INTIWATANA"; el slot de imagen tiene role="img" y aria-label.
- Errores comunicados con icono + texto, no solo por color.

LINT:
- Comando: npm run lint
- Resultado: 0 errores y 0 advertencias (eslint . sin salida). No se usó eslint-disable ni se eliminó ninguna regla.

BUILD:
- Comando: npm run build
- Resultado: éxito. 1743 módulos, dist/assets/index-*.css 48.12 kB, index-*.js 479.88 kB, built in 1.75s.

PRUEBAS:
- Apertura del login: OK (/login, HTTP 200, 0 errores de consola).
- Regreso al inicio: OK (navega a "/" sin recarga completa).
- Envío vacío: OK (validación HTML, sin petición).
- Credenciales incorrectas: OK (error visible, permanece en login, 401 real del backend).
- Credenciales correctas: OK para ROLE_ADMIN y ROLE_CLIENTE, con redirección por rol.
- Loading: OK ("Ingresando...", botón disabled, aria-busy=true, sin envíos duplicados gracias a la guarda `if (loading) return`).
- Mostrar/ocultar contraseña: OK (el atributo type alterna password/text).
- Navegación por teclado: OK (orden lógico; Enter envía el formulario real y redirige a /admin).
- Redirección por rol: OK (CLIENTE -> /pasajes, ADMIN -> /admin).
- Vistas escritorio/tablet/móvil: OK (métricas medidas, sin overflow horizontal).
- Consola: 0 errores de React. El único 401 corresponde al rechazo esperado del backend en la prueba de credenciales incorrectas.
- Sin regresiones: /registrar sigue renderizando (5 campos, título "Crear cuenta", sin overflow). No se modificaron backend, Spring Security, JWT, endpoints, roles ni módulos administrativos.

ERRORES O LIMITACIONES:
- El proyecto no contiene una fotografía de un bus de INTIWATANA. Se usa una imagen local de paisaje andino ya existente en el repositorio (/assets/img/rutas/andabamba.jpeg) como fondo del panel, con degradado superpuesto. El contenedor queda identificado (data-image-slot="foto-oficial-bus" y variable --login-foto-panel) para sustituirla por la foto oficial sin modificar JSX.
- public/assets/hero.png existe pero no lo referencia ningún archivo del código; no se usó porque no es posible confirmar su contenido.
- No se pudo hacer la validación visual por inspección de píxeles: el agente no tiene soporte de entrada de imagen en esta sesión. La fidelidad se verificó de forma estructural y numérica (DOM, estilos calculados, contraste, medidas). Las instantáneas PNG quedaron en C:/Users/USER/AppData/Local/Temp/opencode/ (login-desktop.png, login-tablet.png, login-movil.png).
- La rama de redirección ROLE_CHOFER -> /chofer no se ejercitó porque no hay credenciales de chofer válidas disponibles (chofer1/chofer1 -> 401). Esa línea de código no fue modificada.
- La captura de escritorio se tomó con un mensaje de error visible de forma intencionada, para documentar ese estado.

ESTADO FINAL:
- IMPLEMENTADO Y VERIFICADO
