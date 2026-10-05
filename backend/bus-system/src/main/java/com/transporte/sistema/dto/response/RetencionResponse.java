package com.transporte.sistema.dto.response;

import com.transporte.sistema.enums.CanalVenta;
import com.transporte.sistema.enums.EstadoRetencion;
import com.transporte.sistema.enums.MetodoPago;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Vista pública/segura de una reserva. NUNCA expone datos internos de otros
 * clientes ni el detalle técnico del bloqueo.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RetencionResponse {

    private String referencia;
    private EstadoRetencion estado;
    private CanalVenta canal;

    private Long viajeId;
    private String origen;
    private String destino;
    private String busPlaca;
    private LocalDateTime fechaHoraSalida;

    private Integer cantidadAsientos;
    private BigDecimal precioUnitario;
    private BigDecimal total;
    private List<AsientoReservaResponse> asientos;

    private LocalDateTime expiraEn;
    private long segundosRestantes;

    /** true sólo cuando la reserva está CONFIRMADA (boleto válido para embarque). */
    private boolean validoParaEmbarque;
    /** true cuando todavía falta pagar (RETENIDO o PENDIENTE_PAGO). */
    private boolean requierePago;

    private MetodoPago metodoPagoPreferido;
    private LocalDateTime confirmadaEn;
    private String compradorNombre;
    private String mensaje;

    /** Boletos emitidos (sólo presentes cuando la reserva está CONFIRMADA). */
    private List<BoletoResponse> boletos;
}
