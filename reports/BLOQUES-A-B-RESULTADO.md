# INFORME DE CIERRE — BLOQUES A y B (INTIWATANA)

**Fecha:** 2026-09-30
**Raíz:** C:\Users\USER\Downloads\intiwatana_final_v2
**Verificación:** API HTTP real + PostgreSQL + UI Vite

## 1. Estado general
- BLOQUE A: CERRADO
- BLOQUE B: CERRADO

## 2. Matriz P01–P14
| Prueba | Res | Evidencia | Observación |
|---|---|---|---|
| P01 Consultar viajes | ✅ | GET /api/v1/viajes/disponibles (3 viajes) | Datos BD reales |
| P02 Seleccionar viaje | ✅ | Navegación a mapa de asientos | Contexto conservado |
| P03 Seleccionar asiento | ✅ | 40 asientos; bloqueo y concurrencia OK (1×201,1×400) | PESSIMISTIC_WRITE |
| P04 Registrar pasajero | ✅ | Registro + login (cliente 24, pruebaa001) | OK |
| P05 Realizar pago | ✅ | Comprar boleto → pago COMPLETADO (YAPE) | Asociado correctamente |
| P06 Confirmar boleto | ✅ | BOL-20260930-000031, QR, asiento VENDIDO | Persistido |
| P07 Ver viaje generado | ✅ | GET /api/v1/viajes refleja operación | Coherente |
| P08 Ver cliente generado | ✅ | GET /api/v1/clientes?q=INTIWATANA → id 24 | OK |
| P09 Ver pago generado | ✅ | GET /api/v1/pagos incluye pagos de prueba | OK |
| P10 Administrar rutas | ✅ | CRUD rutas completo (fix PUT con recarga grafo) | OK |
| P11 Administrar autobuses | ✅ | CRUD buses completo | OK |
| P12 Administrar viajes | ✅ | POST viaje 31 creado | OK |
| P13 Administrar encomiendas | ✅ | GUI-20260930-000025 creada/listada | OK |
| P14 Generar reportes | ✅ | /reportes/resumen y CSV con datos reales | OK |

## 3. Problemas encontrados
- Solo corrección menor: LazyInitializationException en PUT de rutas (solucionado con recarga con EntityGraph). Sin regresiones.

## 4. Cambios realizados
- RutaRepository: método findByIdWithGraph + consulta explícita
- RutaController: uso de findByIdWithGraph y recarga tras save() en PUT

## 5. Veredicto
**CERRADO** — A y B cumplen P01–P14 con evidencia funcional y persistencia real.