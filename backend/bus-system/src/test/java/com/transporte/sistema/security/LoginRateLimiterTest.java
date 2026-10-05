package com.transporte.sistema.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Lógica del limite de intentos de login (H-06).
 *
 * Se prueba el componente aislado, con la ventana manipulada por reflection, para
 * comprobar el comportamiento en el tiempo sin dormir de verdad entre pruebas.
 */
@DisplayName("H-06 - LoginRateLimiter")
class LoginRateLimiterTest {

    private LoginRateLimiter limiter(int maxUsuario, int maxIp, long ventanaMs) {
        return new LoginRateLimiter(maxUsuario, ventanaMs, maxIp, System::currentTimeMillis);
    }

    @Test
    @DisplayName("Permite el intento mientras no se supere el maximo por usuario")
    void permiteHastaElMaximo() {
        LoginRateLimiter limiter = limiter(3, 100, 60_000);

        for (int i = 0; i < 3; i++) {
            assertTrue(limiter.permitido("u:ana", "ip:1"), "Intento " + i + " deberia pasar");
            limiter.registrarFallo("u:ana", "ip:1");
        }
        assertFalse(limiter.permitido("u:ana", "ip:1"),
                "El cuarto intento debe estar bloqueado");
    }

    @Test
    @DisplayName("Un login correcto limpia el contador y restablece el acceso")
    void exitoLimpiaElContador() {
        LoginRateLimiter limiter = limiter(2, 100, 60_000);

        limiter.registrarFallo("u:ana", "ip:1");
        limiter.registrarFallo("u:ana", "ip:1");
        assertFalse(limiter.permitido("u:ana", "ip:1"));

        limiter.registrarExito("u:ana");

        assertTrue(limiter.permitido("u:ana", "ip:1"),
                "Tras un login correcto debe poder intentarse de nuevo");
    }

    @Test
    @DisplayName("El bloqueo de una cuenta no arrastra a las demas cuentas de la misma IP")
    void bloqueoPorUsuarioNoAfectaAOtros() {
        LoginRateLimiter limiter = limiter(2, 100, 60_000);

        limiter.registrarFallo("u:ana", "ip:1");
        limiter.registrarFallo("u:ana", "ip:1");

        assertFalse(limiter.permitido("u:ana", "ip:1"), "ana debe estar bloqueada");
        assertTrue(limiter.permitido("u:beto", "ip:1"), "beto no debe verse afectado");
    }

    @Test
    @DisplayName("El limite por IP frena un barrido contra muchas cuentas desde un origen")
    void limitePorIp() {
        LoginRateLimiter limiter = limiter(100, 4, 60_000);

        // Cuatro cuentas distintas desde la misma IP, una falla cada una.
        for (String usuario : new String[]{"u:1", "u:2", "u:3", "u:4"}) {
            limiter.registrarFallo(usuario, "ip:atacante");
        }

        assertFalse(limiter.permitido("u:5", "ip:atacante"),
                "La IP que agota el techo debe quedar bloqueada aunque la cuenta sea nueva");
        assertTrue(limiter.permitido("u:5", "ip:otra"),
                "Una IP distinta no debe quedar bloqueada");
    }

    @Test
    @DisplayName("Transcurrida la ventana, los intentos se olvidan")
    void ventanaDeslizante() {
        long[] ahora = {1_000_000L};
        LoginRateLimiter limiter = new LoginRateLimiter(2, 100, 60_000, () -> ahora[0]);

        limiter.registrarFallo("u:ana", "ip:1");
        limiter.registrarFallo("u:ana", "ip:1");
        assertFalse(limiter.permitido("u:ana", "ip:1"));

        ahora[0] += 60_001L;

        assertTrue(limiter.permitido("u:ana", "ip:1"),
                "Pasada la ventana debe permitirse el intento de nuevo");
    }

    @Test
    @DisplayName("Las claves de usuario e IP distinguen cuentas e IPs distintas")
    void claves() {
        // El case y los espacios NO pueden generar claves distintas: si lo hicieran,
        // el bloqueo se esquivaria con 'Ana' / ' ana ' / 'ANA' sobre la misma cuenta.
        assertEquals(LoginRateLimiter.claveUsuario("Ana"), LoginRateLimiter.claveUsuario("ana"));
        assertEquals(LoginRateLimiter.claveUsuario("Ana"), LoginRateLimiter.claveUsuario(" Ana "));

        assertNotEquals(LoginRateLimiter.claveUsuario("ana"), LoginRateLimiter.claveUsuario("beto"));
        assertNotEquals(LoginRateLimiter.claveIp("10.0.0.1"), LoginRateLimiter.claveIp("10.0.0.2"));
        assertNotEquals(LoginRateLimiter.claveIp("10.0.0.1"), LoginRateLimiter.claveIp(null));
    }

    @Test
    @DisplayName("Cambiar el case del usuario no esquiva el bloqueo ya alcanzado")
    void elCaseNoEsquivaElBloqueo() {
        LoginRateLimiter limiter = limiter(2, 100, 60_000);
        String clave = LoginRateLimiter.claveUsuario("ana");

        limiter.registrarFallo(clave, LoginRateLimiter.claveIp("10.0.0.1"));
        limiter.registrarFallo(clave, LoginRateLimiter.claveIp("10.0.0.1"));
        assertFalse(limiter.permitido(clave, LoginRateLimiter.claveIp("10.0.0.1")));

        assertFalse(limiter.permitido(LoginRateLimiter.claveUsuario("ANA"),
                        LoginRateLimiter.claveIp("10.0.0.1")),
                "El bloqueo debe persistir aunque el atacante cambie el case");
    }

    @Test
    @DisplayName("El constructor de Spring esta marcado: si no, la app no arranca")
    void constructorDeSpringMarcado() {
        // Regresion real detectada al arrancar la aplicacion: al anadir el
        // constructor con reloj para pruebas, Spring dejo de saber cual usar y
        // falló con "No default constructor found". Los tests que instancian la
        // clase a mano no lo detectan, por eso se fija aqui.
        long marcados = java.util.Arrays.stream(LoginRateLimiter.class.getConstructors())
                .filter(c -> c.isAnnotationPresent(
                        org.springframework.beans.factory.annotation.Autowired.class))
                .count();

        assertEquals(1, marcados,
                "Debe existir exactamente un constructor @Autowired para la inyeccion de Spring");
    }
}
