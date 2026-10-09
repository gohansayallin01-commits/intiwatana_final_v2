package com.transporte.sistema.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/** Datos (texto) de la configuración del QR oficial Yape. La imagen se adjunta aparte. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfiguracionYapeRequest {

    @NotBlank(message = "El nombre del receptor es obligatorio")
    @Size(max = 150, message = "Nombre demasiado largo")
    private String receptorNombre;

    @NotBlank(message = "El número de la subcuenta es obligatorio")
    @Size(max = 30, message = "Número demasiado largo")
    private String numeroVisible;

    private Long sucursalId;

    @Size(max = 150, message = "Nombre de sucursal demasiado largo")
    private String sucursalNombre;

    @Size(max = 255, message = "Descripción demasiado larga")
    private String descripcion;

    @Size(max = 500, message = "Instrucciones demasiado largas")
    private String instrucciones;

    @Size(max = 300, message = "Observaciones demasiado largas")
    private String observaciones;
}