package com.transporte.sistema.dto.response;
import com.transporte.sistema.enums.EstadoViaje;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ViajeResponse {
    private Long id;
    private RutaResponse ruta;
    private BusResponse bus;
    /** Datos del chofer asignado (puede ser null si no hay chofer) */
    private ChoferInfo chofer;
    /** @deprecated usar chofer.id */
    private Long choferId;
    /** @deprecated usar chofer.nombreCompleto */
    private String choferNombre;
    private LocalDateTime fechaHoraSalida;
    private LocalDateTime fechaHoraLlegadaEstimada;
    private BigDecimal precioAdulto;
    private BigDecimal precioNino;
    private EstadoViaje estado;
    private Integer asientosDisponibles;
    private Integer totalAsientos;

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ChoferInfo {
        private Long id;
        private String username;
        private String nombres;
        private String apellidos;
        private String nombreCompleto;
        private String telefono;
    }
}
