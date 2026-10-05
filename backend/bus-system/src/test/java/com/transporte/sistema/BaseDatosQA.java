package com.transporte.sistema;

/**
 * Guard de aislamiento de base de datos para las pruebas de integracion (*IT).
 *
 * PROBLEMA QUE RESUELVE: las comprobaciones "la base termina en _qa" hechas en un
 * @Test o en un @BeforeAll llegan TARDE. Para entonces Spring ya arranco el
 * contexto y el DataInitializer ya escribio en la base. Un IT lanzado sin querer
 * contra transporte_bd habria polluting antes de que la comprobacion fallara.
 *
 * SOLUCION: un bloque estatico en la clase IT. El inicializador estatico se
 * ejecuta al CARGAR la clase, que ocurre durante el descubrimiento de pruebas,
 * antes de que Spring levante el contexto. Si la base no es de QA, el fallo sale
 * antes de abrir una sola conexion de escritura.
 *
 * La URL se lee del mismo origen que usa Spring (variable de entorno DB_URL, o
 * la propiedad de sistema db.url si se pasa por -D).
 */
final class BaseDatosQA {

    private BaseDatosQA() {
    }

    /**
     * @param nombreClase nombre de la IT, solo para el mensaje de error
     * @throws IllegalStateException si la base no es claramente de QA
     */
    static void exigirBaseQA(String nombreClase) {
        String url = urlConfigurada();
        if (url == null || url.isBlank()) {
            throw new IllegalStateException(
                    nombreClase + " no se ejecuta: no hay DB_URL definida. "
                            + "Las pruebas de integracion requieren una base propia "
                            + "cuyo nombre termine en _qa.");
        }
        String base = nombreBase(url);
        if (!base.toLowerCase().endsWith("_qa")) {
            throw new IllegalStateException(
                    nombreClase + " no se ejecuta sobre '" + base + "'. "
                            + "Las pruebas de integracion solo pueden correr sobre una base "
                            + "aislada cuyo nombre termine en _qa (por ejemplo "
                            + "intiwatana_recorrido_qa). Se aborta ANTES de escribir nada.");
        }
        System.out.println("[QA] " + nombreClase + " autorizado sobre la base " + base);
    }

    private static String urlConfigurada() {
        String porPropiedad = System.getProperty("db.url");
        if (esUsable(porPropiedad)) {
            return porPropiedad;
        }
        String porEntorno = System.getenv("DB_URL");
        return esUsable(porEntorno) ? porEntorno : null;
    }

    /** Descarta null, vacios y los "${...}" sin resolver que deja Maven. */
    private static boolean esUsable(String valor) {
        return valor != null && !valor.isBlank() && !valor.startsWith("${");
    }

    /**
     * Extrae el nombre de la base de un JDBC URL de PostgreSQL.
     * jdbc:postgresql://host:puerto/mi_base?param=1 -> mi_base
     */
    static String nombreBase(String jdbcUrl) {
        String s = jdbcUrl.trim();
        int interrogacion = s.indexOf('?');
        if (interrogacion >= 0) {
            s = s.substring(0, interrogacion);
        }
        int ultimaBarra = s.lastIndexOf('/');
        return ultimaBarra >= 0 && ultimaBarra < s.length() - 1
                ? s.substring(ultimaBarra + 1)
                : "";
    }
}
