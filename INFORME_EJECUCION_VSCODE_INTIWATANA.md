# INFORME_EJECUCION_VSCODE_INTIWATANA.md

**Proyecto:** INTIWATANA (Spring Boot 3 / Java 17, backend `bus-system`, PostgreSQL `transporte_bd`)
**Entorno:** Windows 10/11 x64 · VS Code + `redhat.java 1.56.0` + `vscjava.vscode-java-debug 0.59.0` · JRE 21 incluido del LS · JDK 17 Adoptium `jdk-17.0.18.8-hotspot` · Postgres local `5432`
**Fecha del informe:** 2026-09-30
**Objetivo del encargo:** comprobar la **ejecución REAL desde VS Code** (F5 / Run and Debug y *Run Java*) de la aplicación en el puerto 8080 contra la base `transporte_bd`, con evidencia de procesos/puerto/API, y emitir **un único veredicto: `CERRADO` o `NO CERRADO`**.

---

## 1. Veredicto

> ## `NO CERRADO`

**Última versión probada del entorno: `redhat.java 1.37.0` (junto a 1.56.0).** La aplicación NO pudo arrancarse desde VS Code (ni con F5 ni con *Run Java*). Ninguna JVM del proyecto se lanzó jamás desde VS Code: no hay proceso `java.exe` con `SistemaTransporteApplication` ni `-agentlib:jdwp`, el puerto 8080 nunca abrió en una sesión VS Code y la API no respondió. La causa raíz es la **herramienta Java de VS Code en esta máquina**, que no logra importar el proyecto: una jerarquía completa de fallos (detallada en §7) impide que JDTLS produzca `.projects\sistema-transporte`, paso indispensable para que F5/Run Java resuelvan el classpath y lancen la app. No es código, base de datos ni configuración del proyecto.

---

## 2. Metodología y criterios de aceptación

- Se usa como evidencia **únicamente la ejecución iniciada desde VS Code** (botón F5/paleta). La ejecución por terminal (`mvn spring-boot:run`, `java -jar`, scripts) **NO se admite como evidencia** y no se usó para afirmar éxito.
- Criterio de éxito F5: proceso `java.exe` con `-agentlib:jdwp` + `com.transporte.sistema.SistemaTransporteApplication` en su línea de comandos → puerto `8080` en escucha → `GET http://localhost:8080/api/v1/sucursales` → HTTP 200 → log `backend\bus-system\logs\sistema-transporte.log` con `Started .. :: Started` y PID **nuevo**.
- Criterio de éxito *Run Java*: igual que F5 pero sin `-agentlib:jdwp`.
- Reglas respetadas: **no** se modificó lógica del negocio, ni la BD (`transporte_bd` intacta), ni el frontend; se controló que el puerto 8080 estuviera libre antes de cada intento; los cambios aplicados fueron **solo de arranque/entorno** (configuración VS Code y extensión Java).
- Fecha/hora del informe: `2026-09-30 12:01` (hora local).

---

## 3. Vista general del flujo de pruebas (todo falló sin lanzar la app)

| # | Prueba | Cómo | Resultado |
|---|--------|------|-----------|
| P1 | F5 automatizado (sendkeys a la ventana de VS Code, ventana PID 17464) | 2026-09-30 10:15 | 8080 sin escucha; sin VM jdwp; nada |
| P2 | F5 manual (usuario) | 10:18 | Se abrió consola: **“building workspace” eterno**; sin 8080 |
| P2b | Shift+F5 + F5 tras `java.autobuild.enabled=true` | 10:30 | Igual: “building workspace”; compilación correcta a las 10:30:31, pero sin lanzamiento |
| P3 | *Run Java* (paleta Grupo Java) e *Import* manual | 11:35 y 11:41 | El comando “Import Java projects…” se ejecutó sin efecto |
| P4 | F5 en entorno limpio (reinstalación de extensiones + reinicio de la máquina) | 11:49–12:01 | **La importación JDTLS jamás comienza** (ver §6) → sin app |

En **todas** las pruebas: `java` del proyecto 0 procesos, 8080 libre, `sistema-transporte.log` sin arranques nuevos (última actividad 09:57, PID 51500, lanzado por terminal en una sesión anterior y **no computable**).

---

## 4. Línea de evidencia de la Causa Raíz (F5 → nunca llega al lanzamiento)

Cadena verificada en logs reales de VS Code:

