package com.transporte.sistema.dto.request;

import com.transporte.sistema.enums.EstadoEncomienda;
import jakarta.validation.constraints.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ActualizarEstadoEncomiendaRequest {

    @NotNull(message = "El nuevo estado es obligatorio")
    private EstadoEncomienda nuevoEstado;

    private Long sucursalActualId;

    @Size(max = 500)
    private String observacion;
}
