package com.transporte.sistema.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Parámetro de reserva/pago configurable SOLO por el ADMIN (respaldo en
 * {@code parametro_venta}). Los valores siempre tienen rango acotado.
 */
@Entity
@Table(name = "parametro_venta")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParametroVenta {

    @Id
    @Column(name = "parametro", length = 60)
    private String parametro;

    @Column(name = "valor", nullable = false, length = 100)
    private String valor;

    @Column(name = "rango_min", nullable = false)
    private Integer rangoMin;

    @Column(name = "rango_max", nullable = false)
    private Integer rangoMax;

    @Column(name = "unidad", nullable = false, length = 20)
    @Builder.Default
    private String unidad = "minutos";

    @Column(name = "actualizado_por", length = 60)
    private String actualizadoPor;

    @Column(name = "motivo", length = 300)
    private String motivo;

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;
}