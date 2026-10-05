# F5 / ARRANQUE EN VS CODE — RESULTADO DE VERIFICACIÓN

- **Proyecto:** `C:\Users\USER\Downloads\intiwatana_final_v2`
- **Fecha:** 2026-10-03
- **Objetivo:** demostrar **físicamente** en VS Code que el Java Language Server termina la importación, que F5 arranca Spring Boot (8080), que Vite (5173) inicia/reutiliza, que el navegador abre tras ambos, y que el sistema se puede detener y reiniciar.
- **Regla de honestidad aplicada:** no se declara F5 probado sin evidencia real.

---

## 1. Método usado

- Diagnóstico del estado real del **Java Language Server** (procesos `redhat.java`, `client.log`, `jdt_ws/.metadata/.log`).
- Intento de pulsación real de **F5** sobre la ventana de VS Code vía automatización de UI:
  - `WScript.Shell.AppActivate` + `SendKeys("{F5}")`, y
  - `user32.SetForegroundWindow` + `SendInput` (escancódigos), precedido de `Ctrl+1` para enfocar el editor.
- Comprobación de resultados por puertos (`8080`/`5173`), procesos `java` y logs de VS Code.

**Resultado de la automatización:** el evento F5 **sí se entregó a la ventana de VS Code** (ventana en primer plano confirmada: `FOCUS_AFTER = HANDLE` objetivo), pero **no produjo ningún arranque ni entrada en logs**. La causa se explica abajo.

---

## 2. Causa raíz encontrada (evidencia dura)

`%APPDATA%\Code\User\workspaceStorage\43e3efbf10782ae448a8bfbb710e8c58\redhat.java\client.log.2026-10-03`:

```
07:53:41 Failed to initialize Language Support for Java ... Connection is disposed.
07:53:49 The Language Support for Java server crashed and will restart.
07:54:06 The Language Support for Java server crashed and will restart.
07:54:13 The Language Support for Java server crashed 5 times in the last 3 minutes.
         The server will not be restarted.
```

`...\redhat.java\jdt_ws\.metadata\.log`:

```
!MESSAGE The workspace exited with unsaved changes in the previous session; refreshing workspace to recover changes.
An error occurred while automatically activating bundle org.eclipse.core.resources (31).
Caused by: org.eclipse.core.internal.dtree.ObjectNotFoundException:
  Tree element '/sistema-transporte/target/generated-sources/annotations/com/transporte/sistema/mapper/ClienteMapperImpl.java' not found.
```

**Interpretación:** el **workspace de JDT quedó corrupto** de una sesión previa: registraba como fuente un archivo *generado* bajo `backend/bus-system/target/generated-sources/...` que había sido borrado (p. ej. por un `mvn clean`). Al abrir el proyecto, JDT intentó refrescar ese árbol, lanzó `ObjectNotFoundException`, tumbó `ResourcesPlugin.start()` y **el servidor Java colapsó 5 veces seguidas**; VS Code **dejó de reiniciarlo** en esa ventana.

**Consecuencias observables (por qué F5 no arranca):**

- Para el proyecto raíz **solo sigue vivo el servidor de sintaxis** (`ss_ws`, p. ej. PID 21936). No existe el servidor Java pleno (`jdt_ws`) → `Get-CimInstance` lo confirma.
- Por eso el editor responde hover/definición (los sirve `ss_ws`) pero la **importación nunca concluye** ("Importing projects").
- F5 depende del modelo de proyecto del servidor Java pleno (extensión `vscjava.vscode-java-debug`); al no existir, la depuración no arranca y no genera logs.

Esto **invalida** el "ARRANQUE LOCAL — CERRADO" anterior: no era un problema de `launch.json`/`tasks.json` (que son correctos), sino de **estado corrupto del lenguaje Java**.

---

## 3. Reparación aplicada (reversible)

Se movieron a un lado (no se borraron) los datos corruptos, para forzar una reimportación limpia:

| Elemento | Acción | Resultado |
|---|---|---|
| `...\redhat.java\ss_ws` | renombrado a `ss_ws.corrupt-20261003-091737` | ✅ movido; VS Code recreó un `ss_ws` nuevo |
| `...\redhat.java\jdt_ws\.metadata` | renombrado a `.metadata.corrupt-091950` | ✅ movido |
| `...\redhat.java\jdt_ws` (carpeta superior) | intento de renombrado | ⛔ bloqueada por Windows (handle vivo) — pero su contenido corrupto ya fue apartado |

No se tocó **ninguna** configuración funcional ni `.vscode/*` (ya eran correctos). No se modificó código de la aplicación.

**Lo que falta (requiere interfaz):** VS Code no volverá a levantar el servidor Java por sí solo en esta ventana (lo deshabilitó tras 5 caídas). Hay que **recargar la ventana**. Se intentó abrir una ventana nueva por CLI (`code -n`, `Code.exe --new-window`) y **no se creó** ventana desde este contexto, por lo que la recarga queda como único paso manual.

