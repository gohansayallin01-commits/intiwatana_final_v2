package com.transporte.sistema.service.impl;

import com.transporte.sistema.dto.response.ImagenPortalResponse;
import com.transporte.sistema.entity.ImagenPortal;
import com.transporte.sistema.exception.ConflictoException;
import com.transporte.sistema.exception.NegocioException;
import com.transporte.sistema.repository.ImagenPortalRepository;
import com.transporte.sistema.service.PortalActor;
import com.transporte.sistema.service.PortalImageValidator;
import com.transporte.sistema.service.PortalImagenService;
import com.transporte.sistema.service.PortalStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PortalImagenServiceImpl implements PortalImagenService {

    private final PortalImageValidator validator;
    private final PortalStorageService storage;
    private final ImagenPortalRepository imagenRepository;

    @Override
    @Transactional
    public ImagenPortalResponse subir(MultipartFile archivo, String seccion, String textoAlt, String autor,
                                      String fuente, String licencia, String urlOrigen,
                                      boolean autorizacionComercial, PortalActor actor) {
        if (archivo == null || archivo.isEmpty()) {
            throw new NegocioException("Debe adjuntar un archivo de imagen");
        }
        if (!almacenamientoDisponible()) {
            throw new NegocioException(
                    "El almacenamiento persistente de imágenes no está configurado. "
                            + "No es posible subir archivos en este entorno.");
        }
        if (textoAlt == null || textoAlt.isBlank()) {
            throw new NegocioException("El texto alternativo (alt) es obligatorio");
        }
        if (autor == null || autor.isBlank()) {
            throw new NegocioException("El autor de la imagen es obligatorio");
        }
        if (fuente == null || fuente.isBlank()) {
            throw new NegocioException("La fuente de la imagen es obligatoria");
        }
        if (licencia == null || licencia.isBlank()) {
            throw new NegocioException("La licencia de la imagen es obligatoria");
        }
        if (!autorizacionComercial) {
            throw new NegocioException("Debe declarar la autorización comercial de la imagen");
        }

        byte[] bytes;
        try {
            bytes = archivo.getBytes();
        } catch (IOException e) {
            throw new NegocioException("No se pudo leer el archivo");
        }

        PortalImageValidator.ImagenValidada v = validator.validar(bytes, archivo.getOriginalFilename());

        List<ImagenPortal> duplicadas = imagenRepository.findByChecksumAndActivoTrue(v.getChecksum());
        if (!duplicadas.isEmpty()) {
            throw new ConflictoException("Ya existe una imagen idéntica registrada (mismo checksum)");
        }

        PortalStorageService.StoredFile original = storage.guardar(bytes, v.getExtension());
        String miniaturaUrl = null;
        if (v.getMiniatura() != null) {
            miniaturaUrl = storage.guardar(v.getMiniatura(), v.getMiniaturaExt()).url();
        }

        ImagenPortal img = ImagenPortal.builder()
                .seccion(seccion)
                .nombreInterno(original.nombreInterno())
                .url(original.url())
                .miniaturaUrl(miniaturaUrl)
                .mime(v.getMime())
                .pesoBytes((long) bytes.length)
                .ancho(v.getAncho())
                .alto(v.getAlto())
                .checksum(v.getChecksum())
                .textoAlt(textoAlt.strip())
                .autor(autor.strip())
                .fuente(fuente.strip())
                .licencia(licencia.strip())
                .urlOrigen(urlOrigen == null ? null : urlOrigen.strip())
                .autorizacionComercial(true)
                .subidoPor(actor.username())
                .build();
        imagenRepository.save(img);
        return toResponse(img);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ImagenPortalResponse> listar() {
        return imagenRepository.findByActivoTrueOrderByIdDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public boolean almacenamientoDisponible() {
        return storage.disponible();
    }

    @Override
    public String tipoAlmacenamiento() {
        return storage.tipo();
    }

    private ImagenPortalResponse toResponse(ImagenPortal i) {
        return ImagenPortalResponse.builder()
                .id(i.getId())
                .seccion(i.getSeccion())
                .url(i.getUrl())
                .miniaturaUrl(i.getMiniaturaUrl())
                .mime(i.getMime())
                .pesoBytes(i.getPesoBytes())
                .ancho(i.getAncho())
                .alto(i.getAlto())
                .checksum(i.getChecksum())
                .textoAlt(i.getTextoAlt())
                .autor(i.getAutor())
                .fuente(i.getFuente())
                .licencia(i.getLicencia())
                .urlOrigen(i.getUrlOrigen())
                .autorizacionComercial(i.getAutorizacionComercial())
                .subidoPor(i.getSubidoPor())
                .build();
    }
}
