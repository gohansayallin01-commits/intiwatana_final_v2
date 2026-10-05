package com.transporte.sistema.dto.request;

import com.transporte.sistema.enums.EstadoPago;
import com.transporte.sistema.enums.MetodoPago;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class CrearPagoRequest {

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero")
    private BigDecimal monto;

    @NotNull(message = "El método de pago es obligatorio")
    private MetodoPago metodo;

    @Size(max = 200)
    private String referencia;

    @Size(max = 300)
    private String observacion;

    private EstadoPago estado;
}