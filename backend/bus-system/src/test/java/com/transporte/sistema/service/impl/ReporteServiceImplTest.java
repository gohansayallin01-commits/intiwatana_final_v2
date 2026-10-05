package com.transporte.sistema.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transporte.sistema.dto.response.ReporteEncomiendasResponse;
import com.transporte.sistema.dto.response.ReporteIngresosDiarioResponse;
import com.transporte.sistema.dto.response.ReporteIngresosResponse;
import com.transporte.sistema.dto.response.ReporteOcupacionViajeResponse;
import com.transporte.sistema.dto.response.ReportePasajesResponse;
import com.transporte.sistema.dto.response.ReporteResumenResponse;
import com.transporte.sistema.dto.response.ReporteSucursalResponse;
import com.transporte.sistema.exception.NegocioException;
import com.transporte.sistema.projection.ReporteEncomiendaEstadoProjection;
import com.transporte.sistema.projection.ReporteEncomiendaRutaProjection;
import com.transporte.sistema.projection.ReporteIngresoMetodoProjection;
import com.transporte.sistema.projection.ReporteOcupacionProjection;
import com.transporte.sistema.projection.ReportePasajesRutaProjection;
import com.transporte.sistema.projection.ReporteSucursalProjection;
import com.transporte.sistema.projection.ReporteTotalProjection;
import com.transporte.sistema.repository.BoletoRepository;
import com.transporte.sistema.repository.EncomiendaRepository;
import com.transporte.sistema.repository.PagoRepository;
import com.transporte.sistema.repository.ViajeRepository;
import com.transporte.sistema.util.CsvExportador;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * Pruebas de la lógica de agregación del módulo de reportes.
 *
 * Los repositorios se sustituyen por dobles para poder comprobar el cálculo
 * (totales, porcentajes,Ticket promedio, agrupamiento por día) sin depender de
 * una base de datos. Lo que NO se comprueba aquí es que las consultas JPQL
 * devolvan datos coherentes: eso exige PostgreSQL y está pendiente del respaldo.
 */
class ReporteServiceImplTest {

    private PagoRepository pagoRepository;
    private ViajeRepository viajeRepository;
    private EncomiendaRepository encomiendaRepository;
    private BoletoRepository boletoRepository;
    private ReporteServiceImpl servicio;

    private static final LocalDateTime DESDE = LocalDateTime.of(2026, 1, 1, 0, 0);
    private static final LocalDateTime HASTA = LocalDateTime.of(2026, 1, 31, 23, 59, 59);

    @BeforeEach
    void setUp() {
        pagoRepository = mock(PagoRepository.class);
        viajeRepository = mock(ViajeRepository.class);
        encomiendaRepository = mock(EncomiendaRepository.class);
        boletoRepository = mock(BoletoRepository.class);

        when(pagoRepository.ingresosTotales(any(), any())).thenReturn(total("1000.00", 10));
        when(pagoRepository.ingresosPasajes(any(), any())).thenReturn(total("700.00", 7));
        when(pagoRepository.ingresosEncomiendas(any(), any())).thenReturn(total("300.00", 3));
        when(pagoRepository.ingresosPorMetodo(any(), any())).thenReturn(List.of());
        when(pagoRepository.ingresosSerieDiaria(any(), any())).thenReturn(List.of());

        when(viajeRepository.viajesDelPeriodo(any(), any())).thenReturn(total("0", 4));
        when(viajeRepository.ventasPorRuta(any(), any())).thenReturn(List.of());
        when(viajeRepository.ocupacionPorViaje(any(), any())).thenReturn(List.of());
        when(viajeRepository.actividadPorSucursal(any(), any())).thenReturn(List.of());

        when(encomiendaRepository.encomiendasDelPeriodo(any(), any())).thenReturn(total("0", 2));
        when(encomiendaRepository.encomiendasPorEstado(any(), any())).thenReturn(List.of());
        when(encomiendaRepository.encomiendasPorRuta(any(), any())).thenReturn(List.of());

        servicio = new ReporteServiceImpl(pagoRepository, viajeRepository,
                encomiendaRepository, boletoRepository, new CsvExportador(new ObjectMapper()));
    }

