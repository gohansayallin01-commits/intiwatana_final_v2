# Datos QA del modulo de Encomiendas y Tracking

Fecha: 2026-10-02
Base de datos: `transporte_bd` (PostgreSQL 17, solo lectura para esta auditoria)
Autor: cierre comercial y preparacion para produccion del modulo de Encomiendas y Tracking
Alcance: identificar y separar los datos de prueba (QA) de una futura base productiva.

> IMPORTANTE: este documento es solo diagnostico. NO se elimino, modifico ni
> archivo ningun registro. Cualquier accion sobre los datos requiere respaldo y
> autorizacion expresa (ver `reports/sql/LIMPIEZA-QA-ENCOMIENDAS-PROPUESTA.sql`).

## 1. Metodo

Se revisaron todos los registros de la tabla `encomiendas` (ids 21 a 38) junto con
sus movimientos (`movimientos_encomienda`), pagos (`pagos`), remitentes
(`clientes`/`usuarios`) y observaciones. La evidencia usada para clasificar:

- Usuario remitente y su proposito (cuentas de auditoria/prueba claramente
  nombradas: `pruebaa001`, `e2e_test01`, `cliente_aud9709`).
- Texto de `observaciones`, `destinatario_nombre` y descripcion con marcas QA.
- Estado y origen (registro interno vs. solicitud desde portal web).

## 2. Inventario completo (21-36)

| id | guia | estado | costo | peso | remitente | destinatario | observaciones (recorte) |
|----|------|--------|-------|------|-----------|--------------|--------------------------|
| 21 | GUI-20260930-000021 | ENTREGADO | 12.50 | 2.500 | (sin cliente) | (vacio) | (vacio) |
| 22 | GUI-20260930-000022 | RECIBIDO | 10.00 | 1.200 | percy7096 | Juana Perez | Destinatario: Juana Perez / Tel: 999111222 |
| 23 | GUI-20260930-000023 | ENTREGADO | 10.00 | 2.000 | cliente_aud9709 | (vacio) | E2E operador |
| 24 | GUI-20260930-000024 | RECIBIDO | 10.00 | 1.000 | percy7096 | (vacio) | (vacio) |
| 25 | GUI-20260930-000025 | RECIBIDO | 12.50 | 2.500 | pruebaa001 | (vacio) | INTIWATANA-PRUEBA |
| 26 | GUI-20261001-000026 | EN_ALMACEN | 10.00 | 3.500 | pruebaa001 | Destinatario INTIWATANA-D-001 | Destinatario: Destinatario INTIWATANA-D-001 / Tel: 999111222 / Remitente: Remitente INTIWATANA-D-001 |
| 27 | GUI-20261001-000027 | RECIBIDO | 10.00 | 3.500 | pruebaa001 | Destinatario INTIWATANA-D-001 | idem 26 |
| 28 | GUI-20261001-000028 | EN_ALMACEN | 10.00 | 3.500 | pruebaa001 | Destinatario INTIWATANA-D-001 | idem 26 |
| 29 | GUI-20261001-000029 | ENTREGADO | 10.00 | 3.500 | pruebaa001 | Destinatario INTIWATANA-D-001 | idem 26 |
| 30 | GUI-20261002-000030 | EN_ALMACEN | 12.50 | 2.500 | (sin cliente) | (vacio) | (vacio) |
| 31 | GUI-20261002-000031 | RECIBIDO | 10.00 | 1.500 | pruebaa001 | Destino Web QA | Destinatario: Destino Web QA / Tel: 999888777 |
| 32 | GUI-20261002-000032 | EN_ALMACEN | 12.50 | 2.500 | (sin cliente) | (vacio) | (vacio) |
| 33 | GUI-20261002-000033 | RECIBIDO | 10.00 | 1.500 | pruebaa001 | Destino Web QA | Destinatario: Destino Web QA / Tel: 999888777 |
| 34 | GUI-20261002-000034 | RECIBIDO | 10.00 | 1.000 | (sin cliente) | (vacio) | (vacio) |
| 35 | GUI-20261002-000035 | RECIBIDO | 16.25 | 3.250 | pruebaa001 | (vacio) | (vacio) |
| 36 | GUI-20261002-000036 | RECIBIDO | 25.00 | 4.500 | pruebaa001 | QA Confirmacion P01 | Destinatario: QA Confirmacion P01 / Tel: 999111222 / QA cierre produccion P01 |
| 37 | GUI-20261002-000037 | RECIBIDO | 15.00 | 2.100 | pruebaa001 | QA Confirmacion P37 | Destinatario: QA Confirmacion P37 / Paquete QA validaciones |
| 38 | GUI-20261002-000038 | RECIBIDO | 20.00 | 3.400 | pruebaa001 | QA UI Pendiente | Destinatario: QA UI Pendiente / Paquete QA UI pendiente |

## 3. Clasificacion propuesta

### 3.1 QA claro (candidatas a separar/archivar) — 12 registros

Evidencia inequivoca de prueba (cuentas y textos de QA):

- **23** — observacion `E2E operador`, remitente `cliente_aud9709`.
- **25** — observacion `INTIWATANA-PRUEBA`, remitente `pruebaa001`.
- **26, 27, 28, 29** — remitente `pruebaa001`, destinatario y observaciones
  `Destinatario INTIWATANA-D-001` / `Remitente INTIWATANA-D-001`.
- **31, 33** — remitente `pruebaa001`, destinatario `Destino Web QA`.
- **35** — remitente `pruebaa001` (lote de pruebas del portal del 2026-10-02).
- **36** — creada durante la verificacion del flujo de confirmacion de recepcion
  (destinatario `QA Confirmacion P01`, observacion `QA cierre produccion P01`).
- **37** — creada durante las pruebas de validacion (destinatario
  `QA Confirmacion P37`, contenido `Paquete QA validaciones`).
- **38** — creada y confirmada durante la verificacion de la interfaz del
  operador/admin (destinatario `QA UI Pendiente`).

### 3.2 Datos ambiguos o demo (NO separar sin decision del negocio) — 6 registros

- **21, 30, 32, 34** — sin cliente remitente asociado (posible carga
  interna/demostracion). Su origen no puede atribuirse con certeza a QA.
- **22, 24** — remitente `percy7096`. El usuario `percy7096` es una cuenta de
  auditoria (`Percy Audit`), pero el contenido (`Juana Perez`) no lleva marca QA
  explicita.

Estos 6 registros se dejan intactos y se recomienda que INTIWATANA determine si
corresponden a operacion real o a demostracion antes de tocarlos.

## 4. Tratamiento recomendado

1. **No eliminar** registros: la Poliza de este cierre prohibe borrado masivo.
2. Separar los 12 QA claros mediante una marca/archivado reversible
   (columna de bandera o tablas de archivo), nunca con borrado fisico en caliente.
3. Ejecutar la propuesta `reports/sql/LIMPIEZA-QA-ENCOMIENDAS-PROPUESTA.sql`
   unicamente con respaldo previo y autorizacion expresa de INTIWATANA.
4. Una vez marcados, una base productiva puede filtrar por la bandera y exportar
   solo informacion limpia. El tracking publico no se ve afectado.

## 5. Verificacion

- Consultas ejecutadas en modo lectura (`SELECT`), sin cambios persistentes.
- No se creo ni elimino ningun registro fuera de las altas de prueba ids 36, 37
  y 38, que se generaron para validar el nuevo flujo de confirmacion de recepcion
  y quedan declaradas aqui como QA.
- No se ejecuto el script de limpieza propuesto.
