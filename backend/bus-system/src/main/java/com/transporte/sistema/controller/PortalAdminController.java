package com.transporte.sistema.controller;

import com.transporte.sistema.dto.request.ContenidoPortalRequest;
import com.transporte.sistema.dto.request.PublicacionPortalRequest;
import com.transporte.sistema.dto.response.ApiResponse;
import com.transporte.sistema.dto.response.ContenidoPortalResponse;
import com.transporte.sistema.dto.response.ContenidoPortalVersionResponse;
import com.transporte.sistema.dto.response.ImagenPortalResponse;
import com.transporte.sistema.service.ContenidoPortalService;
import com.transporte.sistema.service.PortalActor;
import com.transporte.sistema.service.PortalActorResolver;
import com.transporte.sistema.service.PortalImagenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Gestión del contenido editorial del Portal Público. Exclusivo ROLE_ADMIN.
 * Solo se publica lo que el administrador decide; el portal público nunca ve
 * borradores, historial ni metadatos internos.
 */
@RestController
@RequestMapping("/api/v1/portal/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class PortalAdminController {

    private final ContenidoPortalService contenidoService;
    private final PortalImagenService imagenService;
    private final PortalActorResolver actorResolver;

    @GetMapping("/secciones")
    public ResponseEntity<ApiResponse<List<ContenidoPortalResponse>>> listar() {
        return ResponseEntity.ok(ApiResponse.ok("Secciones editoriales", contenidoService.listarSecciones()));
    }

    @GetMapping("/secciones/{seccion}")
    public ResponseEntity<ApiResponse<ContenidoPortalResponse>> obtener(@PathVariable String seccion) {
        return ResponseEntity.ok(ApiResponse.ok("Sección editorial", contenidoService.obtener(norm(seccion))));
    }

    @PutMapping("/secciones/{seccion}/borrador")
    public ResponseEntity<ApiResponse<ContenidoPortalResponse>> guardarBorrador(
            @PathVariable String seccion,
            @Valid @RequestBody ContenidoPortalRequest request,
            Authentication auth) {
        PortalActor actor = actorResolver.resolver(auth);
        ContenidoPortalResponse r = contenidoService.guardarBorrador(
                norm(seccion), request.getContenido(), request.getAutor(), request.getObservacion(), actor);
        return ResponseEntity.ok(ApiResponse.ok("Borrador guardado", r));
    }

    @PostMapping("/secciones/{seccion}/publicar")
    public ResponseEntity<ApiResponse<ContenidoPortalResponse>> publicar(
            @PathVariable String seccion,
            @RequestBody(required = false) PublicacionPortalRequest request,
            Authentication auth) {
        String obs = request == null ? null : request.getObservacion();
        ContenidoPortalResponse r = contenidoService.publicar(norm(seccion), obs, actorResolver.resolver(auth));
        return ResponseEntity.ok(ApiResponse.ok("Sección publicada", r));
    }

    @PostMapping("/secciones/{seccion}/archivar")
    public ResponseEntity<ApiResponse<ContenidoPortalResponse>> archivar(
            @PathVariable String seccion,
            @RequestBody(required = false) PublicacionPortalRequest request,
            Authentication auth) {
        String obs = request == null ? null : request.getObservacion();
        ContenidoPortalResponse r = contenidoService.archivar(norm(seccion), obs, actorResolver.resolver(auth));
        return ResponseEntity.ok(ApiResponse.ok("Sección archivada", r));
    }

    @PostMapping("/secciones/{seccion}/restaurar")
    public ResponseEntity<ApiResponse<ContenidoPortalResponse>> restaurar(
            @PathVariable String seccion,
            @RequestParam Long versionId,
            @RequestBody(required = false) PublicacionPortalRequest request,
            Authentication auth) {
        String obs = request == null ? null : request.getObservacion();
        ContenidoPortalResponse r = contenidoService.restaurar(
                norm(seccion), versionId, obs, actorResolver.resolver(auth));
        return ResponseEntity.ok(ApiResponse.ok("Versión restaurada", r));
    }

    @GetMapping("/secciones/{seccion}/historial")
    public ResponseEntity<ApiResponse<List<ContenidoPortalVersionResponse>>> historial(@PathVariable String seccion) {
        return ResponseEntity.ok(ApiResponse.ok("Historial editorial", contenidoService.historial(norm(seccion))));
    }

    @GetMapping("/imagenes")
    public ResponseEntity<ApiResponse<List<ImagenPortalResponse>>> imagenes() {
        return ResponseEntity.ok(ApiResponse.ok("Imágenes registradas", imagenService.listar()));
    }

    @GetMapping("/almacenamiento")
    public ResponseEntity<ApiResponse<Map<String, Object>>> almacenamiento() {
        return ResponseEntity.ok(ApiResponse.ok("Estado de almacenamiento", Map.of(
                "disponible", imagenService.almacenamientoDisponible(),
                "tipo", imagenService.tipoAlmacenamiento(),
                "persistente", false)));
    }

    @PostMapping(value = "/imagenes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ImagenPortalResponse>> subirImagen(
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam(required = false) String seccion,
            @RequestParam String textoAlt,
            @RequestParam String autor,
            @RequestParam String fuente,
            @RequestParam String licencia,
            @RequestParam(required = false) String urlOrigen,
            @RequestParam(defaultValue = "false") boolean autorizacionComercial,
            Authentication auth) {
        ImagenPortalResponse r = imagenService.subir(archivo, normOrNull(seccion), textoAlt, autor,
                fuente, licencia, urlOrigen, autorizacionComercial, actorResolver.resolver(auth));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Imagen registrada", r));
    }

    private String norm(String seccion) {
        return seccion == null ? null : seccion.trim().toUpperCase(Locale.ROOT);
    }

    private String normOrNull(String seccion) {
        return (seccion == null || seccion.isBlank()) ? null : norm(seccion);
    }
}
