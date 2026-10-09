# Mapa de asientos por vehículo — Resultado

**Fecha:** 2026-10-08
**Alcance:** corregir el mapa de asientos del portal público para que cada viaje
muestre TODOS los asientos reales del vehículo asignado, con capacidad,
distribución, pisos y contadores coherentes (requisitos A–F).
**Estado:** completado y verificado (backend + frontend + E2E navegador).

---

## 1. Causa raíz

El componente `MapaAsientos` de `PasajesPage.jsx` **hardcodeaba** un layout de
Toyota Hiace de 15 asientos y renderizaba por **índice de array**, no por
`fila`/`columna`/`piso`. Con un bus de 40 asientos, la UI mostraba ~15.

El backend sí devolvía los 40 asientos de `GET /api/v1/viajes/{id}/asientos`,
pero la generación usaba un algoritmo fijo (1 piso / 4 columnas) que ignoraba
el número de pisos y no leía ninguna configuración por vehículo.

## 2. Solución

### 2.1 Configuración de asientos por vehículo (backend)
- Nueva columna `buses.configuracion_asientos` (TEXT, JSON).
- Esquema: `{ plantilla, pasillo, pisos:[{ piso, asientosPorFila:[int...] }] }`.
  - `total = suma(asientosPorFila)`; `pasillo = nº de columnas antes del hueco`.
  - `estadoConfiguracion = COMPLETO` si `total == capacidadAsientos`.
- Auto-genera configuración completa al **crear** un bus.
- **Backfill** al arrancar la app (`ApplicationReadyEvent`): los buses existentes
  sin configuración reciben una 4-columnas que, por ser todas las capacidades
  divisibles por 4, queda `COMPLETO`.
- La **creación de viajes valida** que el bus tenga configuración COMPLETA;
  si no, responde `400` (`NegocioException`).
- La **generación de asientos** ahora deriva de `ConfiguracionAsientosService`
  (piso/fila/columna/tipo), no de un layout fijo.