**Prevención de recurrencia:** el disparador más probable fue ejecutar `mvn clean`/`mvn clean verify` **mientras la ventana de VS Code tenía el proyecto abierto** (eso borra `backend/bus-system/target`, incluidos `generated-sources`, que JDT tenía registrados → `ObjectNotFoundException`). Recomendación: si se va a correr `mvn clean`, hacerlo con VS Code **cerrado** o ejecutar después `Java: Clean Java Language Server Workspace`. No se introduce ningún cambio de configuración forzoso por esto.

---

## 4. Pasos manuales exactos (única prueba restante)

1. En VS Code (ventana del proyecto `intiwatana_final_v2`), abre la Paleta de comandos: **Ctrl+Shift+P**.
2. Ejecuta **`Developer: Reload Window`**.
   - Si tras recargar siguiera en rojo/“Importing projects”, ejecuta también **`Java: Clean Java Language Server Workspace`** → confirma **"Restart and delete"**.
3. Espera a que el **Java Language Server termine de importar**:
   - La barra de estado (abajo) deja de mostrar “Importing projects… / Building workspace…”.
   - En `%APPDATA%\Code\User\workspaceStorage\43e3efbf10782ae448a8bfbb710e8c58\redhat.java\client.log.<fecha>` debe aparecer el arranque del servidor **sin** el mensaje “crashed 5 times”.
4. Comprueba que el puerto **8080 está libre** (no debe haber backend previo):
   ```powershell
   Get-NetTCPConnection -State Listen -LocalPort 8080 -ErrorAction SilentlyContinue
   ```
5. Sitúa el foco en el editor y pulsa **F5** (configuración `INTIWATANA: Backend Spring Boot (F5) + Frontend Vite + Navegador`).
6. Secuencia esperada:
   - Corre la `preLaunchTask` **“Frontend: preparar…”** y termina rápido → inicia/reutiliza **Vite en 5173** y lanza el lanzador del navegador.
   - Arranca **Spring Boot**; cuando `http://localhost:8080/actuator/health` responde, el lanzador **abre el navegador** en `http://localhost:5173/`.
   - El panel **Run and Debug** muestra la sesión activa.
7. **Detener:** pulsa **Shift+F5**. Verifica que 8080 se libera.
8. **Reiniciar:** pulsa **F5** de nuevo. Verifica que 8080 vuelve a responder.

**Verificación de que F5 (no Maven) es quien arrancó:** en el paso 6, el proceso `java` que escucha en 8080 debe colgar del **Java Debugger de VS Code** (padre = proceso JDT/debug de `redhat.java`), y **no** de `mvn.cmd`/`cmd`.

---

## 5. Estado de los 7 objetivos

| # | Objetivo | Estado | Evidencia |
|---|---|---|---|
| 1 | Java Language Server termina la importación | ❌ **NO** | `client.log`: caída 5×, “will not be restarted”; solo corre `ss_ws` |
| 2 | F5 deja de quedarse en “Importing projects” | ❌ **NO verificado** | bloqueado por #1 |
| 3 | Spring Boot inicia en 8080 | ✅ por **Maven** / ❌ por **F5** | Maven: `GET :8080/api/v1/viajes` → 200; F5 no arrancó |
| 4 | Vite inicia o se reutiliza en 5173 | ✅ | 5173 responde 200; el script reutiliza la instancia existente |
| 5 | El navegador abre tras responder ambos | ⚠️ lógica verificada estáticamente | `open-browser-after-backend.ps1` espera 5173+8080 antes de abrir; **no** verificable sin F5 real |
| 6 | El sistema puede detenerse | ✅ | detención por CLI (8080/5173 liberados) |
| 7 | El sistema puede iniciarse de nuevo | ✅ por Maven | reinicio `mvn spring-boot:run` → 8080 en 15 s |

---

## 6. Declaración

> ### F5 PENDIENTE DE VERIFICACIÓN MANUAL
> El F5 de VS Code **no puede darse por aprobado** en esta verificación. El Java Language Server está **deshabilitado** tras una caída en bucle causada por un **workspace JDT corrupto**. Se reparó el estado corrupto (de forma reversible) y quedan **instrucciones exactas** para la única prueba manual restante (sección 4).
>
> ### ARRANQUE LOCAL — NO CERRADO
> La configuración `.vscode/*` es correcta y la **alternativa Maven funciona**, pero F5 **no** se considera cerrado hasta que el dueño realice la recarga de ventana y la pulsación física de F5 descrita arriba y confirme: importación terminada → 8080 arriba → 5173 reutilizado → navegador abierto → detener (Shift+F5) → reiniciar (F5).
