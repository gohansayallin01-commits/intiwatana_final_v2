package com.transporte.sistema.service.impl;



import com.transporte.sistema.dto.request.ConfirmarReservaRequest;

import com.transporte.sistema.dto.request.CrearRetencionRequest;

import com.transporte.sistema.dto.request.DatosCompradorRequest;

import com.transporte.sistema.dto.request.VentaPresencialRequest;

import com.transporte.sistema.dto.response.AsientoReservaResponse;

import com.transporte.sistema.dto.response.BoletoResponse;

import com.transporte.sistema.dto.response.ConfiguracionYapePublicaResponse;

import com.transporte.sistema.dto.response.DeclaracionYapeReservaResponse;

import com.transporte.sistema.dto.response.RecuperacionResponse;

import com.transporte.sistema.dto.response.RetencionResponse;

import com.transporte.sistema.entity.Asiento;

import com.transporte.sistema.entity.Boleto;

import com.transporte.sistema.entity.Cliente;

import com.transporte.sistema.entity.DeclaracionPago;

import com.transporte.sistema.entity.Pago;

import com.transporte.sistema.entity.Retencion;

import com.transporte.sistema.entity.Usuario;

import com.transporte.sistema.entity.Viaje;

import com.transporte.sistema.enums.CanalVenta;

import com.transporte.sistema.enums.EstadoAsiento;

import com.transporte.sistema.enums.EstadoBoleto;

import com.transporte.sistema.enums.EstadoDeclaracionPago;

import com.transporte.sistema.enums.EstadoPago;

import com.transporte.sistema.enums.EstadoRetencion;

import com.transporte.sistema.enums.EstadoViaje;

import com.transporte.sistema.enums.MetodoPago;

import com.transporte.sistema.enums.TipoCliente;

import com.transporte.sistema.exception.ConflictoException;

import com.transporte.sistema.exception.NegocioException;

import com.transporte.sistema.exception.RecursoNoEncontradoException;

import com.transporte.sistema.repository.AsientoRepository;

import com.transporte.sistema.repository.BoletoRepository;

import com.transporte.sistema.repository.ClienteRepository;

import com.transporte.sistema.repository.DeclaracionPagoRepository;

import com.transporte.sistema.repository.PagoRepository;

import com.transporte.sistema.repository.RetencionRepository;

import com.transporte.sistema.repository.UsuarioRepository;

import com.transporte.sistema.repository.ViajeRepository;

import com.transporte.sistema.service.AuditoriaService;

import com.transporte.sistema.service.BoletoService;

import com.transporte.sistema.service.GeneradorDocumentos;

import com.transporte.sistema.service.PdfVentaService;

import com.transporte.sistema.service.ConfiguracionVentaService;

import com.transporte.sistema.service.VentaService;

import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.security.core.Authentication;

import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;



import java.math.BigDecimal;

import java.time.Duration;

import java.time.LocalDateTime;

import java.util.ArrayList;

import java.util.HashMap;

import java.util.LinkedHashSet;

import java.util.List;

import java.util.Map;

import java.util.Optional;

import java.util.UUID;

import java.util.stream.Collectors;



/**

 * ImplementaciÃ³n del flujo comercial real.

 *

 * Decisiones clave:

 *  - La disponibilidad es por VIAJE + ASIENTO y la comparte la web con la caja.

 *  - Una retenciÃ³n vencida deja de bloquear comercialmente aunque el barrido

 *    programado no haya corrido todavÃ­a (liberaciÃ³n perezosa).

 *  - Confirmar el pago es la ÃNICA vÃ­a para emitir boletos/QR.

 *  - Todo se apoya en transacciones y bloqueo pesimista de asiento para impedir

 *    la doble venta; ademÃ¡s V4 impone un Ã­ndice Ãºnico parcial en boletos.

 */

@Slf4j

@Service

@RequiredArgsConstructor

public class VentaServiceImpl implements VentaService {



    private final ViajeRepository     viajeRepository;

    private final AsientoRepository   asientoRepository;

    private final RetencionRepository retencionRepository;

    private final ClienteRepository   clienteRepository;

    private final PagoRepository      pagoRepository;

    private final BoletoRepository    boletoRepository;

