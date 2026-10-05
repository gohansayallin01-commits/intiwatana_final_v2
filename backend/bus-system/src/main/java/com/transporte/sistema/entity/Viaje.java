package com.transporte.sistema.entity;

import com.transporte.sistema.enums.EstadoViaje;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Viaje programado: instancia específica de una ruta con fecha, bus y chofer.
 * Al crear el viaje se generan automáticamente los asientos.
 */
@Entity
@Table(name = "viajes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Viaje extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ruta_id", nullable = false)
    private Ruta ruta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bus_id", nullable = false)
    private Bus bus;

    /** Chofer asignado (Usuario con ROLE_CHOFER) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chofer_id")
    private Usuario chofer;

    @Column(name = "fecha_hora_salida", nullable = false)
    private LocalDateTime fechaHoraSalida;

    @Column(name = "fecha_hora_llegada_estimada")
    private LocalDateTime fechaHoraLlegadaEstimada;

    @Column(name = "fecha_hora_llegada_real")
    private LocalDateTime fechaHoraLlegadaReal;

    @Column(name = "precio_adulto", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioAdulto;

    @Column(name = "precio_nino", precision = 10, scale = 2)
    private BigDecimal precioNino;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    @Builder.Default
    private EstadoViaje estado = EstadoViaje.PROGRAMADO;

    @Column(name = "observaciones", length = 500)
    private String observaciones;

    /** Relación inversa: asientos generados para este viaje */
    @OneToMany(mappedBy = "viaje", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Asiento> asientos = new ArrayList<>();

    /** Boletos vendidos para este viaje */
    @OneToMany(mappedBy = "viaje", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Boleto> boletos = new ArrayList<>();
}
