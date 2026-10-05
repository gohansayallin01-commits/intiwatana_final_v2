package com.transporte.sistema.service.impl;

import com.transporte.sistema.dto.request.BoletoRequest;
import com.transporte.sistema.dto.response.AsientoResponse;
import com.transporte.sistema.dto.response.BoletoResponse;
import com.transporte.sistema.dto.response.ClienteResponse;
import com.transporte.sistema.dto.response.ViajeResponse;
import com.transporte.sistema.entity.*;
import com.transporte.sistema.enums.EstadoAsiento;
import com.transporte.sistema.enums.EstadoBoleto;
import com.transporte.sistema.enums.EstadoPago;
import com.transporte.sistema.enums.EstadoViaje;
import com.transporte.sistema.enums.RolNombre;
import com.transporte.sistema.exception.NegocioException;
import com.transporte.sistema.exception.RecursoNoEncontradoException;
import com.transporte.sistema.repository.*;
import com.transporte.sistema.security.PropiedadChoferValidator;
import com.transporte.sistema.service.AuditoriaService;
import com.transporte.sistema.service.BoletoService;
import com.transporte.sistema.service.GeneradorDocumentos;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Servicio de boletos.
 *
 * MEJORAS vs versión anterior:
 * - Lock pesimista (PESSIMISTIC_WRITE) en el asiento antes de marcarlo vendido
 *   → evita condición de carrera cuando dos clientes compran el mismo asiento
 * - Método privado `validarYObtenerAsientoConLock` reúsa la lógica duplicada
 * - Generación de QR extraída a helper privado
 * - Log con nivel INFO sólo en operaciones de escritura exitosas
 */
@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("DataFlowIssue")
public class BoletoServiceImpl implements BoletoService {

    private final BoletoRepository    boletoRepository;
    private final ViajeRepository     viajeRepository;
    private final AsientoRepository   asientoRepository;
    private final ClienteRepository   clienteRepository;
    private final PagoRepository      pagoRepository;
    private final UsuarioRepository   usuarioRepository;
    private final EntityManager       entityManager;
    private final AuditoriaService    auditoriaService;
    private final PropiedadChoferValidator propiedadChofer;
    private final GeneradorDocumentos  generadorDocumentos;

    // ── Venta por cajero ─────────────────────────────────────────────────────

