package com.transporte.sistema.controller;

import com.transporte.sistema.dto.request.ClienteRequest;
import com.transporte.sistema.dto.request.RegistroClienteRequest;
import com.transporte.sistema.dto.response.ApiResponse;
import com.transporte.sistema.dto.response.ClienteResponse;
import com.transporte.sistema.dto.response.LoginResponse;
import com.transporte.sistema.service.ClienteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

/**
 * CRUD de clientes + endpoints del portal self-service.
 */
@RestController
@RequestMapping("/api/v1/clientes")
@RequiredArgsConstructor
public class ClienteController {

    private final ClienteService clienteService;

    // ── CRUD (ADMIN / CAJERO / personal de encomiendas) ───────────────────────
    // El personal de encomiendas (VENDEDOR / OPERADOR_ENCOMIENDAS) también puede
    // buscar y registrar remitentes o destinatarios, porque el flujo de encomiendas
    // exige reutilizar siempre el mismo cliente sin salir del formulario.

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','CAJERO','CHOFER','VENDEDOR','OPERADOR_ENCOMIENDAS')")
    public ResponseEntity<Page<ClienteResponse>> listar(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("id").descending());
        if (q != null && !q.isBlank()) {
            return ResponseEntity.ok(clienteService.buscar(q, pageable));
        }
        return ResponseEntity.ok(clienteService.listarTodos(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','CAJERO','CHOFER','VENDEDOR','OPERADOR_ENCOMIENDAS')")
    public ResponseEntity<ClienteResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(clienteService.obtenerPorId(id));
    }

    @GetMapping("/dni/{dniRuc}")
    @PreAuthorize("hasAnyRole('ADMIN','CAJERO','VENDEDOR','OPERADOR_ENCOMIENDAS')")
    public ResponseEntity<ClienteResponse> obtenerPorDni(@PathVariable String dniRuc) {
        return ResponseEntity.ok(clienteService.obtenerPorDniRuc(dniRuc));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','CAJERO','VENDEDOR','OPERADOR_ENCOMIENDAS')")
    public ResponseEntity<ClienteResponse> crear(@Valid @RequestBody ClienteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clienteService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','CAJERO')")
    public ResponseEntity<ClienteResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ClienteRequest request) {
        return ResponseEntity.ok(clienteService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable Long id) {
        clienteService.eliminar(id);
        return ResponseEntity.ok(ApiResponse.ok("Cliente eliminado correctamente", null));
    }

    // ── Portal self-service ──────────────────────────────────────────────────

    /**
     * POST /api/v1/clientes/registro
     * Endpoint PÚBLICO — sin token.
     * Crea una cuenta de cliente y devuelve JWT para auto-login.
     */
    @PostMapping("/registro")
    public ResponseEntity<LoginResponse> registrarse(
            @Valid @RequestBody RegistroClienteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(clienteService.registrarConCuenta(request));
    }

    /**
     * GET /api/v1/clientes/mi-perfil
     * Solo ROLE_CLIENTE — retorna datos del cliente autenticado.
     * El username se extrae automáticamente del JWT via Principal.
     */
    @GetMapping("/mi-perfil")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<ClienteResponse> miPerfil(Principal principal) {
        return ResponseEntity.ok(clienteService.obtenerPorUsername(principal.getName()));
    }
}
