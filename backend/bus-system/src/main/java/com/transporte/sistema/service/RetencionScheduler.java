package com.transporte.sistema.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Barrido de mantenimiento de reservas vencidas.
 *
 * NO es el único mecanismo de liberación: la disponibilidad expira también de
 * forma perezosa al consultar u operar sobre un viaje, por lo que un proceso
 * caído no provoca bloqueos comerciales permanentes.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RetencionScheduler {

    private final VentaService ventaService;

    @Scheduled(
            fixedDelayString = "${app.ventas.retencion-limpieza-ms:60000}",
            initialDelayString = "${app.ventas.retencion-limpieza-ms:60000}")
    public void liberarVencidas() {
        try {
            ventaService.liberarVencidas();
        } catch (Exception e) {
            log.error("Error en el barrido de reservas vencidas: {}", e.getMessage(), e);
        }
    }
}
