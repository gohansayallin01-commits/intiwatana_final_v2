package com.transporte.sistema.entity;

import com.transporte.sistema.enums.EstadoEncomienda;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Registro inmutable de cada cambio de estado de una encomienda.
 * Equivale al "tracking" que el cliente puede consultar.
 */
@Entity
@Table(name = "movimientos_encomienda")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovimientoEncomienda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "encomienda_id", nullable = false)
    private Encomienda encomienda;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    /** Sucursal donde ocurrió el movimiento (null = en tránsito) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sucursal_actual_id")
    private Sucursal sucursalActual;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior", length = 30)
    private EstadoEncomienda estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_nuevo", nullable = false, length = 30)
    private EstadoEncomienda estadoNuevo;

    @Column(name = "observacion", length = 500)
    private String observacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_responsable_id")
    private Usuario usuarioResponsable;
}
