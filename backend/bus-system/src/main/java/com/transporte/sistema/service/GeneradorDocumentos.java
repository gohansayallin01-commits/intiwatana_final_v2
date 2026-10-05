package com.transporte.sistema.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

/**
 * Generación compartida de códigos QR e identificadores de documentos.
 * Extraído para que la venta por portal/cajero y la confirmación de reservas
 * usen EXACTAMENTE el mismo formato (una sola implementación).
 */
@Slf4j
@Component
public class GeneradorDocumentos {

    public String generarNumeroBoleto(Long id) {
        String fecha = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return String.format("BOL-%s-%06d", fecha, id);
    }

    /** Imagen PNG del QR como data URI base64 (apta para <img> y para PDF). */
    public String generarQrBase64(String contenido) {
        byte[] png = generarQrPng(contenido, 200);
        if (png == null) {
            return null;
        }
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(png);
    }

    public byte[] generarQrPng(String contenido, int tamano) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(contenido, BarcodeFormat.QR_CODE, tamano, tamano);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Error generando QR para {}: {}", contenido, e.getMessage());
            return null;
        }
    }

    /** Decodifica un data URI base64 de imagen al binario PNG. */
    public byte[] decodificarDataUri(String dataUri) {
        if (dataUri == null) {
            return null;
        }
        int coma = dataUri.indexOf(',');
        String b64 = coma >= 0 ? dataUri.substring(coma + 1) : dataUri;
        try {
            return Base64.getDecoder().decode(b64);
        } catch (IllegalArgumentException e) {
            log.warn("QrImagenUrl no decodificable: {}", e.getMessage());
            return null;
        }
    }
}
