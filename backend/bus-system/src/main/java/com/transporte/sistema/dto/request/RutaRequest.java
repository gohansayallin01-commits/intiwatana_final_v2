package com.transporte.sistema.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class RutaRequest {

    @NotBlank @Size(max = 30)
    private String codigo;

    @NotNull
    private Long origenId;

    @NotNull
    private Long destinoId;

    @DecimalMin("0.01")
    private BigDecimal distanciaKm;

    @DecimalMin("0.1")
    private BigDecimal duracionHorasEstimada;

    @DecimalMin("0.01")
    private BigDecimal precioBase;

    @Size(max = 300)
    private String descripcion;
}