    private final UsuarioRepository   usuarioRepository;

    private final AuditoriaService    auditoriaService;

    private final GeneradorDocumentos generadorDocumentos;

    private final BoletoService       boletoService;

    private final PdfVentaService     pdfVentaService;

    private final ConfiguracionVentaService configuracionVenta;

    private final DeclaracionPagoRepository declaracionPagoRepository;



    @Value("${app.ventas.hold-minutos:15}")

    private long holdMinutos;



    @Value("${app.ventas.permitir-pago-pendiente:true}")

    private boolean permitirPagoPendiente;



    @Value("${app.ventas.pago-pendiente-minutos:1440}")

    private long pagoPendienteMinutos;



    // ââ Crear retenciÃ³n âââââââââââââââââââââââââââââââââââââââââââââââââââââ



    @Override

    @Transactional

    public RetencionResponse crearRetencion(CrearRetencionRequest request) {

        return crearRetencion(request, CanalVenta.WEB);

    }



    private RetencionResponse crearRetencion(CrearRetencionRequest request, CanalVenta canal) {

        String key = normalizarKey(request.getIdempotencyKey());

        if (key != null) {

            Optional<Retencion> previa = retencionRepository.findByIdempotencyKey(key);

            if (previa.isPresent()) {

                return toPublic(expirarSiNecesario(previa.get()));

            }

        }



        Viaje viaje = viajeRepository.findById(request.getViajeId())

                .orElseThrow(() -> new RecursoNoEncontradoException("Viaje", request.getViajeId()));

        if (viaje.getEstado() != EstadoViaje.PROGRAMADO) {

            throw new NegocioException("Solo se pueden reservar asientos de viajes PROGRAMADOS");

        }

        LocalDateTime ahora = LocalDateTime.now();

        // ── Reserva web: margen de cierre antes de la salida ──
        if (canal == CanalVenta.WEB) {
            long cierreWeb = Math.max(0, configuracionVenta.cierreWebMinutos());
            LocalDateTime topeWeb = viaje.getFechaHoraSalida().minusMinutes(cierreWeb);
            if (!topeWeb.isAfter(ahora)) {
                throw new NegocioException("La venta web para este viaje ya cerró. Reserva o paga en la agencia INTIWATANA.");
            }
        }

        // ── Control de reservas pendientes (anti "reserva falsa") ──
        if (request.getComprador() != null && request.getComprador().getDocumento() != null) {
            String doc = request.getComprador().getDocumento().trim();
            List<EstadoRetencion> activos = List.of(EstadoRetencion.RETENIDO, EstadoRetencion.PENDIENTE_PAGO);
            if (retencionRepository.existeActivaPorDocumentoYViaje(doc, viaje.getId(), activos, ahora)) {
                throw new ConflictoException("Ya tienes una reserva pendiente para este viaje. Complétala o espera a que venza.");
            }
            if (retencionRepository.countActivasPorDocumento(doc, activos, ahora) >= 2) {
                throw new ConflictoException("Alcanzaste el límite de 2 reservas pendientes por documento.");
            }
            String telefono = request.getComprador().getTelefono();
            if (telefono != null && !telefono.isBlank()
                    && retencionRepository.countActivasPorTelefono(telefono.trim(), activos, ahora) >= 2) {
                throw new ConflictoException("Alcanzaste el límite de 2 reservas pendientes por teléfono.");
            }
        }

        if (viaje.getFechaHoraSalida().isBefore(ahora)) {

            throw new NegocioException("El viaje ya partiÃ³ o no estÃ¡ disponible");

        }



        List<Long> ids = new ArrayList<>(new LinkedHashSet<>(request.getAsientoIds()));

        if (ids.isEmpty()) {

            throw new NegocioException("Debe seleccionar al menos un asiento");

        }



        List<Asiento> asientos = new ArrayList<>();

        for (Long asientoId : ids) {

            Asiento a = asientoRepository.findByIdForUpdate(asientoId)

                    .orElseThrow(() -> new RecursoNoEncontradoException("Asiento", asientoId));

            if (!a.getViaje().getId().equals(viaje.getId())) {

                throw new NegocioException("El asiento " + a.getNumeroAsiento() + " no pertenece al viaje seleccionado");

            }

            liberarSiVencio(a, ahora);

            if (a.getEstado() != EstadoAsiento.DISPONIBLE) {

                throw new ConflictoException("El asiento " + a.getNumeroAsiento() + " ya no estÃ¡ disponible");

            }

            asientos.add(a);

        }



        BigDecimal unitario = viaje.getPrecioAdulto();

        long hold = Math.max(1, configuracionVenta.holdMinutos());

        LocalDateTime expira = ahora.plusMinutes(hold);

        if (canal == CanalVenta.WEB) {

            LocalDateTime topeWeb = viaje.getFechaHoraSalida().minusMinutes(configuracionVenta.cierreWebMinutos());

            if (topeWeb.isBefore(expira)) {

                expira = topeWeb;

            }

        }



        Retencion r = Retencion.builder()

                .referencia(generarReferencia())

                .viaje(viaje)

                .estado(EstadoRetencion.RETENIDO)

                .canal(canal)

                .compradorNombres(request.getComprador().getNombres())

                .compradorApellidos(request.getComprador().getApellidos())

                .compradorDocumento(request.getComprador().getDocumento().trim())

                .compradorTelefono(request.getComprador().getTelefono())

                .compradorEmail(request.getComprador().getEmail())

                .cantidadAsientos(asientos.size())

                .precioUnitario(unitario)

                .total(unitario.multiply(BigDecimal.valueOf(asientos.size())))

                .asientosNumeros(asientos.stream().map(Asiento::getNumeroAsiento).collect(Collectors.joining(", ")))

                .metodoPagoPreferido(request.getMetodoPagoPreferido())

                .idempotencyKey(key)

                .expiraEn(expira)

                .observaciones(request.getObservaciones())

                .activo(true)

                .build();

        r = retencionRepository.save(r);



        for (Asiento a : asientos) {

            a.setEstado(EstadoAsiento.RESERVADO);

            a.setRetencion(r);

            a.setRetenidoHasta(expira);

            asientoRepository.save(a);

        }



        registrarAuditoria("CREAR_RETENCION", r.getId(), r.getReferencia());

        log.info("RetenciÃ³n creada: {} | viaje={} | asientos={}", r.getReferencia(),

                viaje.getId(), r.getAsientosNumeros());

        return toPublic(r);

    }



