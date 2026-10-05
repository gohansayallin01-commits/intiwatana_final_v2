package com.transporte.sistema.service;

import com.transporte.sistema.dto.response.*;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Reportes gerenciales del sistema.
 *
 * Todas las cifras provienen de consultas agregadas sobre PostgreSQL en el
 * momento de la petición: no hay datos de ejemplo ni valores calculados en el
 * frontend.
 *
 * Criterio de fechas (consistente en todos los reportes):
 *  - Ingresos: por la fecha del pago, y sólo pagos en estado COMPLETADO.
 *  - Viajes y boletos: por la fecha de SALIDA del viaje, excluyendo cancelados.
 *  - Encomiendas: por su fecha de registro.
 *
 * Si desde o hasta llegan nulos, el reporte cubre los últimos 30 días.
 */
public interface ReporteService {

    ReporteResumenResponse resumen(LocalDateTime desde, LocalDateTime hasta);

    ReporteIngresosResponse ingresos(LocalDateTime desde, LocalDateTime hasta);

    List<ReporteIngresosDiarioResponse> ingresosSerieDiaria(LocalDateTime desde, LocalDateTime hasta);

    ReportePasajesResponse pasajes(LocalDateTime desde, LocalDateTime hasta);

    List<ReporteOcupacionViajeResponse> ocupacion(LocalDateTime desde, LocalDateTime hasta);

    ReporteEncomiendasResponse encomiendas(LocalDateTime desde, LocalDateTime hasta);

    List<ReporteSucursalResponse> rankingSucursales(LocalDateTime desde, LocalDateTime hasta);

    // Descargas CSV
    ResponseEntity<byte[]> ingresosCsv(LocalDateTime desde, LocalDateTime hasta);

    ResponseEntity<byte[]> ingresosSerieDiariaCsv(LocalDateTime desde, LocalDateTime hasta);

    ResponseEntity<byte[]> pasajesCsv(LocalDateTime desde, LocalDateTime hasta);

    ResponseEntity<byte[]> ocupacionCsv(LocalDateTime desde, LocalDateTime hasta);

    ResponseEntity<byte[]> encomiendasPorEstadoCsv(LocalDateTime desde, LocalDateTime hasta);

    ResponseEntity<byte[]> encomiendasPorRutaCsv(LocalDateTime desde, LocalDateTime hasta);

    ResponseEntity<byte[]> rankingSucursalesCsv(LocalDateTime desde, LocalDateTime hasta);
}
