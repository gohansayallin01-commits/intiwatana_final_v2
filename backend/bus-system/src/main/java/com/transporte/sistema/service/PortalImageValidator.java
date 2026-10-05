package com.transporte.sistema.service;

import com.transporte.sistema.exception.NegocioException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.Set;

/**
 * Validación real de imágenes por contenido (no por extensión).
 *
 * - Detecta el tipo por firma binaria (magic bytes): JPEG, PNG, WebP.
 * - Rechaza SVG, HTML, JavaScript, ejecutables, documentos, y nombres con
 *   doble extensión o rutas relativas.
 * - Extrae dimensiones reales (JPEG/PNG vía ImageIO; WebP vía cabecera).
 * - Calcula checksum SHA-256.
 * - Genera miniatura optimizada para JPEG/PNG.
 */
@Component
@Slf4j
public class PortalImageValidator {

    private static final Set<String> MIMES_PERMITIDOS = Set.of("image/jpeg", "image/png", "image/webp");

    private final long maxBytes;
    private final int minAncho;
    private final int minAlto;
    private final int maxAncho;
    private final int maxAlto;

    public PortalImageValidator(
            @Value("${app.portal.imagen.max-bytes:5242880}") long maxBytes,
            @Value("${app.portal.imagen.min-ancho:200}") int minAncho,
            @Value("${app.portal.imagen.min-alto:200}") int minAlto,
            @Value("${app.portal.imagen.max-ancho:6000}") int maxAncho,
            @Value("${app.portal.imagen.max-alto:6000}") int maxAlto) {
        this.maxBytes = maxBytes;
        this.minAncho = minAncho;
        this.minAlto = minAlto;
        this.maxAncho = maxAncho;
        this.maxAlto = maxAlto;
    }

    @Getter
    public static class ImagenValidada {
        private final String mime;
        private final String extension;
        private final int ancho;
        private final int alto;
        private final String checksum;
        private final byte[] miniatura;
        private final String miniaturaExt;

        public ImagenValidada(String mime, String extension, int ancho, int alto,
                              String checksum, byte[] miniatura, String miniaturaExt) {
            this.mime = mime;
            this.extension = extension;
            this.ancho = ancho;
            this.alto = alto;
            this.checksum = checksum;
            this.miniatura = miniatura;
            this.miniaturaExt = miniaturaExt;
        }
    }

    public ImagenValidada validar(byte[] bytes, String nombreOriginal) {
        if (bytes == null || bytes.length == 0) {
            throw new NegocioException("El archivo está vacío");
        }
        if (bytes.length > maxBytes) {
            throw new NegocioException("La imagen supera el tamaño máximo permitido (" + (maxBytes / 1024 / 1024) + " MB)");
        }
        validarNombre(nombreOriginal);

        String mime = detectarMime(bytes);
        if (mime == null || !MIMES_PERMITIDOS.contains(mime)) {
            throw new NegocioException("Formato no permitido. Solo se aceptan WebP, JPEG o PNG reales.");
        }

        int[] dim = dimensiones(bytes, mime);
        int ancho = dim[0];
        int alto = dim[1];
        if (ancho <= 0 || alto <= 0) {
            throw new NegocioException("No se pudieron leer las dimensiones reales de la imagen.");
        }
        if (ancho < minAncho || alto < minAlto) {
            throw new NegocioException("Imagen demasiado pequeña: mínimo " + minAncho + "×" + minAlto + " px.");
        }
        if (ancho > maxAncho || alto > maxAlto) {
            throw new NegocioException("Imagen demasiado grande: máximo " + maxAncho + "×" + maxAlto + " px.");
        }

        String checksum = sha256(bytes);
        String extension = switch (mime) {
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> "jpg";
        };

        byte[] miniatura = null;
        String miniaturaExt = null;
        if (!"image/webp".equals(mime)) {
            miniatura = generarMiniatura(bytes, extension);
            miniaturaExt = extension;
        }

        return new ImagenValidada(mime, extension, ancho, alto, checksum, miniatura, miniaturaExt);
    }

