package com.transporte.sistema.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registro de tokens revocados por logout (H-06).
 *
 * Un JWT es stateless: sin este registro, "cerrar sesion" en el frontend solo
 * borraba el token del navegador y el token seguimos siendo valido hasta su
 * expiracion (ventana de robo). Aqui el jti del token se marca como revocado
 * hasta su propia fecha de expiracion, momento en el que ya no serviria de nada.
 *
 * CONSISTENCIA: estado en memoria del proceso. Es la unica limitacion conocida y
 * esta documentada (cluster -> Redis/BD). En un nodo unico el logout es efectivo
 * de inmediato.
 */
@Component
public class TokenRevocationService {

    /** jti -> instante de expiracion del token (ms epoch). */
    private final Map<String, Long> revocados = new ConcurrentHashMap<>();

    private final int maxEntradas;

    public TokenRevocationService(
            @Value("${app.security.token.revocacion-max-entradas:20000}") int maxEntradas) {
        this.maxEntradas = maxEntradas;
    }

    /** Marca el token como revocado hasta su expiracion. */
    public void revocar(String jti, Instant expiracion) {
        if (jti == null || expiracion == null) return;
        if (revocados.size() >= maxEntradas) {
            purgarExpired();
            // Si sigue lleno (todas vigentes), se descarta la que expira antes:
            // el tope protege la memoria, no la seguridad.
            if (revocados.size() >= maxEntradas) {
                Map.Entry<String, Long> masAntigua = null;
                for (Map.Entry<String, Long> e : revocados.entrySet()) {
                    if (masAntigua == null || e.getValue() < masAntigua.getValue()) {
                        masAntigua = e;
                    }
                }
                if (masAntigua != null) {
                    revocados.remove(masAntigua.getKey());
                }
            }
        }
        revocados.put(jti, expiracion.toEpochMilli());
    }

    /** @return true si el token fue revocado y su revocacion sigue vigente. */
    public boolean estaRevocado(String jti) {
        if (jti == null) return false;
        Long exp = revocados.get(jti);
        if (exp == null) return false;
        if (exp <= System.currentTimeMillis()) {
            revocados.remove(jti);
            return false;
        }
        return true;
    }

    /** Limpieza periodica de revocaciones ya expiradas (no requieren memoria). */
    @Scheduled(fixedDelayString = "${app.security.token.revocacion-limpieza-ms:600000}")
    public void purgarExpired() {
        long ahora = System.currentTimeMillis();
        Iterator<Map.Entry<String, Long>> it = revocados.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue() <= ahora) {
                it.remove();
            }
        }
    }

    /** Visible para pruebas. */
    public int tamanho() {
        return revocados.size();
    }
}
