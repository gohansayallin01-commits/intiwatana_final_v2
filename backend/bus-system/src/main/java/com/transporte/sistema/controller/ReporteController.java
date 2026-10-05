package com.transporte.sistema.controller;

import com.transporte.sistema.dto.response.*;
import com.transporte.sistema.service.ReporteService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Reportes gerenciales. Acceso exclusivo de ADMIN.
 *
 * Las fechas se reciben en formato ISO: 2026-01-01T00:00:00. Si se omiten,
 * el reporte cubre los últimos 30 días.
 */
@RestController
@RequestMapping("/api/v1/reportes")
@RequiredArgsConstructor
public class ReporteController {

    private final ReporteService reporteService;

    @GetMapping("/resumen")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReporteResumenResponse> resumen(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta) {
        return ResponseEntity.ok(reporteService.resumen(desde, hasta));
    }

    @GetMapping("/ingresos")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReporteIngresosResponse> ingresos(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta) {
        return ResponseEntity.ok(reporteService.ingresos(desde, hasta));
    }

    @GetMapping("/ingresos/serie-diaria")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ReporteIngresosDiarioResponse>> ingresosSerieDiaria(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta) {
        return ResponseEntity.ok(reporteService.ingresosSerieDiaria(desde, hasta));
    }

    @GetMapping("/pasajes")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReportePasajesResponse> pasajes(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta) {
        return ResponseEntity.ok(reporteService.pasajes(desde, hasta));
    }

    @GetMapping("/ocupacion")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ReporteOcupacionViajeResponse>> ocupacion(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta) {
        return ResponseEntity.ok(reporteService.ocupacion(desde, hasta));
    }

    @GetMapping("/encomiendas")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReporteEncomiendasResponse> encomiendas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta) {
        return ResponseEntity.ok(reporteService.encomiendas(desde, hasta));
    }

    @GetMapping("/sucursales")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ReporteSucursalResponse>> rankingSucursales(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta) {
        return ResponseEntity.ok(reporteService.rankingSucursales(desde, hasta));
    }

    // ── Descargas CSV ───────────────────────────────────────────────────────

    @GetMapping("/ingresos/csv")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> ingresosCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta) {
        return reporteService.ingresosCsv(desde, hasta);
    }

    @GetMapping("/ingresos/serie-diaria/csv")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> ingresosSerieDiariaCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta) {
        return reporteService.ingresosSerieDiariaCsv(desde, hasta);
    }

    @GetMapping("/pasajes/csv")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> pasajesCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta) {
        return reporteService.pasajesCsv(desde, hasta);
    }

    @GetMapping("/ocupacion/csv")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> ocupacionCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta) {
        return reporteService.ocupacionCsv(desde, hasta);
    }

    @GetMapping("/encomiendas/por-estado/csv")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> encomiendasPorEstadoCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta) {
        return reporteService.encomiendasPorEstadoCsv(desde, hasta);
    }

    @GetMapping("/encomiendas/por-ruta/csv")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> encomiendasPorRutaCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta) {
        return reporteService.encomiendasPorRutaCsv(desde, hasta);
    }

    @GetMapping("/sucursales/csv")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> rankingSucursalesCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta) {
        return reporteService.rankingSucursalesCsv(desde, hasta);
    }
}
