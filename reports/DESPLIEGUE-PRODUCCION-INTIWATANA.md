# DESPLIEGUE EN PRODUCCION - INTIWATANA

**Proyecto:** intiwatana_final_v2
**Fecha:** 2026-10-02
**Backend:** Spring Boot 3.3.0 / Java 17 · **Frontend:** Vite + React 19
**Base de datos:** PostgreSQL 17 · **Migraciones:** Flyway

> Este documento describe el procedimiento verificado. Los valores sensibles se
> escriben como `<...>`; **ningún secreto real forma parte del repositorio**.

---

## 1. Requisitos previos

| Componente | Versión / condición |
|---|---|
| Java | 17 (probado con Temurin 17.0.18) |
| Maven | 3.9.x (solo para construir) |
| PostgreSQL | 17.x (probado 17.5) |
| Node.js | 18+ (solo para construir el frontend) |
| Red | Backend accesible bajo HTTPS detrás de un reverse proxy |
| Secretos | Gestor de secretos / variables de entorno del orquestador |

---

## 2. Variables de entorno

### 2.1 Obligatorias en producción (si falta alguna, la aplicación NO arranca)
| Variable | Descripción |
|---|---|
| `SPRING_PROFILES_ACTIVE` | Debe ser `prod` |
| `DB_URL` | `jdbc:postgresql://<host>:5432/<base>` |
| `DB_USERNAME` | Usuario de base de datos de mínimo privilegio |
| `DB_PASSWORD` | Contraseña del usuario anterior |
| `JWT_SECRET` | Clave de firma JWT (≥32 bytes, Base64). **Distinta a la de desarrollo** |
| `CORS_ALLOWED_ORIGINS` | Orígenes reales, p. ej. `https://app.dominio.pe` |

### 2.2 Opcionales
| Variable | Valor por defecto |
|---|---|
| `SERVER_PORT` | `8080` |
| `JWT_EXPIRATION_MS` | `86400000` (24 h) |
| `JWT_REFRESH_MS` | `604800000` (7 días) |
| `DB_POOL_MAX` / `DB_POOL_MIN` | `10` / `3` |
| `LOG_LEVEL_APP` | `INFO` |
| `LOG_FILE` | `logs/sistema-transporte.log` |

> En el perfil `prod`, `management.endpoints.web.exposure.include` es `health,info`
> y `management.endpoint.health.show-details` es `never` (no expone detalles).

---

## 3. Base de datos productiva (una sola vez)

```sql
-- Ejecutar como superusuario de PostgreSQL
CREATE ROLE intiwatana_app LOGIN PASSWORD '<clave-fuerte>'
    NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT;
CREATE DATABASE intiwatana_prod OWNER intiwatana_app;
\connect intiwatana_prod
ALTER SCHEMA public OWNER TO intiwatana_app;
REVOKE ALL ON SCHEMA public FROM PUBLIC;
GRANT CONNECT ON DATABASE intiwatana_prod TO intiwatana_app;
```

- La base se crea **vacía**. El esquema lo construye Flyway al primer arranque.
- **No** cargar datos de demostración ni copiar la base de desarrollo.

---

## 4. Backend

### 4.1 Construcción
```powershell
cd backend\bus-system
mvn clean package
# Artefacto: target/sistema-transporte-1.0.0.jar
```
Quality gate: `mvn clean verify` → **54 pruebas, 0 fallos** (build reproduciendo la
sesión de validación).

### 4.2 Arranque
```powershell
$env:SPRING_PROFILES_ACTIVE = "prod"
$env:DB_URL      = "jdbc:postgresql://<host>:5432/intiwatana_prod"
$env:DB_USERNAME = "intiwatana_app"
$env:DB_PASSWORD = "<clave>"
$env:JWT_SECRET  = "<secreto>"
$env:CORS_ALLOWED_ORIGINS = "https://app.dominio.pe"
java -jar target\sistema-transporte-1.0.0.jar
```

### 4.3 Qué ocurre al arrancar
1. Se resuelven las variables (fail-fast si falta alguna).
2. **Flyway** aplica `V1__esquema_inicial.sql` y `V2__catalogos_base.sql`.
3. **Hibernate** (`ddl-auto: validate`) valida el modelo contra el esquema.
4. `DataInitializer` **no se ejecuta** (`@Profile("!prod")`).

---

## 5. Frontend

### 5.1 Construcción
```powershell
cd frontend\Web-Intiwatana
npm ci
npm run lint      # 0 errores
npm run build     # genera dist/
```
El bundle usa rutas relativas `/api`; **no** contiene `localhost` ni secretos.

### 5.2 Publicación
- Servir `dist/` con un servidor estático (Nginx, etc.).
- Configurar el reverse proxy para que `/api` apunte al backend.
- Habilitar HTTPS (TLS) y redirección desde HTTP.

---

## 6. Verificación post-despliegue (lista de humo)

| # | Comprobación | Esperado |
|---|---|---|
| D01 | Arranque con perfil `prod` | Inicia sin datos de demo |
| D02 | Migraciones Flyway | V1 y V2 `success = true` |
| D03 | Esquema completo | 13 tablas + `flyway_schema_history` |
| D04–D07 | Sin usuarios/viajes/encomiendas/pagos de QA | 0 filas |
| D08 | Catálogo de roles | 6 roles |
| D09 | Login con credenciales de desarrollo | `401` |
| D10 | Tracking con guía inválida | `400`, mensaje genérico, sin datos |
| D11 | Endpoints protegidos sin token | `401` |
| D12 | `/actuator/health` | `{"status":"UP"}`, sin detalles |
| D13 | CORS: origen permitido / no permitido | Cabecera permitida / `403` |
| D14 | Falta `JWT_SECRET` | La aplicación no arranca |
| D15–D16 | Bundle sin `localhost` ni secretos | Limpio |
| D17 | Logs sin secretos | Limpio |
| D18–D20 | Respaldo/restauración y arranque | Integridad total |

---

## 7. Seguridad de producción

| Tema | Acción |
|---|---|
| Secretos | Inyectar por entorno/secret manager; nunca en el repositorio |
| JWT | Secreto nuevo y aleatorio (no reutilizar el de desarrollo) |
| CORS | Solo el dominio real, HTTPS |
| Actuator | Solo `health,info`; sin detalles |
| Errores | Sin stack traces ni mensajes internos al cliente |
| Base de datos | Usuario de mínimo privilegio; sin superusuario |
| Backups | Automáticos y offsite (ver `BACKUP-RESTAURACION-POSTGRESQL.md`) |

---

## 8. Rollback

1. Detener la instancia nueva.
2. Volver a desplegar el artefacto anterior.
3. Si el esquema cambió: restaurar el respaldo previo (Flyway no revierte hacia
   atrás automáticamente; usar `pg_restore`).
4. Revalidar con la lista de humo (§6).

---

## 9. Operación

- **Salud:** `GET /actuator/health`.
- **Logs:** `${LOG_FILE}` (por defecto `logs/sistema-transporte.log`).
- **Migraciones:** consultar `flyway_schema_history`.
- **Backups:** seguir el plan de `BACKUP-RESTAURACION-POSTGRESQL.md`.

---

## 10. Pendientes específicos de infraestructura productiva

- Aprovisionar el host/servidor real y el gestor de secretos.
- Registrar el dominio real y emitir certificados TLS.
- Configurar el reverse proxy (frontend + `/api`).
- Automatizar respaldos y su verificación periódica.
- Definir CI/CD y monitoreo/alertas.