    // ── Venta presencial (caja) ─────────────────────────────────────────────



    @Override

    @Transactional

    public List<BoletoResponse> ventaPresencial(VentaPresencialRequest request, String username) {

        if (request.getMetodoPago() == null) {

            throw new NegocioException("El método de pago es obligatorio");

        }

        if (request.getMetodoPago() != MetodoPago.EFECTIVO) {

            throw new NegocioException("En caja solo se admite pago en efectivo");

        }

        if (request.getClienteId() == null) {

            throw new NegocioException("Debe seleccionar un cliente para registrar la venta");

        }



        Cliente cliente = clienteRepository.findById(request.getClienteId())

                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente", request.getClienteId()));



        DatosCompradorRequest comprador = DatosCompradorRequest.builder()

                .nombres(cliente.getNombres() != null ? cliente.getNombres() : cliente.getRazonSocial())

                .apellidos(cliente.getApellidos() != null ? cliente.getApellidos() : "")

                .documento(cliente.getDniRuc())

                .telefono(cliente.getTelefono())

                .email(cliente.getEmail())

                .build();



        CrearRetencionRequest retencionRequest = CrearRetencionRequest.builder()

                .viajeId(request.getViajeId())

                .asientoIds(request.getAsientoIds())

                .idempotencyKey(request.getIdempotencyKey())

                .comprador(comprador)

                .metodoPagoPreferido(request.getMetodoPago())

                .observaciones(request.getObservaciones())

                .build();



        // Bloqueo de asientos + datos del comprador en una retención con canal CAJERO.

        RetencionResponse reserva = crearRetencion(retencionRequest, CanalVenta.CAJERO);



        ConfirmarReservaRequest confirmacion = ConfirmarReservaRequest.builder()

                .metodoPago(request.getMetodoPago())

                .referenciaPago(request.getReferenciaPago())

                .observaciones(request.getObservaciones())

                .documento(cliente.getDniRuc())

                .build();



        // Mismo contexto transaccional: si algo falla, no queda ni reserva ni boleto.

        List<BoletoResponse> emitidos = confirmarPago(reserva.getReferencia(), confirmacion, username);



        log.info("Venta presencial: referencia={} | asientos={} | boletos={} | cajero={}",

                reserva.getReferencia(), request.getAsientoIds().size(), emitidos.size(), username);



        return emitidos;

    }



