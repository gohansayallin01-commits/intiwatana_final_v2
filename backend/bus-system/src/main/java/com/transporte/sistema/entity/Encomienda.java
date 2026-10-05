package com.transporte.sistema.entity;

import com.transporte.sistema.enums.EstadoEncomienda;
import com.transporte.sistema.enums.MetodoPago;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Encomienda/paquete enviado por los buses interprovinciales.
 * Tiene tracking completo mediante MovimientoEncomienda.
 */
@Entity
@Table(name = "encomiendas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Encomienda extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Referencia interna de la SOLICITUD previa, ej: "SOL-2026-000123".
     * La genera el sistema al registrar la solicitud y permite al personal
     * autorizado recuperarla para completar la recepcion. No es una guia oficial
     * y por tanto NO habilita el tracking publico.
     */
    @Column(name = "referencia_solicitud", unique = true, length = 30)
    private String referenciaSolicitud;

    /**
     * Clave de idempotencia del REGISTRO PRESENCIAL. El panel la envia en cada
     * alta; si la misma clave vuelve a llegar (doble clic, reintento o doble
     * pestana) el backend devuelve la encomienda ya creada en vez de generar una
     * segunda guia y un segundo pago. Es NULL cuando el alta no se identifica.
     */
    @Column(name = "clave_idempotencia", length = 60)
    private String claveIdempotencia;

    /**
     * Número de guía oficial único, ej: "GUI-20260804-000001".
     * Es NULL mientras la encomienda es una SOLICITUD_PENDIENTE. Lo genera el
     * backend unicamente cuando el personal autorizado confirma la recepcion
     * fisica, el peso, las dimensiones, la tarifa y el pago.
     */
    @Column(name = "numero_guia", unique = true, length = 30)
    private String numeroGuia;

    /** Quien envía el paquete */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "remitente_id", nullable = false)
    private Cliente remitente;

    /** Quien debe recibir el paquete */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destinatario_id")
    private Cliente destinatario;

    /**
     * Nombre del destinatario cuando no está registrado como Cliente.
     * Es el caso de las encomiendas solicitadas desde el portal web, donde el
     * remitente sólo indica nombre y teléfono de quien recibirá el paquete.
     */
    @Column(name = "destinatario_nombre", length = 200)
    private String destinatarioNombre;

    /**
     * Telefono de contacto que el remitente informo para este envio.
     * Puede diferir del telefono de su cuenta, por eso se guarda aparte del
     * Cliente asociado en lugar de sobrescribirlo.
     */
    @Column(name = "remitente_telefono", length = 20)
    private String remitenteTelefono;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sucursal_origen_id", nullable = false)
    private Sucursal sucursalOrigen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sucursal_destino_id", nullable = false)
    private Sucursal sucursalDestino;

    /** Viaje en el que viaja la encomienda (asignado al despachar) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "viaje_id")
    private Viaje viaje;

    @Column(name = "descripcion_contenido", nullable = false, length = 300)
    private String descripcionContenido;

    /**
     * Peso real medido por el personal autorizado en agencia.
     * Es NULL mientras la encomienda es una SOLICITUD_PENDIENTE: el publico
     * no declara peso, lo determina INTIWATANA al recibir el paquete.
     */
    @Column(name = "peso_kg", precision = 8, scale = 3)
    private BigDecimal pesoKg;

    /**
     * Medidas oficiales en centimetros, registradas al recibir el paquete.
     * Las tres son NULL mientras la encomienda es una SOLICITUD_PENDIENTE.
     */
    @Column(name = "largo_cm", precision = 8, scale = 2)
    private BigDecimal largoCm;

    @Column(name = "ancho_cm", precision = 8, scale = 2)
    private BigDecimal anchoCm;

    @Column(name = "alto_cm", precision = 8, scale = 2)
    private BigDecimal altoCm;

    @Column(name = "volumen_m3", precision = 8, scale = 4)
    private BigDecimal volumenM3;

    /** Valor declarado por el remitente (para seguros) */
    @Column(name = "valor_declarado", precision = 10, scale = 2)
    private BigDecimal valorDeclarado;

    /**
     * Tarifa oficial del servicio. Es NULL mientras la encomienda es una
     * SOLICITUD_PENDIENTE. La fija el backend a partir del dato capturado por
     * el personal autorizado al confirmar la recepcion y el pago.
     */
    @Column(name = "costo", precision = 10, scale = 2)
    private BigDecimal costo;

    @Enumerated(EnumType.STRING)
    /**
     * Metodo de pago del envio. Es NULL mientras la encomienda es una simple
     * SOLICITUD_PENDIENTE: el publico no elige ni registra la forma de pago.
     * Personal autorizado lo define al confirmar la recepcion fisica.
     */
    @Column(name = "metodo_pago", length = 30)
    private MetodoPago metodoPago;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    @Builder.Default
    private EstadoEncomienda estado = EstadoEncomienda.RECIBIDO;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pago_id")
    private Pago pago;

    /** Cajero que registró la encomienda */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cajero_registro_id")
    private Usuario cajeroRegistro;

    /** Cajero que entregó la encomienda */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cajero_entrega_id")
    private Usuario cajeroEntrega;

    /** Historial de movimientos/estados */
    @OneToMany(mappedBy = "encomienda", cascade = CascadeType.ALL,
               fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<MovimientoEncomienda> movimientos = new ArrayList<>();

    @Column(name = "observaciones", length = 500)
    private String observaciones;

    /** Código QR de la guía */
    @Column(name = "codigo_qr", length = 500)
    private String codigoQr;
}
