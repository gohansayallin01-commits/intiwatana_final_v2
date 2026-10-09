package com.transporte.sistema.dto.response;

import lombok.*;

import java.time.LocalDateTime;

/** Vista admin (completa) de la configuración del QR oficial Yape. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfiguracionYapeResponse {
    private Long id;
    private Boolean activo;
    private String receptorNombre;
    private String numeroVisible;
    private Long sucursalId;
    private String sucursalNombre;
    private String descripcion;
    private String instrucciones;
    private String esquema;
    private String imagenUrl;
    private Long imagenPesoBytes;
    private Integer imagenAncho;
    private Integer imagenAlto;
    private LocalDateTime vigenteDesde;
    private LocalDateTime vigenteHasta;
    private String observaciones;
    private String actualizadoPor;
    private LocalDateTime updatedAt;
    private LocalDateTime createdAt;
}