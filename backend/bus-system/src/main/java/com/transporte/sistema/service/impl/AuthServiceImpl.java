package com.transporte.sistema.service.impl;

import com.transporte.sistema.dto.request.LoginRequest;
import com.transporte.sistema.dto.response.LoginResponse;
import com.transporte.sistema.entity.Cliente;
import com.transporte.sistema.entity.Usuario;
import com.transporte.sistema.exception.DemasiadosIntentosException;
import com.transporte.sistema.repository.ClienteRepository;
import com.transporte.sistema.repository.UsuarioRepository;
import com.transporte.sistema.security.JwtUtil;
import com.transporte.sistema.security.LoginRateLimiter;
import com.transporte.sistema.security.TokenRevocationService;
import com.transporte.sistema.service.AuditoriaService;
import com.transporte.sistema.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Servicio de autenticación: valida credenciales y genera JWT.
 * Incluye clienteId en la respuesta si el usuario es un cliente.
 *
 * Endurecimiento H-06: limite de intentos, logout con revocacion efectiva y
 * renovacion de token de acceso por refresh token rotativo.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final AuditoriaService auditoriaService;
    private final LoginRateLimiter loginRateLimiter;
    private final TokenRevocationService tokenRevocationService;
    private final UserDetailsService userDetailsService;

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request, String ipOrigen) {
        String claveUsuario = LoginRateLimiter.claveUsuario(request.getUsername());
        String claveIp = LoginRateLimiter.claveIp(ipOrigen);

        // H-06: antes de tocar la BCrypt (que es costosa a proposito) se descarta
        // la peticion si la cuenta o la IP ya agotaron su ventana de intentos.
        if (!loginRateLimiter.permitido(claveUsuario, claveIp)) {
            log.warn("Login bloqueado por rate limiting (usuario={}, ip={})",
                    request.getUsername(), ipOrigen);
            throw new DemasiadosIntentosException(
                    "Demasiados intentos fallidos. Intenta nuevamente en unos minutos.");
        }

        Authentication auth;
        try {
            // Delegar la autenticación a Spring Security (valida password con BCrypt)
            auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(), request.getPassword()));
        } catch (AuthenticationException e) {
            loginRateLimiter.registrarFallo(claveUsuario, claveIp);
            throw e;
        }

        // Login correcto: el contador de ese usuario se limpia.
        loginRateLimiter.registrarExito(claveUsuario);

        UserDetails userDetails = (UserDetails) auth.getPrincipal();
        return construirLoginResponse(userDetails, request.getUsername(), ipOrigen, "LOGIN",
                "Inicio de sesión exitoso");
    }

    @Override
    @Transactional
    public String logout(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        final String username;
        final String jti;
        final Date expiracion;
        try {
            // Solo un token de acceso vigente y no revocado puede cerrar sesion.
            if (!jwtUtil.esTokenAccesoVigente(token)) {
                return null;
            }
            username = jwtUtil.extraerUsername(token);
            jti = jwtUtil.extraerJti(token);
            expiracion = jwtUtil.extraerExpiracion(token);
        } catch (RuntimeException e) {
            log.warn("Logout con token ilegible: {}", e.getMessage());
            return null;
        }

        // Si ya estaba revocado, la sesion se cerro antes: se confirma al cliente
        // (idempotencia) pero NO se duplica la auditoria de la misma sesion.
        // Se consulta ANTES de revocar, o la comprobacion siempre daria true.
        boolean yaRevocado = tokenRevocationService.estaRevocado(jti);

        tokenRevocationService.revocar(jti, expiracion.toInstant());
        log.info("Logout{}: token revocado para {}",
                yaRevocado ? " (ya estaba revocado)" : "", username);

        if (yaRevocado) {
            return username;
        }

        usuarioRepository.findByUsername(username).ifPresent(u ->
                auditoriaService.registrar(u.getId(), u.getUsername(),
                        u.getRol().getNombre().name(), "LOGOUT", "Login",
                        u.getId(), "Cierre de sesión: token revocado"));

        return username;
    }

    @Override
    @Transactional
    public LoginResponse refresh(String refreshToken) {
        if (!jwtUtil.esRefreshTokenVigente(refreshToken)) {
            throw new org.springframework.security.authentication.BadCredentialsException(
                    "Refresh token inválido o vencido");
        }
        // Un refresh token ya usado no sirve de nuevo (rotacion).
        if (tokenRevocationService.estaRevocado(jwtUtil.extraerJti(refreshToken))) {
            throw new org.springframework.security.authentication.BadCredentialsException(
                    "Refresh token ya utilizado");
        }

        final String username;
        try {
            username = jwtUtil.extraerUsername(refreshToken);
        } catch (RuntimeException e) {
            throw new org.springframework.security.authentication.BadCredentialsException(
                    "Refresh token inválido");
        }

        UserDetails userDetails;
        try {
            userDetails = userDetailsService.loadUserByUsername(username);
        } catch (UsernameNotFoundException e) {
            throw new org.springframework.security.authentication.BadCredentialsException(
                    "El usuario del refresh token ya no existe");
        }

        // Se revoca el refresh token usado antes de emitir el nuevo par.
        tokenRevocationService.revocar(jwtUtil.extraerJti(refreshToken),
                jwtUtil.extraerExpiracion(refreshToken).toInstant());

        log.info("Refresh de token: {}", username);
        return construirLoginResponse(userDetails, username, null, "REFRESH",
                "Renovación de token de acceso");
    }

    /**
     * Emite el par access+refresh y arma la respuesta de login. Se comparte entre
     * login y refresh para que ambos devuelvan exactamente la misma informacion.
     */
    private LoginResponse construirLoginResponse(UserDetails userDetails, String username,
                                                 String ipOrigen, String accion, String detalle) {
        // Obtener datos completos del usuario para incluir en el token
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario autenticado no encontrado en BD"));

        // Claims adicionales en el JWT (accesibles sin consultar la BD)
        Map<String, Object> claims = new HashMap<>();
        claims.put("rol", usuario.getRol().getNombre().name());
        claims.put("usuarioId", usuario.getId());
        if (usuario.getSucursal() != null) {
            claims.put("sucursalId", usuario.getSucursal().getId());
        }

        String token = jwtUtil.generarToken(userDetails, claims);
        String refreshToken = jwtUtil.generarRefreshToken(userDetails, claims);

        // Obtener clienteId si existe (para usuarios ROLE_CLIENTE)
        Long clienteId = null;
        if ("ROLE_CLIENTE".equals(usuario.getRol().getNombre().name())) {
            Optional<Cliente> cliente = clienteRepository.findByUsuarioUsername(usuario.getUsername());
            if (cliente.isPresent()) {
                clienteId = cliente.get().getId();
            }
        }

        if ("LOGIN".equals(accion)) {
            log.info("Login exitoso: {} [{}] ip={}", usuario.getUsername(),
                    usuario.getRol().getNombre(), ipOrigen);
        }

        auditoriaService.registrar(usuario.getId(), usuario.getUsername(),
                usuario.getRol().getNombre().name(), accion, "Login",
                usuario.getId(), detalle);

        return LoginResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .tipo("Bearer")
                .usuarioId(usuario.getId())
                .username(usuario.getUsername())
                .nombreCompleto(usuario.getNombres() + " " +
                        (usuario.getApellidos() != null ? usuario.getApellidos() : ""))
                .rol(usuario.getRol().getNombre().name())
                .sucursalId(usuario.getSucursal() != null ? usuario.getSucursal().getId() : null)
                .sucursalNombre(usuario.getSucursal() != null ? usuario.getSucursal().getNombre() : null)
                .clienteId(clienteId)
                .build();
    }
}
