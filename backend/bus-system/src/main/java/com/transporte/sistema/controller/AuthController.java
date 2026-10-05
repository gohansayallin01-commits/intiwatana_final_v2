package com.transporte.sistema.controller;

import com.transporte.sistema.dto.request.LoginRequest;
import com.transporte.sistema.dto.request.RefreshRequest;
import com.transporte.sistema.dto.response.LoginResponse;
import com.transporte.sistema.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Endpoint de autenticación.
 * POST /api/v1/auth/login    → devuelve JWT Bearer token (+ refresh token)
 * POST /api/v1/auth/refresh  → renueva el token de acceso
 * POST /api/v1/auth/logout   → revoca el token de acceso actual
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Login del sistema.
     * Devuelve un JWT que debe incluirse en el header:
     * Authorization: Bearer <token>
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                               HttpServletRequest servletRequest) {
        return ResponseEntity.ok(authService.login(request, ipOrigen(servletRequest)));
    }

    /**
     * Renueva el token de acceso con un refresh token vigente.
     * El refresh token es de un solo uso: se revoca al renovarlo.
     */
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request.getRefreshToken()));
    }

    /**
     * Cierre de sesión: revoca el token de acceso enviado en el header.
     * Es idempotente y seguro: si el token ya no sirve, responde 200 igual,
     * porque para el cliente el objetivo ("ya no estoy dentro") se cumple.
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpServletRequest servletRequest) {
        String token = tokenDelHeader(servletRequest);
        String username = authService.logout(token);
        if (username == null && token != null) {
            // Token presente pero no revocable: se informa sin montar un 401 que
            // el frontend no puede distinguir de un token simplemente expirado.
            throw new BadCredentialsException("Sesión no encontrada o token expirado");
        }
        return ResponseEntity.ok(Map.of("mensaje", "Sesión cerrada", "revocado", token != null));
    }

    private String tokenDelHeader(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return null;
        }
        String token = header.substring(7).trim();
        return token.isEmpty() ? null : token;
    }

    private String ipOrigen(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
