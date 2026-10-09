package com.transporte.sistema.dto.response;

import lombok.*;

import java.time.LocalDateTime;

/**
 * Vista pública de la configuración Yape mostrada al comprador.
 * El número se entrega ENMASCARADO (ej. 912****34): nunca se expone completo.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfiguracionYapePublicaResponse {
    private Boolean activa;
    private String receptorNombre;
    private String numeroEnmascarado;
    private String sucursalNombre;
    private String descripcion;
    private String instrucciones;
    private String esquema;
    private String imagenUrl;
    private LocalDateTime vigenteHasta;
}