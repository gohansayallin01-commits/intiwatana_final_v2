package com.transporte.sistema.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transporte.sistema.dto.ConfiguracionAsientos;
import com.transporte.sistema.dto.PlantillaAsientos;
import com.transporte.sistema.enums.TipoAsiento;
import com.transporte.sistema.exception.NegocioException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Centraliza la lectura, validacion y derivacion de la configuracion de
 * asientos por vehiculo. La configuracion final pertenece al bus concreto y
 * debe coincidir con su capacidad comercial declarada.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConfiguracionAsientosService {

    /** Maximo de asientos por fila admitido (A..F). */
    public static final int MAX_COLUMNAS = 6;
    /** Columnas usadas al autogenerar una distribucion por capacidad. */
    private static final int COLUMNAS_POR_DEFECTO = 4;
    public static final String ESTADO_COMPLETO = "COMPLETO";
    public static final String ESTADO_INCOMPLETO = "INCOMPLETO";

    private final ObjectMapper objectMapper;

    /** Posicion fisica derivada de la configuracion, lista para crear asientos. */
    public record PosicionAsiento(int piso, int fila, int columna,
                                  TipoAsiento tipo, String numeroAsiento) {
    }

    public ConfiguracionAsientos parse(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, ConfiguracionAsientos.class);
        } catch (Exception e) {
            log.warn("Configuracion de asientos no parseable: {}", e.getMessage());
            return null;
        }
    }

    public String serialize(ConfiguracionAsientos config) {
        if (config == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(config);
        } catch (Exception e) {
            throw new NegocioException("No se pudo guardar la configuracion de asientos");
        }
    }

    public int totalAsientos(ConfiguracionAsientos config) {
        return config == null ? 0 : config.totalAsientos();
    }

    public boolean estaCompleta(ConfiguracionAsientos config, Integer capacidad) {
        return config != null && capacidad != null && config.totalAsientos() == capacidad;
    }

    public String estado(ConfiguracionAsientos config, Integer capacidad) {
        return estaCompleta(config, capacidad) ? ESTADO_COMPLETO : ESTADO_INCOMPLETO;
    }

    /** Valida la forma de la configuracion (pisos, filas y columnas utiles). */
    public void validarEstructura(ConfiguracionAsientos config) {
        if (config == null || config.pisos() == null || config.pisos().isEmpty()) {
            throw new NegocioException("La configuracion de asientos no tiene pisos definidos");
        }
        for (ConfiguracionAsientos.PisoConfig piso : config.pisos()) {
            if (piso == null || piso.asientosPorFila() == null || piso.asientosPorFila().isEmpty()) {
                throw new NegocioException("Cada piso debe tener al menos una fila de asientos");
            }
            for (Integer n : piso.asientosPorFila()) {
                if (n == null || n < 1 || n > MAX_COLUMNAS) {
                    throw new NegocioException(
                            "Cada fila debe tener entre 1 y " + MAX_COLUMNAS + " asientos");
                }
            }
        }
    }

    /** Valida que la configuracion sea coherente y coincida con la capacidad. */
    public void validarContraCapacidad(ConfiguracionAsientos config, Integer capacidad) {
        validarEstructura(config);
        int total = config.totalAsientos();
        if (capacidad == null || total != capacidad) {
            throw new NegocioException("La configuracion tiene " + total
                    + " asiento(s) y la capacidad declarada del vehiculo es " + capacidad
                    + ". Ajusta la distribucion antes de guardar.");
        }
    }

    /**
     * Deriva las posiciones fisicas (piso, fila, columna, tipo y numero) de la
     * configuracion. La numeracion de fila es global entre pisos para que
     * (viaje, numeroAsiento) siga siendo unico.
     */
    public List<PosicionAsiento> generarPosiciones(ConfiguracionAsientos config) {
        List<PosicionAsiento> posiciones = new ArrayList<>();
        if (config == null || config.pisos() == null) {
            return posiciones;
        }
        int filaGlobal = 0;
        int ordenPiso = 1;
        for (ConfiguracionAsientos.PisoConfig piso : config.pisos()) {
            if (piso == null || piso.asientosPorFila() == null) {
                continue;
            }
            int pisoNum = piso.piso() != null ? piso.piso() : ordenPiso;
            int columnas = columnasPiso(piso);
            for (Integer n : piso.asientosPorFila()) {
                if (n == null || n < 1) {
                    continue;
                }
                filaGlobal++;
                for (int col = 1; col <= n; col++) {
                    TipoAsiento tipo = (col == 1 || col == columnas)
                            ? TipoAsiento.VENTANA : TipoAsiento.PASILLO;
                    posiciones.add(new PosicionAsiento(
                            pisoNum, filaGlobal, col, tipo, numeroAsiento(filaGlobal, col)));
                }
            }
            ordenPiso++;
        }
        return posiciones;
    }

    /**
     * Genera una distribucion completa (total == capacidad) repartiendo los
     * asientos entre los pisos y usando filas de hasta 4 columnas.
     */
    public ConfiguracionAsientos generarPorCapacidad(Integer capacidad, Integer numPisos,
                                                     String plantilla) {
        if (capacidad == null || capacidad < 1) {
            return null;
        }
        int pisos = (numPisos == null || numPisos < 1) ? 1 : Math.min(numPisos, 2);
        List<ConfiguracionAsientos.PisoConfig> lista = new ArrayList<>();
        int restante = capacidad;
        for (int p = 1; p <= pisos; p++) {
            int asignados = (p == pisos) ? restante : capacidad / pisos;
            restante -= asignados;
            lista.add(new ConfiguracionAsientos.PisoConfig(
                    p, filas(asignados, COLUMNAS_POR_DEFECTO)));
        }
        String nombre = (plantilla == null || plantilla.isBlank()) ? "AUTO" : plantilla;
        return new ConfiguracionAsientos(nombre, 2, lista);
    }

    /** Plantillas de arranque disponibles para la administracion. */
    public List<PlantillaAsientos> plantillas() {
        return List.of(
                new PlantillaAsientos("VAN", "Van (2+1)", 15, 1, 2,
                        List.of(3, 3, 3, 3, 3)),
                new PlantillaAsientos("MINIBUS", "Minibus", 20, 1, 2,
                        List.of(4, 4, 4, 4, 4)),
                new PlantillaAsientos("BUS_32", "Bus 32", 32, 1, 2,
                        List.of(4, 4, 4, 4, 4, 4, 4, 4)),
                new PlantillaAsientos("BUS_36", "Bus 36", 36, 1, 2,
                        List.of(4, 4, 4, 4, 4, 4, 4, 4, 4)),
                new PlantillaAsientos("BUS_40", "Bus 40", 40, 1, 2,
                        List.of(4, 4, 4, 4, 4, 4, 4, 4, 4, 4)),
                new PlantillaAsientos("BUS_44", "Bus 44", 44, 1, 2,
                        List.of(4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4)),
                new PlantillaAsientos("PERSONALIZADO", "Personalizado", 0, 1, 2,
                        List.of()));
    }

    private int columnasPiso(ConfiguracionAsientos.PisoConfig piso) {
        int max = 0;
        for (Integer n : piso.asientosPorFila()) {
            if (n != null && n > max) {
                max = n;
            }
        }
        return Math.max(max, 1);
    }

    private List<Integer> filas(int asientos, int columnas) {
        List<Integer> filas = new ArrayList<>();
        int restante = asientos;
        while (restante > 0) {
            int fila = Math.min(columnas, restante);
            filas.add(fila);
            restante -= fila;
        }
        if (filas.isEmpty()) {
            filas.add(0);
        }
        return filas;
    }

    private String numeroAsiento(int fila, int columna) {
        char letra = (char) ('A' + (columna - 1));
        return fila + String.valueOf(letra);
    }
}
