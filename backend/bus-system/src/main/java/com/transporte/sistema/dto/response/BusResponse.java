package com.transporte.sistema.dto.response;
import com.transporte.sistema.dto.ConfiguracionAsientos;
import com.transporte.sistema.enums.TipoBus;
import lombok.*;
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class BusResponse {
    private Long id;
    private String placa;
    private String marca;
    private String modelo;
    private Integer anioFabricacion;
    private Integer capacidadAsientos;
    private Integer numPisos;
    private TipoBus tipo;
    private Boolean activo;
    /** Configuracion de asientos del vehiculo (null si no esta definida). */
    private ConfiguracionAsientos configuracionAsientos;
    /** Cantidad de asientos derivada de la configuracion (0 si no hay). */
    private Integer asientosConfigurados;
    /** COMPLETO si asientosConfigurados == capacidadAsientos; si no INCOMPLETO. */
    private String estadoConfiguracion;
}