    // ââ Consulta ââââââââââââââââââââââââââââââââââââââââââââââââââââââââââââ



    @Override

    @Transactional

    public RetencionResponse obtenerPorReferencia(String referencia, String documento) {

        Retencion r = retencionRepository.findByReferencia(referencia)

                .orElseThrow(() -> new RecursoNoEncontradoException(

                        "Reserva no encontrada con la referencia indicada"));

        verificarPropietario(r, documento);

        return toPublic(expirarSiNecesario(r));

    }



    // ââ Reserva pendiente de pago âââââââââââââââââââââââââââââââââââââââââââ



    @Override

    @Transactional

    public RetencionResponse reservarPendientePago(String referencia, MetodoPago metodoPago) {

        Retencion r = retencionRepository.findByReferenciaForUpdate(referencia)

                .orElseThrow(() -> new RecursoNoEncontradoException("Reserva no encontrada"));



        expirarSiNecesario(r);

        if (r.getEstado() == EstadoRetencion.CONFIRMADO) {

            return toPublic(r);

        }

        if (r.getEstado() == EstadoRetencion.VENCIDO || r.getEstado() == EstadoRetencion.CANCELADO) {

            throw new ConflictoException("La reserva ya no estÃ¡ vigente");

        }

        if (!permitirPagoPendiente) {

            throw new NegocioException("El pago posterior en caja no estÃ¡ habilitado");

        }



        LocalDateTime ahoraR = LocalDateTime.now();

        long pendiente = Math.max(1, configuracionVenta.pagoPendienteMinutos());

        LocalDateTime nuevaExpira = ahoraR.plusMinutes(pendiente);

        if (r.getCanal() == CanalVenta.WEB) {

            LocalDateTime topeWeb = r.getViaje().getFechaHoraSalida().minusMinutes(configuracionVenta.cierreWebMinutos());

            if (topeWeb.isBefore(nuevaExpira)) {

                nuevaExpira = topeWeb;

            }

        }

        r.setEstado(EstadoRetencion.PENDIENTE_PAGO);

        r.setExpiraEn(nuevaExpira);

        if (metodoPago != null) {

            r.setMetodoPagoPreferido(metodoPago);

        }

        extenderAsientos(r, nuevaExpira);

        retencionRepository.save(r);



        registrarAuditoria("RESERVAR_PENDIENTE_PAGO", r.getId(), r.getReferencia());

        log.info("Reserva pendiente de pago: {}", r.getReferencia());

        return toPublic(r);

    }



    // ââ ConfirmaciÃ³n de pago (caja/admin) âââââââââââââââââââââââââââââââââââ



    @Override

    @Transactional

