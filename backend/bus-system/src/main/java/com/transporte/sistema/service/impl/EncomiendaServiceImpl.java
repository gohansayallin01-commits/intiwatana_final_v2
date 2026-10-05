package com.transporte.sistema.service.impl;

import com.transporte.sistema.dto.request.ActualizarEstadoEncomiendaRequest;
import com.transporte.sistema.dto.request.ConfirmarRecepcionEncomiendaRequest;
import com.transporte.sistema.dto.request.EncomiendaClienteRequest;
import com.transporte.sistema.dto.request.EncomiendaRequest;
import com.transporte.sistema.dto.response.*;
import com.transporte.sistema.entity.*;
import com.transporte.sistema.enums.EstadoEncomienda;
import com.transporte.sistema.enums.EstadoPago;
import com.transporte.sistema.enums.MetodoPago;
import com.transporte.sistema.exception.NegocioException;
import com.transporte.sistema.exception.RecursoNoEncontradoException;
import com.transporte.sistema.exception.SinAutorizacionException;
import com.transporte.sistema.repository.*;
import com.transporte.sistema.service.AuditoriaService;
import com.transporte.sistema.service.EncomiendaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("DataFlowIssue")
public class EncomiendaServiceImpl implements EncomiendaService {

    private final EncomiendaRepository           encomiendaRepository;
    private final ClienteRepository              clienteRepository;
    private final SucursalRepository             sucursalRepository;
    private final ViajeRepository                viajeRepository;
    private final MovimientoEncomiendaRepository movimientoRepository;
    private final UsuarioRepository              usuarioRepository;
    private final PagoRepository                 pagoRepository;
    private final AuditoriaService               auditoriaService;
    private final TransactionTemplate            transactionTemplate;

    // ── Registro por admin/cajero ─────────────────────────────────────────────

    /**
     * Alta presencial de una encomienda que ya llega a ventanilla.
     *
     * <p>La idempotencia se resuelve en DOS pasos porque dos agentes pueden
     * pulsar "Registrar" a la vez con la misma clave: la consulta previa evita
     * el caso secuencial (doble clic o reintento de red) y, si las dos
     * peticiones llegan realmente simultáneas, el índice único
     * {@code uq_encomiendas_clave_idempotencia} rechaza el segundo INSERT.
     * Ese rechazo es una EXCEPCIÓN de SQL, no un 500 para el usuario: aquí se
     * convierte en la respuesta de la encomienda que la otra petición ya creó,
     * de modo que nunca haya dos guías ni dos pagos para la misma clave.
     */
    @Override
    public EncomiendaResponse registrar(EncomiendaRequest request) {
        String clave = normalizarClaveIdempotencia(request.getClaveIdempotencia());
        if (clave != null) {
            EncomiendaResponse previa = buscarPorClave(clave);
            if (previa != null) {
                log.info("Registro presencial idempotente: clave {} ya corresponde a {}",
                        clave, previa.getNumeroGuia());
                return previa;
            }
        }

        try {
            return transactionTemplate.execute(estado -> registrarEnTransaccion(request));
        } catch (DataIntegrityViolationException carrera) {
            // La otra petición ganó la carrera y ya confirmed su registro.
            EncomiendaResponse ganadora = buscarPorClave(clave);
            if (ganadora != null) {
                log.info("Carrera de idempotencia resuelta: la clave {} ya corresponde a {}",
                        clave, ganadora.getNumeroGuia());
                return ganadora;
            }
            throw carrera;
        }
    }

    /** Lee la encomienda de una clave en su propia transacción y la devuelve mapeada. */
    private EncomiendaResponse buscarPorClave(String claveIdempotencia) {
        if (claveIdempotencia == null) return null;
        return transactionTemplate.execute(estado -> {
            Encomienda existente = encomiendaRepository.findByClaveIdempotencia(claveIdempotencia).orElse(null);
            return existente == null ? null : toResponse(existente);
        });
    }

