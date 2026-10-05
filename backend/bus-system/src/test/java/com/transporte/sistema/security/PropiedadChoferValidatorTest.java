package com.transporte.sistema.security;

import com.transporte.sistema.entity.Boleto;
import com.transporte.sistema.entity.Rol;
import com.transporte.sistema.entity.Usuario;
import com.transporte.sistema.entity.Viaje;
import com.transporte.sistema.enums.RolNombre;
import com.transporte.sistema.exception.SinAutorizacionException;
import com.transporte.sistema.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Regla de propiedad del viaje usada por el panel del chofer.
 *
 * Se verifica con dobles de usuario/viaje/boleto: la propiedad depende solo de
 * los identificadores, así que no hace falta una base de datos.
 */
@DisplayName("PropiedadChoferValidator — autorización por viaje")
class PropiedadChoferValidatorTest {

    private UsuarioRepository usuarioRepository;
    private PropiedadChoferValidator validator;

    @BeforeEach
    void setUp() {
        usuarioRepository = mock(UsuarioRepository.class);
        validator = new PropiedadChoferValidator(usuarioRepository);
    }

    // ── Fixtures ─────────────────────────────────────────────────────────────

    private Usuario usuario(Long id, RolNombre rol) {
        Rol r = new Rol();
        r.setNombre(rol);
        Usuario u = new Usuario();
        u.setId(id);
        u.setRol(r);
        return u;
    }

    private Viaje viaje(Long id, Long choferId) {
        Usuario chofer = choferId == null ? null : usuario(choferId, RolNombre.ROLE_CHOFER);
        Viaje v = new Viaje();
        v.setId(id);
        v.setChofer(chofer);
        return v;
    }

    private Boleto boleto(Long id, Viaje v) {
        Boleto b = new Boleto();
        b.setId(id);
        b.setViaje(v);
        return b;
    }

    private void registrarEnBd(String username, Usuario u) {
        when(usuarioRepository.findByUsername(username)).thenReturn(Optional.ofNullable(u));
    }

    private static final String MSJ = "No tienes autorización para consultar los pasajeros de este viaje.";

    // ── Casos permitidos ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("Permite el acceso")
    class Permite {

        @Test
        @DisplayName("chofer a su propio viaje")
        void choferPropietario() {
            registrarEnBd("chofer1", usuario(1L, RolNombre.ROLE_CHOFER));
            Viaje propio = viaje(31L, 1L);
            assertDoesNotThrow(() -> validator.exigirPropiedadDeViaje("chofer1", propio, MSJ));
            assertTrue(validator.esViajePropio(usuario(1L, RolNombre.ROLE_CHOFER), propio));
        }

        @Test
        @DisplayName("chofer a un boleto de su propio viaje")
        void choferBoletoPropio() {
            registrarEnBd("chofer1", usuario(1L, RolNombre.ROLE_CHOFER));
            Boleto propio = boleto(36L, viaje(31L, 1L));
            assertDoesNotThrow(() ->
                    validator.exigirPropiedadDeBoleto("chofer1", propio, MSJ));
        }

        @Test
        @DisplayName("admin a cualquier viaje")
        void adminLibre() {
            registrarEnBd("admin", usuario(99L, RolNombre.ROLE_ADMIN));
            Viaje ajeno = viaje(8L, 2L);
            assertDoesNotThrow(() -> validator.exigirPropiedadDeViaje("admin", ajeno, MSJ));
            assertTrue(validator.esAdministrador(usuario(99L, RolNombre.ROLE_ADMIN)));
        }

        @ParameterizedTest(name = "rol {0}")
        @DisplayName("cajero y vendedor conservan el acceso que tenían")
        @ValueSource(strings = {"ROLE_CAJERO", "ROLE_VENDEDOR", "ROLE_CLIENTE"})
        void otrosRolesSinRestriccion(String rol) {
            registrarEnBd("otro", usuario(7L, RolNombre.valueOf(rol)));
            Viaje ajeno = viaje(8L, 2L);
            assertDoesNotThrow(() -> validator.exigirPropiedadDeViaje("otro", ajeno, MSJ));
        }

        @Test
        @DisplayName("anónimo en la compra pública no se restringe")
        void anonimoSinRestriccion() {
            Viaje cualquiera = viaje(8L, 2L);
            assertDoesNotThrow(() -> validator.exigirPropiedadDeViaje(null, cualquiera, MSJ));
            assertNull(validator.usuarioAutenticado(null));
        }

        @ParameterizedTest(name = "username [{0}]")
        @DisplayName("usuario ausente en BD no rompe la ruta")
        @NullSource
        @ValueSource(strings = {"", "   "})
        void usuarioInexistente(String username) {
            assertDoesNotThrow(() -> validator.exigirPropiedadDeViaje(username, viaje(8L, 2L), MSJ));
        }
    }

    // ── Casos denegados ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("Deniega el acceso (403)")
    class Deniega {

        @Test
        @DisplayName("chofer a un viaje de otro chofer")
        void choferViajeAjeno() {
            registrarEnBd("chofer1", usuario(1L, RolNombre.ROLE_CHOFER));
            SinAutorizacionException ex = assertThrows(SinAutorizacionException.class,
                    () -> validator.exigirPropiedadDeViaje("chofer1", viaje(8L, 2L), MSJ));
            assertEquals(MSJ, ex.getMessage());
        }

        @Test
        @DisplayName("chofer a un boleto de otro viaje")
        void choferBoletoAjeno() {
            registrarEnBd("chofer1", usuario(1L, RolNombre.ROLE_CHOFER));
            SinAutorizacionException ex = assertThrows(SinAutorizacionException.class,
                    () -> validator.exigirPropiedadDeBoleto("chofer1", boleto(24L, viaje(8L, 2L)), MSJ));
            assertEquals(MSJ, ex.getMessage());
        }

        @Test
        @DisplayName("chofer a un viaje sin chofer asignado")
        void viajeSinChofer() {
            registrarEnBd("chofer1", usuario(1L, RolNombre.ROLE_CHOFER));
            assertThrows(SinAutorizacionException.class,
                    () -> validator.exigirPropiedadDeViaje("chofer1", viaje(50L, null), MSJ));
        }

        @Test
        @DisplayName("chofer a un boleto sin viaje")
        void boletoSinViaje() {
            registrarEnBd("chofer1", usuario(1L, RolNombre.ROLE_CHOFER));
            assertThrows(SinAutorizacionException.class,
                    () -> validator.exigirPropiedadDeBoleto("chofer1", boleto(5L, null), MSJ));
        }

        @Test
        @DisplayName("la excepción se traduce a HTTP 403")
        void codigoHttp() {
            SinAutorizacionException ex = new SinAutorizacionException(MSJ);
            org.springframework.web.bind.annotation.ResponseStatus rs =
                    SinAutorizacionException.class.getAnnotation(
                            org.springframework.web.bind.annotation.ResponseStatus.class);
            assertNotNull(rs);
            assertEquals(org.springframework.http.HttpStatus.FORBIDDEN, rs.value());
            assertEquals(MSJ, ex.getMessage());
        }
    }

    // ── Identidad ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("el usuario se resuelve por username del JWT, no por el id de la URL")
    void usuarioDesdeJwt() {
        when(usuarioRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        assertNull(validator.usuarioAutenticado("inventado"));
        assertFalse(validator.esChofer(null));
        assertFalse(validator.esAdministrador(null));
    }
}