    // ── Utilidades para construir las proyecciones ───────────────────────────

    private static ReporteTotalProjection total(String total, long cantidad) {
        return new Total(new BigDecimal(total), cantidad);
    }

    /** Proyección con total y cantidad nulos, como la devuelve un LEFT JOIN sin coincidencias. */
    private static ReporteTotalProjection sinTotal() {
        return new Total(null, null);
    }

    /**
     * Compara importes por valor y no por escala: BigDecimal.equals distingue
     * 15.0 de 15.00, lo que haría fallar la prueba por un detalle sin
     * importancia. Además exige que el resultado no sea null.
     */
    private static void assertMonto(String esperado, BigDecimal actual, String concepto) {
        assertNotNull(actual, "El importe de '" + concepto + "' no debe ser null");
        assertEquals(0, new BigDecimal(esperado).compareTo(actual),
                "El importe de '" + concepto + "' era " + actual + " y se esperaba " + esperado);
    }

    private static class Total implements ReporteTotalProjection {
        private final BigDecimal total;
        private final Long cantidad;
        Total(BigDecimal total, Long cantidad) { this.total = total; this.cantidad = cantidad; }
        public BigDecimal getTotal() { return total; }
        public Long getCantidad() { return cantidad; }
    }
    private record Ocup(Long id, String placa, Integer capacidad, Long total,
                        Long ocupados, BigDecimal ingresos, String estado)
            implements ReporteOcupacionProjection {
        public Long getViajeId() { return id; }
        public String getPlaca() { return placa; }
        public Integer getCapacidad() { return capacidad; }
        public Long getTotalAsientos() { return total; }
        public Long getOcupados() { return ocupados; }
        public BigDecimal getIngresos() { return ingresos; }
        public String getEstado() { return estado; }
    }

    private record Ruta(String origen, String destino, Long viajes, Long boletos, BigDecimal ingresos)
            implements ReportePasajesRutaProjection {
        public String getOrigen() { return origen; }
        public String getDestino() { return destino; }
        public Long getViajes() { return viajes; }
        public Long getBoletos() { return boletos; }
        public BigDecimal getIngresos() { return ingresos; }
    }

    private record Metodo(String metodo, Long cantidad, BigDecimal total)
            implements ReporteIngresoMetodoProjection {
        public String getMetodo() { return metodo; }
        public Long getCantidad() { return cantidad; }
        public BigDecimal getTotal() { return total; }
    }

    private record Sucursal(Long id, String nombre, String ciudad, Boolean terminal,
                            Long viajesOrigen, Long viajesDestino, Long boletos,
                            BigDecimal ingresosPasajes, Long encomiendas, BigDecimal ingresosEncomiendas)
            implements ReporteSucursalProjection {
        public Long getSucursalId() { return id; }
        public String getSucursal() { return nombre; }
        public String getCiudad() { return ciudad; }
        public Boolean getEsTerminal() { return terminal; }
        public Long getViajesOrigen() { return viajesOrigen; }
        public Long getViajesDestino() { return viajesDestino; }
        public Long getBoletosVendidos() { return boletos; }
        public BigDecimal getIngresosPasajes() { return ingresosPasajes; }
        public Long getEncomiendasRegistradas() { return encomiendas; }
        public BigDecimal getIngresosEncomiendas() { return ingresosEncomiendas; }
    }

    private record EstadoEnc(String estado, Long cantidad, BigDecimal ingresos)
            implements ReporteEncomiendaEstadoProjection {
        public String getEstado() { return estado; }
        public Long getCantidad() { return cantidad; }
        public BigDecimal getIngresos() { return ingresos; }
    }

