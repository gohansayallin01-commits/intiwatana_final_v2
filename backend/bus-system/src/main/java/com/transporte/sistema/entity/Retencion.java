package com.transporte.sistema.entity;

import com.transporte.sistema.enums.CanalVenta;
import com.transporte.sistema.enums.EstadoRetencion;
import com.transporte.sistema.enums.MetodoPago;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Reserva (retención) de uno o varios asientos de un viaje concreto.
 *
 * Es la pieza central del flujo comercial: se crea al confirmar la selección
 * de asientos, bloquea temporalmente la disponibilidad (viaje + asiento) y
 * evoluciona hasta CONFIRMADO (pago real registrado por caja) o VENCIDO/CANCELADO.
 *
 * NO reemplaza a {@link Boleto}: sólo cuando el pago se registra realmente se
 * emiten los boletos y el QR de embarque. Mientras la reserva esté RETENIDA o
 * PENDIENTE_PAGO no existe boleto válido para embarcar.
 */
@Entity
@Table(name = "retenciones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Retencion extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Referencia pública no predecible (ej: RET-7F3A9C21D4E8) */
    @Column(name = "referencia", nullable = false, unique = true, length = 40)
    private String referencia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "viaje_id", nullable = false)
    private Viaje viaje;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    @Builder.Default
    private EstadoRetencion estado = EstadoRetencion.RETENIDO;

    @Enumerated(EnumType.STRING)
    @Column(name = "canal", nullable = false, length = 20)
    @Builder.Default
    private CanalVenta canal = CanalVenta.WEB;

    // ── Datos del comprador (invitado o cliente registrado) ─────────────────

    @Column(name = "comprador_nombres", length = 150)
    private String compradorNombres;

    @Column(name = "comprador_apellidos", length = 150)
    private String compradorApellidos;

    @Column(name = "comprador_documento", nullable = false, length = 15)
    private String compradorDocumento;

    @Column(name = "comprador_telefono", length = 20)
    private String compradorTelefono;

    @Column(name = "comprador_email", length = 150)
    private String compradorEmail;

    // ── Importes (SIEMPRE calculados por el backend) ────────────────────────

    @Column(name = "cantidad_asientos", nullable = false)
    private Integer cantidadAsientos;

    @Column(name = "precio_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioUnitario;

    @Column(name = "total", nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    /** Snapshot legible de los números de asiento (histórico tras liberar). */
    @Column(name = "asientos_numeros", length = 300)
    private String asientosNumeros;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pago_preferido", length = 30)
    private MetodoPago metodoPagoPreferido;

    /** Clave de idempotencia opcional del cliente (índice único parcial). */
    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;

    @Column(name = "expira_en", nullable = false)
    private LocalDateTime expiraEn;

    /** Ventana única de tolerancia de verificación Yape ya utilizada (no renovable). */
    @Column(name = "tolerancia_usada", nullable = false)
    @Builder.Default
    private Boolean toleranciaUsada = false;

    @Column(name = "confirmada_en")
    private LocalDateTime confirmadaEn;

    @Column(name = "liberada_en")
    private LocalDateTime liberadaEn;

    @Column(name = "motivo_liberacion", length = 300)
    private String motivoLiberacion;

    @Column(name = "observaciones", length = 300)
    private String observaciones;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmada_por_id")
    private Usuario confirmadaPor;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pago_id")
    private Pago pago;

    /** Asientos actualmente bloqueados por esta reserva (vivos). */
    @OneToMany(mappedBy = "retencion", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Asiento> asientos = new ArrayList<>();
}
