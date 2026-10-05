package com.transporte.sistema.service.impl;

import com.transporte.sistema.dto.response.*;
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
import com.transporte.sistema.service.ReporteService;
import com.transporte.sistema.util.CsvExportador;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReporteServiceImpl implements ReporteService {

    /** Si no se indica período, se reporta el último mes. */
    private static final int DIAS_POR_DEFECTO = 30;
    private static final DateTimeFormatter ETIQUETA_DIA = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter SUFIJO_ARCHIVO = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm");

    private final PagoRepository pagoRepository;
    private final ViajeRepository viajeRepository;
    private final EncomiendaRepository encomiendaRepository;
    private final BoletoRepository boletoRepository;
    private final CsvExportador csvExportador;

    // ── Helpers de período ──────────────────────────────────────────────────

    private record Periodo(LocalDateTime desde, LocalDateTime hasta) {
    }

    private Periodo periodo(LocalDateTime desde, LocalDateTime hasta) {
        LocalDateTime fin = (hasta == null) ? LocalDateTime.now() : hasta;
        LocalDateTime inicio = (desde == null) ? fin.minusDays(DIAS_POR_DEFECTO) : desde;
        if (inicio.isAfter(fin)) {
            throw new NegocioException(
                    "La fecha inicial no puede ser posterior a la fecha final");
        }
        return new Periodo(inicio, fin);
    }

    private BigDecimal total(ReporteTotalProjection p) {
        return p == null || p.getTotal() == null ? BigDecimal.ZERO : p.getTotal();
    }

    private long cantidad(ReporteTotalProjection p) {
        return p == null || p.getCantidad() == null ? 0L : p.getCantidad();
    }

    private BigDecimal ceroSiNulo(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }

    private BigDecimal promedio(BigDecimal total, long cantidad) {
        if (cantidad <= 0) return BigDecimal.ZERO;
        return total.divide(BigDecimal.valueOf(cantidad), 2, RoundingMode.HALF_UP);
    }

    private String nombreArchivo(String reporte) {
        return "intiwatana-" + reporte + "-" + LocalDateTime.now().format(SUFIJO_ARCHIVO);
    }

    // ── Consultas ───────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public ReporteResumenResponse resumen(LocalDateTime desde, LocalDateTime hasta) {
        Periodo p = periodo(desde, hasta);

        ReporteTotalProjection ingresosPasajes = pagoRepository.ingresosPasajes(p.desde(), p.hasta());
        ReporteTotalProjection ingresosEncomiendas = pagoRepository.ingresosEncomiendas(p.desde(), p.hasta());
        ReporteTotalProjection ingresosTotales = pagoRepository.ingresosTotales(p.desde(), p.hasta());
        ReporteTotalProjection encomiendas = encomiendaRepository.encomiendasDelPeriodo(p.desde(), p.hasta());

        List<ReporteOcupacionViajeResponse> ocupacion = ocupacion(p);

        BigDecimal sumaPorcentajes = BigDecimal.ZERO;
        for (ReporteOcupacionViajeResponse v : ocupacion) {
            sumaPorcentajes = sumaPorcentajes.add(ceroSiNulo(v.getOcupacionPorcentaje()));
        }
        BigDecimal ocupacionPromedio = ocupacion.isEmpty()
                ? BigDecimal.ZERO
                : sumaPorcentajes.divide(BigDecimal.valueOf(ocupacion.size()), 2, RoundingMode.HALF_UP);

        // Boletos vendidos por fecha de PAGO (mismo ancla que ingresosPasajes):
        // refleja la venta del día aunque el viaje sea futuro.
        long boletos = boletoRepository.boletosVendidosDelPeriodo(p.desde(), p.hasta());

        BigDecimal pesoTotal = BigDecimal.ZERO;
        for (ReporteEncomiendaRutaProjection r : encomiendaRepository.encomiendasPorRuta(p.desde(), p.hasta())) {
            pesoTotal = pesoTotal.add(ceroSiNulo(r.getPesoKg()));
        }

        return ReporteResumenResponse.builder()
                .desde(p.desde())
                .hasta(p.hasta())
                .generadoEn(LocalDateTime.now())
                .viajes(cantidad(viajeRepository.viajesDelPeriodo(p.desde(), p.hasta())))
                .boletosVendidos(boletos)
                .ingresosPasajes(total(ingresosPasajes))
                .precioPromedioPasaje(promedio(total(ingresosPasajes), cantidad(ingresosPasajes)))
                .ocupacionPromedio(ocupacionPromedio)
                .encomiendas(cantidad(encomiendas))
                .ingresosEncomiendas(total(ingresosEncomiendas))
                .pesoTotalKg(pesoTotal)
                .ingresosTotales(total(ingresosTotales))
                .transacciones(cantidad(ingresosTotales))
                .ingresosPorMetodo(porMetodo(p))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ReporteIngresosResponse ingresos(LocalDateTime desde, LocalDateTime hasta) {
        Periodo p = periodo(desde, hasta);
        ReporteTotalProjection totales = pagoRepository.ingresosTotales(p.desde(), p.hasta());
        return ReporteIngresosResponse.builder()
                .desde(p.desde())
                .hasta(p.hasta())
                .total(total(totales))
                .totalPasajes(total(pagoRepository.ingresosPasajes(p.desde(), p.hasta())))
                .totalEncomiendas(total(pagoRepository.ingresosEncomiendas(p.desde(), p.hasta())))
                .transacciones(cantidad(totales))
                .porMetodo(porMetodo(p))
                .build();
    }

    private List<ReporteIngresosResponse.IngresoPorMetodo> porMetodo(Periodo p) {
        return pagoRepository.ingresosPorMetodo(p.desde(), p.hasta()).stream()
                .map(m -> ReporteIngresosResponse.IngresoPorMetodo.builder()
                        .metodo(m.getMetodo())
                        .cantidad(m.getCantidad() == null ? 0L : m.getCantidad())
                        .total(ceroSiNulo(m.getTotal()))
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReporteIngresosDiarioResponse> ingresosSerieDiaria(LocalDateTime desde, LocalDateTime hasta) {
        Periodo p = periodo(desde, hasta);

        // Se agrupa en Java porque HQL no ofrece un truncamiento de fecha portable.
        Map<String, BigDecimal> porDiaPasajes = new LinkedHashMap<>();
        Map<String, BigDecimal> porDiaEncomiendas = new LinkedHashMap<>();
        Map<String, Long> porDiaTransacciones = new LinkedHashMap<>();

        for (Object[] fila : pagoRepository.ingresosSerieDiaria(p.desde(), p.hasta())) {
            LocalDateTime fechaPago = (LocalDateTime) fila[0];
            BigDecimal monto = ceroSiNulo((BigDecimal) fila[1]);
            boolean esPasaje = Boolean.TRUE.equals(fila[2]);

            String dia = fechaPago.toLocalDate().format(ETIQUETA_DIA);
            porDiaTransacciones.merge(dia, 1L, Long::sum);
            if (esPasaje) {
                porDiaPasajes.merge(dia, monto, BigDecimal::add);
            } else {
                porDiaEncomiendas.merge(dia, monto, BigDecimal::add);
            }
        }

        List<ReporteIngresosDiarioResponse> serie = new ArrayList<>();
        for (Map.Entry<String, Long> entry : new TreeMap<>(porDiaTransacciones).entrySet()) {
            BigDecimal pasajes = ceroSiNulo(porDiaPasajes.get(entry.getKey()));
            BigDecimal encomiendas = ceroSiNulo(porDiaEncomiendas.get(entry.getKey()));
            serie.add(ReporteIngresosDiarioResponse.builder()
                    .fecha(entry.getKey())
                    .transacciones(entry.getValue())
                    .ingresosPasajes(pasajes)
                    .ingresosEncomiendas(encomiendas)
                    .total(pasajes.add(encomiendas))
                    .build());
        }
        return serie;
    }

    @Override
    @Transactional(readOnly = true)
    public ReportePasajesResponse pasajes(LocalDateTime desde, LocalDateTime hasta) {
        Periodo p = periodo(desde, hasta);

        List<ReportePasajesResponse.Ruta> porRuta = new ArrayList<>();
        long boletos = 0;
        BigDecimal ingresos = BigDecimal.ZERO;
        for (ReportePasajesRutaProjection r : viajeRepository.ventasPorRuta(p.desde(), p.hasta())) {
            BigDecimal rutaIngresos = ceroSiNulo(r.getIngresos());
            long rutaBoletos = r.getBoletos() == null ? 0L : r.getBoletos();
            boletos += rutaBoletos;
            ingresos = ingresos.add(rutaIngresos);
            porRuta.add(ReportePasajesResponse.Ruta.builder()
                    .origen(r.getOrigen())
                    .destino(r.getDestino())
                    .viajes(r.getViajes() == null ? 0L : r.getViajes())
                    .boletos(rutaBoletos)
                    .ingresos(rutaIngresos)
                    .build());
        }

        return ReportePasajesResponse.builder()
                .desde(p.desde())
                .hasta(p.hasta())
                .viajes(cantidad(viajeRepository.viajesDelPeriodo(p.desde(), p.hasta())))
                .boletosVendidos(boletos)
                .ingresos(ingresos)
                .precioPromedio(promedio(ingresos, boletos))
                .porRuta(porRuta)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReporteOcupacionViajeResponse> ocupacion(LocalDateTime desde, LocalDateTime hasta) {
        return ocupacion(periodo(desde, hasta));
    }

    private List<ReporteOcupacionViajeResponse> ocupacion(Periodo p) {
        List<ReporteOcupacionViajeResponse> resultado = new ArrayList<>();
        for (ReporteOcupacionProjection o : viajeRepository.ocupacionPorViaje(p.desde(), p.hasta())) {
            long total = o.getTotalAsientos() == null ? 0L : o.getTotalAsientos();
            long ocupados = o.getOcupados() == null ? 0L : o.getOcupados();
            BigDecimal porcentaje = total == 0
                    ? BigDecimal.ZERO
                    : BigDecimal.valueOf(ocupados)
                            .multiply(BigDecimal.valueOf(100))
                            .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);

            resultado.add(ReporteOcupacionViajeResponse.builder()
                    .viajeId(o.getViajeId())
                    .placa(o.getPlaca())
                    .estado(o.getEstado())
                    .capacidadBus(o.getCapacidad())
                    .totalAsientos(total)
                    .ocupados(ocupados)
                    .disponibles(Math.max(0, total - ocupados))
                    .ocupacionPorcentaje(porcentaje)
                    .ingresos(ceroSiNulo(o.getIngresos()))
                    .build());
        }
        return resultado;
    }

    @Override
    @Transactional(readOnly = true)
    public ReporteEncomiendasResponse encomiendas(LocalDateTime desde, LocalDateTime hasta) {
        Periodo p = periodo(desde, hasta);
        ReporteTotalProjection totales = encomiendaRepository.encomiendasDelPeriodo(p.desde(), p.hasta());

        List<ReporteEncomiendasResponse.Estado> porEstado = encomiendaRepository
                .encomiendasPorEstado(p.desde(), p.hasta()).stream()
                .map(this::aEstado)
                .toList();

        List<ReporteEncomiendasResponse.Corredor> porRuta = new ArrayList<>();
        BigDecimal peso = BigDecimal.ZERO;
        for (ReporteEncomiendaRutaProjection r : encomiendaRepository.encomiendasPorRuta(p.desde(), p.hasta())) {
            peso = peso.add(ceroSiNulo(r.getPesoKg()));
            porRuta.add(ReporteEncomiendasResponse.Corredor.builder()
                    .origen(r.getOrigen())
                    .destino(r.getDestino())
                    .cantidad(r.getCantidad() == null ? 0L : r.getCantidad())
                    .ingresos(ceroSiNulo(r.getIngresos()))
                    .pesoKg(ceroSiNulo(r.getPesoKg()))
                    .build());
        }

        return ReporteEncomiendasResponse.builder()
                .desde(p.desde())
                .hasta(p.hasta())
                .total(cantidad(totales))
                .ingresos(total(totales))
                .pesoTotalKg(peso)
                .ticketPromedio(promedio(total(totales), cantidad(totales)))
                .porEstado(porEstado)
                .porRuta(porRuta)
                .build();
    }

    private ReporteEncomiendasResponse.Estado aEstado(ReporteEncomiendaEstadoProjection e) {
        return ReporteEncomiendasResponse.Estado.builder()
                .estado(e.getEstado())
                .cantidad(e.getCantidad() == null ? 0L : e.getCantidad())
                .ingresos(ceroSiNulo(e.getIngresos()))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReporteSucursalResponse> rankingSucursales(LocalDateTime desde, LocalDateTime hasta) {
        Periodo p = periodo(desde, hasta);
        return viajeRepository.actividadPorSucursal(p.desde(), p.hasta()).stream()
                .map(this::aSucursal)
                .sorted((a, b) -> b.getIngresosTotales().compareTo(a.getIngresosTotales()))
                .toList();
    }

    private ReporteSucursalResponse aSucursal(ReporteSucursalProjection s) {
        BigDecimal ingresosPasajes = ceroSiNulo(s.getIngresosPasajes());
        BigDecimal ingresosEncomiendas = ceroSiNulo(s.getIngresosEncomiendas());
        return ReporteSucursalResponse.builder()
                .sucursalId(s.getSucursalId())
                .sucursal(s.getSucursal())
                .ciudad(s.getCiudad())
                .esTerminal(Boolean.TRUE.equals(s.getEsTerminal()))
                .viajesOrigen(s.getViajesOrigen() == null ? 0L : s.getViajesOrigen())
                .viajesDestino(s.getViajesDestino() == null ? 0L : s.getViajesDestino())
                .boletosVendidos(s.getBoletosVendidos() == null ? 0L : s.getBoletosVendidos())
                .ingresosPasajes(ingresosPasajes)
                .encomiendasRegistradas(s.getEncomiendasRegistradas() == null ? 0L : s.getEncomiendasRegistradas())
                .ingresosEncomiendas(ingresosEncomiendas)
                .ingresosTotales(ingresosPasajes.add(ingresosEncomiendas))
                .build();
    }

    // ── Descargas CSV ───────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> ingresosCsv(LocalDateTime desde, LocalDateTime hasta) {
        return csvExportador.descargar(nombreArchivo("ingresos-por-metodo"),
                ingresos(desde, hasta).getPorMetodo(),
                ReporteIngresosResponse.IngresoPorMetodo.class);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> ingresosSerieDiariaCsv(LocalDateTime desde, LocalDateTime hasta) {
        return csvExportador.descargar(nombreArchivo("ingresos-diarios"),
                ingresosSerieDiaria(desde, hasta),
                ReporteIngresosDiarioResponse.class);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> pasajesCsv(LocalDateTime desde, LocalDateTime hasta) {
        return csvExportador.descargar(nombreArchivo("pasajes-por-ruta"),
                pasajes(desde, hasta).getPorRuta(),
                ReportePasajesResponse.Ruta.class);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> ocupacionCsv(LocalDateTime desde, LocalDateTime hasta) {
        return csvExportador.descargar(nombreArchivo("ocupacion-asientos"),
                ocupacion(desde, hasta),
                ReporteOcupacionViajeResponse.class);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> encomiendasPorEstadoCsv(LocalDateTime desde, LocalDateTime hasta) {
        return csvExportador.descargar(nombreArchivo("encomiendas-por-estado"),
                encomiendas(desde, hasta).getPorEstado(),
                ReporteEncomiendasResponse.Estado.class);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> encomiendasPorRutaCsv(LocalDateTime desde, LocalDateTime hasta) {
        return csvExportador.descargar(nombreArchivo("encomiendas-por-ruta"),
                encomiendas(desde, hasta).getPorRuta(),
                ReporteEncomiendasResponse.Corredor.class);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> rankingSucursalesCsv(LocalDateTime desde, LocalDateTime hasta) {
        return csvExportador.descargar(nombreArchivo("ranking-sucursales"),
                rankingSucursales(desde, hasta),
                ReporteSucursalResponse.class);
    }
}
