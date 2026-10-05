package com.transporte.sistema.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Limitador de intentos de autenticacion (H-06) con ventana deslizante en memoria.
 *
 * OBJETIVO: frenar la fuerza bruta contra POST /api/v1/auth/login.
 *
 * DISENO:
 * - Dos dimensiones independientes: por USUARIO y por IP. El limite por usuario
 *   protege una cuenta concreta; el limite por IP evita que un atacante rotule
 *   usuarios para eludir el primero, sin bloquear a toda una oficina por el
 *   error de un solo usuario (topes distintos).
 * - Ventana deslizante: se guardan los instantes de los intentos fallidos dentro
 *   de la ventana y se descartan los que ya expiraron. No hay "ventana que se
 *   reinicia" que permitiria un burst al final de la ventana.
 * - Un login EXITOSO borra el contador de ese usuario: no se penaliza al usuario
 *   legitimo que se equivoco una vez y luego entra bien.
 *
 * CONCURRENCIA: ArrayDeque NO es seguro entre hilos, asi que TODA operacion sobre
 * una cola dada (leer, limpiar, insertar) se hace dentro de synchronized sobre esa
 * misma cola. Sin esto, un size() en paralelo con un addLast() puede devolver una
 * cuenta falsa o corromper la estructura interna.
 *
 * LIMITACION CONOCIDA: el estado vive en la memoria del proceso. Con varias
 * instancias (cluster) el limite es por instancia; para un despliegue multi-nodo
 * hace falta un almacen compartido (Redis/BD). Documentado en el informe final.
 */
@Component
public class LoginRateLimiter {

    private final int maxIntentos;
    private final long ventanaMs;
    private final int maxIntentosPorIp;
    private final LongSupplier reloj;

    /** clave -> instantes (epoch ms) de los intentos fallidos aun dentro de la ventana. */
    private final Map<String, Deque<Long>> intentosPorClave = new ConcurrentHashMap<>();

    // Con dos constructores, Spring no puede elegir solo: sin @Autowired falla con
    // "No default constructor found" al arrancar (los tests unitarios, que lo
    // instancian a mano, NO lo detectan).
    @Autowired
    public LoginRateLimiter(
            @Value("${app.security.login.max-intentos:5}") int maxIntentos,
            @Value("${app.security.login.ventana-ms:300000}") long ventanaMs,
            @Value("${app.security.login.max-intentos-por-ip:40}") int maxIntentosPorIp) {
        this(maxIntentos, ventanaMs, maxIntentosPorIp, System::currentTimeMillis);
    }

    /** Constructor con reloj inyectado: permite probar la ventana sin dormir. */
    LoginRateLimiter(int maxIntentos, long ventanaMs, int maxIntentosPorIp, LongSupplier reloj) {
        this.maxIntentos = maxIntentos;
        this.ventanaMs = ventanaMs;
        this.maxIntentosPorIp = maxIntentosPorIp;
        this.reloj = reloj;
    }

    /** Clave de la dimension por usuario. */
    public static String claveUsuario(String username) {
        // Minúsculas y sin espacios: 'Ana', ' ana ' y 'ANA' son la misma cuenta y
        // deben compartir contador, o el bloqueo se esquivaria cambiando el case.
        return "user:" + (username == null ? "" : username.trim().toLowerCase());
    }

    /** Clave de la dimension por IP. */
    public static String claveIp(String ip) {
        return "ip:" + (ip == null ? "desconocida" : ip.trim());
    }

    /**
     * @return true si el login puede intentarse; false si esta bloqueado.
     */
    public boolean permitido(String claveUsuario, String claveIp) {
        limpiar(claveUsuario);
        limpiar(claveIp);
        return contar(claveUsuario) < maxIntentos && contar(claveIp) < maxIntentosPorIp;
    }

    /** Registra un fallo de autenticacion para ambas dimensiones. */
    public void registrarFallo(String claveUsuario, String claveIp) {
        long ahora = reloj.getAsLong();
        anadir(claveUsuario, ahora);
        anadir(claveIp, ahora);
    }

    /** Un login correcto no penaliza: se limpia el contador del usuario. */
    public void registrarExito(String claveUsuario) {
        Deque<Long> cola = intentosPorClave.remove(claveUsuario);
        if (cola != null) {
            synchronized (cola) {
                cola.clear();
            }
        }
    }

    /** Intentos fallidos vigentes para la clave (se usa en el mensaje de 429). */
    public int intentosVigentes(String clave) {
        limpiar(clave);
        return contar(clave);
    }

    /** Lectura sincronizada: ArrayDeque solo es valido bajo su propio lock. */
    private int contar(String clave) {
        Deque<Long> cola = intentosPorClave.get(clave);
        if (cola == null) return 0;
        synchronized (cola) {
            return cola.size();
        }
    }

    private void anadir(String clave, long instante) {
        Deque<Long> cola = intentosPorClave.computeIfAbsent(clave, k -> new ArrayDeque<>());
        synchronized (cola) {
            cola.addLast(instante);
        }
    }

    /** Descarta los intentos que ya salieron de la ventana deslizante. */
    private void limpiar(String clave) {
        Deque<Long> cola = intentosPorClave.get(clave);
        if (cola == null) return;
        long limite = reloj.getAsLong() - ventanaMs;
        synchronized (cola) {
            while (!cola.isEmpty() && cola.peekFirst() < limite) {
                cola.pollFirst();
            }
            if (cola.isEmpty()) {
                // remove(clave, valor) solo borra si sigue siendo la MISMA cola:
                // si otro hilo ya la sustituyo, no se pisa su estado.
                intentosPorClave.remove(clave, cola);
            }
        }
    }
}
