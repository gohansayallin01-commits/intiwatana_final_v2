package com.transporte.sistema.service.impl;

import com.transporte.sistema.dto.ConfiguracionAsientos;
import com.transporte.sistema.dto.request.ViajeRequest;
import com.transporte.sistema.dto.response.*;
import com.transporte.sistema.entity.*;
import com.transporte.sistema.enums.EstadoAsiento;
import com.transporte.sistema.enums.EstadoViaje;
import com.transporte.sistema.enums.RolNombre;
import com.transporte.sistema.enums.TipoAsiento;
import com.transporte.sistema.exception.ConflictoException;
import com.transporte.sistema.exception.NegocioException;
import com.transporte.sistema.exception.RecursoNoEncontradoException;
import com.transporte.sistema.exception.SinAutorizacionException;
import com.transporte.sistema.repository.*;
import com.transporte.sistema.security.PropiedadChoferValidator;
import com.transporte.sistema.service.ConfiguracionAsientosService;
import com.transporte.sistema.service.ViajeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("DataFlowIssue")
public class ViajeServiceImpl implements ViajeService {

    private final ViajeRepository   viajeRepository;
    private final RutaRepository    rutaRepository;
    private final BusRepository     busRepository;
    private final UsuarioRepository usuarioRepository;
    private final AsientoRepository asientoRepository;
    private final BoletoRepository  boletoRepository;
    private final PropiedadChoferValidator propiedadChofer;
    private final ConfiguracionAsientosService configuracionAsientosService;

