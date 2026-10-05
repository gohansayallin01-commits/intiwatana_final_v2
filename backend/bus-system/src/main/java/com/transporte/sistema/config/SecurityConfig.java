package com.transporte.sistema.config;

import com.transporte.sistema.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Configuración de seguridad.
 *
 * MEJORAS vs versión anterior:
 * - CORS lee orígenes desde variable de entorno → no wildcard (*) en producción
 * - Headers CORS explícitos (no "todos los headers")
 * - maxAge configurado para reducir preflight requests
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserDetailsService userDetailsService;

    @Value("${app.cors.allowed-origins:http://localhost:5173,http://localhost:3000}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
           .authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/v1/auth/**").permitAll()
    .requestMatchers(HttpMethod.POST, "/api/v1/clientes/registro").permitAll()

    // ── Reglas específicas ANTES de las genéricas (primera coincidencia gana) ──

    // Reportes: información administrativa sensible, exclusiva del ADMIN
    .requestMatchers("/api/v1/reportes/**").hasRole("ADMIN")

    // Chofer: sólo sus propios viajes y el cambio de estado del viaje
    // (deben declararse ANTES de "/api/v1/viajes/**" para que no queden anuladas)
    .requestMatchers(HttpMethod.GET,   "/api/v1/viajes/mis-viajes").hasAnyRole("ADMIN","CHOFER")
    .requestMatchers(HttpMethod.PATCH, "/api/v1/viajes/*/estado").hasAnyRole("ADMIN","CHOFER")

    // Viajes: SÓLO lectura anónima en las rutas que el buscador público usa.
    // El detalle por id NO es público: se exige sesión (coherente con el
    // @PreAuthorize del controlador) en lugar de depender de dos capas que
    // discrepaban. Los verbos restantes caen en anyRequest().authenticated().
    .requestMatchers(HttpMethod.GET, "/api/v1/viajes", "/api/v1/viajes/disponibles").permitAll()
    .requestMatchers(HttpMethod.GET, "/api/v1/viajes/*/asientos").permitAll()
    .requestMatchers(HttpMethod.GET, "/api/v1/viajes/*").authenticated()

    // Sucursales: acceso público total de lectura (selector de pasajes)
    .requestMatchers(HttpMethod.GET, "/api/v1/sucursales").permitAll()
    .requestMatchers(HttpMethod.GET, "/api/v1/sucursales/**").permitAll()

    .requestMatchers(HttpMethod.GET, "/api/v1/rutas/**").permitAll()
    .requestMatchers(HttpMethod.GET, "/api/v1/encomiendas/tracking/**").permitAll()

    // Portal Público: el contenido editorial publicado es de lectura anónima.
    // La gestión (borradores, publicación, imágenes) es exclusiva del ADMIN.
    .requestMatchers(HttpMethod.GET, "/api/v1/portal/publico").permitAll()
    .requestMatchers(HttpMethod.GET, "/media/portal/**").permitAll()
    .requestMatchers("/api/v1/portal/admin/**").hasRole("ADMIN")

    // Flujo comercial público (invitado): retención temporal, consulta segura,
    // constancia/boleto PDF y recuperación por documento. La referencia no
    // predecible y el documento actúan como control de acceso.
    .requestMatchers(HttpMethod.POST, "/api/v1/ventas/retencion").permitAll()
    .requestMatchers(HttpMethod.POST, "/api/v1/ventas/retencion/*/reservar").permitAll()
    .requestMatchers(HttpMethod.GET, "/api/v1/ventas/retencion/**").permitAll()
    .requestMatchers(HttpMethod.GET, "/api/v1/ventas/boleto/**").permitAll()
    .requestMatchers(HttpMethod.GET, "/api/v1/ventas/recuperar").permitAll()

    // Gestión de reservas (listar, confirmar pago, liberar): sólo personal.
    .requestMatchers("/api/v1/ventas/reservas/**").hasAnyRole("ADMIN","CAJERO","VENDEDOR")

    // Roles: catálogo de la organización. Es información interna (permisos y
    // nombres de roles) y solo la consume el panel de administración, que ya va
    // autenticado: se restringe a ADMIN en lugar de exponerlo a cualquier
    // visitante anónimo.
    .requestMatchers(HttpMethod.GET, "/api/v1/roles").hasRole("ADMIN")

    // Actuator: health e info públicos para sondas; el resto (metrics, env,
    // loggers, heapdump…) es información de la aplicación y exige ADMIN.
    .requestMatchers("/actuator/health", "/actuator/info").permitAll()
    .requestMatchers("/actuator/**").hasRole("ADMIN")
    .anyRequest().authenticated()
)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(ex -> ex
                // Sin credenciales (o token inválido/vencido) → 401 JSON claro para el frontend.
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"status\":401,\"error\":\"Unauthorized\"," +
                            "\"mensaje\":\"No autenticado. Inicie sesión para continuar\",\"path\":\"" +
                            request.getRequestURI() + "\"}");
                })
                // Autenticado pero sin el rol que exige la regla de URL → 403 JSON.
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"status\":403,\"error\":\"Forbidden\"," +
                            "\"mensaje\":\"No tiene permisos para esta acción\",\"path\":\"" +
                            request.getRequestURI() + "\"}");
                }))
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .collect(Collectors.toList());
        config.setAllowedOrigins(origins);

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-Requested-With"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
