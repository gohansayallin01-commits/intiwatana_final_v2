package com.transporte.sistema.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Registro append-only de cada cambio de la configuración Yape. */
@Entity
@Table(name = "configuracion_yape_historial")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfiguracionYapeHistorial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "configuracion_id", nullable = false)
    private Long configuracionId;

    @Column(name = "campo", nullable = false, length = 50)
    private String campo;

    @Column(name = "valor_anterior")
    private String valorAnterior;

    @Column(name = "valor_nuevo", nullable = false)
    private String valorNuevo;

    @Column(name = "actualizado_por", length = 60)
    private String actualizadoPor;

    @Column(name = "rol", length = 30)
    private String rol;

    @Column(name = "motivo", length = 300)
    private String motivo;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;
}