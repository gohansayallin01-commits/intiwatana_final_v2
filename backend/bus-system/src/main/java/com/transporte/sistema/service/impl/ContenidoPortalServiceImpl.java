package com.transporte.sistema.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.transporte.sistema.dto.response.ContenidoPortalResponse;
import com.transporte.sistema.dto.response.ContenidoPortalVersionResponse;
import com.transporte.sistema.entity.ContenidoPortal;
import com.transporte.sistema.entity.ContenidoPortalVersion;
import com.transporte.sistema.exception.ConflictoException;
import com.transporte.sistema.exception.NegocioException;
import com.transporte.sistema.exception.RecursoNoEncontradoException;
import com.transporte.sistema.repository.ContenidoPortalRepository;
import com.transporte.sistema.repository.ContenidoPortalVersionRepository;
import com.transporte.sistema.service.ContenidoPortalService;
import com.transporte.sistema.service.ContenidoPortalValidator;
import com.transporte.sistema.service.PortalActor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContenidoPortalServiceImpl implements ContenidoPortalService {

    private static final List<String> SECCIONES = List.of(
            "HERO", "DESTINOS", "BENEFICIOS", "NOSOTROS", "CONTACTO", "FOOTER");

    private final ContenidoPortalRepository contenidoRepository;
    private final ContenidoPortalVersionRepository versionRepository;
    private final ContenidoPortalValidator validator;
    private final ObjectMapper objectMapper;

    // ── Lectura ──────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public List<ContenidoPortalResponse> listarSecciones() {
        asegurarSecciones();
        return contenidoRepository.findAllByOrderBySeccionAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public ContenidoPortalResponse obtener(String seccion) {
        validarSeccion(seccion);
        asegurarSecciones();
        return toResponse(requireSeccion(seccion));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, JsonNode> contenidoPublico() {
        Map<String, JsonNode> salida = new LinkedHashMap<>();
        for (String seccion : SECCIONES) {
            contenidoRepository.findBySeccion(seccion).ifPresent(c -> {
                if ("PUBLICADO".equals(c.getEstado()) && c.getContenidoPublicado() != null) {
                    salida.put(seccion, leerJson(c.getContenidoPublicado()));
                }
            });
        }
        return salida;
    }

    // ── Escritura ────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ContenidoPortalResponse guardarBorrador(String seccion, JsonNode contenido, String autor,
                                                   String observacion, PortalActor actor) {
        validarSeccion(seccion);
        validator.validar(seccion, contenido);
        asegurarSecciones();

        ContenidoPortal c = requireSeccion(seccion);
        boolean nuevo = c.getContenidoBorrador() == null;
        c.setContenidoBorrador(escribirJson(contenido));
        if (autor != null && !autor.isBlank()) c.setAutor(autor.strip());
        c.setActualizadoPor(actor.username());
        if ("ARCHIVADO".equals(c.getEstado())) {
            c.setEstado("BORRADOR");
        }
        contenidoRepository.save(c);

        registrarHistorial(c, nuevo ? "CREAR_BORRADOR" : "ACTUALIZAR_BORRADOR",
                "BORRADOR", c.getContenidoBorrador(), actor, observacion);
        return toResponse(c);
    }

    @Override
    @Transactional
    public ContenidoPortalResponse publicar(String seccion, String observacion, PortalActor actor) {
        validarSeccion(seccion);
        ContenidoPortal c = requireSeccion(seccion);
        if (c.getContenidoBorrador() == null || c.getContenidoBorrador().isBlank()) {
            throw new NegocioException("No hay borrador para publicar en " + seccion);
        }
        validator.validar(seccion, leerJson(c.getContenidoBorrador()));
        if ("PUBLICADO".equals(c.getEstado())
                && c.getContenidoBorrador().equals(c.getContenidoPublicado())) {
            throw new ConflictoException("No hay cambios pendientes por publicar en " + seccion);
        }

        c.setContenidoPublicado(c.getContenidoBorrador());
        c.setVersion((c.getVersion() == null ? 0 : c.getVersion()) + 1);
        c.setEstado("PUBLICADO");
        c.setPublicadoEn(LocalDateTime.now());
        c.setActualizadoPor(actor.username());
        contenidoRepository.save(c);

        registrarHistorial(c, "PUBLICAR", "PUBLICADO", c.getContenidoPublicado(), actor, observacion);
        return toResponse(c);
    }

    @Override
    @Transactional
    public ContenidoPortalResponse archivar(String seccion, String observacion, PortalActor actor) {
        validarSeccion(seccion);
        ContenidoPortal c = requireSeccion(seccion);
        if ("ARCHIVADO".equals(c.getEstado())) {
            throw new ConflictoException("La sección " + seccion + " ya está archivada");
        }
        c.setEstado("ARCHIVADO");
        c.setActualizadoPor(actor.username());
        contenidoRepository.save(c);
        String snapshot = c.getContenidoPublicado() != null ? c.getContenidoPublicado() : c.getContenidoBorrador();
        registrarHistorial(c, "ARCHIVAR", "ARCHIVADO", snapshot, actor, observacion);
        return toResponse(c);
    }

    @Override
    @Transactional
    public ContenidoPortalResponse restaurar(String seccion, Long versionId, String observacion, PortalActor actor) {
        validarSeccion(seccion);
        ContenidoPortalVersion v = versionRepository.findByIdAndSeccion(versionId, seccion)
                .orElseThrow(() -> new RecursoNoEncontradoException("Versión editorial", versionId));
        ContenidoPortal c = requireSeccion(seccion);
        validator.validar(seccion, leerJson(v.getContenido()));

        if ("PUBLICADO".equals(v.getEstado())) {
            c.setContenidoBorrador(v.getContenido());
            c.setContenidoPublicado(v.getContenido());
            c.setVersion(v.getVersion());
            c.setEstado("PUBLICADO");
            c.setPublicadoEn(LocalDateTime.now());
        } else {
            c.setContenidoBorrador(v.getContenido());
            c.setEstado("BORRADOR");
        }
        c.setActualizadoPor(actor.username());
        contenidoRepository.save(c);

        registrarHistorial(c, "RESTAURAR", c.getEstado(), v.getContenido(), actor, observacion);
        return toResponse(c);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContenidoPortalVersionResponse> historial(String seccion) {
        validarSeccion(seccion);
        return versionRepository.findBySeccionOrderByVersionDescIdDesc(seccion).stream()
                .map(this::toVersionResponse)
                .toList();
    }

    // ── Apoyo ────────────────────────────────────────────────────────────────

    private void asegurarSecciones() {
        for (String seccion : SECCIONES) {
            if (contenidoRepository.findBySeccion(seccion).isEmpty()) {
                ContenidoPortal c = ContenidoPortal.builder()
                        .seccion(seccion)
                        .estado("BORRADOR")
                        .version(0)
                        .contenidoBorrador(escribirJson(seed(seccion)))
                        .actualizadoPor("sistema")
                        .build();
                contenidoRepository.save(c);
                registrarHistorial(c, "CREAR_BORRADOR", "BORRADOR", c.getContenidoBorrador(),
                        new PortalActor(null, "sistema", "SISTEMA"), "Valores iniciales (borrador)");
            }
        }
    }

    private ContenidoPortal requireSeccion(String seccion) {
        return contenidoRepository.findBySeccion(seccion)
                .orElseThrow(() -> new RecursoNoEncontradoException("Sección editorial no encontrada: " + seccion));
    }

    private void validarSeccion(String seccion) {
        if (seccion == null || !validator.seccionesValidas().contains(seccion.toUpperCase())) {
            throw new NegocioException("Sección editorial inválida");
        }
    }

    private void registrarHistorial(ContenidoPortal c, String accion, String estado,
                                    String contenido, PortalActor actor, String observacion) {
        if (contenido == null) return;
        versionRepository.save(ContenidoPortalVersion.builder()
                .contenidoId(c.getId())
                .seccion(c.getSeccion())
                .version(c.getVersion() == null ? 0 : c.getVersion())
                .estado(estado)
                .accion(accion)
                .contenido(contenido)
                .usuarioId(actor.usuarioId())
                .username(actor.username())
                .rol(actor.rol())
                .observacion(observacion)
                .fechaHora(LocalDateTime.now())
                .build());
    }

    private JsonNode leerJson(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            throw new IllegalStateException("Contenido editorial corrupto", e);
        }
    }

    private String escribirJson(JsonNode node) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo serializar el contenido", e);
        }
    }

    private ContenidoPortalResponse toResponse(ContenidoPortal c) {
        return ContenidoPortalResponse.builder()
                .id(c.getId())
                .seccion(c.getSeccion())
                .estado(c.getEstado())
                .version(c.getVersion())
                .borrador(c.getContenidoBorrador() == null ? null : leerJson(c.getContenidoBorrador()))
                .publicado(c.getContenidoPublicado() == null ? null : leerJson(c.getContenidoPublicado()))
                .autor(c.getAutor())
                .actualizadoPor(c.getActualizadoPor())
                .publicadoEn(c.getPublicadoEn())
                .actualizadoEn(c.getUpdatedAt())
                .build();
    }

    private ContenidoPortalVersionResponse toVersionResponse(ContenidoPortalVersion v) {
        return ContenidoPortalVersionResponse.builder()
                .id(v.getId())
                .seccion(v.getSeccion())
                .version(v.getVersion())
                .estado(v.getEstado())
                .accion(v.getAccion())
                .contenido(leerJson(v.getContenido()))
                .usuarioId(v.getUsuarioId())
                .username(v.getUsername())
                .rol(v.getRol())
                .observacion(v.getObservacion())
                .fechaHora(v.getFechaHora())
                .build();
    }

    // ── Valores iniciales (borrador) fieles al portal vigente ────────────────

    private JsonNode seed(String seccion) {
        return switch (seccion) {
            case "HERO" -> seedHero();
            case "DESTINOS" -> seedDestinos();
            case "BENEFICIOS" -> seedBeneficios();
            case "NOSOTROS" -> seedNosotros();
            case "CONTACTO" -> seedContacto();
            case "FOOTER" -> seedFooter();
            default -> objectMapper.createObjectNode();
        };
    }

    private ObjectNode seedHero() {
        ObjectNode n = objectMapper.createObjectNode();
        n.put("titulo", "Viaja con INTIWATANA");
        n.put("subtitulo", "Conectamos destinos con seguridad, comodidad y atención cercana.");
        n.put("imagenFondoUrl", "/assets/img/rutas/vilcas.jpg");
        n.put("imagenBusUrl", "");
        n.put("imagenAlt", "Paisaje andino de la región de Ayacucho");
        n.put("autor", "");
        n.put("fuente", "");
        n.put("licencia", "");
        n.put("atribucion", "");
        n.put("mostrarBus", true);
        return n;
    }

    private ObjectNode seedDestinos() {
        ObjectNode root = objectMapper.createObjectNode();
        ArrayNode arr = root.putArray("destinos");
        String[][] datos = {
                {"Vilcas Huaman", "/assets/img/rutas/vilcas.jpg"},
                {"Accomarca", "/assets/img/rutas/accomarca.jpeg"},
                {"Huarcas", "/assets/img/rutas/huarcas.jpeg"},
                {"Pongococha", "/assets/img/rutas/pongococha.jpeg"},
                {"Vischongo", "/assets/img/rutas/vischongo.jpeg"},
                {"Andabamba", "/assets/img/rutas/andabamba.jpeg"},
                {"Manallasacc", "/assets/img/rutas/manallasacc.jpeg"},
                {"Chiribamba", "/assets/img/rutas/chiribamba.jpeg"},
        };
        int orden = 0;
        for (String[] d : datos) {
            ObjectNode o = arr.addObject();
            o.put("titulo", d[0]);
            o.put("texto", "");
            o.put("imagenUrl", d[1]);
            o.put("imagenAlt", d[0]);
            o.put("orden", orden++);
            o.put("visible", true);
        }
        return root;
    }

    private ObjectNode seedBeneficios() {
        ObjectNode root = objectMapper.createObjectNode();
        ArrayNode arr = root.putArray("beneficios");
        Object[][] datos = {
                {"Compra organizada", "Consulta rutas, horarios y asientos antes de viajar.", "ticket"},
                {"Encomiendas con seguimiento", "Registra tu solicitud y sigue tu envío en línea.", "package"},
                {"Atención en agencia", "El personal te atiende en terminal para cada servicio.", "mappin"},
                {"Información clara", "Tarifas y condiciones confirmadas por personal autorizado.", "shield"},
        };
        int orden = 0;
        for (Object[] b : datos) {
            ObjectNode o = arr.addObject();
            o.put("titulo", (String) b[0]);
            o.put("descripcion", (String) b[1]);
            o.put("icono", (String) b[2]);
            o.put("orden", orden++);
            o.put("visible", true);
        }
        return root;
    }

    private ObjectNode seedNosotros() {
        ObjectNode n = objectMapper.createObjectNode();
        n.put("titulo", "Nosotros");
        n.put("descripcion",
                "Empresa de Transportes INTIWATANA S.R.L. es una empresa ayacuchana de transporte "
                        + "interprovincial de pasajeros y envío de encomiendas. Atendemos desde nuestro terminal "
                        + "en Av. Cusco 362, Ayacucho, y contamos con agencias en las ciudades de destino.\n\n"
                        + "Ofrecemos compra de pasajes en línea, consulta de horarios y seguimiento de encomiendas. "
                        + "Las tarifas y condiciones se confirman con el personal autorizado en agencia.");
        n.put("imagenUrl", "");
        n.put("imagenAlt", "");
        n.put("autor", "");
        n.put("fuente", "");
        n.put("licencia", "");
        return n;
    }

    private ObjectNode seedContacto() {
        ObjectNode n = objectMapper.createObjectNode();
        n.put("telefono", "");
        n.put("correo", "");
        n.put("direccion", "Av. Cusco 362, Ayacucho");
        n.put("horario", "");
        n.putArray("redes");
        n.put("visible", false);
        return n;
    }

    private ObjectNode seedFooter() {
        ObjectNode n = objectMapper.createObjectNode();
        n.put("descripcion", "Empresa de Transportes · Ayacucho, Perú");
        ArrayNode enlaces = n.putArray("enlaces");
        enlaces.addObject().put("texto", "Destinos y horarios").put("url", "/#destinos");
        enlaces.addObject().put("texto", "Rastrear encomienda").put("url", "/encomiendas/seguimiento");
        enlaces.addObject().put("texto", "Sobre nosotros").put("url", "/#nosotros");
        n.put("infoLegal", "© 2026 Empresa de Transportes INTIWATANA S.R.L. · Todos los derechos reservados");
        n.put("creditosFotograficos", "");
        return n;
    }
}
