package com.transporte.sistema.dto.response;
import lombok.*;
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class SucursalResponse {
    private Long id;
    private String codigo;
    private String nombre;
    private String ciudad;
    private String provincia;
    private String departamento;
    private String direccion;
    private String telefono;
    private Boolean esTerminal;
    private Boolean activo;
}
