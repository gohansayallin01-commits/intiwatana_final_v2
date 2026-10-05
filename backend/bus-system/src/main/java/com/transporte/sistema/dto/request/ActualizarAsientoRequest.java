package com.transporte.sistema.dto.request;

import com.transporte.sistema.enums.EstadoAsiento;
import com.transporte.sistema.enums.TipoAsiento;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ActualizarAsientoRequest {
    private EstadoAsiento estado;
    private TipoAsiento tipo;
    private BigDecimal precio;
}
