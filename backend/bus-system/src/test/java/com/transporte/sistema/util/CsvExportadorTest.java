package com.transporte.sistema.util;

import com.transporte.sistema.dto.response.ReporteIngresosResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas del exportador a CSV.
 *
 * El CSV es un entregable visible para el usuario final (se descarga y se abre en
 * Excel), así que se comprueban los casos que romperían el archivo: acentos,
 * separadores dentro del texto, fórmulas de Excel e importes en notación
 * científica.
 *
 * No requiere base de datos.
 */
class CsvExportadorTest {

    private CsvExportador exportador;
    private static final String BOM_ESPERADO = "﻿";

    @BeforeEach
    void setUp() {
        exportador = new CsvExportador(new ObjectMapper());
    }

    private String csv(ResponseEntity<byte[]> respuesta) {
        assertNotNull(respuesta.getBody(), "El cuerpo del CSV no debe ser nulo");
        assertEquals(200, respuesta.getStatusCode().value());
        return new String(respuesta.getBody(), StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("Emite el BOM UTF-8 para que Excel muestre los acentos")
    void emiteBom() {
        List<Linea> filas = List.of(new Linea("Arica", new BigDecimal("10.00")));
        ResponseEntity<byte[]> respuesta = exportador.descargar("reporte", filas, Linea.class);

        byte[] cuerpo = respuesta.getBody();
        assertNotNull(cuerpo);
        assertEquals((byte) 0xEF, cuerpo[0], "El primer byte debe ser 0xEF del BOM");
        assertEquals((byte) 0xBB, cuerpo[1], "El segundo byte debe ser 0xBB del BOM");
        assertEquals((byte) 0xBF, cuerpo[2], "El tercer byte debe ser 0xBF del BOM");
        assertTrue(new String(cuerpo, StandardCharsets.UTF_8).startsWith(BOM_ESPERADO));
    }

    @Test
    @DisplayName("Convierte los nombres de campo en cabeceras legibles")
    void humanizaLasCabeceras() {
        String salida = csv(exportador.descargar("r", List.of(), ReporteIngresosResponse.IngresoPorMetodo.class));

        assertTrue(salida.startsWith(BOM_ESPERADO + "Metodo,Cantidad,Total"),
                "La cabecera debe ser 'Metodo,Cantidad,Total'. Fue: " + salida);
    }

    @Test
    @DisplayName("Escribe los importes en decimal plano, nunca en notación científica")
    void importesEnNotacionPlana() {
        // BigDecimal con escala alta es el caso donde toString() produce 1.0E+3
        var total = new BigDecimal("1234.50");

        String salida = csv(exportador.descargar("r", List.of(new Linea("A", total)), Linea.class));

        assertTrue(salida.contains("1234.50"), "Debe Contain 1234.50 literal. Fue: " + salida);
        assertFalse(salida.contains("E+"), "No debe aparecer notación científica. Fue: " + salida);
    }

    @Test
    @DisplayName("Entrecomilla los valores que contienen el separador")
    void entrecomillaValoresConComa() {
        String salida = csv(exportador.descargar("r", List.of(new Linea("a, b", BigDecimal.ONE)), Linea.class));

        assertTrue(salida.contains("\"a, b\""),
                "Un valor con coma debe ir entre comillas. Fue: " + salida);
    }

    @Test
    @DisplayName("Duplica las comillas dobles internas según RFC 4180")
    void duplicaComillasInternas() {
        String salida = csv(exportador.descargar("r", List.of(new Linea("say \"hi\"", BigDecimal.ONE)), Linea.class));

        assertTrue(salida.contains("\"say \"\"hi\"\"\""),
                "Las comillas internas deben duplicarse. Fue: " + salida);
    }

    @Test
    @DisplayName("Neutraliza los valores que Excel interpretaría como fórmula")
    void neutralizaFormulas() {
        List<Linea> filas = new ArrayList<>();
        for (String dangerouso : new String[]{"=1+1", "+CMD()", "-2+3", "@SUM(A1)"}) {
            filas.add(new Linea(dangerouso, BigDecimal.ONE));
        }

        String salida = csv(exportador.descargar("r", filas, Linea.class));

        for (String dangerouso : new String[]{"=1+1", "+CMD()", "-2+3", "@SUM(A1)"}) {
            assertTrue(salida.contains("\"" + dangerouso + "\""),
                    "El valor '" + dangerouso + "' debe ir entre comillas. Fue: " + salida);
        }
    }

    @Test
    @DisplayName("Formatea las fechas en formato local legible")
    void formateaFechas() {
        Linea linea = new Linea("x", BigDecimal.ONE);
        linea.setMomento(LocalDateTime.of(2026, 3, 7, 14, 30, 0));

        String salida = csv(exportador.descargar("r", List.of(linea), Linea.class));

        assertTrue(salida.contains("07/03/2026 14:30"),
                "La fecha debe formatearse dd/MM/yyyy HH:mm. Fue: " + salida);
    }

    @Test
    @DisplayName("Una lista vacía produce solo la cabecera, no un archivo corrupto")
    void listaVaciaMantieneCabecera() {
        String salida = csv(exportador.descargar("r", List.of(), Linea.class));

        assertTrue(salida.startsWith(BOM_ESPERADO + "Etiqueta,Total"), "Debe conservar la cabecera");
        assertEquals(1, salida.lines().count(), "Solo debe haber la línea de cabecera");
    }

    @Test
    @DisplayName("Los valores nulos se escriben como celdas vacías")
    void nulosComoCeldasVacias() {
        String salida = csv(exportador.descargar("r", List.of(new Linea(null, null)), Linea.class));

        assertTrue(salida.contains(","), "La fila debe conservar las comas de las columnas");
        assertFalse(salida.contains("null"), "No debe escribirse la palabra 'null'. Fue: " + salida);
    }

    @Test
    @DisplayName("Propone nombre de archivo y tipo de contenido correctos")
    void cabeceraDeDescarga() {
        var respuesta = exportador.descargar("mi-reporte", List.of(), Linea.class);

        assertTrue(respuesta.getHeaders().getFirst("Content-Disposition").contains("mi-reporte.csv"),
                "El nombre sugerido debe terminar en .csv");
        assertTrue(respuesta.getHeaders().getContentType().toString().contains("csv"),
                "El tipo de contenido debe ser text/csv");
    }

    /**
     * Bean de prueba con los mismos tipos que usan los DTO de reportes.
     * Usa Lombok @Data igual que los DTO reales, porque el exportador lee las
     * propiedades a través de los getters.
     */
    @Data
    static class Linea {
        private String etiqueta;
        private BigDecimal total;
        private LocalDateTime momento;

        Linea(String etiqueta, BigDecimal total) {
            this.etiqueta = etiqueta;
            this.total = total;
        }

        Linea() {
        }
    }
}
