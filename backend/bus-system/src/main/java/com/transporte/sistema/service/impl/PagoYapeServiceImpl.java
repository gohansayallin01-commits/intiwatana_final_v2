package com.transporte.sistema.service.impl;

import com.transporte.sistema.dto.request.ConfirmarReservaRequest;
import com.transporte.sistema.dto.request.DeclararPagoYapeRequest;
import com.transporte.sistema.dto.request.RechazarPagoYapeRequest;
import com.transporte.sistema.dto.response.BoletoResponse;
import com.transporte.sistema.dto.response.CierreViajeResponse;
import com.transporte.sistema.dto.response.DeclaracionYapeResponse;
import com.transporte.sistema.entity.*;
import com.transporte.sistema.enums.EstadoAsiento;
import com.transporte.sistema.enums.EstadoDeclaracionPago;
import com.transporte.sistema.enums.EstadoRetencion;
import com.transporte.sistema.enums.MetodoPago;
import com.transporte.sistema.exception.ConflictoException;
import com.transporte.sistema.exception.NegocioException;
import com.transporte.sistema.exception.RecursoNoEncontradoException;
import com.transporte.sistema.repository.*;
import com.transporte.sistema.service.AuditoriaService;
import com.transporte.sistema.service.ConfiguracionVentaService;
import com.transporte.sistema.service.PagoYapeService;
import com.transporte.sistema.service.PortalImageValidator;
import com.transporte.sistema.service.PortalStorageService;
import com.transporte.sistema.service.VentaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PagoYapeServiceImpl implements PagoYapeService {

    private static final List<EstadoDeclaracionPago> ABIERTAS =
            List.of(EstadoDeclaracionPago.PENDIENTE_VERIFICACION, EstadoDeclaracionPago.APROBADO);

    private final DeclaracionPagoRepository declaracionPagoRepository;
    private final RetencionRepository retencionRepository;
    private final AsientoRepository asientoRepository;
    private final PagoRepository pagoRepository;
    private final BoletoRepository boletoRepository;
    private final ViajeRepository viajeRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaService auditoriaService;
    private final VentaService ventaService;
    private final ConfiguracionVentaService configuracionVenta;
    private final PortalImageValidator validator;
    private final PortalStorageService storage;

    @Override
    @Transactional
    public DeclaracionYapeResponse declarar(String referencia, String documento,
                                            DeclararPagoYapeRequest request, MultipartFile comprobante) {
        Retencion r = retencionRepository.findByReferenciaForUpdate(referencia)
                .orElseThrow(() -> new RecursoNoEncontradoException("Reserva no encontrada con la referencia indicada"));

        if (documento == null || !documento.trim().equals(r.getCompradorDocumento())) {
            throw new RecursoNoEncontradoException("Reserva no encontrada con la referencia indicada");
        }
        if (r.getEstado() == EstadoRetencion.CONFIRMADO) {
            throw new ConflictoException("Esta reserva ya fue pagada en caja");
        }
        if (r.getEstado() == EstadoRetencion.VENCIDO || r.getEstado() == EstadoRetencion.CANCELADO) {
            throw new ConflictoException("La reserva ya no está vigente");
        }

        LocalDateTime ahora = LocalDateTime.now();
        if (r.getExpiraEn() == null || !r.getExpiraEn().isAfter(ahora)) {
            throw new ConflictoException("La reserva venció; los asientos fueron liberados");
        }
        if (r.getEstado() != EstadoRetencion.PENDIENTE_PAGO && r.getEstado() != EstadoRetencion.RETENIDO) {
            throw new ConflictoException("La reserva no está en estado de pago");
        }

        for (DeclaracionPago previa : declaracionPagoRepository.findByRetencionIdOrderByDeclaradoEnDesc(r.getId())) {
            if (ABIERTAS.contains(previa.getEstado())) {
                throw new ConflictoException("Ya registraste un pago Yape para esta reserva; espera la verificación de caja");
            }
        }

        String operacion = request.getOperacion() == null ? null : request.getOperacion().trim();
        if (operacion == null || operacion.isBlank()) {
            throw new NegocioException("El número de operación es obligatorio");
        }
        if (declaracionPagoRepository.existsByOperacionAndEstadoIn(operacion, ABIERTAS)) {
            throw new ConflictoException("El número de operación ya fue registrado en otra reserva");
        }
        if (request.getImporte() == null || request.getImporte().compareTo(r.getTotal()) != 0) {
            throw new ConflictoException("El importe declarado no coincide con el total de la reserva (" + r.getTotal() + " soles)");
        }

        String cUrl = null, cNom = null, cChecksum = null, cMime = null;
        if (comprobante != null && !comprobante.isEmpty()) {
            if (!storage.disponible()) {
                throw new NegocioException("No se puede adjuntar el comprobante en este entorno");
            }
            byte[] bytes;
            try {
                bytes = comprobante.getBytes();
            } catch (IOException e) {
                throw new NegocioException("No se pudo leer el archivo del comprobante");
            }
            PortalImageValidator.ImagenValidada v = validator.validar(bytes, comprobante.getOriginalFilename());
            PortalStorageService.StoredFile guardado = storage.guardar(bytes, v.getExtension());
            cUrl = guardado.url();
            cNom = guardado.nombreInterno();
            cChecksum = v.getChecksum();
            cMime = v.getMime();
        }

        // Ventana única de verificación: si se declara al final del plazo, se
        // extiende el vencimiento UNA sola vez (nunca se renueva de nuevo).
        long tolerancia = configuracionVenta.toleranciaYapeMinutos();
        boolean extendida = false;
        if (tolerancia > 0
                && !Boolean.TRUE.equals(r.getToleranciaUsada())
                && r.getExpiraEn().isBefore(ahora.plusMinutes(tolerancia))) {
            r.setExpiraEn(ahora.plusMinutes(tolerancia));
            r.setToleranciaUsada(true);
            retencionRepository.save(r);
            extenderRetencionAsientos(r, r.getExpiraEn());
            extendida = true;
        }

        DeclaracionPago d = declaracionPagoRepository.save(DeclaracionPago.builder()
                .retencion(r)
                .operacion(operacion)
                .importe(request.getImporte())
                .fechaPagada(request.getFechaPagada())
                .cuentaPagada(request.getCuentaPagada())
                .comprobanteUrl(cUrl)
                .comprobanteNombreInterno(cNom)
                .comprobanteChecksum(cChecksum)
                .comprobanteMime(cMime)
                .estado(EstadoDeclaracionPago.PENDIENTE_VERIFICACION)
                .declaradoDocumento(r.getCompradorDocumento())
                .declaradoTelefono(r.getCompradorTelefono())
                .declaradoEn(ahora)
                .activo(true)
                .build());

        auditoriaService.registrar(null, "invitado", null,
                "DECLARAR_PAGO_YAPE", "DeclaracionPago", d.getId(),
                "referencia=" + r.getReferencia() + " importe=" + d.getImporte()
                        + (extendida ? " (ventana de verificación extendida una vez)" : ""));
        log.info("Pago Yape declarado: referencia={} operacion={} importe={}", r.getReferencia(), operacion, d.getImporte());

        return toResponse(d);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeclaracionYapeResponse> listarPendientes() {
        return declaracionPagoRepository
                .findByEstadoAndRetencionEstado(EstadoDeclaracionPago.PENDIENTE_VERIFICACION,
                        List.of(EstadoRetencion.RETENIDO, EstadoRetencion.PENDIENTE_PAGO))
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeclaracionYapeResponse> historialDelDia() {
        LocalDateTime inicio = LocalDate.now().atStartOfDay();
        return declaracionPagoRepository
                .findByDeclaradoEnGreaterThanEqualOrderByDeclaradoEnDesc(inicio)
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public List<BoletoResponse> aprobar(Long declaracionId, String username) {
        DeclaracionPago d = declaracionPagoRepository.findById(declaracionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Declaración de pago", declaracionId));
        if (d.getEstado() != EstadoDeclaracionPago.PENDIENTE_VERIFICACION) {
            throw new ConflictoException("La declaración de pago ya fue procesada");
        }

        Retencion r = d.getRetencion();
        ConfirmarReservaRequest confirmacion = ConfirmarReservaRequest.builder()
                .metodoPago(MetodoPago.YAPE)
                .referenciaPago(d.getOperacion())
                .documento(r.getCompradorDocumento())
                .observaciones("Pago Yape verificado en caja; operación " + d.getOperacion())
                .build();

        try {
            List<BoletoResponse> boletos = ventaService.confirmarPago(r.getReferencia(), confirmacion, username);
            d.setEstado(EstadoDeclaracionPago.APROBADO);
            d.setVerificador(usuarioRepository.findByUsername(username).orElse(null));
            d.setVerificadoEn(LocalDateTime.now());
            declaracionPagoRepository.save(d);

            auditoriaService.registrar(usuarioIdDe(username), username, rolDe(username),
                    "APROBAR_PAGO_YAPE", "DeclaracionPago", d.getId(),
                    "referencia=" + r.getReferencia() + " boletos=" + boletos.size());
            log.info("Pago Yape aprobado por {}: referencia={} operacion={}", username, r.getReferencia(), d.getOperacion());
            return boletos;
        } catch (ConflictoException e) {
            d.setEstado(EstadoDeclaracionPago.RECHAZADO);
            d.setMotivoRechazo("No se pudo verificar el pago: " + e.getMessage());
            d.setVerificador(usuarioRepository.findByUsername(username).orElse(null));
            d.setVerificadoEn(LocalDateTime.now());
            declaracionPagoRepository.save(d);
            auditoriaService.registrar(usuarioIdDe(username), username, rolDe(username),
                    "FALLO_VERIFICACION_YAPE", "DeclaracionPago", d.getId(),
                    "referencia=" + r.getReferencia() + " motivo=" + d.getMotivoRechazo());
            throw e;
        }
    }

    @Override
    @Transactional
    public DeclaracionYapeResponse rechazar(Long declaracionId, RechazarPagoYapeRequest request, String username) {
        DeclaracionPago d = declaracionPagoRepository.findById(declaracionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Declaración de pago", declaracionId));
        if (d.getEstado() != EstadoDeclaracionPago.PENDIENTE_VERIFICACION) {
            throw new ConflictoException("La declaración de pago ya fue procesada");
        }
        if (request.getMotivo() == null || request.getMotivo().isBlank()) {
            throw new NegocioException("El motivo del rechazo es obligatorio");
        }

        d.setEstado(EstadoDeclaracionPago.RECHAZADO);
        d.setMotivoRechazo(request.getMotivo().trim());
        d.setVerificador(usuarioRepository.findByUsername(username).orElse(null));
        d.setVerificadoEn(LocalDateTime.now());
        declaracionPagoRepository.save(d);

        // No se libera el asiento si la reserva sigue vigente: el comprador
        // puede corregir los datos y volver a declarar. La operación NO se reutiliza.
        auditoriaService.registrar(usuarioIdDe(username), username, rolDe(username),
                "RECHAZAR_PAGO_YAPE", "DeclaracionPago", d.getId(),
                "referencia=" + d.getRetencion().getReferencia() + " motivo=" + d.getMotivoRechazo());
        log.info("Pago Yape rechazado por {}: declaracion={} motivo={}", username, d.getId(), d.getMotivoRechazo());
        return toResponse(d);
    }

    @Override
    @Transactional(readOnly = true)
    public CierreViajeResponse cierrePorViaje(Long viajeId) {
        Viaje viaje = viajeRepository.findById(viajeId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Viaje", viajeId));

        List<Boleto> boletos = boletoRepository.findActivosPorViaje(viajeId);
        List<Asiento> asientos = asientoRepository.findByViajeId(viajeId);

        BigDecimal totalEfectivo = BigDecimal.ZERO;
        BigDecimal totalYape = BigDecimal.ZERO;
        BigDecimal totalOtros = BigDecimal.ZERO;
        for (Object[] fila : pagoRepository.pagosPorMetodoDeViaje(viajeId)) {
            String metodo = String.valueOf(fila[0]);
            BigDecimal suma = (BigDecimal) fila[1];
            if ("EFECTIVO".equals(metodo)) totalEfectivo = totalEfectivo.add(suma);
            else if ("YAPE".equals(metodo)) totalYape = totalYape.add(suma);
            else totalOtros = totalOtros.add(suma);
        }
        BigDecimal totalCobrado = totalEfectivo.add(totalYape).add(totalOtros);

        Map<EstadoRetencion, Long> retenciones = new EnumMap<>(EstadoRetencion.class);
        for (Object[] fila : retencionRepository.conteoEstadosPorViaje(viajeId)) {
            retenciones.put((EstadoRetencion) fila[0], (Long) fila[1]);
        }
        long vendidos = asientos.stream().filter(a -> a.getEstado() == EstadoAsiento.VENDIDO).count();
        long pendientes = declaracionPagoRepository.countPendientesPorViaje(viajeId);
        long rechazadas = declaracionPagoRepository.countRechazadasPorViaje(viajeId);

        BigDecimal totalPendiente = retencionRepository.totalPendientePagoPorViaje(viajeId);

        return CierreViajeResponse.builder()
                .viajeId(viaje.getId())
                .origen(viaje.getRuta().getOrigen().getCiudad())
                .destino(viaje.getRuta().getDestino().getCiudad())
                .busPlaca(viaje.getBus().getPlaca())
                .fechaHoraSalida(viaje.getFechaHoraSalida())
                .capacidadAsientos(asientos.size())
                .asientosVendidos((int) vendidos)
                .boletosActivos(boletos.size())
                .pasajerosConfirmados(boletos.size())
                .reservasPendientes(retenciones.getOrDefault(EstadoRetencion.PENDIENTE_PAGO, 0L).intValue())
                .reservasRetenidas(retenciones.getOrDefault(EstadoRetencion.RETENIDO, 0L).intValue())
                .reservasVencidas(retenciones.getOrDefault(EstadoRetencion.VENCIDO, 0L).intValue())
                .reservasLiberadas(retenciones.getOrDefault(EstadoRetencion.CANCELADO, 0L).intValue())
                .totalCobrado(totalCobrado)
                .totalEfectivo(totalEfectivo)
                .totalYape(totalYape)
                .totalOtrosMetodos(totalOtros)
                .totalPendientePago(totalPendiente)
                .declaracionesPendientes((int) pendientes)
                .declaracionesRechazadas((int) rechazadas)
                .deficitario((pendientes + rechazadas) > 0 || totalPendiente.signum() > 0)
                .generadoEn(LocalDateTime.now())
                .boletosEnmarcados(boletos.stream().map(Boleto::getNumeroBoleto).toList())
                .build();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private DeclaracionYapeResponse toResponse(DeclaracionPago d) {
        Retencion r = d.getRetencion();
        Viaje v = r.getViaje();
        long segundos = r.getExpiraEn() == null ? 0
                : Math.max(0, Duration.between(LocalDateTime.now(), r.getExpiraEn()).getSeconds());
        return DeclaracionYapeResponse.builder()
                .id(d.getId())
                .referencia(r.getReferencia())
                .documento(r.getCompradorDocumento())
                .pasajero((r.getCompradorNombres() + " " + r.getCompradorApellidos()).trim())
                .viaje(v.getRuta().getOrigen().getCiudad() + " → " + v.getRuta().getDestino().getCiudad())
                .busPlaca(v.getBus().getPlaca())
                .fechaHoraSalida(v.getFechaHoraSalida())
                .asientos(r.getAsientosNumeros())
                .totalEsperado(r.getTotal())
                .operacion(d.getOperacion())
                .importeDeclarado(d.getImporte())
                .fechaPagada(d.getFechaPagada())
                .cuentaPagada(d.getCuentaPagada())
                .declaradoEn(d.getDeclaradoEn())
                .segundosRestantes(segundos)
                .comprobanteUrl(d.getComprobanteUrl())
                .estado(d.getEstado().name())
                .motivoRechazo(d.getMotivoRechazo())
                .verificadoEn(d.getVerificadoEn())
                .build();
    }

    private void extenderRetencionAsientos(Retencion r, LocalDateTime nuevaExpira) {
        for (Asiento a : asientoRepository.findByRetencionId(r.getId())) {
            a.setRetenidoHasta(nuevaExpira);
            asientoRepository.save(a);
        }
    }

    private Usuario usuarioActual() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) return null;
            return usuarioRepository.findByUsername(auth.getName()).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private Long usuarioIdDe(String username) {
        Usuario u = usuarioRepository.findByUsername(username).orElse(null);
        return u == null ? null : u.getId();
    }

    private String rolDe(String username) {
        Usuario u = usuarioRepository.findByUsername(username).orElse(null);
        return u != null && u.getRol() != null && u.getRol().getNombre() != null
                ? u.getRol().getNombre().name() : null;
    }
}