    @Override
    @Transactional
    public BoletoResponse vender(BoletoRequest request) {
        // El clienteId es obligatorio en la venta por cajero (a diferencia de la
        // compra por portal, donde el cliente se deduce del JWT).
        if (request.getClienteId() == null) {
            throw new NegocioException("Debe seleccionar un cliente para registrar la venta");
        }

        Viaje viaje = obtenerViajeActivo(request.getViajeId());

        // Lock pesimista: nadie más puede modificar este asiento hasta que terminemos
        Asiento asiento = obtenerAsientoConLock(request.getAsientoId(), viaje.getId());

        Cliente cliente = clienteRepository.findById(request.getClienteId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente", request.getClienteId()));

        Pago pago = crearPago(viaje, request, cliente);
        Usuario cajero = obtenerUsuarioActual();

        BoletoResponse resp = crearYGuardarBoleto(viaje, asiento, cliente, pago, cajero, request.getObservaciones());
        log.info("Boleto vendido: {} | viaje={} | asiento={} | cajero={}",
                resp.getNumeroBoleto(), viaje.getId(), asiento.getNumeroAsiento(),
                cajero != null ? cajero.getUsername() : "sistema");
        registrarAuditoria("VENTA_BOLETO", cliente.getId(), resp.getNumeroBoleto());
        return resp;
    }

    // ── Compra desde portal del cliente ──────────────────────────────────────

    @Override
    @Transactional
    public BoletoResponse comprarPasaje(BoletoRequest request, String username) {
        Cliente cliente = clienteRepository.findByUsuarioUsername(username)
                .orElseThrow(() -> new NegocioException(
                        "No se encontró un perfil de cliente para el usuario: " + username));

        Viaje viaje = obtenerViajeActivo(request.getViajeId());

        // Lock pesimista en el asiento
        Asiento asiento = obtenerAsientoConLock(request.getAsientoId(), viaje.getId());

        Pago pago = crearPago(viaje, request, cliente);

        BoletoResponse resp = crearYGuardarBoleto(viaje, asiento, cliente, pago, null, request.getObservaciones());
        log.info("Pasaje comprado: {} | cliente={} | viaje={} | asiento={}",
                resp.getNumeroBoleto(), username, viaje.getId(), asiento.getNumeroAsiento());
        registrarAuditoria("COMPRA_BOLETO", cliente.getId(), resp.getNumeroBoleto());
        return resp;
    }

    // ── Historial del cliente autenticado ────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<BoletoResponse> misBoletos(String username) {
        Cliente cliente = clienteRepository.findByUsuarioUsername(username)
                .orElseThrow(() -> new NegocioException(
                        "No se encontró perfil de cliente para: " + username));
        return boletoRepository
                .findByClienteIdOrderByCreatedAtDesc(cliente.getId(), Pageable.unpaged())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ── Lecturas ─────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public BoletoResponse obtenerPorId(Long id, String username) {
        Boleto boleto = boletoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Boleto", id));
        propiedadChofer.exigirPropiedadDeBoleto(username, boleto,
                "No tienes autorización para consultar este boleto.");
        return toResponse(boleto);
    }

    @Override
    @Transactional(readOnly = true)
    public BoletoResponse obtenerPorNumero(String numeroBoleto, String username) {
        Boleto boleto = boletoRepository.findByNumeroBoleto(numeroBoleto)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Boleto con número " + numeroBoleto + " no encontrado"));
        propiedadChofer.exigirPropiedadDeBoleto(username, boleto,
                "No tienes autorización para consultar este boleto.");
        return toResponse(boleto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BoletoResponse> listarPorCliente(Long clienteId, Pageable pageable) {
        return boletoRepository.findByClienteIdOrderByCreatedAtDesc(clienteId, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BoletoResponse> listarPorViaje(Long viajeId, Pageable pageable, String username) {
        Viaje viaje = viajeRepository.findById(viajeId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Viaje", viajeId));
        propiedadChofer.exigirPropiedadDeViaje(username, viaje,
                "No tienes autorización para consultar los pasajeros de este viaje.");
        return boletoRepository.findByViajeId(viajeId, pageable).map(this::toResponse);
    }

    // ── Escaneo de QR ────────────────────────────────────────────────────────

    @Override
    @Transactional
    public BoletoResponse escanearQr(String codigoQr, String username) {
        // Lock pesimista: si dos personas escanean el mismo QR a la vez, el segundo
        // espera y ve el estado ya actualizado a USADO, en lugar de validarlo dos veces.
        Boleto boleto = boletoRepository.findByNumeroBoletoForUpdate(codigoQr)
                .orElseThrow(() -> new NegocioException("QR inválido o boleto no encontrado"));
        // La propiedad se comprueba ANTES del estado: no se revela la situación de un
        // boleto ajeno al chofer que intenta escanearlo.
        propiedadChofer.exigirPropiedadDeBoleto(username, boleto,
                "No puedes validar un boleto correspondiente a otro viaje.");
        if (boleto.getEstado() != EstadoBoleto.ACTIVO) {
            throw new NegocioException("El boleto ya fue usado o está cancelado. Estado: " + boleto.getEstado());
        }
        boleto.setEstado(EstadoBoleto.USADO);
        boleto.setFechaHoraUso(LocalDateTime.now());
        log.info("Boleto usado: {} | chofer={}", codigoQr, username);
        registrarAuditoria("ESCANEO_QR", boleto.getId(), codigoQr);
        return toResponse(boletoRepository.save(boleto));
    }

    // ── Cancelación ──────────────────────────────────────────────────────────

    @Override
    @Transactional
    public BoletoResponse cancelar(Long id, String motivo) {
        Boleto boleto = boletoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Boleto", id));
        if (boleto.getEstado() == EstadoBoleto.USADO) {
            throw new NegocioException("No se puede cancelar un boleto ya utilizado");
        }
        if (boleto.getEstado() == EstadoBoleto.CANCELADO) {
            throw new NegocioException("El boleto ya está cancelado");
        }
        boleto.setEstado(EstadoBoleto.CANCELADO);
        boleto.setObservaciones(motivo);

        // Si falta más de 24 horas, marcar pago como reembolsado (política de devolución)
        if (boleto.getPago() != null && boleto.getViaje().getFechaHoraSalida().minusHours(24).isAfter(LocalDateTime.now())) {
            boleto.getPago().setEstado(EstadoPago.REEMBOLSADO);
            pagoRepository.save(boleto.getPago());
            log.info("Pago reembolsado automáticamente por cancelación anticipada (>24h)");
        }

        // Liberar el asiento
        Asiento asiento = boleto.getAsiento();
        asiento.setEstado(EstadoAsiento.DISPONIBLE);
        asientoRepository.save(asiento);

        log.info("Boleto cancelado: {} | motivo: {}", boleto.getNumeroBoleto(), motivo);
        registrarAuditoria("CANCELAR_BOLETO", boleto.getId(), boleto.getNumeroBoleto());
        return toResponse(boletoRepository.save(boleto));
    }
    @Override
    @Transactional
    public BoletoResponse cancelarPorUsuario(Long id, String motivo, String username) {
        Boleto boleto = boletoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Boleto", id));

        Usuario actual = usuarioRepository.findByUsername(username).orElse(null);
        RolNombre rol = (actual != null && actual.getRol() != null) ? actual.getRol().getNombre() : null;
        // getRol().getNombre() devuelve el enum RolNombre, no un String: hay que
        // comparar con el enum o la comprobación siempre daba false.
        boolean esPersonal = rol == RolNombre.ROLE_ADMIN
                || rol == RolNombre.ROLE_CAJERO
                || rol == RolNombre.ROLE_VENDEDOR;

        if (!esPersonal) {
            // Validar propiedad. Cliente.usuario es opcional: un cliente creado por
            // un cajero no tiene cuenta asociada, así que no se puede desreferenciar
            // sin comprobar antes que exista.
            Cliente titular = boleto.getCliente();
            boolean esDueno = titular != null
                    && titular.getUsuario() != null
                    && titular.getUsuario().getUsername().equals(username);
            if (!esDueno) {
                throw new NegocioException("No tienes permiso para cancelar este boleto");
            }

            // Validar tiempo absoluto (mínimo 30 min)
            if (boleto.getViaje().getFechaHoraSalida().minusMinutes(30).isBefore(LocalDateTime.now())) {
                throw new NegocioException("El viaje está por comenzar o ya inició. No se puede cancelar.");
            }

            // Validar si requiere justificación obligatoria (si falta menos de 24h)
            boolean faltaPoco = boleto.getViaje().getFechaHoraSalida().minusHours(24).isBefore(LocalDateTime.now());
            if (faltaPoco && (motivo == null || motivo.trim().isEmpty())) {
                throw new NegocioException("Faltan menos de 24 horas para el viaje. Debe proporcionar una justificación obligatoria.");
            }
        }

        return cancelar(id, motivo);
    }

    // ── Helpers privados ─────────────────────────────────────────────────────

    /**
     * Obtiene el viaje y valida que esté PROGRAMADO.
     */
    private Viaje obtenerViajeActivo(Long viajeId) {
        Viaje viaje = viajeRepository.findById(viajeId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Viaje", viajeId));
        if (viaje.getEstado() != EstadoViaje.PROGRAMADO) {
            throw new NegocioException("Solo se pueden comprar boletos para viajes PROGRAMADOS");
        }
        return viaje;
    }

    /**
     * Obtiene el asiento con LOCK PESIMISTA para evitar condiciones de carrera.
     * Si dos usuarios intentan comprar el mismo asiento simultáneamente,
     * el segundo esperará hasta que el primero complete la transacción.
     */
    private Asiento obtenerAsientoConLock(Long asientoId, Long viajeId) {
        // Primero obtenemos con lock pesimista
        Asiento asiento = entityManager.find(Asiento.class, asientoId, LockModeType.PESSIMISTIC_WRITE);
        if (asiento == null) {
            throw new RecursoNoEncontradoException("Asiento", asientoId);
        }
        if (!asiento.getViaje().getId().equals(viajeId)) {
            throw new NegocioException("El asiento no pertenece al viaje indicado");
        }
        if (asiento.getEstado() != EstadoAsiento.DISPONIBLE) {
            throw new NegocioException("El asiento " + asiento.getNumeroAsiento() + " ya no está disponible");
        }
        return asiento;
    }

    /**
     * Crea y persiste el objeto Pago.
     */
    private Pago crearPago(Viaje viaje, BoletoRequest request, Cliente cliente) {
        Pago pago = Pago.builder()
                .monto(viaje.getPrecioAdulto())
                .fechaPago(LocalDateTime.now())
                .metodo(request.getMetodoPago())
                .estado(EstadoPago.COMPLETADO)
                .referencia(request.getReferenciaPago())
                .cliente(cliente)
                .activo(true)
                .build();
        return pagoRepository.save(pago);
    }

    private BoletoResponse crearYGuardarBoleto(Viaje viaje, Asiento asiento,
                                                Cliente cliente, Pago pago,
                                                Usuario cajero, String observaciones) {
        Boleto boleto = Boleto.builder()
                .numeroBoleto("TEMP")
                .viaje(viaje)
                .asiento(asiento)
                .cliente(cliente)
                .precioPagado(viaje.getPrecioAdulto())
                .estado(EstadoBoleto.ACTIVO)
                .pago(pago)
                .cajero(cajero)
                .observaciones(observaciones)
                .activo(true)
                .build();

        Boleto guardado = boletoRepository.save(boleto);

        String numeroBoleto = generadorDocumentos.generarNumeroBoleto(guardado.getId());
        guardado.setNumeroBoleto(numeroBoleto);
        guardado.setCodigoQr(numeroBoleto);
        guardado.setQrImagenUrl(generadorDocumentos.generarQrBase64(numeroBoleto));

        asiento.setEstado(EstadoAsiento.VENDIDO);
        asientoRepository.save(asiento);

        return toResponse(boletoRepository.save(guardado));
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
                accion, "Boleto", entidadId, detalle);
    }

    @Override
    public BoletoResponse toResponse(Boleto b) {
        Viaje v  = b.getViaje();
        Asiento a = b.getAsiento();
        Cliente c = b.getCliente();

        return BoletoResponse.builder()
                .id(b.getId())
                .numeroBoleto(b.getNumeroBoleto())
                .viaje(ViajeResponse.builder()
                        .id(v.getId())
                        .fechaHoraSalida(v.getFechaHoraSalida())
                        .fechaHoraLlegadaEstimada(v.getFechaHoraLlegadaEstimada())
                        .precioAdulto(v.getPrecioAdulto())
                        .estado(v.getEstado())
                        .build())
                .asiento(AsientoResponse.builder()
                        .id(a.getId())
                        .numeroAsiento(a.getNumeroAsiento())
                        .fila(a.getFila())
                        .columna(a.getColumna())
                        .tipo(a.getTipo())
                        .estado(a.getEstado())
                        .build())
                .cliente(ClienteResponse.builder()
                        .id(c.getId())
                        .dniRuc(c.getDniRuc())
                        .nombreCompleto(c.getNombreCompleto())
                        .telefono(c.getTelefono())
                        .build())
                .precioPagado(b.getPrecioPagado())
                .estado(b.getEstado())
                .codigoQr(b.getCodigoQr())
                .qrImagenUrl(b.getQrImagenUrl())
                .createdAt(b.getCreatedAt())
                .fechaHoraUso(b.getFechaHoraUso())
                .build();
    }
}