    private EncomiendaResponse registrarEnTransaccion(EncomiendaRequest request) {
        if (request.getRemitenteId().equals(request.getDestinatarioId()))
            throw new NegocioException("El remitente y destinatario no pueden ser la misma persona");

        Cliente remitente    = clienteRepository.findById(request.getRemitenteId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Remitente", request.getRemitenteId()));
        Cliente destinatario = clienteRepository.findById(request.getDestinatarioId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Destinatario", request.getDestinatarioId()));
        Sucursal origen      = sucursalRepository.findById(request.getSucursalOrigenId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Sucursal origen", request.getSucursalOrigenId()));
        Sucursal destino     = sucursalRepository.findById(request.getSucursalDestinoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Sucursal destino", request.getSucursalDestinoId()));

        if (origen.getId().equals(destino.getId()))
            throw new NegocioException("La sucursal de origen y destino no pueden ser iguales");

        Viaje viaje = null;
        if (request.getViajeId() != null) {
            viaje = viajeRepository.findById(request.getViajeId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Viaje", request.getViajeId()));
        }

        Usuario cajero = obtenerUsuarioActual();

        // Idempotencia: si el panel reenvía la misma clave, se devuelve la
        // encomienda ya creada en lugar de duplicar guía y pago.
        Encomienda previa = reutilizarRegistroPrevio(request.getClaveIdempotencia());
        if (previa != null) {
            log.info("Registro presencial idempotente: clave {} ya corresponde a {}",
                    request.getClaveIdempotencia(), previa.getNumeroGuia());
            return toResponse(previa);
        }

        // El registro presencial NACE con el paquete físicamente en ventanilla: se
        // crea la encomienda en estado "sin datos oficiales" y a continuación se
        // confirma la recepción con la MISMA operación que convierte una solicitud
        // web. Así ambos caminos dejan exactamente el mismo resultado oficial.
        Encomienda encomienda = Encomienda.builder()
                .remitente(remitente)
                .destinatario(destinatario)
                .remitenteTelefono(remitente.getTelefono())
                .sucursalOrigen(origen)
                .sucursalDestino(destino)
                .viaje(viaje)
                .descripcionContenido(request.getDescripcionContenido())
                .referenciaSolicitud(generarReferenciaSolicitud())
                .claveIdempotencia(normalizarClaveIdempotencia(request.getClaveIdempotencia()))
                .estado(EstadoEncomienda.SOLICITUD_PENDIENTE)
                .cajeroRegistro(cajero)
                .activo(true)
                .observaciones(componerObservaciones(request.getTipoEnvio(),
                        request.getCuidadoEspecial(), request.getObservaciones()))
                .build();

        Encomienda guardada = encomiendaRepository.save(encomienda);
        log.info("Encomienda presencial {} creada como {}; se confirma recepción",
                guardada.getId(), guardada.getReferenciaSolicitud());

        return confirmarRecepcionOperativa(guardada,
                request.getPesoKg(), request.getLargoCm(), request.getAnchoCm(), request.getAltoCm(),
                request.getValorDeclarado(), request.getTarifaOficial(), request.getMetodoPago(),
                request.getObservaciones(), cajero, "REGISTRO_ENCOMIENDA_PRESENCIAL");
    }

    /**
     * Idempotencia del registro presencial: si el panel reenvía la misma clave
     * (doble clic, reintento de red o doble pestaña) se devuelve la encomienda ya
     * creada en lugar de generar una segunda con otra guía y otro pago.
     */
    private Encomienda reutilizarRegistroPrevio(String claveIdempotencia) {
        String clave = normalizarClaveIdempotencia(claveIdempotencia);
        if (clave == null) return null;
        return encomiendaRepository.findByClaveIdempotencia(clave).orElse(null);
    }

    private String normalizarClaveIdempotencia(String clave) {
        if (clave == null || clave.isBlank()) return null;
        String limpia = clave.trim();
        return limpia.length() > 60 ? limpia.substring(0, 60) : limpia;
    }

    // ── Solicitud desde portal cliente ────────────────────────────────────────

    @Override
    @Transactional
    public EncomiendaResponse solicitarEncomienda(EncomiendaClienteRequest request, String username) {
        // El remitente se deduce del usuario autenticado — usa findByUsuarioUsername que sí existe
        Cliente remitente = clienteRepository.findByUsuarioUsername(username)
                .orElseThrow(() -> new NegocioException(
                        "No tienes un perfil de cliente activo. Completa tu registro primero."));

        Sucursal origen  = sucursalRepository.findById(request.getSucursalOrigenId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Sucursal origen", request.getSucursalOrigenId()));
        Sucursal destino = sucursalRepository.findById(request.getSucursalDestinoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Sucursal destino", request.getSucursalDestinoId()));

        if (origen.getId().equals(destino.getId()))
            throw new NegocioException("El origen y destino no pueden ser iguales");

        // AUTORIDAD DE LA SOLICITUD: el cliente declara solo origen, destino,
        // remitente, destinatario y descripcion. La fila nace SIN guía oficial,
        // SIN peso, SIN medidas, SIN volumen, SIN tarifa, SIN método de pago y
        // SIN pago. La base de datos lo garantiza con la constraint
        // chk_encomienda_solicitud_sin_datos_oficiales.
        // El destinatario de una encomienda solicitada desde el portal web no es un
        // Cliente del sistema (el portal sólo pide nombre y teléfono), por lo que se
        // registra su nombre en texto plano. Antes se guardaba al REMITENTE en el campo
        // destinatario, lo que hacía que el reporte mostrara siempre el emisor.
        String nombreDestinatario = request.getDestinatarioNombre().trim();

        Encomienda encomienda = Encomienda.builder()
                .numeroGuia(null)
                .pago(null)
                .remitente(remitente)
                .destinatario(null)
                .destinatarioNombre(nombreDestinatario)
                .remitenteTelefono(request.getRemitenteTelefono().trim())
                .sucursalOrigen(origen)
                .sucursalDestino(destino)
                .descripcionContenido(request.getDescripcionContenido())
                .pesoKg(null)
                .largoCm(null)
                .anchoCm(null)
                .altoCm(null)
                .volumenM3(null)
                .costo(null)
                .metodoPago(null)
                .estado(EstadoEncomienda.SOLICITUD_PENDIENTE)
                .activo(true)
                .observaciones(componerObservaciones(request.getTipoEnvio(),
                        request.getCuidadoEspecial(),
                        agregarContactoDestinatario(request.getObservaciones(),
                                request.getDestinatarioTelefono())))
                .build();

        // La referencia SOL identifica la solicitud ante el personal autorizado.
        // No es una guía y no habilita el tracking público. Se genera ANTES del
        // INSERT porque chk_encomienda_solicitud_sin_datos_oficiales exige que la
        // referencia exista desde que la solicitud nace; por eso usa la secuencia
        // de la base y no el id (que aún no está asignado).
        encomienda.setReferenciaSolicitud(generarReferenciaSolicitud());

        Encomienda guardada = encomiendaRepository.save(encomienda);
        String referencia = guardada.getReferenciaSolicitud();

        log.info("Solicitud {} registrada por cliente {}", referencia, username);
        registrarAuditoria("SOLICITAR_ENCOMIENDA", guardada.getId(),
                referencia + " " + origen.getNombre() + " -> " + destino.getNombre());
        return toResponse(guardada);
    }

    // ── Confirmación de recepción (personal autorizado) ───────────────────────

    @Override
    @Transactional
    public EncomiendaResponse confirmarRecepcion(Long id, ConfirmarRecepcionEncomiendaRequest request) {
        // Bloqueo pesimista: dos peticiones simultáneas sobre la misma solicitud se
        // serializan, de modo que no puedan crearse dos pagos, dos guías ni dos
        // movimientos iniciales.
        Encomienda encomienda = encomiendaRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Encomienda", id));

        if (encomienda.getEstado() != EstadoEncomienda.SOLICITUD_PENDIENTE) {
            // Idempotencia: si la conversión YA se completó (doble clic, reintento o
            // doble pestaña) se devuelve el estado oficial confirmado en lugar de
            // duplicar el pago, la guía o el movimiento.
            if (encomienda.getNumeroGuia() != null && encomienda.getPago() != null) {
                log.info("Confirmación idempotente: encomienda {} ya oficializada como {}",
                        id, encomienda.getNumeroGuia());
                return toResponse(encomienda);
            }
            throw new NegocioException(
                    "Solo una solicitud pendiente puede confirmar su recepción. Estado actual: "
                            + encomienda.getEstado());
        }

        Usuario operador = obtenerUsuarioActual();
        return confirmarRecepcionOperativa(encomienda,
                request.getPesoKg(), request.getLargoCm(), request.getAnchoCm(), request.getAltoCm(),
                request.getValorDeclarado(), request.getTarifaOficial(), request.getMetodoPago(),
                request.getObservaciones(), operador, "CONFIRMAR_RECEPCION_ENCOMIENDA");
    }

    /**
     * ÚNICA operación que convierte una solicitud en encomienda oficial.
     *
     * La comparten el registro presencial en ventanilla y la confirmación de
     * recepción de una solicitud web, de modo que ambos caminos produzcan
     * exactamente el mismo resultado: peso real, medidas, volumen calculado por
     * el backend, tarifa validada, un único pago completado, una única guía GUI
     * y un único movimiento inicial, todo dentro de la misma transacción.
     */
    private EncomiendaResponse confirmarRecepcionOperativa(Encomienda encomienda,
                                                           BigDecimal pesoKg,
                                                           BigDecimal largoCm,
                                                           BigDecimal anchoCm,
                                                           BigDecimal altoCm,
                                                           BigDecimal valorDeclarado,
                                                           BigDecimal tarifaOficial,
                                                           MetodoPago metodoPago,
                                                           String observaciones,
                                                           Usuario operador,
                                                           String accionAuditoria) {

        validarDatosOperativos(encomienda, pesoKg, largoCm, anchoCm, altoCm, tarifaOficial, metodoPago);

        BigDecimal tarifa = tarifaOficial.setScale(2, RoundingMode.HALF_UP);
        BigDecimal volumen = calcularVolumenOficial(largoCm, anchoCm, altoCm);
        EstadoEncomienda estadoAnterior = encomienda.getEstado();

        // Un solo pago por encomienda, siempre COMPLETADO: el cobro es presencial
        // e inmediato al recibir el paquete.
        Pago pago = encomienda.getPago();
        if (pago == null) {
            pago = Pago.builder()
                    .cliente(encomienda.getRemitente())
                    .activo(true)
                    .build();
        }
        pago.setMonto(tarifa);
        pago.setFechaPago(LocalDateTime.now());
        pago.setMetodo(metodoPago);
        pago.setEstado(EstadoPago.COMPLETADO);
        pago.setCajero(operador);
        pago = pagoRepository.save(pago);

        encomienda.setPesoKg(pesoKg);
        encomienda.setLargoCm(largoCm);
        encomienda.setAnchoCm(anchoCm);
        encomienda.setAltoCm(altoCm);
        encomienda.setVolumenM3(volumen);
        encomienda.setValorDeclarado(valorDeclarado);
        encomienda.setCosto(tarifa);
        encomienda.setMetodoPago(metodoPago);
        encomienda.setPago(pago);
        encomienda.setEstado(EstadoEncomienda.RECIBIDO);
        if (encomienda.getCajeroRegistro() == null) {
            encomienda.setCajeroRegistro(operador);
        }
        if (encomienda.getRemitenteTelefono() == null || encomienda.getRemitenteTelefono().isBlank()) {
            encomienda.setRemitenteTelefono(encomienda.getRemitente().getTelefono());
        }

        // La guía oficial se emite SOLO aquí: con recepción física, medidas,
        // tarifa y pago ya confirmados.
        String numeroGuia = generarNumeroGuia(encomienda.getId());
        encomienda.setNumeroGuia(numeroGuia);
        Encomienda guardada = encomiendaRepository.save(encomienda);

        StringBuilder detalle = new StringBuilder()
                .append("Recepción física confirmada en ")
                .append(encomienda.getSucursalOrigen().getNombre()).append(". Peso real: ")
                .append(pesoKg.stripTrailingZeros().toPlainString()).append(" kg. Medidas: ")
                .append(largoCm.stripTrailingZeros().toPlainString()).append("x")
                .append(anchoCm.stripTrailingZeros().toPlainString()).append("x")
                .append(altoCm.stripTrailingZeros().toPlainString())
                .append(" cm (").append(volumen.stripTrailingZeros().toPlainString()).append(" m3)")
                .append(". Tarifa oficial: S/ ").append(tarifa.toPlainString())
                .append(". Pago recibido: ").append(metodoPago);
        if (observaciones != null && !observaciones.isBlank()) {
            detalle.append(". ").append(observaciones.trim());
        }

        registrarMovimiento(guardada, estadoAnterior, EstadoEncomienda.RECIBIDO,
                encomienda.getSucursalOrigen(), detalle.toString(), operador);
        registrarAuditoria(accionAuditoria, guardada.getId(),
                numeroGuia + " peso=" + pesoKg.toPlainString()
                        + " medidas=" + largoCm.toPlainString() + "x" + anchoCm.toPlainString()
                        + "x" + altoCm.toPlainString()
                        + " tarifa=" + tarifa.toPlainString()
                        + " pago=" + metodoPago);
        // La tarifa oficial queda auditada con usuario y fecha (Aut AuditoriaService).
        registrarAuditoria("FIJAR_TARIFA_ENCOMIENDA", guardada.getId(),
                numeroGuia + " tarifa oficial S/ " + tarifa.toPlainString()
                        + " fijada por " + (operador != null ? operador.getUsername() : "sistema"));

        log.info("Encomienda {} oficializada ({}), peso {} kg, tarifa S/ {}",
                numeroGuia, accionAuditoria, pesoKg.toPlainString(), tarifa.toPlainString());
        return toResponse(guardada);
    }

    /**
     * Validaciones de los datos operativos oficiales. El backend valida siempre,
     * sin confiar en lo que haya enviado el frontend.
     */
    private void validarDatosOperativos(Encomienda encomienda,
                                        BigDecimal pesoKg,
                                        BigDecimal largoCm,
                                        BigDecimal anchoCm,
                                        BigDecimal altoCm,
                                        BigDecimal tarifaOficial,
                                        MetodoPago metodoPago) {

        if (encomienda.getRemitente() == null)
            throw new NegocioException("La encomienda no tiene remitente válido");
        String nombreDest = encomienda.getDestinatario() != null
                ? encomienda.getDestinatario().getNombreCompleto()
                : encomienda.getDestinatarioNombre();
        if (nombreDest == null || nombreDest.isBlank())
            throw new NegocioException("La encomienda no tiene destinatario válido");
        if (encomienda.getSucursalOrigen() == null || encomienda.getSucursalDestino() == null)
            throw new NegocioException("La encomienda no tiene origen y destino válidos");
        if (encomienda.getSucursalOrigen().getId().equals(encomienda.getSucursalDestino().getId()))
            throw new NegocioException("El origen y destino no pueden ser iguales");
        if (encomienda.getDescripcionContenido() == null || encomienda.getDescripcionContenido().isBlank())
            throw new NegocioException("La encomienda no tiene descripción de contenido válida");

        if (pesoKg == null || pesoKg.compareTo(BigDecimal.ZERO) <= 0)
            throw new NegocioException("El peso real debe ser mayor a cero");
        if (largoCm == null || anchoCm == null || altoCm == null
                || largoCm.compareTo(BigDecimal.ZERO) <= 0
                || anchoCm.compareTo(BigDecimal.ZERO) <= 0
                || altoCm.compareTo(BigDecimal.ZERO) <= 0)
            throw new NegocioException("Las medidas del paquete (largo, ancho y alto) son obligatorias y mayores a cero");
        if (tarifaOficial == null || tarifaOficial.compareTo(BigDecimal.ZERO) <= 0)
            throw new NegocioException("La tarifa oficial debe ser mayor a cero");
        if (metodoPago == null)
            throw new NegocioException("El método de pago es obligatorio");
        if (metodoPago == MetodoPago.CONTRA_ENTREGA)
            throw new NegocioException("No se admite pago contra entrega: el pago de encomiendas es presencial e inmediato");
    }

    /** Volumen oficial en m3 calculado por el backend a partir de las medidas. */
    private BigDecimal calcularVolumenOficial(BigDecimal largoCm, BigDecimal anchoCm, BigDecimal altoCm) {
        return largoCm.multiply(anchoCm).multiply(altoCm)
                .divide(BigDecimal.valueOf(1_000_000), 4, RoundingMode.HALF_UP);
    }

    // ── Consultas ─────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public EncomiendaResponse obtenerPorId(Long id) {
        return toResponse(encomiendaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Encomienda", id)));
    }

    @Override
    @Transactional(readOnly = true)
    public EncomiendaResponse obtenerPorGuia(String numeroGuia) {
        return toResponse(encomiendaRepository.findByNumeroGuia(numeroGuia)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Encomienda con guía " + numeroGuia + " no encontrada")));
    }

    /**
     * Seguimiento público. Devuelve sólo el estado del envío: sin remitente, sin
     * documentos, sin teléfonos y sin el personal responsable de los movimientos.
     */
    @Override
    @Transactional(readOnly = true)
    public TrackingEncomiendaResponse consultarTracking(String numeroGuia) {
        // El tracking público solo acepta GUI. Una referencia SOL todavía no es
        // un envío oficial: no tiene guía, peso, tarifa ni movements.
        if (numeroGuia != null && numeroGuia.trim().toUpperCase().startsWith("SOL-")) {
            throw new NegocioException("El número ingresado es una referencia de solicitud, "
                    + "no una guía. El seguimiento se habilita cuando el personal de la agencia "
                    + "confirme la recepción y registre el envío.");
        }
        Encomienda e = encomiendaRepository.findByNumeroGuia(numeroGuia)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Encomienda con guía " + numeroGuia + " no encontrada"));
        return toTrackingResponse(e);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EncomiendaResponse> listarTodas() {
        return encomiendaRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Listado de SOLICITUDES PENDIENTES para el panel del cajero.
     * Todos los criterios son opcionales; documento y teléfono se devuelven
     * enmascarados porque son datos personales.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<SolicitudPendienteResponse> listarSolicitudesPendientes(
            String q, Long sucursalOrigenId, Long sucursalDestinoId,
            LocalDateTime desde, LocalDateTime hasta, Pageable pageable) {

        String texto = q == null || q.isBlank() ? null : q.trim();
        Page<Encomienda> pagina = encomiendaRepository.buscarSolicitudesPendientes(
                EstadoEncomienda.SOLICITUD_PENDIENTE, texto, sucursalOrigenId, sucursalDestinoId,
                desde, hasta, pageable);
        return pagina.map(this::toSolicitudPendienteResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public SolicitudPendienteResponse buscarSolicitudPorReferencia(String referenciaSolicitud) {
        if (referenciaSolicitud == null || referenciaSolicitud.isBlank())
            throw new NegocioException("Ingresa la referencia de la solicitud (SOL-AAAA-NNNNNN)");
        String ref = referenciaSolicitud.trim().toUpperCase();
        if (!ref.matches("SOL-\\d{4}-\\d{6,}"))
            throw new NegocioException("Formato de referencia inválido. Usa el formato SOL-AAAA-000000.");
        Encomienda encomienda = encomiendaRepository.findByReferenciaSolicitudIgnoreCase(ref)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe una solicitud con la referencia " + ref));
        // Una referencia ya oficializada no vuelve a ser "pendiente": tiene guía,
        // peso, tarifa y pago, y se gestiona desde la encomienda, no desde el
        // formulario de recepción.
        if (encomienda.getEstado() != EstadoEncomienda.SOLICITUD_PENDIENTE)
            throw new NegocioException("La solicitud " + ref + " ya fue recibida en agencia. "
                    + "Su guía oficial es " + encomienda.getNumeroGuia() + ".");
        return toSolicitudPendienteResponse(encomienda);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EncomiendaResponse> listarPorRemitente(Long remitenteId, Pageable pageable) {
        return encomiendaRepository.findByRemitenteIdOrderByCreatedAtDesc(remitenteId, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EncomiendaResponse> listarPorSucursalOrigen(Long sucursalId, Pageable pageable) {
        return encomiendaRepository.findBySucursalOrigenIdOrderByCreatedAtDesc(sucursalId, pageable)
                .map(this::toResponse);
    }

    // ── Portal cliente ────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<EncomiendaResponse> misEncomiendas(String username) {
        Cliente cliente = clienteRepository.findByUsuarioUsername(username)
                .orElseThrow(() -> new NegocioException(
                        "No se encontró perfil de cliente para: " + username));
        return encomiendaRepository
                .findByRemitenteIdOrderByCreatedAtDesc(cliente.getId(), Pageable.unpaged())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovimientoEncomiendaResponse> obtenerMovimientos(Long encomiendaId, String username) {
        Encomienda encomienda = encomiendaRepository.findById(encomiendaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Encomienda", encomiendaId));

        // Sólo se restringe por propiedad a los usuarios con rol CLIENTE; el
        // personal autorizado puede consultar los movimientos de cualquier encomienda.
        boolean esCliente = SecurityContextHolder.getContext().getAuthentication() != null
                && SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                    .anyMatch(a -> "ROLE_CLIENTE".equals(a.getAuthority()));
        if (esCliente) {
            // El remitente o el destinatario deben ser el titular autenticado.
            // Si cualquiera de los dos esta vacio o sin cuenta vinculada, la
            // comprobacion FALLA (fail-closed): antes se omitia en ese caso y
            // cualquier cliente del portal podia leer los movimientos de una
            // encomienda creada en ventanilla.
            boolean esRemitente = esTitular(encomienda.getRemitente(), username);
            boolean esDestinatario = esTitular(encomienda.getDestinatario(), username);
            if (!esRemitente && !esDestinatario) {
                throw new SinAutorizacionException("No tienes permiso para ver esta encomienda");
            }
        }

        return movimientoRepository
                .findByEncomiendaIdOrderByFechaHoraAsc(encomiendaId)
                .stream()
                .map(m -> MovimientoEncomiendaResponse.builder()
                        .id(m.getId())
                        .fechaHora(m.getFechaHora())
                        .sucursalActual(m.getSucursalActual() != null
                                ? m.getSucursalActual().getNombre() : "En tránsito")
                        .estadoAnterior(m.getEstadoAnterior())
                        .estadoNuevo(m.getEstadoNuevo())
                        .observacion(m.getObservacion())
                        .usuarioResponsable(m.getUsuarioResponsable() != null
                                ? m.getUsuarioResponsable().getNombres() : null)
                        .build())
                .toList();
    }

    /**
     * Indica si el cliente indicado es el titular autenticado.
     * Devuelve false si no hay cliente, si el cliente no tiene cuenta vinculada
     * o si el nombre de usuario no coincide.
     */
    private boolean esTitular(Cliente cliente, String username) {
        return cliente != null
                && cliente.getUsuario() != null
                && username != null
                && username.equals(cliente.getUsuario().getUsername());
    }

    @Override
    @Transactional
    public EncomiendaResponse actualizarEstado(Long id, ActualizarEstadoEncomiendaRequest request) {
        Encomienda encomienda = encomiendaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Encomienda", id));
        EstadoEncomienda estadoAnterior = encomienda.getEstado();
        // La recepción de una solicitud pendiente NO se hace por este endpoint
        // genérico: exige peso real y precio oficial (ver confirmarRecepcion).
        if (estadoAnterior == EstadoEncomienda.SOLICITUD_PENDIENTE)
            throw new NegocioException(
                    "La recepción de una solicitud pendiente debe confirmarse con peso real y tarifa oficial (Confirmar recepción).");
        validarTransicionEstado(estadoAnterior, request.getNuevoEstado());

        Sucursal sucursalActual = null;
        if (request.getSucursalActualId() != null) {
            sucursalActual = sucursalRepository.findById(request.getSucursalActualId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Sucursal", request.getSucursalActualId()));
        }

        encomienda.setEstado(request.getNuevoEstado());

        // Cobro contra entrega: al entregar la encomienda se completa el pago pendiente.
        if (request.getNuevoEstado() == EstadoEncomienda.ENTREGADO
                && encomienda.getPago() != null
                && encomienda.getPago().getEstado() == EstadoPago.PENDIENTE) {
            encomienda.getPago().setEstado(EstadoPago.COMPLETADO);
            pagoRepository.save(encomienda.getPago());
            log.info("Pago contra entrega completado para encomienda {}", encomienda.getNumeroGuia());
        }

        Usuario responsable = obtenerUsuarioActual();
        encomiendaRepository.save(encomienda);
        registrarMovimiento(encomienda, estadoAnterior, request.getNuevoEstado(),
                sucursalActual, request.getObservacion(), responsable);
        registrarAuditoria("CAMBIO_ESTADO_ENCOMIENDA", encomienda.getId(),
                encomienda.getNumeroGuia() + " " + estadoAnterior + " -> " + request.getNuevoEstado());
        return toResponse(encomienda);
    }

    // ── Helpers privados ──────────────────────────────────────────────────────

    private String generarNumeroGuia(Long id) {
        return String.format("GUI-%s-%06d",
                DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDateTime.now()), id);
    }

    /**
     * Referencia interna de la solicitud previa (SOL-AAAA-NNNNNN).
     * No es guía oficial: identifica la solicitud para que el personal autorizado
     * la recupere, pero NO habilita el tracking público.
     *
     * La parte numérica sale de la secuencia seq_encomienda_sol porque la
     * referencia debe existir ANTES del INSERT (la constraint de base de datos la
     * exige). El índice único uq_encomiendas_referencia_solicitud protege ante
     * cualquier colisión.
     */
    private String generarReferenciaSolicitud() {
        Long correlativo = encomiendaRepository.siguienteCorrelativoSolicitud();
        if (correlativo == null) {
            // Sin correlativo no se puede emitir una referencia válida, y la
            // referencia es obligatoria desde el INSERT (chk_encomienda_solicitud_
            // sin_datos_oficiales). Se falla de forma explícita en lugar de
            // inventar un número que rompería el formato SOL-AAAA-NNNNNN.
            throw new NegocioException(
                    "No se pudo generar la referencia de la solicitud. Reinicia el servicio e inténtalo nuevamente.");
        }
        return String.format("SOL-%s-%06d",
                DateTimeFormatter.ofPattern("yyyy").format(LocalDateTime.now()), correlativo);
    }

    /** Consigna tipo de envío, cuidado especial y observaciones del cliente. */
    private String componerObservaciones(String tipoEnvio, String cuidadoEspecial, String observaciones) {
        StringBuilder obs = new StringBuilder();
        if (tipoEnvio != null && !tipoEnvio.isBlank())
            obs.append("Tipo: ").append(tipoEnvio.trim());
        if (cuidadoEspecial != null && !cuidadoEspecial.isBlank()) {
            if (!obs.isEmpty()) obs.append(" | ");
            obs.append("Cuidado especial: ").append(cuidadoEspecial.trim());
        }
        if (observaciones != null && !observaciones.isBlank()) {
            if (!obs.isEmpty()) obs.append(" | ");
            obs.append(observaciones.trim());
        }
        return obs.isEmpty() ? null : obs.toString();
    }

    /** Añade el teléfono de contacto del destinatario a las observaciones. */
    private String agregarContactoDestinatario(String observaciones, String telefono) {
        if (telefono == null || telefono.isBlank()) return observaciones;
        return componerObservaciones(null, null, "Tel. contacto: " + telefono.trim()
                + (observaciones == null || observaciones.isBlank() ? "" : " | " + observaciones.trim()));
    }

    /**
     * Mensaje público de cada estado. El tracking es anónimo, por lo que nunca se
     * expone el detalle interno del movimiento (tarifa, medidas, personal).
     */
    private String observacionPublica(EstadoEncomienda estado) {
        if (estado == null) return null;
        return switch (estado) {
            case RECIBIDO          -> "Paquete recibido en la agencia de origen";
            case EN_ALMACEN        -> "Paquete almacenado en la agencia de origen";
            case CARGADO_EN_BUS    -> "Paquete cargado en el bus";
            case EN_TRANSITO       -> "Paquete en tránsito";
            case EN_DESTINO        -> "Paquete en la agencia de destino";
            case LLEGADO_A_DESTINO -> "Paquete llegado a la agencia de destino";
            case LISTO_ENTREGA     -> "Paquete listo para la entrega";
            case ENTREGADO         -> "Paquete entregado al destinatario";
            case DEVUELTO          -> "Paquete devuelto al remitente";
            case PERDIDO           -> "Paquete marcado como perdido";
            case SOLICITUD_PENDIENTE -> "Solicitud pendiente de recepción en agencia";
        };
    }

    /** Enmascara un documento o teléfono para mostrarlo en listados del personal. */
    private String enmascarar(String valor) {
        if (valor == null || valor.isBlank()) return null;
        String v = valor.trim();
        if (v.length() <= 4) return "****";
        return "****" + v.substring(v.length() - 4);
    }

    private void registrarMovimiento(Encomienda e, EstadoEncomienda anterior,
                                     EstadoEncomienda nuevo, Sucursal sucursal,
                                     String obs, Usuario responsable) {
        movimientoRepository.save(MovimientoEncomienda.builder()
                .encomienda(e)
                .fechaHora(LocalDateTime.now())
                .sucursalActual(sucursal)
                .estadoAnterior(anterior)
                .estadoNuevo(nuevo)
                .observacion(obs)
                .usuarioResponsable(responsable)
                .build());
    }

    private void validarTransicionEstado(EstadoEncomienda actual, EstadoEncomienda nuevo) {
        boolean valido = switch (actual) {
            // La oficialización de una solicitud (SOL -> RECIBIDO) NUNCA ocurre por
            // aquí: exige peso real, medidas, tarifa y pago, por lo que solo puede
            //arse por confirmarRecepcion. La base de datos tampoco admite una
            // solicitud con datos oficiales a medio registrar.
            case SOLICITUD_PENDIENTE -> false;
            case RECIBIDO           -> nuevo == EstadoEncomienda.EN_ALMACEN
                    || nuevo == EstadoEncomienda.CARGADO_EN_BUS
                    || nuevo == EstadoEncomienda.EN_TRANSITO;
            case EN_ALMACEN         -> nuevo == EstadoEncomienda.CARGADO_EN_BUS
                    || nuevo == EstadoEncomienda.EN_TRANSITO;
            case CARGADO_EN_BUS     -> nuevo == EstadoEncomienda.EN_TRANSITO;
            case EN_TRANSITO        -> nuevo == EstadoEncomienda.EN_DESTINO
                    || nuevo == EstadoEncomienda.LLEGADO_A_DESTINO;
            case EN_DESTINO         -> nuevo == EstadoEncomienda.LISTO_ENTREGA
                    || nuevo == EstadoEncomienda.ENTREGADO;
            case LLEGADO_A_DESTINO  -> nuevo == EstadoEncomienda.LISTO_ENTREGA
                    || nuevo == EstadoEncomienda.ENTREGADO;
            case LISTO_ENTREGA      -> nuevo == EstadoEncomienda.ENTREGADO
                    || nuevo == EstadoEncomienda.DEVUELTO;
            case ENTREGADO, DEVUELTO, PERDIDO -> false;
        };
        if (!valido)
            throw new NegocioException("Transición inválida: " + actual + " → " + nuevo);
    }

    private Usuario obtenerUsuarioActual() {
        try {
            String username = SecurityContextHolder.getContext()
                    .getAuthentication().getName();
            return usuarioRepository.findByUsername(username).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private void registrarAuditoria(String accion, Long entidadId, String detalle) {
        Usuario u = obtenerUsuarioActual();
        auditoriaService.registrar(u != null ? u.getId() : null,
                u != null ? u.getUsername() : null,
                u != null && u.getRol() != null ? u.getRol().getNombre().name() : null,
                accion, "Encomienda", entidadId, detalle);
    }

    public EncomiendaResponse toResponse(Encomienda e) {
        return EncomiendaResponse.builder()
                .id(e.getId())
                .referenciaSolicitud(e.getReferenciaSolicitud())
                .numeroGuia(e.getNumeroGuia())
                .remitente(toClienteResponse(e.getRemitente()))
                .destinatario(toClienteResponse(e.getDestinatario()))
                .destinatarioNombre(e.getDestinatario() != null
                        ? e.getDestinatario().getNombreCompleto()
                        : e.getDestinatarioNombre())
                .remitenteTelefono(e.getRemitenteTelefono())
                .sucursalOrigen(toSucursalResponse(e.getSucursalOrigen()))
                .sucursalDestino(toSucursalResponse(e.getSucursalDestino()))
                .descripcionContenido(e.getDescripcionContenido())
                .pesoKg(e.getPesoKg())
                .largoCm(e.getLargoCm())
                .anchoCm(e.getAnchoCm())
                .altoCm(e.getAltoCm())
                .volumenM3(e.getVolumenM3())
                .costo(e.getCosto())
                .metodoPago(e.getMetodoPago())
                .estado(e.getEstado())
                .codigoQr(e.getCodigoQr())
                .movimientos(e.getMovimientos().stream()
                        .map(m -> MovimientoEncomiendaResponse.builder()
                                .id(m.getId())
                                .fechaHora(m.getFechaHora())
                                .sucursalActual(m.getSucursalActual() != null
                                        ? m.getSucursalActual().getNombre() : "En tránsito")
                                .estadoAnterior(m.getEstadoAnterior())
                                .estadoNuevo(m.getEstadoNuevo())
                                .observacion(m.getObservacion())
                                .usuarioResponsable(m.getUsuarioResponsable() != null
                                        ? m.getUsuarioResponsable().getNombres() : null)
                                .build())
                        .toList())
                .createdAt(e.getCreatedAt())
                .build();
    }

    private ClienteResponse toClienteResponse(Cliente c) {
        if (c == null) return null;
        return ClienteResponse.builder()
                .id(c.getId())
                .dniRuc(c.getDniRuc())
                .nombreCompleto(c.getNombreCompleto())
                .telefono(c.getTelefono())
                .build();
    }

    /** Vista de solicitud pendiente con datos personales enmascarados. */
    private SolicitudPendienteResponse toSolicitudPendienteResponse(Encomienda e) {
        Cliente remitente = e.getRemitente();
        String tipo = null;
        String cuidado = null;
        if (e.getObservaciones() != null && !e.getObservaciones().isBlank()) {
            tipo = extraerObservacion(e.getObservaciones(), "Tipo:");
            cuidado = extraerObservacion(e.getObservaciones(), "Cuidado especial:");
        }
        StringBuilder antiguedad = new StringBuilder();
        if (e.getCreatedAt() != null) {
            long horas = java.time.Duration.between(e.getCreatedAt(), LocalDateTime.now()).toHours();
            antiguedad.append(horas < 24 ? ("hace " + horas + " h")
                    : ("hace " + (horas / 24) + " día(s)"));
        }
        return SolicitudPendienteResponse.builder()
                .id(e.getId())
                .referenciaSolicitud(e.getReferenciaSolicitud())
                .estado(e.getEstado())
                .remitenteNombre(remitente != null ? remitente.getNombreCompleto() : null)
                .remitenteDocumento(remitente != null ? enmascarar(remitente.getDniRuc()) : null)
                .remitenteTelefono(enmascarar(e.getRemitenteTelefono() != null
                        ? e.getRemitenteTelefono()
                        : (remitente != null ? remitente.getTelefono() : null)))
                .destinatarioNombre(e.getDestinatario() != null
                        ? e.getDestinatario().getNombreCompleto() : e.getDestinatarioNombre())
                .destinatarioTelefono(enmascarar(extraerObservacion(
                        e.getObservaciones(), "Tel. contacto:")))
                .sucursalOrigen(toSucursalResponse(e.getSucursalOrigen()))
                .sucursalDestino(toSucursalResponse(e.getSucursalDestino()))
                .descripcionContenido(e.getDescripcionContenido())
                .tipoEnvio(tipo)
                .cuidadoEspecial(cuidado)
                .observaciones(sinTelefono(e.getObservaciones()))
                .createdAt(e.getCreatedAt())
                .antiguedad(antiguedad.isEmpty() ? null : antiguedad.toString())
                .numeroGuia(null)
                .costo(null)
                .build();
    }

    /**
     * Elimina de las observaciones el teléfono del destinatario: ese dato se
     * expone solo en un campo dedicado y enmascarado.
     */
    private String sinTelefono(String observaciones) {
        if (observaciones == null || observaciones.isBlank()) return null;
        StringBuilder limpio = new StringBuilder();
        for (String parte : observaciones.split("\\|")) {
            String p = parte.trim();
            if (p.regionMatches(true, 0, "Tel. contacto:", 0, "Tel. contacto:".length())) continue;
            if (p.isEmpty()) continue;
            if (limpio.length() > 0) limpio.append(" | ");
            limpio.append(p);
        }
        return limpio.isEmpty() ? null : limpio.toString();
    }

    /** Extrae un campo "Prefijo: valor" de las observaciones de la encomienda. */
    private String extraerObservacion(String observaciones, String prefijo) {
        if (observaciones == null) return null;
        for (String parte : observaciones.split("\\|")) {
            String p = parte.trim();
            if (p.regionMatches(true, 0, prefijo, 0, prefijo.length())) {
                return p.substring(prefijo.length()).trim();
            }
        }
        return null;
    }

    private SucursalResponse toSucursalResponse(Sucursal s) {
        if (s == null) return null;
        return SucursalResponse.builder()
                .id(s.getId())
                .codigo(s.getCodigo())
                .nombre(s.getNombre())
                .ciudad(s.getCiudad())
                .build();
    }

    private TrackingEncomiendaResponse toTrackingResponse(Encomienda e) {
        return TrackingEncomiendaResponse.builder()
                .numeroGuia(e.getNumeroGuia())
                .estado(e.getEstado() != null ? e.getEstado().name() : null)
                .descripcionContenido(e.getDescripcionContenido())
                .pesoKg(e.getPesoKg())
                .sucursalOrigen(e.getSucursalOrigen() == null ? null
                        : TrackingEncomiendaResponse.SucursalTracking.builder()
                            .nombre(e.getSucursalOrigen().getNombre())
                            .ciudad(e.getSucursalOrigen().getCiudad())
                            .build())
                .sucursalDestino(e.getSucursalDestino() == null ? null
                        : TrackingEncomiendaResponse.SucursalTracking.builder()
                            .nombre(e.getSucursalDestino().getNombre())
                            .ciudad(e.getSucursalDestino().getCiudad())
                            .build())
                .fechaRegistro(e.getCreatedAt())
                .movimientos(e.getMovimientos().stream()
                        .map(m -> TrackingEncomiendaResponse.MovimientoTracking.builder()
                                .fechaHora(m.getFechaHora())
                                .estadoNuevo(m.getEstadoNuevo() != null ? m.getEstadoNuevo().name() : null)
                                .observacion(observacionPublica(m.getEstadoNuevo()))
                                .sucursalActual(m.getSucursalActual() != null
                                        ? m.getSucursalActual().getNombre() : "En tránsito")
                                .build())
                        .toList())
                .build();
    }
}