# Respaldo de la base de datos — INTIWATANA S.R.L.

## Estado actual

| Elemento | Estado |
|---|---|
| `intiwatana_transporte_bd.backup` | **PENDIENTE — todavía no generado** |
| Motivo | La contraseña del usuario `postgres` no es conocida. `application.yml` declaraba `password: <REDACTADO>` y PostgreSQL responde `FATAL: la autenticación password falló para el usuario «postgres»`. |
| Formato previsto | Custom de `pg_dump` (`-Fc`), portable con `pg_restore` |

No se ha generado ningún archivo de respaldo. Este documento describe el
procedimiento exacto a seguir en cuanto se disponga de la credencial.

> **Este archivo NO es un respaldo.** Para restaurar la base de datos hay que usar
> `intiwatana_transporte_bd.backup`, no este README ni
> `backend/bus-system/src/main/resources/schema.sql` (que además contiene
> `DROP TABLE` y es destructivo).

---

## Diagnóstico del bloqueo

Ejecutado sobre el servidor local. El bloqueo es **solo de credencial**, no de
configuración: todo lo demás está listo.

| Comprobación | Resultado |
|---|---|
| Servicio `postgresql-x64-17` | **Activo** |
| Puerto 5432 | **Escuchando** en `0.0.0.0` y `::` |
| Versión de las herramientas | `psql (PostgreSQL) 17.5` |
| `psql.exe`, `pg_dump.exe`, `pg_restore.exe`, `createdb.exe`, `dropdb.exe` | **Presentes** en `C:\Program Files\PostgreSQL\17\bin` |
| Conexión por IPv4 `127.0.0.1` | **FALLA** — `FATAL: la autenticación password falló para el usuario «postgres»` |
| Conexión por IPv6 `::1` | **FALLA** — mismo error |
| `pg_hba.conf` | Todas las líneas activas exigen `scram-sha-256`. **No hay ninguna regla `trust`** |
| `.pgpass` en `%APPDATA%\postgresql`, `%USERPROFILE%` y el directorio de datos | **No existe** |
| Variables `PGPASSWORD` / `DB_PASSWORD` / `DB_URL` | **No definidas** |

Conclusión: hace falta la contraseña real del rol `postgres`. Con
`scram-sha-256` y sin regla `trust` no existe ninguna vía alternativa de
autenticación local. Modificar `pg_hba.conf` o resetear la contraseña del rol
requiere autorización expresa y **no se ha hecho**.

> **Aviso sobre la pérdida de información:** con `scram-sha-256` y sin regla
> `trust`, un rol inexistente y una contraseña incorrecta producen el mismo
> mensaje. Por eso este diagnóstico **no puede confirmar** que el rol `postgres`
> exista ni que la base `transporte_bd` esté creada; solo constata que la
> autenticación falla.

---

## Procedimiento

Todos los comandos usan las herramientas de PostgreSQL 17 instaladas en
`C:\Program Files\PostgreSQL\17\bin`.

### 1. Probar la conexión

```powershell
$env:PGPASSWORD='<CONTRASENA>'
& "C:\Program Files\PostgreSQL\17\bin\psql.exe" -U postgres -h 127.0.0.1 -p 5432 -d transporte_bd -c "SELECT version();"
```

### 2. Generar el respaldo (formato custom, comprimido)

```powershell
New-Item -ItemType Directory -Force -Path database | Out-Null
$env:PGPASSWORD='<CONTRASENA>'
& "C:\Program Files\PostgreSQL\17\bin\pg_dump.exe" -Fc -U postgres -h 127.0.0.1 -p 5432 -d transporte_bd -f "database\intiwatana_transporte_bd.backup"
```

### 3. Validar la integridad del archivo

```powershell
& "C:\Program Files\PostgreSQL\17\bin\pg_restore.exe" --list "database\intiwatana_transporte_bd.backup"
```

Debe listar las tablas, índices, secuencias y restricciones. Un archivo corrupto
falla aquí.

### 4. Probar la restauración en una base temporal

```powershell
& "C:\Program Files\PostgreSQL\17\bin\createdb.exe" -U postgres -h 127.0.0.1 -p 5432 transporte_bd_restore_test
& "C:\Program Files\PostgreSQL\17\bin\pg_restore.exe" -U postgres -h 127.0.0.1 -p 5432 -d transporte_bd_restore_test "database\intiwatana_transporte_bd.backup"
& "C:\Program Files\PostgreSQL\17\bin\psql.exe" -U postgres -h 127.0.0.1 -p 5432 -d transporte_bd_restore_test -c "\dt"
```

