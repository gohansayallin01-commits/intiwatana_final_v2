package com.transporte.sistema.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Respuesta al login exitoso.
 * Incluye el rol para que el frontend pueda dirigir al panel correcto:
 *   ROLE_ADMIN   → AdminDashboard
 *   ROLE_CLIENTE → ClienteDashboard
 *   ROLE_CAJERO / ROLE_CHOFER → panel correspondiente (futuro)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    /** JWT Bearer token */
    private String token;

    /**
     * Token de refresco (H-06). Vive mas que {@link #token} y solo sirve para
     * renovar el token de acceso en POST /api/v1/auth/refresh. Se rota en cada
     * renovacion. Nunca debe enviarse como Authorization para usar la API.
     */
    private String refreshToken;

    /** Siempre "Bearer" */
    private String tipo;

    private Long   usuarioId;
    private String username;
    private String nombreCompleto;

    /**
     * Nombre del rol tal como está en la tabla roles:
     * "ROLE_ADMIN" | "ROLE_CAJERO" | "ROLE_CHOFER" | "ROLE_CLIENTE"
     * El frontend compara: rol.includes("CLIENTE") → ClienteDashboard
     */
    private String rol;

    /** Sucursal asignada al usuario (null si no tiene o es ROLE_CLIENTE) */
    private Long   sucursalId;
    private String sucursalNombre;

    /**
     * ID del cliente vinculado al usuario.
     * Solo tiene valor cuando rol = "ROLE_CLIENTE".
     * null para ADMIN, CAJERO, CHOFER.
     */
    private Long   clienteId;
}
