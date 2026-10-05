package com.transporte.sistema.security;

import com.transporte.sistema.entity.Boleto;
import com.transporte.sistema.entity.Usuario;
import com.transporte.sistema.entity.Viaje;
import com.transporte.sistema.enums.RolNombre;
import com.transporte.sistema.exception.SinAutorizacionException;
import com.transporte.sistema.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Regla centralizada de propiedad del viaje para el panel del chofer.
 *
 * Un ROLE_CHOFER solo puede operar sobre viajes que tiene asignados y sobre los
 * boletos de esos viajes. ROLE_ADMIN conserva el acceso administrativo que el
 * sistema ya permite y el resto de roles (CAJERO, VENDEDOR, CLIENTE) conservan
 * exactamente el acceso que tenían hasta ahora, incluido el anónimo de la
 * consulta de disponibilidad y del mapa de asientos.
 *
 * El usuario se resuelve SIEMPRE desde el nombre de usuario que entrega Spring
 * Security (o sea, el del JWT), nunca desde un identificador enviado por el
 * cliente en la URL o en el cuerpo.
 */
@Component
@RequiredArgsConstructor
public class PropiedadChoferValidator {

    private final UsuarioRepository usuarioRepository;

    /** Usuario asociado al JWT; null si la petición es anónima. */
    public Usuario usuarioAutenticado(String username) {
        if (username == null || username.isBlank()) return null;
        return usuarioRepository.findByUsername(username).orElse(null);
    }

    public boolean esAdministrador(Usuario usuario) {
        return usuario != null
                && usuario.getRol() != null
                && usuario.getRol().getNombre() == RolNombre.ROLE_ADMIN;
    }

    public boolean esChofer(Usuario usuario) {
        return usuario != null
                && usuario.getRol() != null
                && usuario.getRol().getNombre() == RolNombre.ROLE_CHOFER;
    }

    /** True cuando el viaje está asignado al chofer indicado. */
    public boolean esViajePropio(Usuario usuario, Viaje viaje) {
        return usuario != null
                && viaje != null
                && viaje.getChofer() != null
                && viaje.getChofer().getId() != null
                && viaje.getChofer().getId().equals(usuario.getId());
    }

    /**
     * Regla única de autorización por viaje.
     *
     * @param username nombre de usuario del JWT (puede ser null si la ruta es pública)
     * @param viaje    viaje afectado por la operación
     * @param mensaje  mensaje 403 que se devuelve al cliente
     */
    public void exigirPropiedadDeViaje(String username, Viaje viaje, String mensaje) {
        Usuario usuario = usuarioAutenticado(username);
        // Petición anónima: la autorización la resuelve el @PreAuthorize del endpoint.
        if (usuario == null) return;
        // ADMIN y los demás roles conservan su acceso actual.
        if (esAdministrador(usuario) || !esChofer(usuario)) return;
        if (!esViajePropio(usuario, viaje)) {
            throw new SinAutorizacionException(mensaje);
        }
    }

    /** Aplica la misma regla sobre el viaje al que pertenece el boleto. */
    public void exigirPropiedadDeBoleto(String username, Boleto boleto, String mensaje) {
        exigirPropiedadDeViaje(username, boleto == null ? null : boleto.getViaje(), mensaje);
    }
}
