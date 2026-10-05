package com.transporte.sistema.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Registro de revocacion por logout (H-06).
 */
@DisplayName("H-06 - TokenRevocationService")
class TokenRevocationServiceTest {

    private TokenRevocationService servicio(int max) {
        return new TokenRevocationService(max);
    }

    @Test
    @DisplayName("Un token revocado deja de estar revocado cuando su expiracion pasa")
    void revocaHastaExpiracion() {
        TokenRevocationService servicio = servicio(100);

        servicio.revocar("jti-1", Instant.now().plusSeconds(3600));
        assertTrue(servicio.estaRevocado("jti-1"), "El token recien revocado debe rechazarse");

        // Ya expirado: la revocacion es irrelevante y no debe memoria.
        servicio.revocar("jti-2", Instant.now().minusSeconds(1));
        assertFalse(servicio.estaRevocado("jti-2"),
                "Un token ya expirado no necesita revocacion");
        assertEquals(1, servicio.tamanho(), "La revocacion expirada debe purgarse sola");
    }

    @Test
    @DisplayName("Los tokens no revocados siguen siendo válidos")
    void noRevocaLoQueNoSeRevoca() {
        TokenRevocationService servicio = servicio(100);
        assertFalse(servicio.estaRevocado("jti-desconocido"));
        assertFalse(servicio.estaRevocado(null), "un jti null no debe romper el filtro");
    }

    @Test
    @DisplayName("Revocar es idempotente: el mismo jti no duplica entrada")
    void revocarEsIdempotente() {
        TokenRevocationService servicio = servicio(100);
        Instant exp = Instant.now().plusSeconds(3600);

        servicio.revocar("jti-1", exp);
        servicio.revocar("jti-1", exp);

        assertEquals(1, servicio.tamanho());
        assertTrue(servicio.estaRevocado("jti-1"));
    }

    @Test
    @DisplayName("Al superar el tope se descarta la revocacion que expira antes, no la nueva")
    void topeDeEntradas() {
        TokenRevocationService servicio = servicio(3);

        // La mas antigua expira pronto: es la candidata a ser descartada.
        servicio.revocar("caduca-antes", Instant.now().plusSeconds(10));
        servicio.revocar("larga-1", Instant.now().plusSeconds(3600));
        servicio.revocar("larga-2", Instant.now().plusSeconds(7200));
        servicio.revocar("larga-3", Instant.now().plusSeconds(10800));

        assertTrue(servicio.tamanho() <= 3, "El tope debe respetarse: " + servicio.tamanho());
        assertTrue(servicio.estaRevocado("larga-3"), "El token recien revocado debe conservarse");
        assertFalse(servicio.estaRevocado("caduca-antes"),
                "Se espera que se descarte la revocacion que expira primero");
    }

    @Test
    @DisplayName("purgarExpired deja solo las revocaciones vigentes")
    void purga() {
        TokenRevocationService servicio = servicio(100);
        servicio.revocar("viejo", Instant.now().minusSeconds(5));
        servicio.revocar("nuevo", Instant.now().plusSeconds(3600));

        servicio.purgarExpired();

        assertEquals(1, servicio.tamanho());
        assertFalse(servicio.estaRevocado("viejo"));
        assertTrue(servicio.estaRevocado("nuevo"));
    }

    @Test
    @DisplayName("revocar ignora jti o expiracion ausentes")
    void datosIncompletos() {
        TokenRevocationService servicio = servicio(100);
        servicio.revocar(null, Instant.now().plusSeconds(60));
        servicio.revocar("jti", null);

        assertEquals(0, servicio.tamanho(), "No debe registrarse nada sin datos suficientes");
    }

    @Test
    @DisplayName("El servicio es seguro frente a uso concurrente")
    void concurrencia() throws InterruptedException {
        // Tope holgado a proposito: aqui se comprueba que no se PIERDEN
        // revocaciones por concurrencia, no el comportamiento de tope (que cubre
        // topeDeEntradas). Si el tope mordiera, la asercion de abajo seria falsa.
        TokenRevocationService servicio = servicio(5000);
        int hilos = 8;
        int porHilo = 100;
        Thread[] ts = new Thread[hilos];

        for (int i = 0; i < hilos; i++) {
            final int id = i;
            ts[i] = new Thread(() -> {
                for (int j = 0; j < porHilo; j++) {
                    String jti = "h" + id + "-" + j;
                    servicio.revocar(jti, Instant.now().plusSeconds(3600));
                    servicio.estaRevocado(jti);
                }
            });
            ts[i].start();
        }
        for (Thread t : ts) {
            t.join();
        }

        // Las revocaciones sobreviven a la concurrencia: el registro es la fuente
        // de verdad del logout y no puede perder entradas en silencio.
        int presentes = 0;
        for (int i = 0; i < hilos; i++) {
            for (int j = 0; j < porHilo; j++) {
                if (servicio.estaRevocado("h" + i + "-" + j)) {
                    presentes++;
                }
            }
        }
        assertEquals(hilos * porHilo, presentes,
                "Toda revocacion registrada debe seguir presente");
        assertEquals(hilos * porHilo, servicio.tamanho());
    }
}
