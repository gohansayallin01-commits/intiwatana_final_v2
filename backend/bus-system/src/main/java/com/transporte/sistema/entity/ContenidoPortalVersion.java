package com.transporte.sistema.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * Historial versionado del contenido editorial del Portal Público.
 *
 * Cada fila es una instantánea inmutable de la sección en el momento de una
 * acción (guardar borrador, publicar, archivar o restaurar). Nunca se elimina
 * físicamente: es la bitácora para comparación y reversión.
 */
@Entity
@Table(name = "contenido_portal_version")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ContenidoPortalVersion extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "contenido_id")
    private Long contenidoId;

    @Column(name = "seccion", nullable = false, length = 30)
    private String seccion;

    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado;

    /** CREAR_BORRADOR, ACTUALIZAR_BORRADOR, PUBLICAR, ARCHIVAR, RESTAURAR */
    @Column(name = "accion", nullable = false, length = 30)
    private String accion;

    /** Snapshot JSON del contenido en ese momento. */
    @Column(name = "contenido", nullable = false, columnDefinition = "text")
    private String contenido;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "username", length = 80)
    private String username;

    @Column(name = "rol", length = 40)
    private String rol;

    @Column(name = "observacion", length = 500)
    private String observacion;

    @Column(name = "fecha_hora", nullable = false)
    @Builder.Default
    private LocalDateTime fechaHora = LocalDateTime.now();
}
