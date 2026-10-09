package com.transporte.sistema.dto;

import java.util.List;

/**
 * Configuracion de asientos de un vehiculo concreto (no de un modelo).
 *
 * <p>La cantidad de asientos derivada de esta configuracion debe coincidir con
 * la capacidad comercial declarada del bus. Cada piso lista los asientos que
 * tiene cada fila, de adelante hacia atras; esto permite filas completas
 * (2+2 = 4) y filas parciales sin inventar asientos.</p>
 *
 * <p>Ejemplo (bus de 40, 1 piso, 4 columnas, pasillo central):</p>
 * <pre>
 * { "plantilla": "BUS_40", "pasillo": 2, "pisos": [ { "piso": 1,
 *   "asientosPorFila": [4,4,4,4,4,4,4,4,4,4] } ] }
 * </pre>
 */
public record ConfiguracionAsientos(
        String plantilla,
        Integer pasillo,
        List<PisoConfig> pisos
) {

    /** Filas de un piso: cada valor es la cantidad de asientos de esa fila. */
    public record PisoConfig(Integer piso, List<Integer> asientosPorFila) {
    }

    /** Total de asientos definidos por la configuracion. */
    public int totalAsientos() {
        if (pisos == null) {
            return 0;
        }
        int total = 0;
        for (PisoConfig piso : pisos) {
            if (piso == null || piso.asientosPorFila() == null) {
                continue;
            }
            for (Integer n : piso.asientosPorFila()) {
                if (n != null && n > 0) {
                    total += n;
                }
            }
        }
        return total;
    }

    /** Cantidad de pisos declarados. */
    public int numeroPisos() {
        return pisos == null ? 0 : pisos.size();
    }
}
