package com.transporte.sistema.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Exporta una lista de registros a CSV para descargar desde los reportes.
 *
 * El separador es coma y el archivo se genera en UTF-8 con BOM, que es lo que
 * Excel necesita para mostrar bien los acentos (importe, número de guía, etc.).
 */
@Component
@RequiredArgsConstructor
public class CsvExportador {

    private static final String SEPARADOR = ",";
    private static final char ESCAPE = '"';
    /** BOM UTF-8: sin él, Excel lee los acentos como caracteres corruptos. */
    private static final byte[] BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ObjectMapper objectMapper;

    /**
     * @param nombreArchivo nombre sugerido, sin extensión
     * @param filas         registros a exportar
     * @param tipo          clase de los registros, para deducir las columnas
     */
    public <T> ResponseEntity<byte[]> descargar(String nombreArchivo, Collection<T> filas, Class<T> tipo) {
        List<String> columnas = columnas(tipo);
        StringBuilder csv = new StringBuilder();

        // Cabecera
        for (int i = 0; i < columnas.size(); i++) {
            if (i > 0) csv.append(SEPARADOR);
            csv.append(escapar(etiqueta(columnas.get(i))));
        }
        csv.append("\r\n");

        // Filas
        for (T fila : filas) {
            Map<String, Object> valores = aMapa(fila, columnas);
            for (int i = 0; i < columnas.size(); i++) {
                if (i > 0) csv.append(SEPARADOR);
                csv.append(escapar(formatear(valores.get(columnas.get(i)))));
            }
            csv.append("\r\n");
        }

        byte[] cuerpo = csv.toString().getBytes(StandardCharsets.UTF_8);
        byte[] contenido = new byte[BOM.length + cuerpo.length];
        System.arraycopy(BOM, 0, contenido, 0, BOM.length);
        System.arraycopy(cuerpo, 0, contenido, BOM.length, cuerpo.length);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombreArchivo + ".csv\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(contenido);
    }

    /** Nombres de las propiedades del bean, en el orden de declaración. */
    private <T> List<String> columnas(Class<T> tipo) {
        return objectMapper.getSerializationConfig()
                .introspect(objectMapper.constructType(tipo))
                .findProperties()
                .stream()
                .map(p -> p.getName())
                .filter(nombre -> !"ganchos".equals(nombre))
                .toList();
    }

    /**
     * Lee los valores de una fila conservando su tipo original.
     *
     * No se usa objectMapper.convertValue(): con el ObjectMapper de Spring las
     * fechas LocalDateTime ya vienen convertidas a texto ISO, y
     * {@link #formatear(Object)} nunca las reconocería, además de fallar con
     * cualquier mapper sin el módulo de java.time. Invocando los getters se
     * conserva el tipo real y el formateo es fiable.
     */
    private <T> Map<String, Object> aMapa(T fila, List<String> columnas) {
        Map<String, Object> resultado = new LinkedHashMap<>();
        for (String columna : columnas) {
            resultado.put(columna, leerPropiedad(fila, columna));
        }
        return resultado;
    }

    /** Invoca el getter de la propiedad; si no existe, devuelve null. */
    private Object leerPropiedad(Object bean, String propiedad) {
        try {
            return Arrays.stream(
                            Introspector.getBeanInfo(bean.getClass(), Object.class)
                                    .getPropertyDescriptors())
                    .filter(pd -> pd.getName().equals(propiedad))
                    .map(PropertyDescriptor::getReadMethod)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .map(lector -> {
                        try {
                            return lector.invoke(bean);
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            throw new IllegalStateException(
                                    "No se pudo leer la propiedad '" + propiedad + "' de "
                                            + bean.getClass().getSimpleName(), e);
                        }
                    })
                    .orElse(null);
        } catch (IntrospectionException e) {
            throw new IllegalStateException("No se pudo inspeccionar " + bean.getClass().getName(), e);
        }
    }

    /**
     * Convierte "totalPasajes" en "Total Pasajes".
     * Las iniciales en medio de la palabra se capitalizan: "dniRuc" → "Dni Ruc".
     */
    private String etiqueta(String propiedad) {
        StringBuilder palabras = new StringBuilder();
        for (int i = 0; i < propiedad.length(); i++) {
            char c = propiedad.charAt(i);
            if (c == '_' || c == '-') {
                palabras.append(' ');
            } else if (Character.isUpperCase(c) && i > 0) {
                palabras.append(' ').append(c);
            } else {
                palabras.append(c);
            }
        }
        // Capitalizar la primera letra de cada palabra, respetando los acentos.
        String[] partes = palabras.toString().split(" ");
        StringBuilder salida = new StringBuilder();
        for (String parte : partes) {
            if (parte.isEmpty()) {
                salida.append(' ');
                continue;
            }
            salida.append(Character.toUpperCase(parte.charAt(0)))
                    .append(parte.substring(1).toLowerCase())
                    .append(' ');
        }
        return salida.toString().trim();
    }

    private String formatear(Object valor) {
        if (valor == null) return "";
        if (valor instanceof LocalDateTime fechaHora) return FECHA_HORA.format(fechaHora);
        if (valor instanceof LocalDate fecha) return FECHA_HORA.format(fecha.atStartOfDay());
        if (valor instanceof BigDecimal decimal) return decimal.toPlainString();
        if (valor instanceof Enum<?> enumerado) return enumerado.name();
        return String.valueOf(valor);
    }

    /**
     * Escapa un valor según RFC 4180. En PostgreSQL/Excel los valores de tipo
     * fórmula (=, +, -, @) se entrecomillan para evitar que se interpreten.
     */
    private String escapar(String valor) {
        boolean necesitaComillas = valor.indexOf(SEPARADOR) >= 0
                || valor.indexOf('"') >= 0
                || valor.indexOf('\n') >= 0
                || valor.indexOf('\r') >= 0
                || (!valor.isEmpty() && esInicioDeFormula(valor.charAt(0)));
        String limpio = valor.replace("\"", "\"\"");
        return necesitaComillas ? ESCAPE + limpio + ESCAPE : limpio;
    }

    private boolean esInicioDeFormula(char c) {
        return c == '=' || c == '+' || c == '-' || c == '@';
    }

    /** Lista vacía como respuesta válida, para que el CSV nunca rompa el flujo. */
    public <T> ResponseEntity<byte[]> vacio(String nombreArchivo, Class<T> tipo) {
        return descargar(nombreArchivo, new ArrayList<T>(), tipo);
    }
}