    private void validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) return;
        String n = nombre.replace("\\", "/");
        String base = n.substring(n.lastIndexOf('/') + 1).toLowerCase(Locale.ROOT);
        if (base.contains("../") || base.contains("..") || base.startsWith(".")) {
            throw new NegocioException("Nombre de archivo no permitido.");
        }
        long puntos = base.chars().filter(c -> c == '.').count();
        if (puntos > 1) {
            throw new NegocioException("Nombre con doble extensión no permitido.");
        }
    }

    private String detectarMime(byte[] b) {
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G'
                && (b[4] & 0xFF) == 0x0D && (b[5] & 0xFF) == 0x0A && (b[6] & 0xFF) == 0x1A && (b[7] & 0xFF) == 0x0A) {
            return "image/png";
        }
        if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return "image/webp";
        }
        return null;
    }

    private int[] dimensiones(byte[] bytes, String mime) {
        if ("image/webp".equals(mime)) {
            int[] d = dimensionesWebp(bytes);
            if (d != null) return d;
        }
        try (ByteArrayInputStream in = new ByteArrayInputStream(bytes)) {
            BufferedImage img = ImageIO.read(in);
            if (img != null) return new int[]{img.getWidth(), img.getHeight()};
        } catch (Exception e) {
            log.debug("ImageIO no pudo leer dimensiones: {}", e.getMessage());
        }
        return new int[]{0, 0};
    }

    /** Parser de cabecera WebP (VP8 / VP8L / VP8X). */
    private int[] dimensionesWebp(byte[] b) {
        try {
            int pos = 12; // tras RIFF + size + WEBP
            while (pos + 8 <= b.length) {
                String fourCC = new String(b, pos, 4, java.nio.charset.StandardCharsets.US_ASCII);
                int size = le32(b, pos + 4);
                int data = pos + 8;
                if ("VP8 ".equals(fourCC)) {
                    int w = (b[data + 6] & 0xFF) | ((b[data + 7] & 0xFF) << 8);
                    int h = (b[data + 8] & 0xFF) | ((b[data + 9] & 0xFF) << 8);
                    return new int[]{w & 0x3FFF, h & 0x3FFF};
                }
                if ("VP8L".equals(fourCC)) {
                    int b0 = b[data + 1] & 0xFF, b1 = b[data + 2] & 0xFF,
                        b2 = b[data + 3] & 0xFF, b3 = b[data + 4] & 0xFF;
                    int bits = b0 | (b1 << 8) | (b2 << 16) | (b3 << 24);
                    int w = (bits & 0x3FFF) + 1;
                    int h = ((bits >> 14) & 0x3FFF) + 1;
                    return new int[]{w, h};
                }
                if ("VP8X".equals(fourCC)) {
                    int w = (b[data + 4] & 0xFF) | ((b[data + 5] & 0xFF) << 8) | ((b[data + 6] & 0xFF) << 16);
                    int h = (b[data + 7] & 0xFF) | ((b[data + 8] & 0xFF) << 8) | ((b[data + 9] & 0xFF) << 16);
                    return new int[]{w + 1, h + 1};
                }
                // chunk desconocido: saltar (los chunks se alinean a 2 bytes)
                pos = data + size + (size % 2);
            }
        } catch (Exception e) {
            log.debug("Parser WebP falló: {}", e.getMessage());
        }
        return null;
    }

    private int le32(byte[] b, int i) {
        return (b[i] & 0xFF) | ((b[i + 1] & 0xFF) << 8) | ((b[i + 2] & 0xFF) << 16) | ((b[i + 3] & 0xFF) << 24);
    }

    private byte[] generarMiniatura(byte[] bytes, String extension) {
        try (ByteArrayInputStream in = new ByteArrayInputStream(bytes)) {
            BufferedImage original = ImageIO.read(in);
            if (original == null) return null;
            int maxW = 400;
            double ratio = Math.min(1.0, (double) maxW / original.getWidth());
            int w = Math.max(1, (int) Math.round(original.getWidth() * ratio));
            int h = Math.max(1, (int) Math.round(original.getHeight() * ratio));
            BufferedImage thumb = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = thumb.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(original, 0, 0, w, h, null);
            g.dispose();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            String fmt = "png".equals(extension) ? "png" : "jpg";
            ImageIO.write(thumb, fmt, out);
            return out.toByteArray();
        } catch (Exception e) {
            log.warn("No se pudo generar la miniatura: {}", e.getMessage());
            return null;
        }
    }

    private String sha256(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(bytes);
            StringBuilder sb = new StringBuilder();
            for (byte x : hash) sb.append(String.format("%02x", x));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo calcular el checksum", e);
        }
    }
}
