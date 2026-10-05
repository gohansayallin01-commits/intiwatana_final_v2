package com.transporte.sistema.service;

import com.transporte.sistema.entity.Usuario;
import com.transporte.sistema.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

/** Construye el {@link PortalActor} a partir del contexto de seguridad del JWT. */
@Component
@RequiredArgsConstructor
public class PortalActorResolver {

    private final UsuarioRepository usuarioRepository;

    public PortalActor resolver(Authentication auth) {
        String username = (auth == null || auth.getName() == null) ? "sistema" : auth.getName();
        String rol = "DESCONOCIDO";
        if (auth != null && auth.getAuthorities() != null) {
            rol = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .filter(a -> a != null && a.startsWith("ROLE_"))
                    .map(a -> a.substring(5))
                    .findFirst()
                    .orElse("DESCONOCIDO");
        }
        Long usuarioId = usuarioRepository.findByUsername(username)
                .map(Usuario::getId)
                .orElse(null);
        return new PortalActor(usuarioId, username, rol);
    }
}