    private record Corredor(String origen, String destino, Long cantidad, BigDecimal ingresos, BigDecimal peso)
            implements ReporteEncomiendaRutaProjection {
        public String getOrigen() { return origen; }
        public String getDestino() { return destino; }
        public Long getCantidad() { return cantidad; }
        public BigDecimal getIngresos() { return ingresos; }
        public BigDecimal getPesoKg() { return peso; }
    }

    // ── Período ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Período por defecto")
    class PeriodoPorDefecto {

        @Test
        @DisplayName("Sin fechas covers los últimos 30 días")
        void usaTreintaDias() {
            servicio.ingresos(null, null);

            ArgumentCaptor<LocalDateTime> desde = ArgumentCaptor.forClass(LocalDateTime.class);
            ArgumentCaptor<LocalDateTime> hasta = ArgumentCaptor.forClass(LocalDateTime.class);
            verify(pagoRepository).ingresosTotales(desde.capture(), hasta.capture());

            long dias = java.time.Duration.between(desde.getValue(), hasta.getValue()).toDays();
            assertEquals(30, dias, "El período por defecto debe abarcar 30 días");
        }

        @Test
        @DisplayName("Si solo viene 'desde', 'hasta' es el momento actual")
        void completaHastaConAhora() {
            servicio.ingresos(DESDE, null);

            ArgumentCaptor<LocalDateTime> hasta = ArgumentCaptor.forClass(LocalDateTime.class);
            verify(pagoRepository).ingresosTotales(any(), hasta.capture());

            assertTrue(hasta.getValue().isAfter(LocalDateTime.now().minusMinutes(2)),
                    "hasta debería ser aproximadamente ahora");
        }

        @Test
        @DisplayName("Rechaza un rango invertido en lugar de devolver cifras sin sentido")
        void rechazaRangoInvertido() {
            NegocioException error = assertThrows(NegocioException.class,
                    () -> servicio.ingresos(HASTA, DESDE));

            assertTrue(error.getMessage().toLowerCase().contains("posterior"),
                    "El mensaje debe explicar el problema. Fue: " + error.getMessage());
            verify(pagoRepository, never()).ingresosTotales(any(), any());
        }

        @Test
        @DisplayName("Propaga las fechas recibidas sin alterarlas")
        void propagaFechas() {
            servicio.ingresos(DESDE, HASTA);

            verify(pagoRepository).ingresosTotales(DESDE, HASTA);
        }
    }

    // ── Ingresos ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Reporte de ingresos")
    class Ingresos {

        @Test
        @DisplayName("Separa pasajes, encomiendas y total")
        void separaOrigenes() {
            ReporteIngresosResponse r = servicio.ingresos(DESDE, HASTA);

            assertMonto("1000.00", r.getTotal(), "ingresos totales");
            assertMonto("700.00", r.getTotalPasajes(), "ingresos de pasajes");
            assertMonto("300.00", r.getTotalEncomiendas(), "ingresos de encomiendas");
            assertEquals(10L, r.getTransacciones());
        }

        @Test
        @DisplayName("Conserva el desglose por método de pago")
        void conservaDesglosePorMetodo() {
            when(pagoRepository.ingresosPorMetodo(any(), any())).thenReturn(List.of(
                    new Metodo("EFECTIVO", 6L, new BigDecimal("600.00")),
                    new Metodo("YAPE", 4L, new BigDecimal("400.00"))));

            List<ReporteIngresosResponse.IngresoPorMetodo> metodos = servicio.ingresos(DESDE, HASTA).getPorMetodo();

            assertEquals(2, metodos.size());
            assertEquals("EFECTIVO", metodos.get(0).getMetodo());
            assertMonto("600.00", metodos.get(0).getTotal(), "total por método");
            assertEquals(6L, metodos.get(0).getCantidad());
        }

