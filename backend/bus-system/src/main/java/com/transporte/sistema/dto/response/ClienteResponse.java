package com.transporte.sistema.dto.response;

import com.transporte.sistema.enums.TipoCliente;
import lombok.*;
import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ClienteResponse {
    private Long id;
    private String dniRuc;
    private String nombres;
    private String apellidos;
    private String razonSocial;
    private String nombreCompleto;
    private String telefono;
    private String email;
    private String direccion;
    private String distrito;
    private String ciudad;
    private TipoCliente tipoCliente;
    private LocalDateTime createdAt;
}
