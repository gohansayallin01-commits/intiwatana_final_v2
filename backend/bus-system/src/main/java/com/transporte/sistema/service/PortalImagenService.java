package com.transporte.sistema.service;

import com.transporte.sistema.dto.response.ImagenPortalResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface PortalImagenService {

    ImagenPortalResponse subir(MultipartFile archivo, String seccion, String textoAlt, String autor,
                               String fuente, String licencia, String urlOrigen,
                               boolean autorizacionComercial, PortalActor actor);

    List<ImagenPortalResponse> listar();

    boolean almacenamientoDisponible();

    String tipoAlmacenamiento();
}
