package com.transporte.sistema.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Configuración vigente del QR oficial Yape (spvestionado por ADMIN).
 * La imagen se guarda como referencia segura en el almacenamiento oficial;
 * nunca como Base64. Hay una única fila activa (soft-delete / archivo).
 */
@Entity
@Table(name = "configuracion_yape")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ConfiguracionYape extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "receptor_nombre", length = 150)
    private String receptorNombre;

    @Column(name = "numero_visible", length = 30)
    private String numeroVisible;

    @Column(name = "sucursal_id")
    private Long sucursalId;

    @Column(name = "sucursal_nombre", length = 150)
    private String sucursalNombre;

    @Column(name = "descripcion", length = 255)
    private String descripcion;

    @Column(name = "instrucciones", length = 500)
    private String instrucciones;

    @Column(name = "esquema", length = 30)
    private String esquema;

    @Column(name = "imagen_url", length = 300)
    private String imagenUrl;

    @Column(name = "imagen_nombre_interno", length = 200)
    private String imagenNombreInterno;

    @Column(name = "imagen_checksum", length = 64)
    private String imagenChecksum;

    @Column(name = "imagen_peso_bytes")
    private Long imagenPesoBytes;

    @Column(name = "imagen_ancho")
    private Integer imagenAncho;

    @Column(name = "imagen_alto")
    private Integer imagenAlto;

    @Column(name = "vigente_desde")
    private LocalDateTime vigenteDesde;

    @Column(name = "vigente_hasta")
    private LocalDateTime vigenteHasta;

    @Column(name = "observaciones", length = 300)
    private String observaciones;

    @Column(name = "actualizado_por", length = 60)
    private String actualizadoPor;
}