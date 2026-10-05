package com.transporte.sistema.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.transporte.sistema.entity.Asiento;
import com.transporte.sistema.entity.Boleto;
import com.transporte.sistema.entity.Retencion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Generación de documentos PDF del flujo comercial con OpenPDF.
 * Dos documentos claramente distintos:
 *   - BOLETO DE VIAJE (sólo reserva CONFIRMADA; incluye QR de embarque)
 *   - CONSTANCIA DE RESERVA PENDIENTE DE PAGO (sin QR de embarque, no válido para embarcar)
 */
@Service
@RequiredArgsConstructor
public class PdfVentaService {

    private static final String EMPRESA = "INTIWATANA";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final Color VERDE = new Color(18, 115, 105);
    private static final Color GRIS = new Color(90, 100, 100);
    private static final Color ROJO = new Color(170, 40, 40);

    private final GeneradorDocumentos generadorDocumentos;

    // ── Constancia (pendiente de pago) ──────────────────────────────────────

    public byte[] generarConstancia(Retencion r, List<Asiento> asientos) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 40, 40, 40, 40);
        try {
            PdfWriter.getInstance(doc, baos);
            doc.open();

            titulo(doc, EMPRESA);
            parrafo(doc, "CONSTANCIA DE RESERVA PENDIENTE DE PAGO",
                    new Font(Font.HELVETICA, 15, Font.BOLD, VERDE), Element.ALIGN_CENTER);
            parrafo(doc, "FALTA PAGAR · NO VÁLIDO PARA EMBARQUE",
                    new Font(Font.HELVETICA, 11, Font.BOLD, ROJO), Element.ALIGN_CENTER);
            doc.add(new Paragraph(" "));

            PdfPTable t = tabla();
            fila(t, "Referencia", r.getReferencia());
            fila(t, "Estado", "PENDIENTE DE PAGO");
            fila(t, "Comprador", r.getCompradorNombres() + " " + r.getCompradorApellidos());
            fila(t, "Documento", maskDocumento(r.getCompradorDocumento()));
            fila(t, "Origen", r.getViaje().getRuta().getOrigen().getCiudad() + " - "
                    + r.getViaje().getRuta().getOrigen().getNombre());
            fila(t, "Destino", r.getViaje().getRuta().getDestino().getCiudad() + " - "
                    + r.getViaje().getRuta().getDestino().getNombre());
            fila(t, "Fecha y hora de salida", fmt(r.getViaje().getFechaHoraSalida()));
            fila(t, "Bus / Placa", r.getViaje().getBus().getPlaca());
            fila(t, "Asiento(s)", r.getAsientosNumeros());
            fila(t, "Importe pendiente", "S/ " + monto(r.getTotal()));
            fila(t, "Válido hasta", fmt(r.getExpiraEn()));
            doc.add(t);

            doc.add(new Paragraph(" "));
            parrafo(doc, "Instrucciones", new Font(Font.HELVETICA, 11, Font.BOLD, GRIS), Element.ALIGN_LEFT);
            parrafo(doc,
                    "Presente esta constancia en cualquier agencia INTIWATANA para pagar y confirmar su reserva. "
                            + "Al confirmarse el pago se emitirá su boleto de viaje con el código QR de embarque. "
                            + "Si no paga antes del vencimiento, los asientos serán liberados automáticamente.",
                    new Font(Font.HELVETICA, 10, Font.NORMAL, Color.BLACK), Element.ALIGN_LEFT);
            doc.add(new Paragraph(" "));
            parrafo(doc, "Este documento NO habilita el embarque. Es una constancia de reserva.",
                    new Font(Font.HELVETICA, 9, Font.ITALIC, ROJO), Element.ALIGN_CENTER);

            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar la constancia PDF", e);
        }
    }

    // ── Boleto confirmado ───────────────────────────────────────────────────

    public byte[] generarBoleto(Boleto b) {
        Asiento a = b.getAsiento();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 40, 40, 40, 40);
        try {
            PdfWriter.getInstance(doc, baos);
            doc.open();

            titulo(doc, EMPRESA);
            parrafo(doc, "BOLETO DE VIAJE",
                    new Font(Font.HELVETICA, 16, Font.BOLD, VERDE), Element.ALIGN_CENTER);
            parrafo(doc, "PAGO CONFIRMADO · VÁLIDO PARA EMBARQUE",
                    new Font(Font.HELVETICA, 11, Font.BOLD, VERDE), Element.ALIGN_CENTER);
            doc.add(new Paragraph(" "));

            PdfPTable t = tabla();
            fila(t, "N° de boleto", b.getNumeroBoleto());
            fila(t, "Pasajero", b.getCliente().getNombreCompleto());
            fila(t, "Documento", maskDocumento(b.getCliente().getDniRuc()));
            fila(t, "Origen", b.getViaje().getRuta().getOrigen().getCiudad() + " - "
                    + b.getViaje().getRuta().getOrigen().getNombre());
            fila(t, "Destino", b.getViaje().getRuta().getDestino().getCiudad() + " - "
                    + b.getViaje().getRuta().getDestino().getNombre());
            fila(t, "Fecha y hora de salida", fmt(b.getViaje().getFechaHoraSalida()));
            fila(t, "Bus / Placa", b.getViaje().getBus().getPlaca());
            fila(t, "ASIENTO", a.getNumeroAsiento());
            fila(t, "Importe pagado", "S/ " + monto(b.getPrecioPagado()));
            if (b.getPago() != null && b.getPago().getMetodo() != null) {
                fila(t, "Método de pago", b.getPago().getMetodo().name());
            }
            fila(t, "Fecha de emisión", fmt(b.getCreatedAt() != null ? b.getCreatedAt() : LocalDateTime.now()));
            fila(t, "Canal", b.getCajero() != null ? "CAJERO / AGENCIA" : "WEB");
            doc.add(t);

            doc.add(new Paragraph(" "));
            byte[] qr = generadorDocumentos.decodificarDataUri(b.getQrImagenUrl());
            if (qr != null) {
                Image img = Image.getInstance(qr);
                img.scaleToFit(130, 130);
                img.setAlignment(Element.ALIGN_CENTER);
                doc.add(img);
                parrafo(doc, b.getCodigoQr(),
                        new Font(Font.COURIER, 10, Font.BOLD, Color.BLACK), Element.ALIGN_CENTER);
            }
            doc.add(new Paragraph(" "));
            parrafo(doc, "Presente este boleto (impreso o digital) y su documento al momento del embarque.",
                    new Font(Font.HELVETICA, 10, Font.NORMAL, GRIS), Element.ALIGN_CENTER);

            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el boleto PDF", e);
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private void titulo(Document doc, String texto) throws Exception {
        parrafo(doc, texto, new Font(Font.HELVETICA, 20, Font.BOLD, VERDE), Element.ALIGN_CENTER);
        parrafo(doc, "Sistema de Transporte Interprovincial",
                new Font(Font.HELVETICA, 9, Font.NORMAL, GRIS), Element.ALIGN_CENTER);
    }

    private void parrafo(Document doc, String texto, Font font, int align) throws Exception {
        Paragraph p = new Paragraph(texto, font);
        p.setAlignment(align);
        doc.add(p);
    }

    private PdfPTable tabla() throws Exception {
        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(100);
        t.setWidths(new float[]{1.6f, 3.4f});
        return t;
    }

    private void fila(PdfPTable t, String etiqueta, String valor) {
        PdfPCell c1 = new PdfPCell(new Paragraph(etiqueta, new Font(Font.HELVETICA, 10, Font.BOLD, GRIS)));
        c1.setPadding(6);
        c1.setBorderColor(new Color(210, 215, 215));
        PdfPCell c2 = new PdfPCell(new Paragraph(valor != null ? valor : "—",
                new Font(Font.HELVETICA, 10, Font.NORMAL, Color.BLACK)));
        c2.setPadding(6);
        c2.setBorderColor(new Color(210, 215, 215));
        t.addCell(c1);
        t.addCell(c2);
    }

    private String fmt(LocalDateTime fecha) {
        return fecha == null ? "—" : fecha.format(FMT);
    }

    private String monto(BigDecimal m) {
        return m == null ? "0.00" : m.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    /** Enmascara el documento: conserva sólo los 4 primeros caracteres. */
    private String maskDocumento(String doc) {
        if (doc == null || doc.length() <= 4) {
            return "****";
        }
        return doc.substring(0, 4) + "*".repeat(doc.length() - 4);
    }
}