        @Test
        @DisplayName("Un período sin pagos devuelve cero, no null")
        void sinPagosDevuelveCero() {
            when(pagoRepository.ingresosTotales(any(), any())).thenReturn(sinTotal());
            when(pagoRepository.ingresosPasajes(any(), any())).thenReturn(sinTotal());
            when(pagoRepository.ingresosEncomiendas(any(), any())).thenReturn(sinTotal());

            ReporteIngresosResponse r = servicio.ingresos(DESDE, HASTA);

            assertMonto("0", r.getTotal(), "total sin pagos");
            assertEquals(0L, r.getTransacciones());
        }
    }

    // ── Serie diaria ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Serie diaria de ingresos")
    class SerieDiaria {

        @Test
        @DisplayName("Agrupa por día y separa pasajes de encomiendas")
        void agrupaPorDia() {
            when(pagoRepository.ingresosSerieDiaria(any(), any())).thenReturn(List.<Object[]>of(
                    new Object[]{LocalDateTime.of(2026, 1, 5, 8, 0), new BigDecimal("100.00"), true},
                    new Object[]{LocalDateTime.of(2026, 1, 5, 18, 0), new BigDecimal("50.00"), false},
                    new Object[]{LocalDateTime.of(2026, 1, 6, 9, 0), new BigDecimal("25.00"), true}));

            List<ReporteIngresosDiarioResponse> serie = servicio.ingresosSerieDiaria(DESDE, HASTA);

            assertEquals(2, serie.size(), "Los dos pagos del 05/01 deben colapsar en un día");

            ReporteIngresosDiarioResponse dia5 = serie.get(0);
            assertEquals("2026-01-05", dia5.getFecha());
            assertEquals(2L, dia5.getTransacciones());
            assertMonto("100.00", dia5.getIngresosPasajes(), "pasajes del 05/01");
            assertMonto("50.00", dia5.getIngresosEncomiendas(), "encomiendas del 05/01");
            assertMonto("150.00", dia5.getTotal(), "total del 05/01");

            ReporteIngresosDiarioResponse dia6 = serie.get(1);
            assertEquals("2026-01-06", dia6.getFecha());
            assertEquals(1L, dia6.getTransacciones());
        }

        @Test
        @DisplayName("Un día con cero en una categoría no anula el total")
        void diaSinUnaCategoria() {
            when(pagoRepository.ingresosSerieDiaria(any(), any())).thenReturn(List.<Object[]>of(
                    new Object[]{LocalDateTime.of(2026, 1, 7, 10, 0), new BigDecimal("80.00"), true}));

            List<ReporteIngresosDiarioResponse> serie = servicio.ingresosSerieDiaria(DESDE, HASTA);

            assertEquals(1, serie.size());
            assertMonto("0", serie.get(0).getIngresosEncomiendas(), "encomiendas del 07/01");
            assertMonto("80.00", serie.get(0).getTotal(), "total del 07/01");
        }

        @Test
        @DisplayName("Devuelve una lista vacía si no hay pagos")
        void sinPagosDevuelveListaVacia() {
            assertTrue(servicio.ingresosSerieDiaria(DESDE, HASTA).isEmpty());
        }

        @Test
        @DisplayName("Ordena los días cronológicamente")
        void ordenaCronologicamente() {
            when(pagoRepository.ingresosSerieDiaria(any(), any())).thenReturn(List.<Object[]>of(
                    new Object[]{LocalDateTime.of(2026, 1, 20, 10, 0), new BigDecimal("10.00"), true},
                    new Object[]{LocalDateTime.of(2026, 1, 3, 10, 0), new BigDecimal("10.00"), true},
                    new Object[]{LocalDateTime.of(2026, 1, 11, 10, 0), new BigDecimal("10.00"), true}));

            List<ReporteIngresosDiarioResponse> serie = servicio.ingresosSerieDiaria(DESDE, HASTA);

            assertEquals("2026-01-03", serie.get(0).getFecha());
            assertEquals("2026-01-11", serie.get(1).getFecha());
            assertEquals("2026-01-20", serie.get(2).getFecha());
        }
    }

