package com.transporte.sistema;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Clase principal del Sistema de Transporte Interprovincial.
 * Gestiona venta de boletos y encomiendas.
 */
@SpringBootApplication
public class SistemaTransporteApplication {

    public static void main(String[] args) {
        SpringApplication.run(SistemaTransporteApplication.class, args);
    }
}