    public List<BoletoResponse> confirmarPago(String referencia, ConfirmarReservaRequest request, String username) {

        Retencion r = retencionRepository.findByReferenciaForUpdate(referencia)

                .orElseThrow(() -> new RecursoNoEncontradoException("Reserva no encontrada"));



        // Idempotencia: si ya estÃ¡ confirmada, devolver los boletos ya emitidos.

        if (r.getEstado() == EstadoRetencion.CONFIRMADO) {

            return boletosDe(r);

        }

        if (r.getEstado() == EstadoRetencion.VENCIDO || r.getEstado() == EstadoRetencion.CANCELADO) {

            throw new ConflictoException("La reserva ya no estÃ¡ vigente y no puede confirmarse");

        }



        LocalDateTime ahora = LocalDateTime.now();

        if (r.getExpiraEn().isBefore(ahora)) {

            expirar(r, ahora);

            throw new ConflictoException("La reserva venciÃ³; los asientos fueron liberados");

        }



        // Bloqueo fÃ­sico de cada asiento y verificaciÃ³n de que sigue retenido por esta reserva.

        List<Asiento> asientos = asientoRepository.findByRetencionId(r.getId());

        if (asientos.isEmpty()) {

            throw new ConflictoException("La reserva no tiene asientos vigentes");

        }

        for (Asiento a : asientos) {

            Asiento bloqueado = asientoRepository.findByIdForUpdate(a.getId())

                    .orElseThrow(() -> new RecursoNoEncontradoException("Asiento", a.getId()));

            if (bloqueado.getEstado() != EstadoAsiento.RESERVADO

                    || bloqueado.getRetencion() == null

                    || !bloqueado.getRetencion().getId().equals(r.getId())) {

                throw new ConflictoException("El asiento " + bloqueado.getNumeroAsiento()

                        + " ya no estÃ¡ retenido por esta reserva");

            }

        }



        // Un número de operación Yape no se reutiliza entre reservas.
        if (request.getMetodoPago() == MetodoPago.YAPE
                && request.getReferenciaPago() != null
                && !request.getReferenciaPago().isBlank()
                && pagoRepository.existsByReferencia(request.getReferenciaPago())) {
            throw new ConflictoException("El número de operación " + request.getReferenciaPago() + " ya fue utilizado");
        }

        Cliente cliente = resolverCliente(r);

        Usuario cajero = usuarioRepository.findByUsername(username).orElse(null);



        Pago pago = pagoRepository.save(Pago.builder()

                .monto(r.getTotal())

                .fechaPago(ahora)

                .metodo(request.getMetodoPago())

                .estado(EstadoPago.COMPLETADO)

                .referencia(request.getReferenciaPago())

                .observacion(request.getObservaciones())

                .cliente(cliente)

                .cajero(cajero)

                .activo(true)

                .build());



        List<BoletoResponse> emitidos = new ArrayList<>();

        for (Asiento a : asientos) {

            Boleto b = boletoRepository.save(Boleto.builder()

                    .numeroBoleto("TMP-" + java.util.UUID.randomUUID().toString().substring(0, 26))

                    .viaje(r.getViaje())

                    .asiento(a)

                    .cliente(cliente)

                    .precioPagado(r.getPrecioUnitario())

                    .estado(EstadoBoleto.ACTIVO)

                    .pago(pago)

                    .cajero(cajero)

                    .observaciones(request.getObservaciones())

                    .activo(true)

                    .build());



            String numero = generadorDocumentos.generarNumeroBoleto(b.getId());

            b.setNumeroBoleto(numero);

            b.setCodigoQr(numero);

            b.setQrImagenUrl(generadorDocumentos.generarQrBase64(numero));

            b = boletoRepository.save(b);



            a.setEstado(EstadoAsiento.VENDIDO);

            a.setRetencion(null);

            a.setRetenidoHasta(null);

            asientoRepository.save(a);



            emitidos.add(boletoService.toResponse(b));

        }



        r.setEstado(EstadoRetencion.CONFIRMADO);

        r.setConfirmadaEn(ahora);

        r.setConfirmadaPor(cajero);

        r.setPago(pago);

        retencionRepository.save(r);



        registrarAuditoria("CONFIRMAR_RESERVA", r.getId(), r.getReferencia());

        log.info("Reserva confirmada: {} | boletos={} | cajero={}", r.getReferencia(),

                emitidos.size(), username);

        return emitidos;

    }



    // ââ LiberaciÃ³n explÃ­cita ââââââââââââââââââââââââââââââââââââââââââââââââ



    @Override

    @Transactional

    public RetencionResponse liberar(String referencia, String motivo, String username) {

        Retencion r = retencionRepository.findByReferenciaForUpdate(referencia)

                .orElseThrow(() -> new RecursoNoEncontradoException("Reserva no encontrada"));

        if (r.getEstado() == EstadoRetencion.CONFIRMADO) {

            throw new ConflictoException("No se puede liberar una reserva ya confirmada; cancele el boleto.");

        }

        if (r.getEstado() == EstadoRetencion.VENCIDO || r.getEstado() == EstadoRetencion.CANCELADO) {

            return toPublic(r);

        }

        LocalDateTime ahora = LocalDateTime.now();

        liberarAsientos(r, ahora);

        r.setEstado(EstadoRetencion.CANCELADO);

        r.setLiberadaEn(ahora);

        r.setMotivoLiberacion(motivo);

        retencionRepository.save(r);



        registrarAuditoria("LIBERAR_RETENCION", r.getId(), r.getReferencia());

        log.info("Reserva liberada: {} | por={} | motivo={}", r.getReferencia(), username, motivo);

        return toPublic(r);

    }



