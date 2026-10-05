package com.transporte.sistema.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * Estado actual del contenido editorial de una sección del Portal Público.
 *
 * Mantiene simultáneamente:
 *  - {@code contenidoBorrador}: la copia editable (no visible al visitante).
 *  - {@code contenidoPublicado}: la copia viva (única visible al visitante).
 *
 * No almacena datos operativos (precios, horarios, viajes, etc.).
 */
@Entity
@Table(name = "contenidos_portal")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ContenidoPortal extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** HERO, DESTINOS, BENEFICIOS, NOSOTROS, CONTACTO, FOOTER */
    @Column(name = "seccion", nullable = false, unique = true, length = 30)
    private String seccion;

    /** BORRADOR, PUBLICADO, ARCHIVADO */
    @Column(name = "estado", nullable = false, length = 20)
    @Builder.Default
    private String estado = "BORRADOR";

    /** Número de la última versión publicada (0 = nunca publicado). */
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Integer version = 0;

    @Column(name = "contenido_borrador", columnDefinition = "text")
    private String contenidoBorrador;

    @Column(name = "contenido_publicado", columnDefinition = "text")
    private String contenidoPublicado;

    /** Autor declarado del contenido (no necesariamente el usuario del sistema). */
    @Column(name = "autor", length = 120)
    private String autor;

    @Column(name = "actualizado_por", length = 120)
    private String actualizadoPor;

    @Column(name = "publicado_en")
    private LocalDateTime publicadoEn;
}
