# RESPALDO Y RESTAURACION - POSTGRESQL

**Proyecto:** intiwatana_final_v2
**Fecha:** 2026-10-02
**Motor:** PostgreSQL 17.5 (cliente y servidor)
**Herramientas:** `pg_dump` / `pg_restore` (formato custom `-Fc`)
**Naturaleza de las pruebas:** no destructivas. Nunca se modificó la base de
desarrollo `transporte_bd`; el respaldo se genera en modo lectura y la
restauración se hace en bases independientes.

---

## 1. Objetivo

Demostrar que (a) se puede generar un respaldo confiable, (b) el respaldo puede
verificarse, (c) se puede restaurar en una base nueva y (d) la aplicación arranca
correctamente contra la base restaurada.

---

## 2. Bases de datos usadas

| Base | Rol | Propósito |
|---|---|---|
| `transporte_bd` | Origen (desarrollo/QA) | Fuente del respaldo "real" (con datos) |
| `transporte_restore_check` | Restaurada | Verificación de integridad de `transporte_bd` |
| `transporte_prod_validation` | Productiva vacía (validación) | Respaldo/restauración de una base productiva limpia |
| `transporte_restore_validation` | Restaurada | Verificación de la base de validación |

---

## 3. Procedimiento ejecutado

### 3.1 Respaldo (modo lectura)
```powershell
$env:PGPASSWORD = "<clave>"
pg_dump -h localhost -U <usuario> -d <base> -Fc --no-owner --no-privileges -f <archivo>.dump
```

### 3.2 Verificación del respaldo (índice interno, sin restaurar)
```powershell
pg_restore -l <archivo>.dump    # lista tablas, secuencias, datos
```

### 3.3 Creación de la base destino
```sql
CREATE DATABASE <base_destino> OWNER <usuario>;
ALTER SCHEMA public OWNER TO <usuario>;
REVOKE ALL ON SCHEMA public FROM PUBLIC;
```

### 3.4 Restauración
```powershell
pg_restore -h localhost -U <usuario> -d <base_destino> --no-owner --no-privileges <archivo>.dump
```

### 3.5 Arranque contra la base restaurada
```powershell
java -jar target/sistema-transporte-1.0.0.jar --spring.profiles.active=prod
# DB_URL apunta a la base restaurada; Flyway detecta el historial y Hibernate valida.
```

---

## 4. Evidencia

### 4.1 Base de desarrollo `transporte_bd`

Respaldo generado: **71 985 bytes** (`pg_dump -Fc`).

Comparación de filas origen vs. restaurada (**13/13 tablas idénticas**):

| Tabla | Origen | Restaurada |
|---|---:|---:|
| asientos | 308 | 308 |
| auditoria | 330 | 330 |
| boletos | 24 | 24 |
| buses | 6 | 6 |
| clientes | 9 | 9 |
| encomiendas | 18 | 18 |
| movimientos_encomienda | 42 | 42 |
| pagos | 43 | 43 |
| roles | 6 | 6 |
| rutas | 14 | 14 |
| sucursales | 6 | 6 |
| usuarios | 12 | 12 |
| viajes | 11 | 11 |

**Resultado: IDÉNTICO.**

### 4.2 Base productiva de validación `transporte_prod_validation`

- Respaldo: **52 030 bytes**.
- `pg_restore -l`: 28 entradas de tabla/relación; incluye `flyway_schema_history`
  y `roles`.
- Restauración en `transporte_restore_validation`: **14 tablas** (13 de negocio +
  historial Flyway), **6 roles**, **0** usuarios/viajes/encomiendas/pagos,
  **2** migraciones exitosas.

### 4.3 Arranque contra la base restaurada
- Perfil `prod` sobre `transporte_restore_validation`, puerto 8083.
- Spring Boot inició correctamente; **Flyway no reaplicó migraciones** (historial
  presente) y **Hibernate `validate` pasó**.
- `GET /actuator/health` → `{"status":"UP"}`.

---

## 5. Plan de respaldos recomendado (producción)

| Aspecto | Recomendación |
|---|---|
| Frecuencia | Diario (completo) + WAL archiving / PITR si el negocio lo exige |
| Formato | Custom `-Fc` (`pg_dump`) para permitir restauración selectiva |
| Retención | 7 diarios, 4 semanales, 12 mensuales (ajustable a política) |
| Destino | **Fuera del host de base de datos** (almacenamiento externo/objeto) |
| Cifrado | Almacenamiento cifrado en reposo; transporte por canal seguro |
| Verificación | Restaurar periódicamente en un entorno de ensayo (prueba como la de §4) |
| Automatización | Tarea programada / cron + alerta ante fallo |
| Responsable | Rotar la clave de respaldo y documentar el procedimiento |

> Nota: las pruebas de esta sesión son **locales**. Un esquema de respaldo
> productivo requiere destino offsite y su propia validación periódica.

---

## 6. Limitaciones y pendientes

- Los respaldos se generaron en el equipo local; **no hay copia offsite**.
- No se configuró todavía un programador de tareas de respaldo.
- La base de validación es un entorno de ensayo, no la base productiva real.

---

## 7. Veredicto

**MECANISMO DE RESPALDO/RESTAURACIÓN VERIFICADO.** El respaldo puede generarse,
inspeccionarse y restaurarse con integridad total (13/13 tablas idénticas en el
caso con datos), y la aplicación arranca contra la base restaurada. Queda
pendiente la **automatización y el almacenamiento offsite** propios del entorno
productivo.
