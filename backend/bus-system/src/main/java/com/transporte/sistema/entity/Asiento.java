package com.transporte.sistema.entity;

import com.transporte.sistema.enums.EstadoAsiento;
import com.transporte.sistema.enums.TipoAsiento;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * Asiento físico de un viaje específico.
 * Se generan automáticamente al crear el viaje según la capacidad del bus.
 */
@Entity
@Table(name = "asientos",
       uniqueConstraints = @UniqueConstraint(
           name = "uk_asiento_viaje_numero",
           columnNames = {"viaje_id", "numero_asiento"}
       ))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Asiento extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "viaje_id", nullable = false)
    private Viaje viaje;

    /** Número de asiento visible al pasajero, ej: 1, 2, 3... o "1A", "1B" */
    @Column(name = "numero_asiento", nullable = false, length = 10)
    private String numeroAsiento;

    /** Fila del asiento en el bus (para el mapa visual) */
    @Column(name = "fila")
    private Integer fila;

    /** Columna del asiento (A=1, B=2, C=3, D=4) */
    @Column(name = "columna")
    private Integer columna;

    /** Piso del bus (1 o 2) */
    @Column(name = "piso", nullable = false)
    @Builder.Default
    private Integer piso = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private TipoAsiento tipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    @Builder.Default
    private EstadoAsiento estado = EstadoAsiento.DISPONIBLE;

    @Column(name = "precio", precision = 10, scale = 2)
    private java.math.BigDecimal precio;

    /**
     * Retención (reserva) que mantiene bloqueado este asiento mientras está
     * RESERVADO. Es nulo cuando el asiento está DISPONIBLE o VENDIDO.
     * Permite liberar de forma perezosa al vencer {@link #retenidoHasta}.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "retencion_id")
    private Retencion retencion;

    /** Momento en que expira la retención vigente (null = sin retención). */
    @Column(name = "retenido_hasta")
    private java.time.LocalDateTime retenidoHasta;

    /** Referencia inversa al boleto (si está vendido/reservado) */
    @OneToOne(mappedBy = "asiento", fetch = FetchType.LAZY)
    private Boleto boleto;
}
