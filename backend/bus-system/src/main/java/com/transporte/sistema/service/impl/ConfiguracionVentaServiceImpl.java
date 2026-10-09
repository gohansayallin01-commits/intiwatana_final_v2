package com.transporte.sistema.service.impl;

import com.transporte.sistema.dto.request.ConfigurarTiempoRequest;
import com.transporte.sistema.dto.request.ConfiguracionYapeRequest;
import com.transporte.sistema.dto.response.*;
import com.transporte.sistema.entity.*;
import com.transporte.sistema.exception.NegocioException;
import com.transporte.sistema.repository.*;
import com.transporte.sistema.service.AuditoriaService;
import com.transporte.sistema.service.ConfiguracionVentaService;
import com.transporte.sistema.service.PortalImageValidator;
import com.transporte.sistema.service.PortalStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConfiguracionVentaServiceImpl implements ConfiguracionVentaService {

    public static final String RETENCION_WEB_MINUTOS = "RETENCION_WEB_MINUTOS";
    public static final String PAGO_PENDIENTE_MINUTOS = "PAGO_PENDIENTE_MINUTOS";
    public static final String CIERRE_WEB_MINUTOS = "CIERRE_WEB_MINUTOS";
    public static final String TOLERANCIA_YAPE_MINUTOS = "TOLERANCIA_YAPE_MINUTOS";

    private static final long DEFAULT_HOLD = 5;
    private static final long DEFAULT_PENDIENTE = 30;
    private static final long DEFAULT_CIERRE = 30;
    private static final long DEFAULT_TOLERANCIA = 5;

    private final ParametroVentaRepository parametroVentaRepository;
    private final ParametroVentaHistorialRepository parametroHistorialRepository;
    private final ConfiguracionYapeRepository yapeRepository;
    private final ConfiguracionYapeHistorialRepository yapeHistorialRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaService auditoriaService;
    private final PortalImageValidator validator;
    private final PortalStorageService storage;

    // ── Tiempos ──────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public long holdMinutos() {
        return leerNumero(RETENCION_WEB_MINUTOS, DEFAULT_HOLD);
    }

    @Override
    @Transactional(readOnly = true)
    public long pagoPendienteMinutos() {
        return leerNumero(PAGO_PENDIENTE_MINUTOS, DEFAULT_PENDIENTE);
    }

    @Override
    @Transactional(readOnly = true)
    public long cierreWebMinutos() {
        return leerNumero(CIERRE_WEB_MINUTOS, DEFAULT_CIERRE);
    }

    @Override
    @Transactional(readOnly = true)
    public long toleranciaYapeMinutos() {
        return leerNumero(TOLERANCIA_YAPE_MINUTOS, DEFAULT_TOLERANCIA);
    }

    private long leerNumero(String parametro, long fallback) {
        return parametroVentaRepository.findById(parametro)
                .map(p -> {
                    try {
                        return Long.parseLong(p.getValor());
                    } catch (NumberFormatException e) {
                        return fallback;
                    }
                })
                .orElse(fallback);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParametroVentaResponse> listarTiempos() {
        return parametroVentaRepository.findAll().stream()
                .sorted(java.util.Comparator.comparing(ParametroVenta::getParametro))
                .map(this::toTiempoResponse)
                .toList();
    }

    @Override
    @Transactional
    public ParametroVentaResponse actualizarTiempo(ConfigurarTiempoRequest request, String username) {
        String parametro = request.getParametro().trim().toUpperCase();
        ParametroVenta p = parametroVentaRepository.findById(parametro)
                .orElseThrow(() -> new NegocioException("Parámetro de reserva no reconocido: " + parametro));

        if (request.getValor() == null) {
            throw new NegocioException("El valor es obligatorio");
        }
        if (request.getValor() < p.getRangoMin() || request.getValor() > p.getRangoMax()) {
            throw new NegocioException("Valor fuera de rango para " + parametro
                    + ": debe estar entre " + p.getRangoMin() + " y " + p.getRangoMax() + " minutos");
        }
        if (request.getMotivo() == null || request.getMotivo().isBlank()) {
            throw new NegocioException("Debe indicar el motivo del cambio");
        }

        String anterior = p.getValor();
        String nuevo = String.valueOf(request.getValor());

        Usuario u = usuarioRepository.findByUsername(username).orElse(null);
        LocalDateTime ahora = LocalDateTime.now();

        p.setValor(nuevo);
        p.setActualizadoPor(username);
        p.setMotivo(request.getMotivo().trim());
        p.setActualizadoEn(ahora);
        parametroVentaRepository.save(p);

        parametroHistorialRepository.save(ParametroVentaHistorial.builder()
                .parametro(parametro)
                .valorAnterior(anterior)
                .valorNuevo(nuevo)
                .actualizadoPor(username)
                .rol(rolDe(u))
                .motivo(request.getMotivo().trim())
                .fecha(ahora)
                .build());

        auditoriaService.registrar(usuarioId(u), username, rolDe(u),
                "CONFIGURAR_TIEMPO_VENTA", "ParametroVenta", null,
                parametro + ": " + anterior + " -> " + nuevo + " | " + request.getMotivo().trim());
        log.info("Tiempo de reserva actualizado: {} = {} (por {})", parametro, nuevo, username);

        return toTiempoResponse(p);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParametroVentaHistorialResponse> historialTiempos(String parametro) {
        return parametroHistorialRepository
                .findTop40ByParametroOrderByFechaDesc(parametro == null ? "" : parametro.trim().toUpperCase())
                .stream()
                .map(h -> ParametroVentaHistorialResponse.builder()
                        .parametro(h.getParametro())
                        .valorAnterior(h.getValorAnterior())
                        .valorNuevo(h.getValorNuevo())
                        .actualizadoPor(h.getActualizadoPor())
                        .rol(h.getRol())
                        .motivo(h.getMotivo())
                        .fecha(h.getFecha())
                        .build())
                .toList();
    }

    // ── QR oficial Yape ─────────────────────────────────────────────────────

    @Override
    @Transactional
    public ConfiguracionYapeResponse guardarConfiguracionYape(ConfiguracionYapeRequest request,
                                                              MultipartFile imagen, String username) {
        if (request.getReceptorNombre() == null || request.getReceptorNombre().isBlank()) {
            throw new NegocioException("El nombre del receptor es obligatorio");
        }
        if (request.getNumeroVisible() == null || request.getNumeroVisible().isBlank()) {
            throw new NegocioException("El número de la subcuenta es obligatorio");
        }
        if (imagen != null && !imagen.isEmpty() && !storage.disponible()) {
            throw new NegocioException(
                    "El almacenamiento persistente de imágenes no está configurado. No es posible subir el QR.");
        }

        ConfiguracionYape anterior = yapeRepository.findFirstByActivoTrueOrderByIdDesc().orElse(null);

        ConfiguracionYape.ConfiguracionYapeBuilder b = ConfiguracionYape.builder()
                .receptorNombre(request.getReceptorNombre().trim())
                .numeroVisible(request.getNumeroVisible().trim())
                .sucursalId(request.getSucursalId())
                .sucursalNombre(request.getSucursalNombre() == null ? null : request.getSucursalNombre().trim())
                .descripcion(request.getDescripcion() == null ? null : request.getDescripcion().trim())
                .instrucciones(request.getInstrucciones() == null ? null : request.getInstrucciones().trim())
                .observaciones(request.getObservaciones() == null ? null : request.getObservaciones().trim())
                .vigenteDesde(LocalDateTime.now())
                .actualizadoPor(username)
                .activo(true);

        if (anterior != null) {
            b.imagenUrl(anterior.getImagenUrl())
                    .imagenNombreInterno(anterior.getImagenNombreInterno())
                    .imagenChecksum(anterior.getImagenChecksum())
                    .imagenPesoBytes(anterior.getImagenPesoBytes())
                    .imagenAncho(anterior.getImagenAncho())
                    .imagenAlto(anterior.getImagenAlto())
                    .esquema(anterior.getEsquema());
        }

        if (imagen != null && !imagen.isEmpty()) {
            byte[] bytes;
            try {
                bytes = imagen.getBytes();
            } catch (IOException e) {
                throw new NegocioException("No se pudo leer el archivo del QR");
            }
            PortalImageValidator.ImagenValidada v = validator.validar(bytes, imagen.getOriginalFilename());
            PortalStorageService.StoredFile guardado = storage.guardar(bytes, v.getExtension());
            b.imagenUrl(guardado.url())
                    .imagenNombreInterno(guardado.nombreInterno())
                    .imagenChecksum(v.getChecksum())
                    .imagenPesoBytes((long) bytes.length)
                    .imagenAncho(v.getAncho())
                    .imagenAlto(v.getAlto())
                    .esquema(v.getMime());
        }

        // Única configuración activa: se archiva la anterior y se inserta la nueva.
        if (anterior != null) {
            anterior.setActivo(false);
            yapeRepository.save(anterior);
        }

        ConfiguracionYape nueva = yapeRepository.save(b.build());
        LocalDateTime ahora = LocalDateTime.now();

        for (String[] campo : new String[][]{
                {"receptor_nombre", anterior == null ? null : anterior.getReceptorNombre(), nueva.getReceptorNombre()},
                {"numero_visible", anterior == null ? null : anterior.getNumeroVisible(), nueva.getNumeroVisible()},
                {"sucursal_nombre", anterior == null ? null : anterior.getSucursalNombre(), nueva.getSucursalNombre()},
                {"descripcion", anterior == null ? null : anterior.getDescripcion(), nueva.getDescripcion()},
                {"instrucciones", anterior == null ? null : anterior.getInstrucciones(), nueva.getInstrucciones()},
                {"imagen", anterior == null ? null : anterior.getImagenChecksum(), nueva.getImagenChecksum()},
        }) {
            String previo = campo[1];
            String actual = campo[2] == null ? "" : campo[2];
            if (previo == null && (actual == null || actual.isBlank())) {
                continue;
            }
            if (previo != null && previo.trim().equals(actual.trim()) && !"imagen".equals(campo[0])) {
                continue;
            }
            yapeHistorialRepository.save(ConfiguracionYapeHistorial.builder()
                    .configuracionId(nueva.getId())
                    .campo(campo[0])
                    .valorAnterior(previo)
                    .valorNuevo(actual == null || actual.isBlank() ? "—" : "***")
                    .actualizadoPor(username)
                    .rol(rolDe(usuarioRepository.findByUsername(username).orElse(null)))
                    .motivo("Configuración del QR oficial Yape")
                    .fecha(ahora)
                    .build());
        }

        Usuario u = usuarioRepository.findByUsername(username).orElse(null);
        auditoriaService.registrar(usuarioId(u), username, rolDe(u),
                "CONFIGURAR_YAPE", "ConfiguracionYape", nueva.getId(),
                "QR oficial Yape activo " + (nueva.getImagenUrl() == null ? "sin imagen" : "con imagen"));
        log.info("Configuración Yape guardada: id={} por {}", nueva.getId(), username);

        return toYapeResponse(nueva);
    }

    @Override
    @Transactional(readOnly = true)
    public ConfiguracionYapeResponse yapeActiva() {
        return yapeRepository.findFirstByActivoTrueOrderByIdDesc().map(this::toYapeResponse).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public ConfiguracionYapePublicaResponse yapeActivaPublica() {
        ConfiguracionYape c = yapeRepository.findFirstByActivoTrueOrderByIdDesc().orElse(null);
        if (c == null) {
            return ConfiguracionYapePublicaResponse.builder().activa(false).build();
        }
        String numero = c.getNumeroVisible();
        String enmascarado;
        if (numero == null || numero.isBlank()) {
            enmascarado = null;
        } else if (numero.length() <= 6) {
            enmascarado = "***";
        } else {
            enmascarado = numero.substring(0, Math.min(3, numero.length()))
                    + "****" + numero.substring(numero.length() - 2);
        }
        return ConfiguracionYapePublicaResponse.builder()
                .activa(true)
                .receptorNombre(c.getReceptorNombre())
                .numeroEnmascarado(enmascarado)
                .sucursalNombre(c.getSucursalNombre())
                .descripcion(c.getDescripcion())
                .instrucciones(c.getInstrucciones())
                .esquema(c.getEsquema())
                .imagenUrl(c.getImagenUrl())
                .vigenteHasta(c.getVigenteHasta())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConfiguracionYapeResponse> listarYapeHistorico() {
        return yapeRepository.findAllByOrderByCreatedAtDesc().stream().map(this::toYapeResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConfiguracionYapeHistorialResponse> historialYape() {
        List<ConfiguracionYapeHistorialResponse> out = new ArrayList<>();
        for (ConfiguracionYape c : yapeRepository.findAllByOrderByCreatedAtDesc()) {
            yapeHistorialRepository.findTop40ByConfiguracionIdOrderByFechaDesc(c.getId()).stream()
                    .map(h -> ConfiguracionYapeHistorialResponse.builder()
                            .configuracionId(h.getConfiguracionId())
                            .campo(h.getCampo())
                            .valorAnterior(h.getValorAnterior())
                            .valorNuevo(h.getValorNuevo())
                            .actualizadoPor(h.getActualizadoPor())
                            .rol(h.getRol())
                            .motivo(h.getMotivo())
                            .fecha(h.getFecha())
                            .build())
                    .forEach(out::add);
        }
        out.sort(java.util.Comparator.comparing(ConfiguracionYapeHistorialResponse::getFecha).reversed());
        return out;
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private ParametroVentaResponse toTiempoResponse(ParametroVenta p) {
        Integer valor = null;
        try {
            valor = Integer.parseInt(p.getValor());
        } catch (NumberFormatException ignored) {
        }
        return ParametroVentaResponse.builder()
                .parametro(p.getParametro())
                .valor(valor)
                .rangoMin(p.getRangoMin())
                .rangoMax(p.getRangoMax())
                .unidad(p.getUnidad())
                .actualizadoPor(p.getActualizadoPor())
                .motivo(p.getMotivo())
                .actualizadoEn(p.getActualizadoEn())
                .build();
    }

    private ConfiguracionYapeResponse toYapeResponse(ConfiguracionYape c) {
        return ConfiguracionYapeResponse.builder()
                .id(c.getId())
                .activo(c.getActivo())
                .receptorNombre(c.getReceptorNombre())
                .numeroVisible(c.getNumeroVisible())
                .sucursalId(c.getSucursalId())
                .sucursalNombre(c.getSucursalNombre())
                .descripcion(c.getDescripcion())
                .instrucciones(c.getInstrucciones())
                .esquema(c.getEsquema())
                .imagenUrl(c.getImagenUrl())
                .imagenPesoBytes(c.getImagenPesoBytes())
                .imagenAncho(c.getImagenAncho())
                .imagenAlto(c.getImagenAlto())
                .vigenteDesde(c.getVigenteDesde())
                .vigenteHasta(c.getVigenteHasta())
                .observaciones(c.getObservaciones())
                .actualizadoPor(c.getActualizadoPor())
                .updatedAt(c.getUpdatedAt())
                .createdAt(c.getCreatedAt())
                .build();
    }

    private Long usuarioId(Usuario u) {
        return u == null ? null : u.getId();
    }

    private String rolDe(Usuario u) {
        return u != null && u.getRol() != null && u.getRol().getNombre() != null
                ? u.getRol().getNombre().name() : null;
    }
}