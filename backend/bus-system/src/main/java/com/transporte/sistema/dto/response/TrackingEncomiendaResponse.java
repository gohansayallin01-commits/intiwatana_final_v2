package com.transporte.sistema.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Vista PÚBLICA del seguimiento de una encomienda.
 *
 * El endpoint de tracking es anónimo (cualquiera que conozca el número de guía
 * puede consultarlo), por lo que no debe devolver datos personales: aquí NO se
 * incluyen el remitente, el documento del destinatario, el teléfono, ni el
 * personal responsable de cada movimiento.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrackingEncomiendaResponse {

private String numeroGuia;
    private String estado;
    private String descripcionContenido;
    private BigDecimal pesoKg;
    private SucursalTracking sucursalOrigen;
    private SucursalTracking sucursalDestino;
    private LocalDateTime fechaRegistro;
    private List<MovimientoTracking> movimientos;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SucursalTracking {
        private String nombre;
        private String ciudad;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MovimientoTracking {
        private LocalDateTime fechaHora;
        private String estadoNuevo;
        private String observacion;
        private String sucursalActual;
    }
}
