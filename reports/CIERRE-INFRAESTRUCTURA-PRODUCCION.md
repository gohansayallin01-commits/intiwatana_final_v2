# CIERRE - INFRAESTRUCTURA Y PREPARACION PARA PRODUCCION

**Proyecto:** intiwatana_final_v2
**Fecha:** 2026-10-02
**Tipo:** Cierre de tarea de infraestructura / despliegue
**Veredicto:** **B — PREPARADO PARA PRODUCCION, PENDIENTE DE INFRAESTRUCTURA PRODUCTIVA**

---

## 1. Resumen ejecutivo

La aplicación quedó **separada del entorno de desarrollo/QA** y **preparada para
producción**: los secretos se externalizaron, el esquema es reproducible con
Flyway, los datos de demostración no se crean en producción y el arranque es
fail-fast ante configuración incompleta. Todo se validó contra una **base
productiva de ensayo vacía** (`transporte_prod_validation`), con **20 pruebas de
humo (D01–D20) superadas**, incluyendo respaldo y restauración con integridad
total.

No se declara el entorno productivo **desplegado** porque aún no existe la
infraestructura real (host, dominio, TLS, gestor de secretos, respaldos
automatizados). No hay bloqueos de código o configuración.

---

## 2. Alcance probado

| Entorno | Base | Perfil | Propósito |
|---|---|---|---|
| Desarrollo/QA | `transporte_bd` | `dev` | Se conserva intacto (QA permitido) |
| Validación productiva | `transporte_prod_validation` | `prod` | Ensayo del arranque productivo |
| Restauración | `transporte_restore_validation` | `prod` | Arranque contra base restaurada |
| Restauración con datos | `transporte_restore_check` | — | Integridad de `transporte_bd` |

---

## 3. Evidencia de las pruebas D01–D20

Puerto de validación productiva: **8081**; base restaurada: **8083**.

| # | Prueba | Resultado | Evidencia |
|---|---|---|---|
| D01 | Arranque con perfil `prod` | OK | "The following 1 profile is active: prod"; PID 4380 |
| D02 | Migraciones Flyway | OK | `flyway_schema_history`: V1, V2 `success = true` |
| D03 | Esquema completo | OK | 13 tablas + historial = 14 |
| D04 | Sin usuarios de demo | OK | `usuarios = 0` |
| D05 | Sin viajes de QA | OK | `viajes = 0` |
| D06 | Sin encomiendas de QA | OK | `encomiendas = 0` |
| D07 | Sin pagos de QA | OK | `pagos = 0` |
| D08 | Roles presentes | OK | 6 roles |
| D09 | Login con credenciales de desarrollo | OK | `POST /api/v1/auth/login` → **401** |
| D10 | Tracking con guía inválida | OK | **400** con mensaje genérico, sin datos |
| D11 | Endpoints protegidos sin token | OK | `/usuarios` y `/encomiendas` → **401** |
| D12 | Actuator | OK | `health` → `UP` (solo `status`); `info` → `{}`; `metrics` no expuesto (401) |
| D13 | CORS | OK | Origen permitido → 200 + `ACAO: http://localhost:5173`; origen ajeno → **403 Invalid CORS request** |
| D14 | Falta `JWT_SECRET` | OK | No arranca: `Could not resolve placeholder 'JWT_SECRET'` (exit 1) |
| D15 | Bundle sin `localhost` | OK | 0 ocurrencias |
| D16 | Bundle sin secretos | OK | 0 ocurrencias (claves, JWT, credenciales) |
| D17 | Logs sin secretos | OK | 0 ocurrencias (clave BD, JWT, `admin123`, `Bearer`) |
| D18 | Respaldo + verificación | OK | `pg_dump -Fc` 71 985 B (dev) / 52 030 B (validación); `pg_restore -l` correcto |
| D19 | Restauración con integridad | OK | 13/13 tablas idénticas en `transporte_bd`; base de validación íntegra |
| D20 | Arranque contra base restaurada | OK | Perfil `prod` en 8083; Flyway sin reaplicar; `health = UP` |

### Quality gates
- Backend: `mvn clean verify` → **54 pruebas, 0 fallos, 0 errores**.
- Frontend: `npm run lint` → **0 errores**; `npm run build` → OK.

---

## 4. Entregables producidos

| Artefacto | Contenido |
|---|---|
| `backend/bus-system/src/main/resources/application.yml` | Configuración común sin secretos |
| `application-dev.yml` / `application-test.yml` / `application-prod.yml` | Perfiles por entorno |
| `db/migration/V1__esquema_inicial.sql` | Esquema de las 13 tablas |
| `db/migration/V2__catalogos_base.sql` | 6 roles base |
| `DataInitializer.java` / `AdminToolsController.java` | `@Profile("!prod")` |
| `JwtUtil.java` | Secreto obligatorio, sin valor por defecto |
| `.env.example` / `.gitignore` | Plantilla y exclusiones |
| `frontend/Web-Intiwatana/vite.config.js` / `.env.example` | Proxy externalizado |
| `reports/SANEAMIENTO-SECRETOS-RESULTADO.md` | Auditoría y saneamiento |
| `reports/BACKUP-RESTAURACION-POSTGRESQL.md` | Procedimiento y evidencia de respaldo |
| `reports/DESPLIEGUE-PRODUCCION-INTIWATANA.md` | Guía de despliegue y operación |
| `reports/CIERRE-INFRAESTRUCTURA-PRODUCCION.md` | Este documento |

---

## 5. Riesgos residuales

| Riesgo | Severidad | Mitigación / estado |
|---|---|---|
| Credenciales de desarrollo no rotadas | Media | Externalizadas; rotación planificada para producción |
| Contraseñas de demo en `DataInitializer` (solo `!prod`) | Baja | No se ejecuta en prod; prod con 0 usuarios |
| `database/transporte_bd.sql` con hashes BCrypt | Media | Añadido a `.gitignore`; no distribuir |
| Respaldo solo local (sin offsite) | Media | Plan de respaldo offsite documentado, pendiente de implementar |
| Sin TLS/reverse proxy aún | Alta (si se expusiera tal cual) | Despliegue real pendiente de infraestructura |

---

## 6. Pendientes de infraestructura productiva

1. Aprovisionar host/servidor y gestor de secretos.
2. Crear la base productiva real y su usuario de mínimo privilegio.
3. Definir dominio, certificados TLS y reverse proxy (frontend + `/api`).
4. Automatizar respaldos offsite y su verificación periódica.
5. Configurar CI/CD y monitoreo/alertas.
6. Rotar secretos de desarrollo y confirmar la custodia productiva.

---

## 7. Veredicto

### **B — PREPARADO PARA PRODUCCION, PENDIENTE DE INFRAESTRUCTURA PRODUCTIVA**

- **No es A:** el sistema **no está desplegado** en el entorno productivo real
  (host, dominio, TLS, secretos productivos y respaldos automáticos aún no
  existen).
- **No es C:** no hay defectos de código ni de configuración bloqueantes. La
  construcción productiva arranca, migra, valida y supera las 20 pruebas de humo,
  y el respaldo/restauración se verificó con integridad total.

Las tareas de la sesión quedan **cerradas** con este veredicto y con la lista de
pendientes de infraestructura documentada en §6.