    // ââ Listados y recuperaciÃ³n âââââââââââââââââââââââââââââââââââââââââââââ



    @Override

    @Transactional(readOnly = true)

    public List<RetencionResponse> listarReservas(List<EstadoRetencion> estados) {

        List<EstadoRetencion> filtro = (estados == null || estados.isEmpty())

                ? List.of(EstadoRetencion.RETENIDO, EstadoRetencion.PENDIENTE_PAGO)

                : estados;

        return retencionRepository

                .findByEstadoIn(filtro, org.springframework.data.domain.Pageable.unpaged())

                .getContent().stream()

                .map(this::toPublic)

                .toList();

    }



    @Override

    @Transactional

    public RecuperacionResponse recuperar(String documento, String referencia) {

        if (documento == null || documento.isBlank()) {

            throw new NegocioException("Debe indicar el documento del comprador");

        }

        String doc = documento.trim();

        List<RetencionResponse> reservas = retencionRepository

                .findByCompradorDocumentoOrderByCreatedAtDesc(doc).stream()

                .filter(r -> referencia == null || referencia.isBlank()

                        || r.getReferencia().equalsIgnoreCase(referencia.trim()))

                .map(this::toPublic)

                .toList();



        List<BoletoResponse> boletos = boletoRepository.findVigentesPorDocumento(doc).stream()

                .map(boletoService::toResponse)

                .toList();



        // Anexa el estado del pago Yape declarado por el cliente (sólo estado, nunca operación completa).
        Map<String, DeclaracionPago> declaracionPorReferencia = new HashMap<>();
        for (DeclaracionPago dd : declaracionPagoRepository.findByDocumento(doc)) {
            declaracionPorReferencia.putIfAbsent(dd.getRetencion().getReferencia(), dd);
        }
        reservas.forEach(r -> {
            DeclaracionPago dd = declaracionPorReferencia.get(r.getReferencia());
            if (dd != null) {
                r.setPagoYape(DeclaracionYapeReservaResponse.builder()
                        .estado(dd.getEstado())
                        .operacionEnmascarada(enmascararOperacion(dd.getOperacion()))
                        .exponeRenovar(dd.getEstado() == EstadoDeclaracionPago.RECHAZADO
                                && dd.getRetencion().getEstado() == EstadoRetencion.PENDIENTE_PAGO
                                && dd.getRetencion().getExpiraEn() != null
                                && dd.getRetencion().getExpiraEn().isAfter(java.time.LocalDateTime.now()))
                        .build());
            }
        });

        registrarAuditoria("RECUPERAR_RESERVA", null, "documento solicitado");

        return RecuperacionResponse.builder()
                .reservas(reservas)
                .boletos(boletos)
                .yapeConfiguracion(configuracionVenta.yapeActivaPublica())
                .build();

    }



    // ââ PDFs ââââââââââââââââââââââââââââââââââââââââââââââââââââââââââââââââ



    @Override

    @Transactional

    public byte[] constanciaPdf(String referencia, String documento) {

        Retencion r = retencionRepository.findByReferencia(referencia)

                .orElseThrow(() -> new RecursoNoEncontradoException("Reserva no encontrada"));

        verificarPropietario(r, documento);

        if (r.getEstado() == EstadoRetencion.CONFIRMADO) {

            throw new NegocioException("La reserva ya estÃ¡ confirmada; descargue el boleto de viaje");

        }

        List<Asiento> asientos = asientoRepository.findByRetencionId(r.getId());

        return pdfVentaService.generarConstancia(r, asientos);

    }



    @Override

    @Transactional

