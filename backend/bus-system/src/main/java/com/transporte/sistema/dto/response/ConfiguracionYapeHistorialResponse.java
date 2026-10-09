package com.transporte.sistema.dto.response;

import lombok.*;

import java.time.LocalDateTime;

/** Cambio registrado de la configuración del QR Yape. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfiguracionYapeHistorialResponse {
    private Long configuracionId;
    private String campo;
    private String valorAnterior;
    private String valorNuevo;
    private String actualizadoPor;
    private String rol;
    private String motivo;
    private LocalDateTime fecha;
}