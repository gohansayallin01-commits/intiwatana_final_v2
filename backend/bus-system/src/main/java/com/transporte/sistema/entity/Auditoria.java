package com.transporte.sistema.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * Bitácora funcional de operaciones relevantes (login, venta, cancelación,
 * escaneo, encomiendas...). Complementa a @EnableJpaAuditing (timestamps) y a
 * movimientos_encomienda (tracking de envíos): aquí se registra QUIÉN hizo QUÉ.
 */
@Entity
@Table(name = "auditoria")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Auditoria extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "username", length = 80)
    private String username;

    @Column(name = "rol", length = 40)
    private String rol;

    /** Verbo de la acción: LOGIN, VENTA_BOLETO, ESCANEO_QR, CANCELAR_BOLETO, ... */
    @Column(name = "accion", nullable = false, length = 100)
    private String accion;

    /** Entidad afectada: Boleto, Encomienda, Usuario, Login... */
    @Column(name = "entidad", length = 100)
    private String entidad;

    @Column(name = "entidad_id")
    private Long entidadId;

    @Column(name = "fecha_hora", nullable = false)
    @Builder.Default
    private LocalDateTime fechaHora = LocalDateTime.now();

    /** Resultado de la operación: OK o ERROR */
    @Column(name = "resultado", nullable = false, length = 20)
    @Builder.Default
    private String resultado = "OK";

    /** Detalle libre (número de boleto/gula, motivo, IP, etc.) */
    @Column(name = "detalle", columnDefinition = "text")
    private String detalle;
}