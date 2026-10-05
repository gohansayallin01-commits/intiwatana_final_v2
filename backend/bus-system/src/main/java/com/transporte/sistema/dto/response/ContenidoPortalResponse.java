package com.transporte.sistema.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContenidoPortalResponse {

    private Long id;
    private String seccion;
    private String estado;
    private Integer version;
    private JsonNode borrador;
    private JsonNode publicado;
    private String autor;
    private String actualizadoPor;
    private LocalDateTime publicadoEn;
    private LocalDateTime actualizadoEn;
}