### 2.2 API
| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/v1/buses/plantillas` | Plantillas base (VAN, MINIBUS, BUS_32/36/40/44, PERSONALIZADO) |
| PUT | `/api/v1/buses/{id}/configuracion-asientos` | Guarda configuración; valida contra la capacidad |
| — | `GET /api/v1/buses` | Incluye `configuracionAsientos`, `asientosConfigurados`, `estadoConfiguracion` |

### 2.3 Mapa público (frontend)
`MapaAsientos` reescrito como **dinámico**: agrupa por `piso → fila → columna`,
inserta el hueco de pasillo según `configuracion.pasillo`, agrega numeración de
fila, mantiene scroll **interno vertical** y contadores coherentes
(`disponibles + seleccionados + reservados + vendidos + bloqueados == total`).
El asiento seleccionado se descuenta de *disponibles* (no se cuenta doble).

### 2.4 Administración (frontend)
`CrudBuses` con columnas **"Asientos config."** y **"Estado"** (badge
COMPLETO/INCOMPLETO) y acción **"Configurar asientos"** que abre un modal con:
plantilla base, nº de pisos, pasillo, ajuste rápido, **generar por capacidad**,
editor de filas/columnas, vista previa y badge COMPLETO/INCOMPLETO (guardar
deshabilitado si está incompleto). El selector de bus en `CrudViajes` **marca y
deshabilita** buses con mapa INCOMPLETO.

### 2.5 Migración
`V8__configuracion_asientos_bus.sql` (`ALTER TABLE buses ADD COLUMN IF NOT EXISTS
configuracion_asientos TEXT`). En `dev` (perfil activo) Flyway está desactivado y
Hibernate aplica la columna; la migración cubre `prod`.

## 3. Archivos

**Backend — nuevos**
- `.../dto/ConfiguracionAsientos.java`
- `.../dto/PlantillaAsientos.java`
- `.../service/ConfiguracionAsientosService.java`
- `.../config/ConfiguracionAsientosInitializer.java`
- `.../resources/db/migration/V8__configuracion_asientos_bus.sql`
- `.../test/.../service/ConfiguracionAsientosServiceTest.java`

**Backend — modificados**
- `.../entity/Bus.java`
- `.../dto/response/BusResponse.java`
- `.../controller/BusController.java`
- `.../service/impl/ViajeServiceImpl.java`

**Frontend**
- `src/components/PasajesPage.jsx` (mapa dinámico)
- `src/components/AdminDashboard.jsx` (CrudBuses + modal + badge + selector)
- `src/components/reserva-premium.css` (estilos `.iwr-map-*`, responsive)

## 4. Pruebas

### 4.1 Unitarias (backend)
`mvn -q -Dtest=ConfiguracionAsientosServiceTest test` → **7/7 PASS**.
`mvn -q -DskipTests compile` y `package` → **OK**.

### 4.2 E2E backend (API real, con restore de datos)
- Backfill: 5 buses → `COMPLETO`, `asientosConfigurados == capacidad`.
- Crear viaje temporal (bus 40) → **40 asientos** generados (filas 1–10,
  columnas 1–4, piso 1; cols 1/4 = VENTANA, 2/3 = PASILLO). Eliminado tras probar.
- PUT config de 36 en bus de 40 → **HTTP 400** (rechazo).
- PUT config válida de 40 → **200**, `COMPLETO`.
- Capacidad 40→39 → `INCOMPLETO`; `POST /viajes` → **HTTP 400 (bloqueado)**;
  restaurado a 40 → `COMPLETO`. Sin datos residuales (`viajes = 9`).

### 4.3 Lint / Build frontend
- `npm run lint` → **sin errores**.
- `npm run build` → **OK** (1761 módulos; único aviso: tamaño de chunk >500 kB).

### 4.4 Navegador (Playwright, sin errores de consola ni requests fallidos)
Viaje 33 (ABC-123, ECONOMICO, Huamanga → Huarcas, 09 oct 13:00):

| Métrica | Desktop | Móvil 390×844 |
|---|---|---|
| Asientos renderizados | **40** | **40** |
| Filas (`1`–`10`) | 10 | 10 |
| Pasillos | 10 | 10 |
| Celdas vacías | 0 | 0 |
| Pisos | 1 | 1 |
| Primero / último | `1A` / `10D` | `1A` / `10D` |
| Scroll interno vertical | `overflow-y:auto` | `overflow-y:auto` |
| Overflow horizontal de página | 0 px | 0 px |
| Leyenda | Disp 36 · Sel 0 · Res 4 · Vend 0 · Bloq 0 (=40) | igual |

- Al seleccionar un asiento: Disp 36→35, **Seleccionado 1**, total sigue **40**.
- Click en asiento RESERVADO: **no cambia la selección** (no seleccionable).
- Administración: cabeceras `Asientos config.`/`Estado`, 5 buses `COMPLETO`,
  5 botones "Configurar asientos"; el modal muestra plantillas, pisos, pasillo,
  editor y badge `COMPLETO` con `Configurados: N / N`.

## 5. Evidencia
`reports/evidencia-mapa-asientos/`
- `mapa-desktop.png`
- `mapa-movil.png`
- `admin-config-asientos.png`

## 6. Anomalías históricas preexistentes (no modificadas)
- Viajes **9** y **10** (`FINALIZADO`) no tienen asientos almacenados (0).
- Viaje **6** tiene 28 asientos frente a capacidad 40.
Se dejaron intactos por la restricción "no alterar viajes históricos". Los viajes
nuevos y los consistentes (2, 3, 5, 8, 31, 33) cuadran al 100 %.

## 7. Veredicto
**Corregido y verificado.** El mapa público del viaje 33 renderiza los **40**
asientos reales del vehículo (antes ~15), los contadores suman la capacidad
total, no hay overflow horizontal en escritorio ni móvil, la selección y los
asientos reservados se comportan correctamente, y la administración permite
configurar/validar el mapa por vehículo. Sin errores de consola ni de red.

## 8. Cómo reproducir
1. Backend: `mvn -DskipTests package` en `backend/bus-system`; ejecutar el jar
   (perfil `dev`, `localhost:8080`).
2. Frontend: `npm run dev` en `frontend/Web-Intiwatana` (Vite en `5173`,
   proxy `/api` → `8080`).
3. Portal: `http://localhost:5173/pasajes` → origen **Huamanga**, destino
   **Huarcas**, Buscar → tarjeta ABC-123 → "Elegir asiento".
4. Admin: `http://localhost:5173/login` (`admin`/`admin123`) → **Autobuses**.
