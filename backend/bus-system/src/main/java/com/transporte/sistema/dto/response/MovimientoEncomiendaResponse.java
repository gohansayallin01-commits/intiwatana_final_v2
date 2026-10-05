package com.transporte.sistema.dto.response;
import com.transporte.sistema.enums.EstadoEncomienda;
import lombok.*;
import java.time.LocalDateTime;
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class MovimientoEncomiendaResponse {
    private Long id;
    private LocalDateTime fechaHora;
    private String sucursalActual;
    private EstadoEncomienda estadoAnterior;
    private EstadoEncomienda estadoNuevo;
    private String observacion;
    private String usuarioResponsable;
}