    // ── Pasajes ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Reporte de pasajes")
    class Pasajes {

        @Test
        @DisplayName("Suma boletos e ingresos de todas las rutas")
        void sumaTodasLasRutas() {
            when(viajeRepository.ventasPorRuta(any(), any())).thenReturn(List.of(
                    new Ruta("Ayacucho", "Lima", 5L, 30L, new BigDecimal("1500.00")),
                    new Ruta("Lima", "Cusco", 3L, 12L, new BigDecimal("720.00"))));

            ReportePasajesResponse r = servicio.pasajes(DESDE, HASTA);

            assertEquals(42L, r.getBoletosVendidos());
            assertMonto("2220.00", r.getIngresos(), "ingresos de pasajes");
            assertEquals(2, r.getPorRuta().size());
        }

        @Test
        @DisplayName("Calcula el ticket promedio con dos decimales")
        void calculaTicketPromedio() {
            when(viajeRepository.ventasPorRuta(any(), any())).thenReturn(List.of(
                    new Ruta("A", "B", 1L, 3L, new BigDecimal("100.00"))));

            ReportePasajesResponse r = servicio.pasajes(DESDE, HASTA);

            assertMonto("33.33", r.getPrecioPromedio(), "precio promedio del pasaje");
        }

        @Test
        @DisplayName("Sin boletos no divide por cero")
        void sinBoletosNoDividePorCero() {
            ReportePasajesResponse r = servicio.pasajes(DESDE, HASTA);

            assertMonto("0", r.getPrecioPromedio(), "precio promedio sin boletos");
            assertEquals(0L, r.getBoletosVendidos());
        }
    }

    // ── Ocupación ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Ocupación de asientos")
    class Ocupacion {

        @Test
        @DisplayName("Calcula el porcentaje de ocupación con dos decimales")
        void calculaPorcentaje() {
            when(viajeRepository.ocupacionPorViaje(any(), any())).thenReturn(List.of(
                    new Ocup(1L, "ABC-123", 40, 40L, 30L, new BigDecimal("1500.00"), "PROGRAMADO")));

            List<ReporteOcupacionViajeResponse> r = servicio.ocupacion(DESDE, HASTA);

            assertEquals(1, r.size());
            assertMonto("75.00", r.get(0).getOcupacionPorcentaje(), "ocupación");
            assertEquals(10L, r.get(0).getDisponibles());
            assertEquals("ABC-123", r.get(0).getPlaca());
        }

        @Test
        @DisplayName("Un viaje sin asientos no produce división por cero")
        void viajeSinAsientos() {
            when(viajeRepository.ocupacionPorViaje(any(), any())).thenReturn(List.of(
                    new Ocup(2L, "XYZ-999", 0, 0L, 0L, BigDecimal.ZERO, "PROGRAMADO")));

            List<ReporteOcupacionViajeResponse> r = servicio.ocupacion(DESDE, HASTA);

            assertMonto("0", r.get(0).getOcupacionPorcentaje(), "ocupación sin asientos");
            assertEquals(0L, r.get(0).getDisponibles());
        }

        @Test
        @DisplayName("Los disponibles nunca salen negativos si hay sobreventa")
        void disponiblesNoNegativos() {
            when(viajeRepository.ocupacionPorViaje(any(), any())).thenReturn(List.of(
                    new Ocup(3L, "OVER-01", 4, 4L, 5L, BigDecimal.ZERO, "PROGRAMADO")));

            List<ReporteOcupacionViajeResponse> r = servicio.ocupacion(DESDE, HASTA);

            assertEquals(0L, r.get(0).getDisponibles(), "No puede haber asientos libres negativos");
            assertMonto("125.00", r.get(0).getOcupacionPorcentaje(), "ocupación con sobreventa");
        }
    }

