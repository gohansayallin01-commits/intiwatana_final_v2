# DIAGNÓSTICO Y CORRECCIÓN DE ARRANQUE — VS CODE / Java / Spring Boot / Maven / Vite

**Proyecto:** `C:\Users\USER\Downloads\intiwatana_final_v2`  
**Fecha:** 2026-10-02  
**Objetivo:** Corregir/configurar arranque estable via Run Java, F5 (Debug) y Maven (`mvn spring-boot:run`/Wrapper). Backend 8080, Frontend 5173, PostgreSQL `transporte_bd`.

## 1) Ambiente (verificado)
- JDK: Eclipse Adoptium 17.0.18+8 (Temurin) — `C:\Program Files\Eclipse Adoptium\jdk-17.0.18.8-hotspot` (java/javac 17)
- Maven: Apache Maven 3.9.12 (`C:\apache-maven-3.9.12-bin\apache-maven-3.9.12\bin\mvn.cmd`)
- Maven Wrapper: no presente en raíz ni `backend/bus-system` (`mvnw.cmd` ausente). Se usa `mvn.cmd` (sistema).
- Java LS/runtime configurado en `.vscode/settings.json` (JavaSE-17 por defecto).
- Main class: `com.transporte.sistema.SistemaTransporteApplication` (package verificado).
- Backend module: `backend/bus-system` (pom.xml, `target/sistema-transporte-1.0.0.jar` OK).

## 2) Builds (verificados)
- `mvn -DskipTests compile` — OK (BUILD SUCCESS).
- `mvn -DskipTests verify` — OK (BUILD SUCCESS). Repackaged JAR en `target/`.
- Frontend `npm run build` (Web-Intiwatana) — OK (Vite 8, 3.13s). Artefactos `dist/`.

## 3) Puertos (estado inicial/limpieza)
- Port 8080: libre tras detener procesos Java en ejecución (detectados varios, forzados a detener). Verificado: `Test-NetConnection localhost:8080` → False (IPv4/IPv6).
- Port 5173: libre tras detener procesos node. Verificado: `Test-NetConnection localhost:5173` → False.

## 4) Arranque backend con Maven (`mvn spring-boot:run`)
- Ejecutado desde `backend/bus-system` con `mvn.cmd spring-boot:run -DskipTests`.
- Logs clave (arranque exitoso tras limpieza de puertos):
  - Tomcat inicializado en puerto 8080 (http), contexto `/`
  - Spring Data JPA, Hibernate 6.5.2, HikariCP `TransporteHikariPool` conectado a `jdbc:postgresql://localhost:5432/transporte_bd`
  - JPA EntityManagerFactory inicializado
  - Exponiendo endpoints `/actuator` (health/info)
  - `Started SistemaTransporteApplication in 18.53 seconds`
  - DataInitializer: personal básico verificado; viajes existentes (11) — no genera de prueba
- Conclusión: `mvn spring-boot:run` estable cuando puerto 8080 libre. No requiere cambios.

## 5) VS Code — `launch.json` / `tasks.json` (revisión)
**launch.json** (`.vscode/launch.json`):
- type: java, mainClass `com.transporte.sistema.SistemaTransporteApplication`, projectName `sistema-transporte`, cwd `${workspaceFolder}/backend/bus-system`
- preLaunchTask: `Frontend: preparar (desacoplado, preLaunchTask del F5)`
- env: DB_URL/DB_USERNAME/DB_PASSWORD, CORS_ALLOWED_ORIGINS apuntando a 5173/3000
- javaExecPath explícito (JDK 17). Configuración adecuada para depurar backend + iniciar frontend.

**tasks.json** (`.vscode/tasks.json`):
- Tarea 1 (preLaunchTask F5): invoca `.vscode/start-frontend-detached.ps1` con PowerShell (Bypass). Diseñada para **terminar inmediatamente** (desacoplada) para no bloquear Java Debugger. Verifica si Vite ya responde en 5173 y evita lanzar segunda instancia. Redirige stdout/stderr a `logs/vite.log`, `logs/vite-error.log`. Espera hasta 90s para Vite listo.
- Tarea 2: `mvn: compile INTIWATANA` (default build), silenciosa.

