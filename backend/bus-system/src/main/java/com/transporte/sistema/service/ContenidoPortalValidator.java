package com.transporte.sistema.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.transporte.sistema.exception.NegocioException;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Valida el JSON editorial por sección antes de guardarlo o publicarlo.
 *
 * Principios:
 *  - Lista blanca de claves: cualquier clave desconocida se rechaza (evita que
 *    el contenido editorial transporte datos operativos o campos arbitrarios).
 *  - Texto plano: se rechaza HTML/JS y caracteres de control; sin URL relativas
 *    peligrosas. Los enlaces solo admiten http(s):// o rutas internas "/".
 *  - Límites de longitud y de número de elementos.
 *  - Sin confianza en el frontend: el Portal Público solo recibe lo publicado.
 */
@Component
public class ContenidoPortalValidator {

    private static final Pattern URL_SEGURA = Pattern.compile("^(https?://|/)[^\\s<>\"']+$", Pattern.CASE_INSENSITIVE);
    private static final Pattern CORREO = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Set<String> ICONOS_PERMITIDOS = Set.of(
            "ticket", "package", "mappin", "shield", "clock", "user", "bus",
            "route", "heart", "star", "check", "info", "phone", "mail");

    private static final Map<String, Set<String>> CLAVES = Map.of(
            "HERO", Set.of("titulo", "subtitulo", "imagenFondoUrl", "imagenBusUrl",
                    "imagenAlt", "autor", "fuente", "licencia", "atribucion", "mostrarBus"),
            "DESTINOS", Set.of("destinos"),
            "BENEFICIOS", Set.of("beneficios"),
            "NOSOTROS", Set.of("titulo", "descripcion", "imagenUrl", "imagenAlt",
                    "autor", "fuente", "licencia"),
            "CONTACTO", Set.of("telefono", "correo", "direccion", "horario", "redes", "visible"),
            "FOOTER", Set.of("descripcion", "enlaces", "infoLegal", "creditosFotograficos"),
            "LOGIN", Set.of("imagenUrl", "imagenMobileUrl", "imagenAlt", "tituloInstitucional",
                    "frasePrincipal", "textoComplementario", "servicios", "serviciosVisible",
                    "focoX", "focoY", "overlay", "autor", "fuente", "licencia", "atribucion")
    );

    private static final Set<String> SECCIONES = CLAVES.keySet();

    public Set<String> seccionesValidas() {
        return SECCIONES;
    }

    public void validar(String seccion, JsonNode root) {
        if (seccion == null || !SECCIONES.contains(seccion)) {
            throw new NegocioException("Sección editorial inválida");
        }
        if (root == null || !root.isObject()) {
            throw new NegocioException("El contenido debe ser un objeto JSON");
        }
        Set<String> permitidas = CLAVES.get(seccion);
        Iterator<String> it = root.fieldNames();
        while (it.hasNext()) {
            String campo = it.next();
            if (!permitidas.contains(campo)) {
                throw new NegocioException("Campo no permitido en " + seccion + ": " + campo);
            }
        }
        switch (seccion) {
            case "HERO" -> validarHero(root);
            case "DESTINOS" -> validarDestinos(root);
            case "BENEFICIOS" -> validarBeneficios(root);
            case "NOSOTROS" -> validarNosotros(root);
            case "CONTACTO" -> validarContacto(root);
            case "FOOTER" -> validarFooter(root);
            case "LOGIN" -> validarLogin(root);
            default -> throw new NegocioException("Sección editorial inválida");
        }
    }

    private void validarHero(JsonNode r) {
        textoObligatorio(r, "titulo", 80);
        textoObligatorio(r, "subtitulo", 160);
        urlObligatoria(r, "imagenFondoUrl", 500);
        urlOpcional(r, "imagenBusUrl", 500);
        textoOpcional(r, "imagenAlt", 200);
        textoOpcional(r, "autor", 150);
        textoOpcional(r, "fuente", 200);
        textoOpcional(r, "licencia", 100);
        textoOpcional(r, "atribucion", 300);
        booleanoOpcional(r, "mostrarBus");
    }

