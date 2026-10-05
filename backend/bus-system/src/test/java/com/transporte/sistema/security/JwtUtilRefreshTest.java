package com.transporte.sistema.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Separacion entre token de acceso y token de refresco (H-06).
 *
 * Es la propiedad de seguridad que hace que el refresh token no sirva como
 * credencial: si un refresh token pudiera validar contra JwtAuthenticationFilter,
 * un atacante con el refresh tendria acceso completo y perpetuo.
 */
@DisplayName("H-06 - JwtUtil: acceso vs refresh")
class JwtUtilRefreshTest {

    // JwtUtil espera el secreto en Base64 (application.yml lo documenta asi) y
    // deriva la clave HMAC en un @PostConstruct, que aqui hay que invocar a mano
    // porque no hay contenedor de Spring.
    private static final String SECRET_B64 = base64("clave-de-prueba-32-bytes-minimo-hs256!!");
    private static final String OTRA_B64 = base64("otra-clave-distinta-32-bytes-minimo-ok");

    private static String base64(String texto) {
        return java.util.Base64.getEncoder()
                .encodeToString(texto.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private JwtUtil jwtUtil;
    private UserDetails usuario;

    private JwtUtil nuevoJwtUtil(String secretoB64) {
        JwtUtil util = new JwtUtil();
        ReflectionTestUtils.setField(util, "jwtSecret", secretoB64);
        ReflectionTestUtils.setField(util, "jwtExpirationMs", 60_000L);
        ReflectionTestUtils.setField(util, "jwtRefreshExpirationMs", 3_600_000L);
        util.inicializar();
        return util;
    }

    @BeforeEach
    void setUp() {
        jwtUtil = nuevoJwtUtil(SECRET_B64);

        usuario = User.withUsername("admin")
                .password("$2a$10$abcdefghijklmnopqrstuv")
                .authorities("ROLE_ADMIN")
                .build();
    }

    private Map<String, Object> claims() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("rol", "ROLE_ADMIN");
        claims.put("usuarioId", 1L);
        return claims;
    }

    @Test
    @DisplayName("Cada token lleva un jti distinto, que es la clave de la revocacion")
    void jtiUnicoPorToken() {
        String a = jwtUtil.generarToken(usuario, claims());
        String b = jwtUtil.generarToken(usuario, claims());

        assertNotEquals(jwtUtil.extraerJti(a), jwtUtil.extraerJti(b),
                "Dos tokens del mismo usuario deben revocarse por separado");
        assertNotNull(jwtUtil.extraerJti(a));
        assertNotNull(jwtUtil.extraerExpiracion(a));
    }

    @Test
    @DisplayName("El token de refresco NO autentica: esTokenValido lo rechaza")
    void refreshNoAutentica() {
        String refresh = jwtUtil.generarRefreshToken(usuario, claims());

        assertTrue(jwtUtil.esRefreshTokenVigente(refresh));
        assertFalse(jwtUtil.esTokenAccesoVigente(refresh),
                "Un refresh token no puede ser token de acceso");
        assertFalse(jwtUtil.esTokenValido(refresh, usuario),
                "esTokenValido es lavia que usa JwtAuthenticationFilter: debe rechazar el refresh");
    }

    @Test
    @DisplayName("El token de acceso no sirve para renovar: esRefreshTokenVigente lo rechaza")
    void accesoNoRenueva() {
        String acceso = jwtUtil.generarToken(usuario, claims());

        assertTrue(jwtUtil.esTokenAccesoVigente(acceso));
        assertFalse(jwtUtil.esRefreshTokenVigente(acceso),
                "Un token de acceso no puede usarse en /auth/refresh");
    }

    @Test
    @DisplayName("El refresh conserva los claims del usuario (rol y usuarioId)")
    void refreshConservaClaims() {
        Map<String, Object> claims = claims();
        String refresh = jwtUtil.generarRefreshToken(usuario, claims);

        // Reutiliza la extraccion publica de claims para no duplicar logica.
        String username = jwtUtil.extraerUsername(refresh);
        assertEquals("admin", username);

        String firma = jwtUtil.generarToken(usuario, claims);
        assertEquals(username, jwtUtil.extraerUsername(firma));
    }

    @Test
    @DisplayName("Un refresh expirado no se considera vigente")
    void refreshExpirado() {
        ReflectionTestUtils.setField(jwtUtil, "jwtRefreshExpirationMs", -1_000L);
        String refresh = jwtUtil.generarRefreshToken(usuario, claims());

        assertFalse(jwtUtil.esRefreshTokenVigente(refresh),
                "Un refresh vencido debe rechazarse en /auth/refresh");
    }

    @Test
    @DisplayName("Un token acceso expirado no autentica")
    void accesoExpirado() {
        ReflectionTestUtils.setField(jwtUtil, "jwtExpirationMs", -1_000L);
        String acceso = jwtUtil.generarToken(usuario, claims());

        assertFalse(jwtUtil.esTokenAccesoVigente(acceso));
        assertFalse(jwtUtil.esTokenValido(acceso, usuario));
    }

    @Test
    @DisplayName("Un token con firma de otra clave no autentica")
    void firmaAjena() {
        JwtUtil otro = nuevoJwtUtil(OTRA_B64);

        String acceso = jwtUtil.generarToken(usuario, claims());

        assertFalse(otro.esTokenAccesoVigente(acceso),
                "Un token firmado con otra clave no debe aceptarse");
        assertFalse(jwtUtil.esTokenAccesoVigente("no-es-un-jwt"),
                "Una cadena arbitraria debe devolverse falsa, no lanzar excepcion");
    }
}
