# RESULTADO BLOQUES C – D (CHOFER Y ENCOMIENDAS)
Proyecto: intiwatana_final_v2
Fecha ejecución: 2026-10-02
Ambiente: Frontend http://localhost:5173/, Backend http://localhost:8080 (actuator/health OK), PostgreSQL transporte_bd (localhost, usuario postgres, clave <REDACTADO>), JDK 17, Maven 3.9.12

## 1. C – BLOQUE CHOFER
Baseline: BLOQUES-A-B-RESULTADO.md y LOGIN-INTIWATANA.md cerrados.

### C01 – Login chofer1 (clave <REDACTADO>) → /chofer, rol ROLE_CHOFER
- UI: login correcto, redirección a /chofer, JWT en sesión.
- Verificado: sesión activa, rol ROLE_CHOFER.

### C02 – Listado de 5 viajes de chofer1 (31,4,7,1,10)
- API `/api/v1/viajes/mis-viajes` (chofer1): listado coincide con UI/DB. IDs observados: 31, 4, 7, 1, 10.

### C03 – Mapa de asientos viaje 1 visible; asientos VENDIDO con pasajero Prueba INTIWATANA-A-001
- `/api/v1/viajes/1/asientos`: mapa visible; asientos con estado VENDIDO identificados.

### C04 – PROGRAMADO → EN_CURSO (viaje 1)
- Cambio UI, persistencia tras recarga OK. 200.

### C05 – Transiciones inválidas rechazadas
- EN_CURSO → EN_CURSO: 400
- FINALIZADO → EN_CURSO: 400
- PROGRAMADO → FINALIZADO: 400
- Cliente: 403
- Sin token: 401

### C06 – Viaje 4: PROGRAMADO → EN_CURSO → FINALIZADO (UI)
- OK, contadores y persistencia tras recarga.

### C07 – Casos adicionales
- PROGRAMADO → FINALIZADO: 400
- FINALIZADO → FINALIZADO: 400 "Transición de estado inválida: FINALIZADO → FINALIZADO"
- Chofer sobre viaje ajeno: 400 "El viaje no está asignado a tu usuario", sin cambios
- Chofer sobre viaje propio: 200
- Admin sobre viaje de chofer: 200

### C08 – Estadísticas chofer1
- 1 activo, 0 en curso, 4 completados (API/UI coherentes). Admin refleja PROGRAMADO y FINALIZADO.

### Corrección ownership (backend)
- `ViajeService.cambiarEstado` recibe `usuarioId`; `ViajeController` obtiene desde `Authentication`; `ViajeServiceImpl.validarResponsable` permite `ROLE_ADMIN` y exige asignación al chofer.

Pruebas:
- chofer1 → viaje 8 (ajeno): 400
- chofer1 → viaje 5 (ajeno): 400
- chofer2 → viaje 5 (propio): 200
- admin → viaje 2 (chofer2): 200

Estado viajes tras C: FINALIZADO 1,2,3,4,5,7,9,10; PROGRAMADO 6,8,31.

## 2. D – ENCOMIENDAS
### D01–D02
- Cliente crea encomiendas 26–29. Guía más reciente `GUI-20261001-000029`, estado inicial RECIBIDO. Verificada en panel admin.

### D03–D04
- `RECIBIDO → EN_ALMACEN` OK. Intento inválido desde `EN_TRANSITO` → `RECIBIDO`: 400, estado permanece `EN_TRANSITO` (modal queda abierto con toast de error — UX menor).

### D05–D07
`GUI-20261001-000029` (ID 29): `RECIBIDO`(21)→`EN_ALMACEN`(22)→`CARGADO_EN_BUS`(23)→`EN_TRANSITO`(24)→`LLEGADO_A_DESTINO`(25)→`ENTREGADO`(26). Responsable admin. Estado final `ENTREGADO`, 6 movimientos. Persistencia tras recarga OK.

### D08
- Tracking público sin token: `/api/v1/encomiendas/tracking/GUI-20261001-000029` → 200 (estado ENTREGADO, 6 movimientos)
- Inexistente: `/api/v1/encomiendas/tracking/GUI-00000000-000000` → 404

## 3. REGRESIÓN A+B
### R1
- `/api/v1/viajes/disponibles?origenId=1&destinoId=2&page=0&size=10` → 200 (viaje 31 PROGRAMADO libres 40/40, precio 15.00). Formulario visible.

### R2
- Compra cliente pruebaa001: boletos BOL-20261002-000032 (1A), 000033 (1B), 000035 (1D), 000036 (1C) — pagos EFECTIVO 22,23,25,26 COMPLETADOS. Asientos 1A–1D VENDIDO.

### R3–R4
- Paneles admin (10 secciones) cargan OK. Chofer1: 5 viajes API, contadores 1 programado, 0 en curso, 4 finalizados.

## 4. SIDE EFFECTS
- Pruebas C ajustaron estados viajes 1–5,7; ownership afectó 2,3,5.
- D01 creó 26–29 (29 completado; 26–28 parciales).
- Regresión generó boletos 32,33,35,36 y pagos sobre viaje 31.

## 5. CONCLUSIÓN
C y D completados con evidencia real UI/API/DB. Brecha ownership corregida y verificada. Regresión A+B satisfactoria. E–F fuera de alcance.