    public byte[] boletoPdf(String numeroBoleto, String documento) {

        Boleto b = boletoRepository.findByNumeroBoleto(numeroBoleto)

                .orElseThrow(() -> new RecursoNoEncontradoException("Boleto no encontrado"));

        if (documento == null || !documento.trim().equals(b.getCliente().getDniRuc())) {

            throw new RecursoNoEncontradoException("Boleto no encontrado");

        }

        if (b.getEstado() != EstadoBoleto.ACTIVO && b.getEstado() != EstadoBoleto.USADO) {

            throw new NegocioException("El boleto no estÃ¡ vigente");

        }

        return pdfVentaService.generarBoleto(b);

    }



    // ââ Mantenimiento âââââââââââââââââââââââââââââââââââââââââââââââââââââââ



    @Override

    @Transactional

    public int liberarVencidas() {

        LocalDateTime ahora = LocalDateTime.now();

        List<Retencion> vencidas = retencionRepository.findVencidas(

                List.of(EstadoRetencion.RETENIDO, EstadoRetencion.PENDIENTE_PAGO), ahora);

        for (Retencion r : vencidas) {

            expirar(r, ahora);

        }

        if (!vencidas.isEmpty()) {

            log.info("Barrido de reservas vencidas: {} liberadas", vencidas.size());

        }

        return vencidas.size();

    }



    // ââ Helpers internos ââââââââââââââââââââââââââââââââââââââââââââââââââââ



    private static final List<EstadoRetencion> ACTIVAS =

            List.of(EstadoRetencion.RETENIDO, EstadoRetencion.PENDIENTE_PAGO);



    private Retencion expirarSiNecesario(Retencion r) {

        if (ACTIVAS.contains(r.getEstado()) && r.getExpiraEn().isBefore(LocalDateTime.now())) {

            expirar(r, LocalDateTime.now());

        }

        return r;

    }



    private void expirar(Retencion r, LocalDateTime ahora) {

        if (!ACTIVAS.contains(r.getEstado())) {

            return;

        }

        liberarAsientos(r, ahora);

        r.setEstado(EstadoRetencion.VENCIDO);

        r.setLiberadaEn(ahora);

        r.setMotivoLiberacion("Vencimiento automÃ¡tico");

        retencionRepository.save(r);

        registrarAuditoria("VENCER_RETENCION", r.getId(), r.getReferencia());

    }



    private void liberarAsientos(Retencion r, LocalDateTime ahora) {

        for (Asiento a : asientoRepository.findByRetencionId(r.getId())) {

            a.setEstado(EstadoAsiento.DISPONIBLE);

            a.setRetencion(null);

            a.setRetenidoHasta(null);

            asientoRepository.save(a);

        }

    }



    private void extenderAsientos(Retencion r, LocalDateTime nuevaExpira) {

        for (Asiento a : asientoRepository.findByRetencionId(r.getId())) {

            a.setRetenidoHasta(nuevaExpira);

            asientoRepository.save(a);

        }

    }



    private boolean estaDisponible(Asiento a, LocalDateTime ahora) {

        if (a.getEstado() == EstadoAsiento.DISPONIBLE) {

            return true;

        }

        return a.getEstado() == EstadoAsiento.RESERVADO

                && (a.getRetenidoHasta() == null || !a.getRetenidoHasta().isAfter(ahora));

    }



    private void liberarSiVencio(Asiento a, LocalDateTime ahora) {

        if (estaDisponible(a, ahora) && a.getEstado() != EstadoAsiento.DISPONIBLE) {

            a.setEstado(EstadoAsiento.DISPONIBLE);

            a.setRetencion(null);

            a.setRetenidoHasta(null);

            asientoRepository.save(a);

        }

    }



    private Cliente resolverCliente(Retencion r) {

        return clienteRepository.findByDniRuc(r.getCompradorDocumento())

                .orElseGet(() -> clienteRepository.save(Cliente.builder()

                        .dniRuc(r.getCompradorDocumento())

                        .nombres(r.getCompradorNombres())

                        .apellidos(r.getCompradorApellidos())

                        .telefono(r.getCompradorTelefono())

                        .email(r.getCompradorEmail())

                        .tipoCliente(TipoCliente.PERSONA)

                        .activo(true)

                        .build()));

    }



    private List<BoletoResponse> boletosDe(Retencion r) {

        if (r.getPago() == null) {

            return List.of();

        }

        return boletoRepository.findAllByPagoId(r.getPago().getId()).stream()

                .map(boletoService::toResponse)

                .toList();

    }



