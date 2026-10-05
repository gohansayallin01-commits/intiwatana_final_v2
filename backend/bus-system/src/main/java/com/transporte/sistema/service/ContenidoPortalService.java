package com.transporte.sistema.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.transporte.sistema.dto.response.ContenidoPortalResponse;
import com.transporte.sistema.dto.response.ContenidoPortalVersionResponse;

import java.util.List;
import java.util.Map;

public interface ContenidoPortalService {

    List<ContenidoPortalResponse> listarSecciones();

    ContenidoPortalResponse obtener(String seccion);

    ContenidoPortalResponse guardarBorrador(String seccion, JsonNode contenido, String autor,
                                            String observacion, PortalActor actor);

    ContenidoPortalResponse publicar(String seccion, String observacion, PortalActor actor);

    ContenidoPortalResponse archivar(String seccion, String observacion, PortalActor actor);

    ContenidoPortalResponse restaurar(String seccion, Long versionId, String observacion, PortalActor actor);

    List<ContenidoPortalVersionResponse> historial(String seccion);

    /** Contenido vivo que ve el Portal Público: solo secciones PUBLICADO. */
    Map<String, JsonNode> contenidoPublico();
}
