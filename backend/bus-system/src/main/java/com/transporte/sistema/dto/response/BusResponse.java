package com.transporte.sistema.dto.response;
import com.transporte.sistema.enums.TipoBus;
import lombok.*;
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class BusResponse {
    private Long id;
    private String placa;
    private String marca;
    private String modelo;
    private Integer anioFabricacion;
    private Integer capacidadAsientos;
    private Integer numPisos;
    private TipoBus tipo;
    private Boolean activo;
}
