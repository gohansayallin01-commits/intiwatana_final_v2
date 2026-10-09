package com.transporte.sistema.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Registro append-only de cada cambio de un parámetro de reserva/pago. */
@Entity
@Table(name = "parametro_venta_historial")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParametroVentaHistorial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "parametro", nullable = false, length = 60)
    private String parametro;

    @Column(name = "valor_anterior", length = 100)
    private String valorAnterior;

    @Column(name = "valor_nuevo", nullable = false, length = 100)
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