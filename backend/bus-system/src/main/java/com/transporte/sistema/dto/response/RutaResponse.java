package com.transporte.sistema.dto.response;
import lombok.*;
import java.math.BigDecimal;
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class RutaResponse {
    private Long id;
    private String codigo;
    private SucursalResponse origen;
    private SucursalResponse destino;
    private BigDecimal distanciaKm;
    private BigDecimal duracionHorasEstimada;
    private BigDecimal precioBase;
}
