package com.transporte.sistema.dto.response;
import com.transporte.sistema.enums.EstadoAsiento;
import com.transporte.sistema.enums.TipoAsiento;
import lombok.*;
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class AsientoResponse {
    private Long id;
    private String numeroAsiento;
    private Integer fila;
    private Integer columna;
    private Integer piso;
    private TipoAsiento tipo;
    private EstadoAsiento estado;
    private java.math.BigDecimal precio;
    private String pasajero;
}