    private void validarDestinos(JsonNode r) {
        JsonNode arr = r.get("destinos");
        if (arr == null || !arr.isArray()) {
            throw new NegocioException("'destinos' debe ser una lista");
        }
        if (arr.size() > 12) {
            throw new NegocioException("Máximo 12 destinos destacados");
        }
        for (JsonNode d : arr) {
            if (!d.isObject()) throw new NegocioException("Cada destino debe ser un objeto");
            Set<String> claves = Set.of("titulo", "texto", "imagenUrl", "imagenAlt", "orden", "visible");
            verificarClaves("DESTINO", d, claves);
            textoObligatorio(d, "titulo", 80);
            textoOpcional(d, "texto", 240);
            urlOpcional(d, "imagenUrl", 500);
            textoOpcional(d, "imagenAlt", 200);
            enteroOpcional(d, "orden");
            booleanoOpcional(d, "visible");
        }
    }

    private void validarBeneficios(JsonNode r) {
        JsonNode arr = r.get("beneficios");
        if (arr == null || !arr.isArray()) {
            throw new NegocioException("'beneficios' debe ser una lista");
        }
        if (arr.size() > 12) {
            throw new NegocioException("Máximo 12 beneficios");
        }
        for (JsonNode b : arr) {
            if (!b.isObject()) throw new NegocioException("Cada beneficio debe ser un objeto");
            Set<String> claves = Set.of("titulo", "descripcion", "icono", "orden", "visible");
            verificarClaves("BENEFICIO", b, claves);
            textoObligatorio(b, "titulo", 80);
            textoOpcional(b, "descripcion", 240);
            String icono = textoOpcional(b, "icono", 30);
            if (icono != null && !icono.isBlank() && !ICONOS_PERMITIDOS.contains(icono.toLowerCase())) {
                throw new NegocioException("Icono no permitido: " + icono);
            }
            enteroOpcional(b, "orden");
            booleanoOpcional(b, "visible");
        }
    }

    private void validarNosotros(JsonNode r) {
        textoObligatorio(r, "titulo", 80);
        textoObligatorio(r, "descripcion", 1200);
        urlOpcional(r, "imagenUrl", 500);
        textoOpcional(r, "imagenAlt", 200);
        textoOpcional(r, "autor", 150);
        textoOpcional(r, "fuente", 200);
        textoOpcional(r, "licencia", 100);
    }

    private void validarContacto(JsonNode r) {
        textoOpcional(r, "telefono", 30);
        String correo = textoOpcional(r, "correo", 120);
        if (correo != null && !correo.isBlank() && !CORREO.matcher(correo).matches()) {
            throw new NegocioException("Correo de contacto inválido");
        }
        textoOpcional(r, "direccion", 200);
        textoOpcional(r, "horario", 200);
        booleanoOpcional(r, "visible");
        JsonNode redes = r.get("redes");
        if (redes != null) {
            if (!redes.isArray()) throw new NegocioException("'redes' debe ser una lista");
            if (redes.size() > 10) throw new NegocioException("Máximo 10 redes");
            for (JsonNode red : redes) {
                verificarClaves("RED", red, Set.of("nombre", "url"));
                textoObligatorio(red, "nombre", 40);
                urlObligatoria(red, "url", 500);
            }
        }
    }

    private void validarFooter(JsonNode r) {
        textoOpcional(r, "descripcion", 400);
        textoOpcional(r, "infoLegal", 1000);
        textoOpcional(r, "creditosFotograficos", 1000);
        JsonNode enlaces = r.get("enlaces");
        if (enlaces != null) {
            if (!enlaces.isArray()) throw new NegocioException("'enlaces' debe ser una lista");
            if (enlaces.size() > 20) throw new NegocioException("Máximo 20 enlaces");
            for (JsonNode e : enlaces) {
                verificarClaves("ENLACE", e, Set.of("texto", "url"));
                textoObligatorio(e, "texto", 60);
                urlObligatoria(e, "url", 500);
            }
        }
    }

