package com.transporte.sistema.entity;

import com.transporte.sistema.enums.EstadoDeclaracionPago;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Declaración de pago Yape hecha por el cliente sobre una reserva.
 *
 * Es evidencia de un pago DECLARADO (operación, importe, fecha y captura
 * opcional). No aprueba nada por sí sola: la caja la verifica y aprueba
 * (emitiendo boletos vía {@code confirmarPago}) o la rechaza con motivo.
 */
@Entity
@Table(name = "declaracion_pago")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeclaracionPago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "retencion_id", nullable = false)
    private Retencion retencion;

    @Column(name = "operacion", nullable = false, length = 60)
    private String operacion;

    @Column(name = "importe", nullable = false, precision = 10, scale = 2)
    private BigDecimal importe;

    @Column(name = "fecha_pagada")
    private LocalDateTime fechaPagada;

    @Column(name = "cuenta_pagada", length = 40)
    private String cuentaPagada;

    @Column(name = "comprobante_url", length = 300)
    private String comprobanteUrl;

    @Column(name = "comprobante_nombre_interno", length = 200)
    private String comprobanteNombreInterno;

    @Column(name = "comprobante_checksum", length = 64)
    private String comprobanteChecksum;

    @Column(name = "comprobante_mime", length = 60)
    private String comprobanteMime;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 25)
    @Builder.Default
    private EstadoDeclaracionPago estado = EstadoDeclaracionPago.PENDIENTE_VERIFICACION;

    @Column(name = "declarado_documento", nullable = false, length = 15)
    private String declaradoDocumento;

    @Column(name = "declarado_telefono", length = 20)
    private String declaradoTelefono;

    @Column(name = "declarado_en", nullable = false)
    private LocalDateTime declaradoEn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verificador_id")
    private Usuario verificador;

    @Column(name = "verificado_en")
    private LocalDateTime verificadoEn;

    @Column(name = "motivo_rechazo", length = 300)
    private String motivoRechazo;

    @Column(name = "observaciones", length = 300)
    private String observaciones;

    @Column(name = "activo", nullable = false)
    @Builder.Default
    private Boolean activo = true;
}