    // ── Encomiendas ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Reporte de encomiendas")
    class Encomiendas {

        @Test
        @DisplayName("Suma el peso de todos los corredores")
        void sumaPeso() {
            when(encomiendaRepository.encomiendasDelPeriodo(any(), any())).thenReturn(total("120.00", 3));
            when(encomiendaRepository.encomiendasPorRuta(any(), any())).thenReturn(List.of(
                    new Corredor("Ayacucho", "Lima", 2L, new BigDecimal("80.00"), new BigDecimal("10.5")),
                    new Corredor("Lima", "Huamanga", 1L, new BigDecimal("40.00"), new BigDecimal("4.5"))));

            ReporteEncomiendasResponse r = servicio.encomiendas(DESDE, HASTA);

            assertMonto("15", r.getPesoTotalKg(), "peso total");
            assertMonto("120.00", r.getIngresos(), "ingresos de encomiendas");
            assertMonto("40.00", r.getTicketPromedio(), "ticket promedio");
        }

        @Test
        @DisplayName("Conserva el desglose por estado")
        void conservaDesglosePorEstado() {
            when(encomiendaRepository.encomiendasPorEstado(any(), any())).thenReturn(List.of(
                    new EstadoEnc("ENTREGADO", 5L, new BigDecimal("100.00")),
                    new EstadoEnc("EN_TRANSITO", 2L, new BigDecimal("60.00"))));

            ReporteEncomiendasResponse r = servicio.encomiendas(DESDE, HASTA);

            assertEquals(2, r.getPorEstado().size());
            assertEquals("ENTREGADO", r.getPorEstado().get(0).getEstado());
            assertEquals(5L, r.getPorEstado().get(0).getCantidad());
        }

        @Test
        @DisplayName("Sin encomiendas devuelve ticket promedio cero")
        void sinEncomiendas() {
            when(encomiendaRepository.encomiendasDelPeriodo(any(), any())).thenReturn(sinTotal());

            ReporteEncomiendasResponse r = servicio.encomiendas(DESDE, HASTA);

            assertMonto("0", r.getTicketPromedio(), "ticket promedio sin encomiendas");
            assertEquals(0L, r.getTotal());
        }
    }

    // ── Sucursales ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Ranking de sucursales")
    class Ranking {

        @Test
        @DisplayName("Suma pasajes y encomiendas y ordena de mayor a menor")
        void sumaYOrdena() {
            when(viajeRepository.actividadPorSucursal(any(), any())).thenReturn(List.of(
                    new Sucursal(1L, "Ayacucho", "Ayacucho", true, 5L, 3L, 40L,
                            new BigDecimal("2000.00"), 10L, new BigDecimal("500.00")),
                    new Sucursal(2L, "Lima", "Lima", true, 2L, 5L, 20L,
                            new BigDecimal("1000.00"), 4L, new BigDecimal("250.00"))));

            List<ReporteSucursalResponse> r = servicio.rankingSucursales(DESDE, HASTA);

            assertEquals(2, r.size());
            assertEquals("Ayacucho", r.get(0).getSucursal(), "La de mayores ingresos va primero");
            assertMonto("2500.00", r.get(0).getIngresosTotales(), "ingresos de Ayacucho");
            assertMonto("1250.00", r.get(1).getIngresosTotales(), "ingresos de Lima");
        }

        @Test
        @DisplayName("Un valor nulo en ingresos no rompe el cálculo")
        void nulosNoRompen() {
            when(viajeRepository.actividadPorSucursal(any(), any())).thenReturn(List.of(
                    new Sucursal(3L, "Huanta", "Huanta", false, 0L, 0L, 0L, null, 0L, null)));

            List<ReporteSucursalResponse> r = servicio.rankingSucursales(DESDE, HASTA);

            assertMonto("0", r.get(0).getIngresosTotales(), "ingresos nulos");
        }
    }

