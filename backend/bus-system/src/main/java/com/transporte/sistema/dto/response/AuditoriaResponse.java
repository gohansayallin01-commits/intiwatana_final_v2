package com.transporte.sistema.dto.response;
import lombok.*;
import java.time.LocalDateTime;
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditoriaResponse {
    private Long id;
    private Long usuarioId;
    private String username;
    private String rol;
    private String accion;
    private String entidad;
    private Long entidadId;
    private LocalDateTime fechaHora;
    private String resultado;
    private String detalle;
}