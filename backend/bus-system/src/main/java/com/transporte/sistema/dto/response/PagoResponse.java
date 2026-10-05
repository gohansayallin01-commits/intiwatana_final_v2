package com.transporte.sistema.dto.response;

import com.transporte.sistema.enums.EstadoPago;
import com.transporte.sistema.enums.MetodoPago;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class PagoResponse {
    private Long id;
    private BigDecimal monto;
    private LocalDateTime fechaPago;
    private MetodoPago metodo;
    private EstadoPago estado;
    private String referencia;
    private String cliente;
    private String referenciaEntidad;
}