package com.transporte.sistema.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfigurarTiempoRequest {

    @NotBlank(message = "El parámetro es obligatorio")
    private String parametro;

    @NotNull(message = "El valor es obligatorio")
    @Min(value = 0, message = "El valor no puede ser negativo")
    @Max(value = 1000, message = "El valor excede el rango permitido")
    private Integer valor;

    @NotBlank(message = "El motivo del cambio es obligatorio")
    @Size(max = 300, message = "El motivo no puede superar 300 caracteres")
    private String motivo;
}