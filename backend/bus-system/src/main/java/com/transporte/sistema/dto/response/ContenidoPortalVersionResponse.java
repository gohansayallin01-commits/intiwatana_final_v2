package com.transporte.sistema.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContenidoPortalVersionResponse {

    private Long id;
    private String seccion;
    private Integer version;
    private String estado;
    private String accion;
    private JsonNode contenido;
    private Long usuarioId;
    private String username;
    private String rol;
    private String observacion;
    private LocalDateTime fechaHora;
}