    @Override
    @Transactional
    public ViajeResponse crear(ViajeRequest request) {
        Ruta ruta = rutaRepository.findById(request.getRutaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Ruta", request.getRutaId()));
        Bus bus = busRepository.findById(request.getBusId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Bus", request.getBusId()));

        ConfiguracionAsientos configuracion = configuracionAsientosService
                .parse(bus.getConfiguracionAsientos());
        if (!configuracionAsientosService.estaCompleta(configuracion, bus.getCapacidadAsientos())) {
            throw new NegocioException("El vehiculo " + bus.getPlaca()
                    + " tiene el mapa de asientos incompleto o no configurado ("
                    + configuracionAsientosService.totalAsientos(configuracion)
                    + "/" + bus.getCapacidadAsientos()
                    + "). Configure los asientos antes de programar un viaje.");
        }

        validarDisponibilidadBus(bus.getId(),
                request.getFechaHoraSalida(),
                request.getFechaHoraLlegadaEstimada(),
                null);

        Usuario chofer = null;
        if (request.getChoferId() != null) {
            chofer = usuarioRepository.findById(request.getChoferId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Chofer", request.getChoferId()));
        }

        Viaje viaje = Viaje.builder()
                .ruta(ruta)
                .bus(bus)
                .chofer(chofer)
                .fechaHoraSalida(request.getFechaHoraSalida())
                .fechaHoraLlegadaEstimada(request.getFechaHoraLlegadaEstimada())
                .precioAdulto(request.getPrecioAdulto())
                .precioNino(request.getPrecioNino())
                .estado(EstadoViaje.PROGRAMADO)
                .observaciones(request.getObservaciones())
                .activo(true)
                .build();

        viaje = viajeRepository.save(viaje);
        generarAsientos(viaje, bus, configuracion);

        log.info("Viaje creado: id={} | {} → {} | {}",
                viaje.getId(),
                ruta.getOrigen().getCiudad(),
                ruta.getDestino().getCiudad(),
                viaje.getFechaHoraSalida());

        return toResponse(viaje);
    }

    @Override
    @Transactional
    public ViajeResponse actualizar(Long id, ViajeRequest request) {
        Viaje viaje = viajeRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Viaje", id));

        Ruta ruta = rutaRepository.findById(request.getRutaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Ruta", request.getRutaId()));
        Bus bus = busRepository.findById(request.getBusId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Bus", request.getBusId()));

        validarDisponibilidadBus(bus.getId(),
                request.getFechaHoraSalida(),
                request.getFechaHoraLlegadaEstimada(),
                id);

        Usuario chofer = null;
        if (request.getChoferId() != null) {
            chofer = usuarioRepository.findById(request.getChoferId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Chofer", request.getChoferId()));
        }

        viaje.setRuta(ruta);
        viaje.setBus(bus);
        viaje.setChofer(chofer);
        viaje.setFechaHoraSalida(request.getFechaHoraSalida());
        viaje.setFechaHoraLlegadaEstimada(request.getFechaHoraLlegadaEstimada());
        viaje.setPrecioAdulto(request.getPrecioAdulto());
        viaje.setPrecioNino(request.getPrecioNino());
        if (request.getObservaciones() != null) viaje.setObservaciones(request.getObservaciones());

        if (request.getEstado() != null && request.getEstado() != viaje.getEstado()) {
            validarTransicionEstado(viaje.getEstado(), request.getEstado());
            viaje.setEstado(request.getEstado());
            if (request.getEstado() == EstadoViaje.FINALIZADO) {
                viaje.setFechaHoraLlegadaReal(LocalDateTime.now());
            }
        }

        log.info("Viaje actualizado: id={} | estado={}", id, viaje.getEstado());
        return toResponse(viajeRepository.save(viaje));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ViajeResponse> listar() {
        return viajeRepository.findAll().stream()
                .filter(v -> !Boolean.FALSE.equals(v.getActivo()))
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ViajeResponse obtenerPorId(Long id) {
        return toResponse(viajeRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Viaje", id)));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ViajeResponse> buscarDisponibles(
            Long origenId, Long destinoId, LocalDateTime desde, LocalDateTime hasta, Pageable pageable) {
        return viajeRepository.buscarDisponibles(origenId, destinoId, desde, hasta, pageable)
                .map(this::toResponseLigero);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AsientoResponse> obtenerAsientos(Long viajeId, String username) {
        Viaje viaje = viajeRepository.findById(viajeId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Viaje", viajeId));
        // Solo se restringe al rol CHOFER; el acceso anónimo de la compra pública
        // y el de ADMIN se mantienen exactamente como estaban.
        propiedadChofer.exigirPropiedadDeViaje(username, viaje,
                "No tienes autorización para consultar los asientos de este viaje.");

        List<Asiento> asientos = asientoRepository.findByViajeId(viajeId);
        
        // Obtener nombres de pasajeros para asientos ocupados
        Map<Long, String> ocupantes = boletoRepository.findByViajeId(viajeId, Pageable.unpaged())
                .getContent().stream()
                .filter(b -> b.getEstado() != com.transporte.sistema.enums.EstadoBoleto.CANCELADO)
                .collect(Collectors.toMap(
                        b -> b.getAsiento().getId(),
                        b -> b.getCliente().getNombreCompleto(),
                        (v1, v2) -> v1
                ));

        LocalDateTime ahora = LocalDateTime.now();
        return asientos.stream()
                .map(a -> {
                    AsientoResponse resp = toAsientoResponse(a);
                    // Una retención vencida deja de bloquear comercialmente aunque el
                    // barrido de mantenimiento todavía no haya corrido (liberación perezosa).
                    if (a.getEstado() == EstadoAsiento.RESERVADO
                            && a.getRetenidoHasta() != null
                            && !a.getRetenidoHasta().isAfter(ahora)) {
                        resp.setEstado(EstadoAsiento.DISPONIBLE);
                    }
                    // Privacidad: los nombres de pasajeros sólo se exponen a usuarios
                    // autenticados (chofer/admin); la consulta pública es anónima.
                    resp.setPasajero(username == null ? null : ocupantes.get(a.getId()));
                    return resp;
                })
                .toList();
    }

    @Override
    @Transactional
    public ViajeResponse cambiarEstado(Long id, EstadoViaje nuevoEstado, Long usuarioId) {
        Viaje viaje = viajeRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Viaje", id));

        validarResponsable(viaje, usuarioId);
        validarTransicionEstado(viaje.getEstado(), nuevoEstado);

        EstadoViaje estadoAnterior = viaje.getEstado();
        viaje.setEstado(nuevoEstado);
        if (nuevoEstado == EstadoViaje.FINALIZADO && viaje.getFechaHoraLlegadaEstimada() != null) {
            viaje.setFechaHoraLlegadaReal(LocalDateTime.now());
        }
        log.info("Viaje {} cambió estado: {} → {}", id, estadoAnterior, nuevoEstado);
        return toResponse(viajeRepository.save(viaje));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ViajeResponse> listarPorChoferIdList(Long choferId) {
        return viajeRepository.findAll().stream()
                .filter(v -> v.getChofer() != null && v.getChofer().getId().equals(choferId))
                .filter(v -> !Boolean.FALSE.equals(v.getActivo()))
                .sorted(java.util.Comparator.comparing(com.transporte.sistema.entity.Viaje::getFechaHoraSalida).reversed())
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ViajeResponse> listarPorChofer(Long choferId, Pageable pageable) {
        return viajeRepository.findByChoferIdOrderByFechaHoraSalidaDesc(choferId, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Viaje viaje = viajeRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Viaje", id));

        if (viaje.getEstado() == EstadoViaje.EN_CURSO) {
            throw new NegocioException("No se puede eliminar un viaje en curso");
        }

        long boletosActivos = boletoRepository.countByViajeIdAndEstadoActivo(viaje.getId());
        if (boletosActivos > 0) {
            throw new NegocioException(
                    "No se puede eliminar el viaje porque tiene " + boletosActivos + " boleto(s) activo(s)");
        }

        viaje.softDelete();
        viajeRepository.save(viaje);
        log.info("Viaje {} eliminado (soft delete)", id);
    }

    private void validarDisponibilidadBus(Long busId, LocalDateTime inicio,
                                           LocalDateTime fin, Long excludeViajeId) {
        if (inicio == null || fin == null) return;
        if (inicio.isAfter(fin)) {
            throw new NegocioException("La fecha de salida debe ser anterior a la llegada estimada");
        }
        List<Viaje> solapados = viajeRepository.findByBusIdEnRango(busId, inicio, fin);
        solapados.removeIf(v -> v.getId().equals(excludeViajeId));
        if (!solapados.isEmpty()) {
            throw new ConflictoException(
                    "El bus ya tiene un viaje en ese horario (id: " + solapados.get(0).getId() + ")");
        }
    }

    /**
     * Un ADMIN puede operar cualquier viaje. Un CHOFER solo los que tiene asignados:
     * evita que un chofer inicio/finalice viajes de otro conductor.
     */
    private void validarResponsable(Viaje viaje, Long usuarioId) {
        if (usuarioId == null) {
            throw new NegocioException("No se pudo identificar al usuario autenticado");
        }
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario", usuarioId));

        boolean esAdmin = usuario.getRol() != null && usuario.getRol().getNombre() == RolNombre.ROLE_ADMIN;
        if (esAdmin) {
            return;
        }
        boolean asignado = viaje.getChofer() != null && viaje.getChofer().getId().equals(usuarioId);
        if (!asignado) {
            throw new SinAutorizacionException("El viaje no está asignado a tu usuario");
        }
    }

    private void validarTransicionEstado(EstadoViaje actual, EstadoViaje nuevo) {
        boolean valida = switch (actual) {
            case PROGRAMADO -> nuevo == EstadoViaje.EN_CURSO   || nuevo == EstadoViaje.CANCELADO;
            case EN_CURSO   -> nuevo == EstadoViaje.FINALIZADO || nuevo == EstadoViaje.CANCELADO;
            case FINALIZADO, CANCELADO -> false;
        };
        if (!valida) {
            throw new NegocioException(
                    "Transición de estado inválida: " + actual + " → " + nuevo);
        }
    }

    private void generarAsientos(Viaje viaje, Bus bus, ConfiguracionAsientos configuracion) {
        List<ConfiguracionAsientosService.PosicionAsiento> posiciones =
                configuracionAsientosService.generarPosiciones(configuracion);
        List<Asiento> lista = new ArrayList<>(posiciones.size());

        for (ConfiguracionAsientosService.PosicionAsiento p : posiciones) {
            lista.add(Asiento.builder()
                    .viaje(viaje)
                    .numeroAsiento(p.numeroAsiento())
                    .fila(p.fila())
                    .columna(p.columna())
                    .piso(p.piso())
                    .tipo(p.tipo())
                    .estado(EstadoAsiento.DISPONIBLE)
                    .activo(true)
                    .build());
        }
        asientoRepository.saveAll(lista);
        log.info("Generados {} asientos para viaje {} (capacidad vehiculo {})",
                lista.size(), viaje.getId(), bus.getCapacidadAsientos());
    }

    private ViajeResponse toResponse(Viaje v) {
        if (v == null) return null;
        long disponibles = 0;
        try {
            disponibles = asientoRepository.countDisponiblesByViajeId(v.getId());
        } catch (Exception e) {
            log.warn("No se pudo contar asientos del viaje {}: {}", v.getId(), e.getMessage());
        }
        Bus bus   = v.getBus();
        Ruta ruta = v.getRuta();
        ViajeResponse.ChoferInfo choferInfo = null;
        if (v.getChofer() != null) {
            Usuario ch = v.getChofer();
            choferInfo = ViajeResponse.ChoferInfo.builder()
                    .id(ch.getId())
                    .username(ch.getUsername())
                    .nombres(ch.getNombres())
                    .apellidos(ch.getApellidos())
                    .nombreCompleto(ch.getNombres() + " " + ch.getApellidos())
                    .telefono(ch.getTelefono())
                    .build();
        }
        return ViajeResponse.builder()
                .id(v.getId())
                .ruta(toRutaResponse(ruta))
                .bus(toBusResponse(bus))
                .chofer(choferInfo)
                .choferId(v.getChofer() != null ? v.getChofer().getId() : null)
                .choferNombre(v.getChofer() != null
                        ? v.getChofer().getNombres() + " " + v.getChofer().getApellidos()
                        : null)
                .fechaHoraSalida(v.getFechaHoraSalida())
                .fechaHoraLlegadaEstimada(v.getFechaHoraLlegadaEstimada())
                .precioAdulto(v.getPrecioAdulto())
                .precioNino(v.getPrecioNino())
                .estado(v.getEstado())
                .asientosDisponibles((int) disponibles)
                .totalAsientos(bus != null && bus.getCapacidadAsientos() != null
                        ? bus.getCapacidadAsientos() : 0)
                .build();
    }

    private ViajeResponse toResponseLigero(Viaje v) {
        if (v == null) return null;
        Bus bus = v.getBus();
        long disponibles = v.getAsientos() != null
                ? v.getAsientos().stream()
                    .filter(a -> EstadoAsiento.DISPONIBLE.equals(a.getEstado()))
                    .count()
                : 0;
        ViajeResponse.ChoferInfo choferInfoL = null;
        if (v.getChofer() != null) {
            Usuario chl = v.getChofer();
            choferInfoL = ViajeResponse.ChoferInfo.builder()
                    .id(chl.getId()).username(chl.getUsername())
                    .nombres(chl.getNombres()).apellidos(chl.getApellidos())
                    .nombreCompleto(chl.getNombres() + " " + chl.getApellidos()).build();
        }
        return ViajeResponse.builder()
                .id(v.getId())
                .ruta(toRutaResponse(v.getRuta()))
                .bus(toBusResponse(bus))
                .chofer(choferInfoL)
                .choferId(v.getChofer() != null ? v.getChofer().getId() : null)
                .choferNombre(v.getChofer() != null
                        ? v.getChofer().getNombres() + " " + v.getChofer().getApellidos()
                        : null)
                .fechaHoraSalida(v.getFechaHoraSalida())
                .fechaHoraLlegadaEstimada(v.getFechaHoraLlegadaEstimada())
                .precioAdulto(v.getPrecioAdulto())
                .precioNino(v.getPrecioNino())
                .estado(v.getEstado())
                .asientosDisponibles((int) disponibles)
                .totalAsientos(bus != null && bus.getCapacidadAsientos() != null
                        ? bus.getCapacidadAsientos() : 0)
                .build();
    }

    private RutaResponse toRutaResponse(Ruta r) {
        if (r == null) return null;
        return RutaResponse.builder()
                .id(r.getId())
                .codigo(r.getCodigo())
                .origen(toSucursalResponse(r.getOrigen()))
                .destino(toSucursalResponse(r.getDestino()))
                .distanciaKm(r.getDistanciaKm())
                .duracionHorasEstimada(r.getDuracionHorasEstimada())
                .precioBase(r.getPrecioBase())
                .build();
    }

    private SucursalResponse toSucursalResponse(Sucursal s) {
        if (s == null) return null;
        return SucursalResponse.builder()
                .id(s.getId())
                .codigo(s.getCodigo())
                .nombre(s.getNombre())
                .ciudad(s.getCiudad())
                .departamento(s.getDepartamento())
                .esTerminal(s.getEsTerminal())
                .build();
    }

    private BusResponse toBusResponse(Bus b) {
        if (b == null) return null;
        ConfiguracionAsientos config = configuracionAsientosService.parse(b.getConfiguracionAsientos());
        return BusResponse.builder()
                .id(b.getId())
                .placa(b.getPlaca())
                .marca(b.getMarca())
                .modelo(b.getModelo())
                .capacidadAsientos(b.getCapacidadAsientos())
                .numPisos(b.getNumPisos())
                .tipo(b.getTipo())
                .activo(b.getActivo())
                .configuracionAsientos(config)
                .asientosConfigurados(configuracionAsientosService.totalAsientos(config))
                .estadoConfiguracion(configuracionAsientosService.estado(config, b.getCapacidadAsientos()))
                .build();
    }

    private AsientoResponse toAsientoResponse(Asiento a) {
        return AsientoResponse.builder()
                .id(a.getId())
                .numeroAsiento(a.getNumeroAsiento())
                .fila(a.getFila())
                .columna(a.getColumna())
                .piso(a.getPiso())
                .tipo(a.getTipo())
                .estado(a.getEstado())
                .precio(a.getPrecio())
                .build();
    }
}