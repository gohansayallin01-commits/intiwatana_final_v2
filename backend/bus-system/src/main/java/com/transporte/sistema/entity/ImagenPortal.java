package com.transporte.sistema.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * Metadatos de un recurso gráfico subido y validado para el Portal Público.
 *
 * El binario se guarda a través de la abstracción {@code PortalStorageService};
 * aquí solo se registran referencias y metadatos de trazabilidad (licencia,
 * autor, checksum, dimensiones…). No se publica ninguna imagen sin autorización.
 */
@Entity
@Table(name = "imagenes_portal")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ImagenPortal extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "seccion", length = 30)
    private String seccion;

    @Column(name = "nombre_interno", nullable = false, length = 120)
    private String nombreInterno;

    @Column(name = "url", nullable = false, length = 500)
    private String url;

    @Column(name = "miniatura_url", length = 500)
    private String miniaturaUrl;

    @Column(name = "mime", nullable = false, length = 50)
    private String mime;

    @Column(name = "peso_bytes", nullable = false)
    private Long pesoBytes;

    @Column(name = "ancho")
    private Integer ancho;

    @Column(name = "alto")
    private Integer alto;

    @Column(name = "checksum", nullable = false, length = 64)
    private String checksum;

    @Column(name = "texto_alt", length = 300)
    private String textoAlt;

    @Column(name = "autor", length = 150)
    private String autor;

    @Column(name = "fuente", length = 200)
    private String fuente;

    @Column(name = "licencia", length = 100)
    private String licencia;

    @Column(name = "url_origen", length = 500)
    private String urlOrigen;

    @Column(name = "autorizacion_comercial", nullable = false)
    @Builder.Default
    private Boolean autorizacionComercial = false;

    @Column(name = "subido_por", length = 80)
    private String subidoPor;
}
