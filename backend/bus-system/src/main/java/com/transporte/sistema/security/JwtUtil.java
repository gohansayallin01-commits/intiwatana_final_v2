package com.transporte.sistema.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * Utilidad para generar y validar tokens JWT.
 * Usa JJWT 0.12+ con SecretKey firmada con HS256.
 *
 * SEGURIDAD: el secreto (app.jwt.secret) es OBLIGATORIO y se resuelve desde la
 * variable de entorno JWT_SECRET (o el .env local en desarrollo). En produccion
 * el perfil prod no define valor por defecto: si falta, la aplicacion no arranca.
 */
@Slf4j
@Component
public class JwtUtil {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    @Value("${app.jwt.refresh-expiration-ms}")
    private long jwtRefreshExpirationMs;

    /** Clave del claim que distingue un token de acceso de uno de refresco. */
    public static final String CLAIM_TIPO = "tipo";

    /** Valor del claim {@link #CLAIM_TIPO} para tokens de acceso. */
    public static final String TIPO_ACCESO = "acceso";

    /** Valor del claim {@link #CLAIM_TIPO} para tokens de refresco. */
    public static final String TIPO_REFRESCO = "refresh";

    /** Clave derivada cacheada: recalcularla en cada llamada es innecesario. */
    private SecretKey signingKey;

    @PostConstruct
    void inicializar() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    /** Devuelve la clave de firma HMAC derivada del secret en Base64 */
    private SecretKey getSigningKey() {
        return signingKey;
    }

    /**
     * Genera un token JWT con claims adicionales (rol, sucursalId).
     * Cada token lleva un jti unico: es la clave con la que el logout revoca
     * el token (ver TokenRevocationService).
     */
    public String generarToken(UserDetails userDetails, Map<String, Object> extraClaims) {
        return construir(userDetails, extraClaims, jwtExpirationMs, TIPO_ACCESO);
    }

    /** Genera token básico sin claims extra */
    public String generarToken(UserDetails userDetails) {
        return generarToken(userDetails, new HashMap<>());
    }

    /**
     * Genera un token de refresco (H-06). Vive mas que el token de acceso y solo
     * sirve para renovar un token de acceso; NO habilita por si solo ningun endpoint,
     * porque JwtAuthenticationFilter solo acepta tokens de tipo "acceso".
     */
    public String generarRefreshToken(UserDetails userDetails, Map<String, Object> extraClaims) {
        return construir(userDetails, extraClaims, jwtRefreshExpirationMs, TIPO_REFRESCO);
    }

    private String construir(UserDetails userDetails, Map<String, Object> extraClaims,
                             long expirationMs, String tipo) {
        Map<String, Object> claims = new HashMap<>(extraClaims);
        claims.put(CLAIM_TIPO, tipo);
        return Jwts.builder()
                .claims(claims)
                .subject(userDetails.getUsername())
                .id(UUID.randomUUID().toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    /** Identificador unico del token (jti), usado para revocar en el logout. */
    public String extraerJti(String token) {
        return extraerClaim(token, Claims::getId);
    }

    /** Fecha de expiracion del token. */
    public Date extraerExpiracion(String token) {
        return extraerClaim(token, Claims::getExpiration);
    }

    /**
     * @return true si el token es un token de REFRESCO vigente. Un token de acceso
     *         (o uno sin claim de tipo) devuelve false.
     */
    public boolean esRefreshTokenVigente(String token) {
        try {
            return TIPO_REFRESCO.equals(extraerClaim(token, c -> c.get(CLAIM_TIPO, String.class)))
                    && !estaExpirado(token);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Refresh token inválido: {}", e.getMessage());
            return false;
        }
    }

    /**
     * @return true si el token es un token de ACCESO vigente. Un token de refresco
     *         devuelve false: por eso un refresh token no puede usarse como
     *         Authorization para llamar a la API.
     */
    public boolean esTokenAccesoVigente(String token) {
        try {
            return TIPO_ACCESO.equals(extraerClaim(token, c -> c.get(CLAIM_TIPO, String.class)))
                    && !estaExpirado(token);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Token de acceso inválido: {}", e.getMessage());
            return false;
        }
    }

    /** Extrae el username (subject) del token */
    public String extraerUsername(String token) {
        return extraerClaim(token, Claims::getSubject);
    }

    /** Extrae cualquier claim genérico del token */
    public <T> T extraerClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = extraerTodosLosClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Valida que el token sea válido para el usuario dado.
     * Exige que sea un token de ACCESO: un refresh token nunca autentica.
     */
    public boolean esTokenValido(String token, UserDetails userDetails) {
        try {
            String username = extraerUsername(token);
            return esTokenAccesoVigente(token) && username.equals(userDetails.getUsername());
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Token JWT inválido: {}", e.getMessage());
            return false;
        }
    }

    /** Verifica si el token ha expirado */
    public boolean estaExpirado(String token) {
        return extraerClaim(token, Claims::getExpiration).before(new Date());
    }

    private Claims extraerTodosLosClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