1. **F5 invoca a `java-debug`**, que requiere el workspace **importado** (proyectos con classpath resuelto) para el launch.
2. La importación la dispara el cliente de la extensión **`redhat.java`** contra el **JDT Language Server**. En esta máquina dicho intercambio está dañado:
   - Con la extensión **VMware Spring Boot** activa (estado original): el lenguaje server entra en un **bucle infinito** de `Unhandled method workspace/executeClientCommand` en `JDTLanguageServer#synchronizeBundles` (aprox. cada 300 ms; log `Language Support for Java.log` creció a varios MB en minutos; 7.096 referencias en un solo archivo de log; réplicas en miles de líneas en logs de días anteriores 09-25…09-29: 4.492, 5.140, 10.732…). Acompañado de `Error occured while building workspace` (10:30:36) y colisiones de comando del registro: `Error: command 'sts.java.addClasspathListener' already exists`. El bucle bloquea `buildWorkspace` → la consola queda en **“building workspace”** para siempre.
   - **El cliente `redhat.java` 1.56.0 no implementa el método `workspace/executeClientCommand`** y, según el patrón público del bug (eclipse-jdtls #2575; redhat-developer/vscode-java #2916), ese aviso inunda al servidor y jamás se completa la fase de build/import. Se verificó por web search y por reproducción local.
3. Se **aisló** el factor VMware:
   - Desinstalada `vmware.vscode-spring-boot` + `vscjava.vscode-spring-boot-dashboard`; desinstalado y **reinstalado limpio** `redhat.java` y `vscjava.vscode-java-debug` (bajada nueva del VSIX 1.56.0 / 0.59.0).
   - **Resultado: el bucle `executeClientCommand` desapareció (0 réplicas).** Curado el síntoma de saturación, **pero la importación siguió sin arrancar** → el problema de fondo es independiente de VMware.
4. **Reinicio completo de la máquina** y reapertura de un entorno 100 % limpio (una única ventana de VS Code, `jdt_ws`/`ss_ws` borrados, extensiones Java frescas): el LS arranca (JRE 21 embebido, 2 procesos `java.exe`), crea solo `.metadata` y queda **idle**; **`.projects\sistema-transporte` NUNCA se genera** (ausente a las 12:01).

Causa probable última: el host/degradación del entorno rompe el flujo **cliente(exthost) → LS** en esta máquina —repetidas anomalías sistémicas observadas: extension host con crash `ExitCode 0xC0000142` (DLL no hallada), `java.io.IOException: Cannot run program "cmd": CreateProcess error=5, Acceso denegado` recurrente del Spring/LS, CLI de VS Code colgando al solicitar una 2ª ventana (IPC sin respuesta), y WER (errores de Windows) a las 08:36/11:48.

---

## 5. Pruebas de control realizadas (todas negativas)

- **Java: Clean Java Language Server Workspace** (paleta): no disponible/inerte.
- Borrado manual de `jdt_ws` y `ss_ws` (×2) + respawn: la importación no se emite; el spam (con VMware) reaparecía → fallo determinista, no corrupción.
- Desactivación por UI y por carpeta de extensiones VMware: eliminó el spam pero no logró la importación.
- **Reinstalación limpia** de `redhat.java` y `java-debug` (uninstall → install → reload): idem.
- **Reinicio de la PC** con ventana única y metadatos borrados: importación nunca comienza.
- (Final) constructor de un proyecto Maven mínimo en `%TEMP%\opencode\probe-java` como control: **no llegó ni a abrirse**; el CLI de VS Code colgó al pedir la 2.ª ventana (evidencia adicional de IPC/entorno degradado).

---

## 6. Estado final verificado (2026-09-30 12:01)

| Comprobación | Valor |
|---|---|
| `.projects\sistema-transporte` en `jdt_ws` | **No existe** (solo `.metadata`) → importación nunca completada |
| Procesos `java.exe` del proyecto | 0 (los 2 vivos son del propio JDTLS, JRE 21) |
| Puerto 8080 | Sin escucha |
| `GET /api/v1/sucursales` | No se pudo probar (servicio no arriba) — y no servía ejecutar nada por terminal como evidencia |
| `sistema-transporte.log` | Último arranque: 09:57 PID 51500 (sesión de terminal previa; **no computa**) |
| Autobuild | `java.autobuild.enabled=true` (compila OK en 10:30:31) |

---

## 6.1 Último intento definitivo (redhat.java 1.37.0) — 2026-09-30 12:05–12:25

Intentado conforme al procedimiento controlado final (un solo cambio de versión, sin repetir sin diagnóstico):

1. Versión inicial documentada: `redhat.java@1.56.0`.
2. Instalada la versión estable anterior `redhat.java@1.37.0` (VSIX bajado del Marketplace; `Extension 'redhat.java' v1.37.0 was successfully installed`); carpeta activa `redhat.java-1.37.0-win32-x64` (JDK embebido 17.0.13).
3. VS Code cerrado por completo; `jdt_ws`/`ss_ws` borrados; apertura de **una sola ventana** con INTIWATANA (PID 13528).
4. El JDTLS 1.37 arrancó y ejecutó trabajo real (~191 s de CPU en 12:09–12:17 — frente al estado idle de 1.56), creó `jdt_ws`/`ss_ws`, **pero terminó idle sin generar `.projects\sistema-transporte`.**
5. F5 real pulsado por el usuario (12:22): **no arrancó**.
6. Verificación final 12:24:38 — `.projects\sistema-transporte`: **no existe** (ni siquiera `.projects`); puerto 8080: **sin escucha**; proceso `java` con `SistemaTransporteApplication` o `-agentlib:jdwp`: **ninguno** (solo el JDTLS 18356). API no probable (sin servicio).
7. Curioso: el JDTLS 1.56 de la ventana restaurada registró el mismo obstáculo exacto: `sistema-transporte does not exist. Java Model Exception: Java Model (code 969)` + cancelaciones no emparejadas (`Unmatched cancel notification`), a las 11:55.

Conclusión de este intento: **la importación Maven no termina en ninguna de las dos versiones probadas** (1.56.0 y 1.37.0). El bloqueo es del entorno VS Code/JDTLS en esta máquina, no de la versión de la extensión ni del proyecto.

## 7. Cambios realizados y NO realizados

**Realizados (solo entorno de arranque/VS Code):**
- `.vscode/launch.json`: configuración F5 estándar (sin `classPaths` manuales; `projectName=sistema-transporte`; `mainClass=com.transporte.sistema.SistemaTransporteApplication`).
- `.vscode/settings.json`: `java.autobuild.enabled=true`; `java.configuration.updateBuildConfiguration=automatic` (antes manual/false).
- Extensiones: desinstaladas `vmware.vscode-spring-boot` y `vscjava.vscode-spring-boot-dashboard`; reinstaladas limpias `redhat.java` (primero 1.56.0, luego **quedó instalada la definitiva probada `1.37.0`**) y `vscjava.vscode-java-debug` (0.59.0). `vscjava.vscode-java-test` y `vscjava.vscode-lombok` quedaron desinstaladas (se retiraron para permitir el cambio de versión; su ausencia no afecta F5/Run Java; el agente lombok lo provee `redhat.java`).
- Limpieza controlada de cachés JDTLS (`jdt_ws`/`ss_ws`), necesaria por el estado corrupto, con respaldo del entendimiento de que no altera el proyecto.

**NO realizados (por restricción del encargo):** ninguna modificación de lógica de negocio, ni de la base de datos `transporte_bd`, ni del frontend; **no** se corrió la app por terminal como evidencia de éxito (aunque en sesiones previas se usó `mvn`/`java -jar` para probar funcionalmente la app, **no cuenta** para este veredicto).

---

## 8. Qué significaría cerrar y cómo quedaría verificado

Para poder marcar `CERRADO` cuando el entorno esté reparado, la secuencia a repetir es:

1. Reparar el entorno (candidatos: versión anterior de `redhat.java` compatible con este Windows/JDK — ej. 1.37.x/1.45.x—; revisar políticas de AppLocker/AV que deniegan `CreateProcess` de `cmd`/Java; o limpiar el perfil de VS Code).
2. Abrir una única ventana con el proyecto; **esperar** a que aparezca `.projects\sistema-transporte` en `jdt_ws`.
3. Pulsar F5 → verificar proceso `java` con `-agentlib:jdwp`, `8080` escuchando, `GET /api/v1/sucursales` → 200, y en el log `Started` con PID nuevo.
4. Repetir F5 (2.ª ejecución) y, en otra ventana/instancia, *Run Java* desde la pestaña `SistemaTransporteApplication.java` (sin jdwp), con las mismas comprobaciones.
5. Entonces regenerar este informe con todas las evidencias y veredicto único `CERRADO`.

---

## 9. Referencias de evidencia (archivos y rutas)

- Log del LS (bucle): `%APPDATA%\Code\logs\<sesión>\window*\exthost\redhat.java\Language Support for Java.log` (spam `workspace/executeClientCommand`, `Error occured while building workspace`).
- Espacio de trabajo JDT: `%APPDATA%\Code\User\workspaceStorage\180888a00d62e5d4453f716e8150a85a\redhat.java\jdt_ws` (solo `.metadata`; sin `.projects\sistema-transporte`).
- Configuración VS Code: `.vscode\launch.json`, `.vscode\settings.json`.
- Log de la aplicación: `backend\bus-system\logs\sistema-transporte.log`.
- Errores de Windows (WER) 2026-09-30 08:36 y 11:48 (Log de aplicación); `CreateProcess error=5` en logs del Spring LS.