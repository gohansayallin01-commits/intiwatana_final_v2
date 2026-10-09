package com.transporte.sistema.dto;

import java.util.List;

/**
 * Plantilla de distribucion de asientos ofrecida como punto de partida para
 * configurar un vehiculo. No es la configuracion final: el administrador debe
 * revisarla, ajustarla y confirmar que coincide con la capacidad declarada.
 */
public record PlantillaAsientos(
        String codigo,
        String nombre,
        int capacidad,
        int pisos,
        int pasillo,
        List<Integer> asientosPorFila
) {
}
