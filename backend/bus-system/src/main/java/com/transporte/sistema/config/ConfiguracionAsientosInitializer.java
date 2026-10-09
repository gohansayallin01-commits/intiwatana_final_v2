package com.transporte.sistema.config;

import com.transporte.sistema.dto.ConfiguracionAsientos;
import com.transporte.sistema.entity.Bus;
import com.transporte.sistema.repository.BusRepository;
import com.transporte.sistema.service.ConfiguracionAsientosService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Garantiza que todo vehiculo existente tenga una configuracion de asientos.
 * Los vehiculos creados antes de esta funcionalidad reciben una distribucion
 * base coherente con su capacidad declarada, de modo que el mapa nunca muestre
 * menos asientos que la capacidad comercial. El administrador puede luego
 * personalizarla.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConfiguracionAsientosInitializer {

    private final BusRepository busRepository;
    private final ConfiguracionAsientosService configuracionService;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void backfillConfiguracion() {
        List<Bus> buses = busRepository.findAll();
        int actualizados = 0;
        for (Bus bus : buses) {
            if (bus.getConfiguracionAsientos() != null
                    && !bus.getConfiguracionAsientos().isBlank()) {
                continue;
            }
            ConfiguracionAsientos config = configuracionService.generarPorCapacidad(
                    bus.getCapacidadAsientos(), bus.getNumPisos(), "AUTO");
            if (config != null) {
                bus.setConfiguracionAsientos(configuracionService.serialize(config));
                busRepository.save(bus);
                actualizados++;
            }
        }
        if (actualizados > 0) {
            log.info("Backfill configuracion de asientos: {} vehiculo(s) inicializados",
                    actualizados);
        }
    }
}
