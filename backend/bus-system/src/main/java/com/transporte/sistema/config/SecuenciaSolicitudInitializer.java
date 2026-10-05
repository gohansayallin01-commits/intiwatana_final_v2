package com.transporte.sistema.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Garantiza que la secuencia de la referencia SOL exista en los perfiles donde
 * el esquema NO lo construye Flyway (desarrollo: ddl-auto=update con
 * spring.flyway.enabled=false).
 *
 * La fuente de verdad es la migracion V5: en produccion y en el perfil de
 * pruebas la secuencia la crea Flyway y este componente no interviene. Aqui solo
 * se evita que el flujo de solicitudes quede inutilizable por un
 * "relation seq_encomienda_sol does not exist", porque sin la secuencia el
 * correlativo de SOL-AAAA-NNNNNN no puede emitirse antes del INSERT.
 *
 * Es idempotente y nunca reinicia una secuencia existente: si ya esta, no la
 * toca, de modo que no se altera el correlativo en curso.
 */
@Slf4j
@Component
@Profile("!prod")
@RequiredArgsConstructor
public class SecuenciaSolicitudInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Value("${spring.flyway.enabled:true}")
    private boolean flywayActivo;

    @Override
    public void run(String... args) {
        if (flywayActivo) {
            // El esquema lo construyo Flyway (V5): la secuencia ya existe.
            return;
        }
        if (existeSecuencia()) {
            return;
        }
        crearSecuencia();
        log.warn("Perfil sin Flyway (spring.flyway.enabled=false): se creo la secuencia seq_encomienda_sol "
                + "para el correlativo de las referencias SOL. En produccion la crea la migracion V5.");
    }

    private boolean existeSecuencia() {
        Object existe = jdbcTemplate.queryForObject(
                "SELECT to_regclass('public.seq_encomienda_sol')", Object.class);
        return existe != null;
    }

    /**
     * La secuencia arranca por encima del id maximo existente, igual que V5, para
     * que una base ya poblada no genere una referencia SOL ya usada.
     */
    private void crearSecuencia() {
        jdbcTemplate.execute("CREATE SEQUENCE IF NOT EXISTS seq_encomienda_sol START WITH 1 INCREMENT BY 1");
        jdbcTemplate.queryForObject(
                "SELECT setval('seq_encomienda_sol', GREATEST(COALESCE((SELECT MAX(id) FROM encomiendas), 0), 1))",
                Long.class);
    }
}
