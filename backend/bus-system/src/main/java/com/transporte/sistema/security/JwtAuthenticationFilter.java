package com.transporte.sistema.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro JWT: extrae el token del header Authorization,
 * lo valida y establece el contexto de autenticación de Spring Security.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final TokenRevocationService tokenRevocationService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        // Si no hay token o no es Bearer, continuar sin autenticar
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);
        String username = null;

        try {
            username = jwtUtil.extraerUsername(jwt);
        } catch (Exception e) {
            log.warn("No se pudo extraer username del JWT: {}", e.getMessage());
        }

        // Si tenemos username y no hay autenticación activa, validar
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // loadUserByUsername puede lanzar UsernameNotFoundException si el usuario
            // fue eliminado o desactivado. Sin este try/catch la excepción escapaba
            // del filtro y abortaba la petición con un 500 en lugar de devolver 401.
            try {
                // Logout (H-06): un token revocado deja de autenticar aunque no haya
                // expirado. Se comprueba antes de tocar la base de datos.
                if (tokenRevocationService.estaRevocado(jwtUtil.extraerJti(jwt))) {
                    log.info("Token revocado por logout: {}", username);
                    SecurityContextHolder.clearContext();
                    filterChain.doFilter(request, response);
                    return;
                }

                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if (jwtUtil.esTokenValido(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            } catch (AuthenticationException e) {
                log.warn("Usuario del JWT no autenticable ({}): {}",
                        username, e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