    private void verificarPropietario(Retencion r, String documento) {

        if (documento == null || !documento.trim().equals(r.getCompradorDocumento())) {

            // Mensaje intencionalmente genÃ©rico: no se revela la existencia de datos ajenos.

            throw new RecursoNoEncontradoException("Reserva no encontrada con la referencia indicada");

        }

    }



    private String generarReferencia() {

        return "RET-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();

    }



    private String normalizarKey(String key) {

        return (key == null || key.isBlank()) ? null : key.trim();

    }



    private void registrarAuditoria(String accion, Long entidadId, String detalle) {

        Usuario u = usuarioActual();

        auditoriaService.registrar(u != null ? u.getId() : null,

                u != null ? u.getUsername() : "sistema",

                u != null && u.getRol() != null ? u.getRol().getNombre().name() : null,

                accion, "Retencion", entidadId, detalle);

    }



    private Usuario usuarioActual() {

        try {

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            if (auth == null || !auth.isAuthenticated()) {

                return null;

            }

            return usuarioRepository.findByUsername(auth.getName()).orElse(null);

        } catch (Exception e) {

            return null;

        }

    }



    private RetencionResponse toPublic(Retencion r) {

        Viaje v = r.getViaje();

        LocalDateTime ahora = LocalDateTime.now();

        long segundos = r.getExpiraEn() == null ? 0

                : Math.max(0, Duration.between(ahora, r.getExpiraEn()).getSeconds());



        List<Asiento> seats = asientoRepository.findByRetencionId(r.getId());

        List<AsientoReservaResponse> asientos;

        if (!seats.isEmpty()) {

            asientos = seats.stream()

                    .map(a -> AsientoReservaResponse.builder()

                            .id(a.getId())

                            .numeroAsiento(a.getNumeroAsiento())

                            .precio(r.getPrecioUnitario())

                            .build())

                    .toList();

        } else {

            asientos = parsearAsientos(r.getAsientosNumeros(), r.getPrecioUnitario());

        }



        boolean confirmado = r.getEstado() == EstadoRetencion.CONFIRMADO;

        return RetencionResponse.builder()

                .referencia(r.getReferencia())

                .estado(r.getEstado())

                .canal(r.getCanal())

                .viajeId(v.getId())

                .origen(v.getRuta().getOrigen().getCiudad())

                .destino(v.getRuta().getDestino().getCiudad())

                .busPlaca(v.getBus().getPlaca())

                .fechaHoraSalida(v.getFechaHoraSalida())

                .cantidadAsientos(r.getCantidadAsientos())

                .precioUnitario(r.getPrecioUnitario())

                .total(r.getTotal())

                .asientos(asientos)

                .expiraEn(r.getExpiraEn())

                .segundosRestantes(segundos)

                .validoParaEmbarque(confirmado)

                .requierePago(!confirmado)

                .metodoPagoPreferido(r.getMetodoPagoPreferido())

                .confirmadaEn(r.getConfirmadaEn())

                .compradorNombre((r.getCompradorNombres() + " " + r.getCompradorApellidos()).trim())

                .mensaje(confirmado

                        ? "Pago confirmado. Boleto vÃ¡lido para embarque."

                        : "Reserva registrada. Falta confirmar el pago en caja para emitir el boleto.")

                .boletos(confirmado ? boletosDe(r) : null)

                .build();

    }



    private List<AsientoReservaResponse> parsearAsientos(String numeros, BigDecimal precio) {

        if (numeros == null || numeros.isBlank()) {

            return List.of();

        }

        return java.util.Arrays.stream(numeros.split(","))

                .map(String::trim)

                .filter(s -> !s.isEmpty())

                .map(n -> AsientoReservaResponse.builder().numeroAsiento(n).precio(precio).build())

                .toList();

    }

    /** Enmascara el número de operación Yape para no exponerlo en vistas públicas. */
    private String enmascararOperacion(String operacion) {

        if (operacion == null || operacion.isBlank()) {

            return null;

        }

        String op = operacion.trim();

        if (op.length() <= 6) {

            return "***";

        }

        return op.substring(0, 3) + "***" + op.substring(op.length() - 2);

    }

}

