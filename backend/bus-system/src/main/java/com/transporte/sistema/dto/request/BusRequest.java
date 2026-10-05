package com.transporte.sistema.dto.request;

import com.transporte.sistema.enums.TipoBus;
import jakarta.validation.constraints.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class BusRequest {

    @NotBlank @Size(max = 10)
    private String placa;

    @NotBlank @Size(max = 80)
    private String marca;

    @Size(max = 80)
    private String modelo;

    private Integer anioFabricacion;

    @NotNull @Min(1) @Max(100)
    private Integer capacidadAsientos;

    @Min(1) @Max(2)
    @Builder.Default
    private Integer numPisos = 1;

    @NotNull
    private TipoBus tipo;

    @Size(max = 500)
    private String observaciones;
}