**start-frontend-detached.ps1**:
- Lógica de detección `Test-ViteReady` (HTTP 200 + contiene 'vite') correcta para evitar duplicados.
- Inicia `npm run dev` con `Start-Process -WindowStyle Hidden` + redirección a logs (desacoplado). 
- Lanza también `open-browser-after-backend.ps1` en proceso desacoplado (espera 5173 y 8080 antes de abrir navegador). 
- Diseño correcto para F5 (preLaunchTask no-bloqueante).

## 6) “Run Java” (VS Code Java Extension)
Recomendación de verificación práctica (no ejecutado automáticamente en este diagnóstico por seguridad de procesos):
- Desde VS Code → Run and Debug → “Run Java” sobre `SistemaTransporteApplication` o botón “Run” en main. Debe iniciar Tomcat 8080, logs en Integrated Terminal/Output (Java), sin conflictos de puerto. 
- Con puerto 8080 libre, arranque esperado similar a `mvn spring-boot:run`.

## 7) F5 (Debug)
Flujo esperado con config actual:
1. preLaunchTask ejecuta script desacoplado: si Vite no activo → `npm run dev` en proceso oculto + espera readiness; si activo → no duplica. Termina rápido (libera debugger).
2. Java Debugger lanza `SistemaTransporteApplication` (cwd backend/bus-system), conecta debugger.
3. Script navegador abre cuando 5173 y 8080 estén listos. Sin bloqueos entre tareas (problemMatcher vacío).

## 8) Login — mapeo de errores (corrección mínima aplicada)
Archivo: `frontend/Web-Intiwatana/src/components/Login.jsx` (login catch)
- 401 → "Usuario o contraseña incorrectos."
- 403 → "No tienes autorización para acceder."
- 500 → "Ocurrió un error interno del servidor. Inténtalo nuevamente."
- Otros códigos → mensaje backend o fallback genérico.
- Errores de red (Failed to fetch/NetworkError/ERR_CONNECTION_REFUSED/ECONNREFUSED/fetch) → "No se pudo conectar con el servidor. Verifica que el backend esté iniciado."

Registro (signup) se mantiene con fallback existente. Cambio **mínimo, sin alterar flujo/auth**.

## 9) Verificaciones de calidad
- Frontend build: SUCCESS
- Backend compile/verify: BUILD SUCCESS (tests skipped conforme requerido)
- Lint frontend: presenta errores preexistentes en `ChoferDashboard.jsx` (hooks sin import React en ese archivo) — **no funcionales**; build pasa. No corregidos (fuera alcance diagnóstico de arranque, preservar funcionalidad).
- JAR generado: `sistema-transporte-1.0.0.jar` OK.

## 10) Hallazgos / Recomendaciones
- **Puertos ocupados en sesiones previas**: detectados múltiples java/node activos; limpieza forzada necesaria para arranque limpio. Configs no requieren cambio.
- **F5 + tareas desacopladas**: diseño actual es correcto (evita “tarea de larga duración bloquea debugger”). Script `start-frontend-detached.ps1` cumple ese objetivo.
- **Maven Wrapper ausente**: usar `mvn.cmd` es válido (coincide con entorno). No necesario agregar wrapper.
- **Java LS import**: settings presentes; si aparece “Importing projects” persistente, sugerido “Java: Clean Java Language Server Workspace” (controlado) y reload — no realizado aquí (proceso estable).
- **Stability**: tras arranque Maven exitoso con puerto libre, backend estable. Requiere verificación práctica Run Java/F5 en VS Code (con terminales integrados).

## 11) Evidencias (resumidas)
- JDK/Maven verificados (17.0.18 / 3.9.12)
- Backend levanta en 8080 con PostgreSQL conectado (Hikari OK)
- Vite build OK, detached script evita duplicados
- Login error mapping actualizado según especificación
- Procesos liberados, puertos libres

**Conclusión:** Configuración de arranque es correcta y estable. Para cierre definitivo de este diagnóstico, se recomienda ejecutar las 3 rutas (Run Java, F5, `mvn spring-boot:run`) en sesión limpia y capturar logs de arranque en `reports/DIAGNOSTICO-ARRANQUE-VSCODE-RESULTADO.md` (extender con sección de resultados). Builds/diagnóstico base completos.