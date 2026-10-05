package com.transporte.sistema.entity;

import com.transporte.sistema.enums.TipoBus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * Buses/vehículos de la empresa.
 * La configuración de asientos se define al crear el viaje.
 */
@Entity
@Table(name = "buses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder

public class Bus extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Placa única del vehículo, ej: "A6Z-123" */
    @Column(name = "placa", nullable = false, unique = true, length = 10)
    private String placa;

    @Column(name = "marca", nullable = false, length = 80)
    private String marca;

    @Column(name = "modelo", length = 80)
    private String modelo;

    @Column(name = "anio_fabricacion")
    private Integer anioFabricacion;

    @Column(name = "capacidad_asientos", nullable = false)
    private Integer capacidadAsientos;

    /** Número de pisos (1 o 2) */
    @Column(name = "num_pisos", nullable = false)
    @Builder.Default
    private Integer numPisos = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private TipoBus tipo;

    @Column(name = "foto_url", length = 500)
    private String fotoUrl;

    @Column(name = "observaciones", length = 500)
    private String observaciones;
    
    
}