    private void validarLogin(JsonNode r) {
        textoObligatorio(r, "tituloInstitucional", 80);
        textoObligatorio(r, "frasePrincipal", 200);
        textoOpcional(r, "textoComplementario", 500);
        urlOpcional(r, "imagenUrl", 500);
        urlOpcional(r, "imagenMobileUrl", 500);
        textoOpcional(r, "imagenAlt", 200);
        textoOpcional(r, "autor", 150);
        textoOpcional(r, "fuente", 200);
        textoOpcional(r, "licencia", 100);
        textoOpcional(r, "atribucion", 300);
        booleanoOpcional(r, "serviciosVisible");
        numeroRango(r, "focoX", 0, 100);
        numeroRango(r, "focoY", 0, 100);
        numeroRango(r, "overlay", 0, 1);
        JsonNode servicios = r.get("servicios");
        if (servicios != null) {
            if (!servicios.isArray()) throw new NegocioException("'servicios' debe ser una lista");
            if (servicios.size() > 8) throw new NegocioException("Máximo 8 servicios en el Login");
            for (JsonNode s : servicios) {
                if (!s.isObject()) throw new NegocioException("Cada servicio debe ser un objeto");
                verificarClaves("SERVICIO", s, Set.of("titulo", "subtitulo", "icono", "visible"));
                textoObligatorio(s, "titulo", 60);
                textoOpcional(s, "subtitulo", 120);
                String icono = textoOpcional(s, "icono", 30);
                if (icono != null && !icono.isBlank() && !ICONOS_PERMITIDOS.contains(icono.toLowerCase())) {
                    throw new NegocioException("Icono no permitido: " + icono);
                }
                booleanoOpcional(s, "visible");
            }
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private void verificarClaves(String contexto, JsonNode obj, Set<String> permitidas) {
        Iterator<String> it = obj.fieldNames();
        while (it.hasNext()) {
            String campo = it.next();
            if (!permitidas.contains(campo)) {
                throw new NegocioException("Campo no permitido en " + contexto + ": " + campo);
            }
        }
    }

    private String textoObligatorio(JsonNode r, String campo, int max) {
        JsonNode n = r.get(campo);
        if (n == null || !n.isTextual() || n.asText().isBlank()) {
            throw new NegocioException("El campo '" + campo + "' es obligatorio");
        }
        return sanitizar(n.asText(), campo, max);
    }

    private String textoOpcional(JsonNode r, String campo, int max) {
        JsonNode n = r.get(campo);
        if (n == null || n.isNull()) return null;
        if (!n.isTextual()) {
            throw new NegocioException("El campo '" + campo + "' debe ser texto");
        }
        return sanitizar(n.asText(), campo, max);
    }

    private String sanitizar(String valor, String campo, int max) {
        String v = valor.strip();
        if (v.length() > max) {
            throw new NegocioException("El campo '" + campo + "' supera " + max + " caracteres");
        }
        if (v.indexOf('<') >= 0 || v.indexOf('>') >= 0) {
            throw new NegocioException("El campo '" + campo + "' no admite HTML");
        }
        for (int i = 0; i < v.length(); i++) {
            char c = v.charAt(i);
            if (c < 0x20 && c != '\n' && c != '\t') {
                throw new NegocioException("El campo '" + campo + "' contiene caracteres no permitidos");
            }
        }
        return v;
    }

    private String urlObligatoria(JsonNode r, String campo, int max) {
        JsonNode n = r.get(campo);
        if (n == null || !n.isTextual() || n.asText().isBlank()) {
            throw new NegocioException("El campo '" + campo + "' es obligatorio");
        }
        return validarUrl(n.asText().strip(), campo, max);
    }

    private String urlOpcional(JsonNode r, String campo, int max) {
        JsonNode n = r.get(campo);
        if (n == null || n.isNull() || (n.isTextual() && n.asText().isBlank())) return null;
        if (!n.isTextual()) {
            throw new NegocioException("El campo '" + campo + "' debe ser una URL");
        }
        return validarUrl(n.asText().strip(), campo, max);
    }

    private String validarUrl(String url, String campo, int max) {
        if (url.length() > max) {
            throw new NegocioException("El campo '" + campo + "' supera " + max + " caracteres");
        }
        if (!URL_SEGURA.matcher(url).matches()) {
            throw new NegocioException("URL no permitida en '" + campo + "'");
        }
        return url;
    }

    private void booleanoOpcional(JsonNode r, String campo) {
        JsonNode n = r.get(campo);
        if (n != null && !n.isNull() && !n.isBoolean()) {
            throw new NegocioException("El campo '" + campo + "' debe ser booleano");
        }
    }

    private void enteroOpcional(JsonNode r, String campo) {
        JsonNode n = r.get(campo);
        if (n != null && !n.isNull() && !n.isInt() && !n.isLong()) {
            throw new NegocioException("El campo '" + campo + "' debe ser un entero");
        }
    }

    private void numeroRango(JsonNode r, String campo, double min, double max) {
        JsonNode n = r.get(campo);
        if (n == null || n.isNull()) return;
        if (!n.isNumber()) {
            throw new NegocioException("El campo '" + campo + "' debe ser un número");
        }
        double v = n.asDouble();
        if (v < min || v > max) {
            throw new NegocioException("El campo '" + campo + "' debe estar entre " + min + " y " + max);
        }
    }
}