    // ── Resumen ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Resumen del período")
    class Resumen {

        @Test
        @DisplayName("Calcula la ocupación promedio entre los viajes")
        void calculaOcupacionPromedio() {
            when(viajeRepository.ocupacionPorViaje(any(), any())).thenReturn(List.of(
                    new Ocup(1L, "A", 10, 10L, 10L, BigDecimal.ZERO, "PROGRAMADO"),
                    new Ocup(2L, "B", 10, 10L, 5L, BigDecimal.ZERO, "PROGRAMADO")));
            when(boletoRepository.boletosVendidosDelPeriodo(any(), any())).thenReturn(15L);

            ReporteResumenResponse r = servicio.resumen(DESDE, HASTA);

            assertMonto("75.00", r.getOcupacionPromedio(), "ocupación promedio del resumen");
            assertEquals(15L, r.getBoletosVendidos());
        }

        @Test
        @DisplayName("Sin viajes la ocupación promedio es cero, no NaN")
        void sinViajesOcupacionCero() {
            ReporteResumenResponse r = servicio.resumen(DESDE, HASTA);

            assertMonto("0", r.getOcupacionPromedio(), "ocupación promedio sin viajes");
            assertEquals(0L, r.getBoletosVendidos());
        }

        @Test
        @DisplayName("Calcula el precio promedio del pasaje")
        void calculaPrecioPromedioPasaje() {
            ReporteResumenResponse r = servicio.resumen(DESDE, HASTA);

            assertMonto("100.00", r.getPrecioPromedioPasaje(), "precio promedio del resumen");
            assertMonto("1000.00", r.getIngresosTotales(), "ingresos totales del resumen");
        }
    }

    // ── CSV ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Descargas CSV")
    class Descargas {

        @Test
        @DisplayName("Cada endpoint devuelve un archivo CSV válido")
        void descargaValida() {
            assertNotNull(servicio.ingresosCsv(DESDE, HASTA).getBody());
            assertNotNull(servicio.ingresosSerieDiariaCsv(DESDE, HASTA).getBody());
            assertNotNull(servicio.pasajesCsv(DESDE, HASTA).getBody());
            assertNotNull(servicio.ocupacionCsv(DESDE, HASTA).getBody());
            assertNotNull(servicio.encomiendasPorEstadoCsv(DESDE, HASTA).getBody());
            assertNotNull(servicio.encomiendasPorRutaCsv(DESDE, HASTA).getBody());
            assertNotNull(servicio.rankingSucursalesCsv(DESDE, HASTA).getBody());
        }

        @Test
        @DisplayName("El archivo de ingresos incluye las cabeceras de los desgloses")
        void cabeceraDeIngresos() {
            when(pagoRepository.ingresosPorMetodo(any(), any())).thenReturn(List.of(
                    new Metodo("EFECTIVO", 6L, new BigDecimal("600.00"))));

            String csv = new String(servicio.ingresosCsv(DESDE, HASTA).getBody(),
                    java.nio.charset.StandardCharsets.UTF_8);

            assertTrue(csv.contains("Metodo"), "Debe incluir la columna Metodo. Fue: " + csv);
            assertTrue(csv.contains("Cantidad"));
            assertTrue(csv.contains("Total"));
            assertTrue(csv.contains("EFECTIVO"), "Debe incluir los datos, no solo la cabecera");
        }

        @Test
        @DisplayName("El nombre del archivo incluye el nombre del reporte")
        void nombreDeArchivo() {
            String disposicion = servicio.ocupacionCsv(DESDE, HASTA)
                    .getHeaders().getFirst("Content-Disposition");

            assertTrue(disposicion.contains("ocupacion-asientos"), "Fue: " + disposicion);
            assertTrue(disposicion.endsWith(".csv\""));
        }
    }
}
