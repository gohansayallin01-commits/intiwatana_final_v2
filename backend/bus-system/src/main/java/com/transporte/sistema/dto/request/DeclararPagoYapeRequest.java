package com.transporte.sistema.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Declaración de pago Yape realizada por el cliente sobre una reserva. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeclararPagoYapeRequest {

    @NotBlank(message = "El número de operación es obligatorio")
    @Size(max = 60, message = "El número de operación no puede superar 60 caracteres")
    private String operacion;

    @NotNull(message = "El importe pagado es obligatorio")
    @DecimalMin(value = "0.01", message = "El importe debe ser mayor a cero")
    private BigDecimal importe;

    private LocalDateTime fechaPagada;

    @Size(max = 40, message = "El origen no puede superar 40 caracteres")
    private String cuentaPagada;

    @Size(max = 300, message = "Las observaciones no pueden superar 300 caracteres")
    private String observaciones;
}