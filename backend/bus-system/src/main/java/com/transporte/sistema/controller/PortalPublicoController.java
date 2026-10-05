package com.transporte.sistema.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.transporte.sistema.dto.response.ApiResponse;
import com.transporte.sistema.service.ContenidoPortalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Endpoint público de solo lectura. Devuelve únicamente las secciones en estado
 * PUBLICADO. Si una sección no está publicada, el portal usa sus valores de
 * respaldo locales, de modo que una falla editorial nunca rompe la página.
 */
@RestController
@RequestMapping("/api/v1/portal")
@RequiredArgsConstructor
public class PortalPublicoController {

    private final ContenidoPortalService contenidoService;

    @GetMapping("/publico")
    public ResponseEntity<ApiResponse<Map<String, JsonNode>>> publico() {
        return ResponseEntity.ok(ApiResponse.ok("Contenido publicado", contenidoService.contenidoPublico()));
    }
}
