package com.transporte.sistema.controller;

import com.transporte.sistema.dto.request.UsuarioRequest;
import com.transporte.sistema.dto.response.ApiResponse;
import com.transporte.sistema.entity.Rol;
import com.transporte.sistema.entity.Sucursal;
import com.transporte.sistema.entity.Usuario;
import com.transporte.sistema.exception.ConflictoException;
import com.transporte.sistema.exception.RecursoNoEncontradoException;
import com.transporte.sistema.repository.RolRepository;
import com.transporte.sistema.repository.SucursalRepository;
import com.transporte.sistema.repository.UsuarioRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

/**
 * Gestión de usuarios del sistema (empleados).
 * Solo ADMIN puede crear/modificar usuarios.
 */
@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
@SuppressWarnings("DataFlowIssue")
public class UsuarioController {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final SucursalRepository sucursalRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional  // evita LazyInitializationException al acceder a Sucursal (LAZY)
    public ResponseEntity<List<UsuarioResponse>> listar() {
        return ResponseEntity.ok(
                usuarioRepository.findAll().stream()
                        .filter(u -> !Boolean.FALSE.equals(u.getActivo()))
                        .map(this::toResponse).toList());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<UsuarioResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(toResponse(obtenerEntidad(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<UsuarioResponse> crear(@Valid @RequestBody UsuarioRequest request) {
        if (usuarioRepository.existsByUsername(request.getUsername()))
            throw new ConflictoException("El username ya está en uso: " + request.getUsername());
        if (request.getEmail() != null && usuarioRepository.existsByEmail(request.getEmail()))
            throw new ConflictoException("El email ya está en uso: " + request.getEmail());

        Rol rol = rolRepository.findById(request.getRolId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Rol", request.getRolId()));
        Sucursal sucursal = null;
        if (request.getSucursalId() != null) {
            sucursal = sucursalRepository.findById(request.getSucursalId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Sucursal", request.getSucursalId()));
        }

        if (request.getPassword() == null || request.getPassword().isBlank())
            throw new com.transporte.sistema.exception.NegocioException("La contraseña es obligatoria al crear un usuario");
        Usuario usuario = Usuario.builder()
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .nombres(request.getNombres())
                .apellidos(request.getApellidos())
                .email(request.getEmail())
                .telefono(request.getTelefono())
                .rol(rol)
                .sucursal(sucursal)
                .dniRuc(request.getDniRuc())
                .primerLogin(true)
                .activo(true)
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(usuarioRepository.save(usuario)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<UsuarioResponse> actualizar(@PathVariable Long id, @Valid @RequestBody UsuarioRequest request) {
        Usuario u = obtenerEntidad(id);
        if (request.getNombres() != null)   u.setNombres(request.getNombres());
        if (request.getApellidos() != null) u.setApellidos(request.getApellidos());
        if (request.getEmail() != null)     u.setEmail(request.getEmail());
        if (request.getTelefono() != null)  u.setTelefono(request.getTelefono());
        if (request.getDniRuc() != null)    u.setDniRuc(request.getDniRuc());
        if (request.getPassword() != null && !request.getPassword().isBlank())
            u.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        if (request.getRolId() != null) {
            Rol rol = rolRepository.findById(request.getRolId())
                    .orElseThrow(() -> new com.transporte.sistema.exception.RecursoNoEncontradoException("Rol", request.getRolId()));
            u.setRol(rol);
        }
        if (request.getSucursalId() != null) {
            Sucursal suc = sucursalRepository.findById(request.getSucursalId())
                    .orElseThrow(() -> new com.transporte.sistema.exception.RecursoNoEncontradoException("Sucursal", request.getSucursalId()));
            u.setSucursal(suc);
        } else {
            u.setSucursal(null);
        }
        return ResponseEntity.ok(toResponse(usuarioRepository.save(u)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable Long id) {
        Usuario u = obtenerEntidad(id);
        u.softDelete();
        usuarioRepository.save(u);
        return ResponseEntity.ok(ApiResponse.ok("Usuario eliminado", null));
    }

    private Usuario obtenerEntidad(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario", id));
    }

    private UsuarioResponse toResponse(Usuario u) {
        // Acceder a sucursal dentro de la sesión JPA (requiere @Transactional en el caller)
        Long sucursalId = null;
        String sucursalNombre = null;
        try {
            if (u.getSucursal() != null) {
                sucursalId = u.getSucursal().getId();
                sucursalNombre = u.getSucursal().getNombre();
            }
        } catch (Exception ignored) {
            // LazyInitializationException silenciada — sucursal queda null
        }

        return UsuarioResponse.builder()
                .id(Objects.requireNonNull(u.getId()))
                .username(u.getUsername())
                .nombres(u.getNombres())
                .apellidos(u.getApellidos())
                .email(u.getEmail())
                .telefono(u.getTelefono())
                .rol(u.getRol().getNombre().name())
                .sucursalId(sucursalId)
                .sucursalNombre(sucursalNombre)
                .activo(Objects.requireNonNull(u.getActivo()))
                .build();
    }

    /** DTO de respuesta interno al controller */
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    static class UsuarioResponse {
        private Long id;
        private String username;
        private String nombres;
        private String apellidos;
        private String email;
        private String telefono;
        private String rol;
        private Long sucursalId;
        private String sucursalNombre;
        private Boolean activo;
    }
}
