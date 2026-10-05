package com.transporte.sistema.entity;

import com.transporte.sistema.enums.EstadoBoleto;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Boleto/pasaje vendido para un viaje específico.
 * Contiene referencia al asiento y genera un código QR único.
 */
@Entity
@Table(name = "boletos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Boleto extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Número de boleto único, ej: "BOL-2024-000001" */
    @Column(name = "numero_boleto", nullable = false, unique = true, length = 30)
    private String numeroBoleto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "viaje_id", nullable = false)
    private Viaje viaje;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asiento_id", nullable = false)
    private Asiento asiento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(name = "precio_pagado", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioPagado;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    @Builder.Default
    private EstadoBoleto estado = EstadoBoleto.ACTIVO;

    /** Contenido del QR: puede ser el numeroBoleto o un hash firmado */
    @Column(name = "codigo_qr", length = 500)
    private String codigoQr;

    /**
     * Imagen del QR de embarque. Se guarda como data URI base64 (PNG) y por su
     * tamaño real se persiste como TEXT, no VARCHAR(500) (evita truncamientos).
     */
    @Column(name = "qr_imagen_url", columnDefinition = "text")
    private String qrImagenUrl;

    /** Relación con el pago (puede ser nulo si es contra entrega) */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pago_id")
    private Pago pago;

    /** Cajero que vendió el boleto */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cajero_id")
    private Usuario cajero;

    /** Fecha y hora en que el pasajero abordó (scaneó QR) */
    @Column(name = "fecha_hora_uso")
    private LocalDateTime fechaHoraUso;

    @Column(name = "observaciones", length = 300)
    private String observaciones;
}
