package com.transporte.sistema.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Habilita el soporte de tareas programadas (@Scheduled).
 * Se usa para el mantenimiento de reservas vencidas; la liberación efectiva de
 * asientos NO depende de este proceso (es perezosa al consultar/operar), pero
 * este barrido mantiene la base de datos consistente y auditada.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