### 5. Comparar el número de filas entre el original y la copia restaurada

```powershell
# Sustituir <tabla> por cada tabla con datos de negocio
& "C:\Program Files\PostgreSQL\17\bin\psql.exe" -U postgres -h 127.0.0.1 -p 5432 -d transporte_bd            -c "SELECT count(*) FROM <tabla>;"
& "C:\Program Files\PostgreSQL\17\bin\psql.exe" -U postgres -h 127.0.0.1 -p 5432 -d transporte_bd_restore_test -c "SELECT count(*) FROM <tabla>;"
```

Los totales deben coincidir. Revisar como mínimo: `sucursales`, `rutas`, `buses`,
`viajes`, `asientos`, `clientes`, `boletos`, `pagos`, `encomiendas`,
`movimientos_encomienda`, `usuarios`.

### 6. Eliminar SÓLO la base temporal

```powershell
& "C:\Program Files\PostgreSQL\17\bin\dropdb.exe" -U postgres -h 127.0.0.1 -p 5432 transporte_bd_restore_test
```

`transporte_bd` (la original) no se toca en ningún paso.

---

## Restaurar el respaldo en una máquina nueva

```powershell
New-Item -ItemType Directory -Force -Path database | Out-Null
Copy-Item "database\intiwatana_transporte_bd.backup" "<RUTA>\database\"

# Crear la base vacía (si aún no existe)
& "C:\Program Files\PostgreSQL\17\bin\createdb.exe" -U postgres -h 127.0.0.1 -p 5432 transporte_bd

# Restaurar
$env:PGPASSWORD='<CONTRASENA>'
& "C:\Program Files\PostgreSQL\17\bin\pg_restore.exe" -U postgres -h 127.0.0.1 -p 5432 -d transporte_bd --clean --if-exists "database\intiwatana_transporte_bd.backup"
```

`--clean --if-exists` borra los objetos que ya existan en el destino antes de
crearlos. **Ejecutar `pg_restore --clean` sobre `transporte_bd` elimina los datos
actuales de esa base**, así que solo debe usarse contra una base recién creada o
descartable.

---

## Configuración de la aplicación

El backend lee la configuración de estas variables de entorno, con estos valores
por defecto para el entorno local:

| Variable | Por defecto | Para qué |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/transporte_bd` | Cadena de conexión |
| `DB_USERNAME` | `postgres` | Usuario |
| `DB_PASSWORD` | *(sin valor por defecto)* | Contraseña — obligatoria por variables de entorno |
| `JWT_SECRET` | secreto de desarrollo | Firma de los tokens JWT |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://localhost:3000` | Orígenes permitidos |

En producción es obligatorio definir `DB_PASSWORD` y `JWT_SECRET` con valores
propios. Si `JWT_SECRET` se deja en el valor por defecto, la aplicación escribe un
aviso en el log al arrancar.

> **Inconsistencia resuelta:** antes, `backend/bus-system/.env.example` declaraba
> `DB_URL=jdbc:postgresql://localhost:5432/transporte_db` mientras que `application.yml`
> usaba `transporte_bd`. Ya se unificó: ambos archivos apuntan a la base `transporte_bd`,
> que es la misma base respaldada en `database/intiwatana_transporte_bd.backup` y la
> exportada en `database/transporte_bd.sql`.

---

## Notas importantes

- **PostgreSQL es el único motor** del proyecto. No hay MySQL, SQLite ni otro.
- `spring.jpa.hibernate.ddl-auto: update` está activo: al arrancar, Spring
  **modifica el esquema** de la base de datos (añade columnas o tablas que falten).
  Por eso hay que tener el respaldo antes de levantar la aplicación contra la base
  de datos de producción.
- `backend/bus-system/src/main/resources/schema.sql` contiene `DROP TABLE` y sirve
  para crear una base **nueva desde cero**. No es un respaldo y nunca debe
  ejecutarse contra `transporte_bd`.
- La migración
  `backend/bus-system/src/main/resources/db/migracion-2026-01-destinatario-encomienda.sql`
  también requiere que el respaldo exista y esté verificado.
