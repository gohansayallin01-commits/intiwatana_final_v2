package com.transporte.sistema.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Guardado de borrador de una sección editorial.
 * El contenido llega como JSON libre pero es validado por sección en el servicio
 * (longitudes, campos obligatorios, iconos permitidos, sin HTML/JS).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContenidoPortalRequest {

    @NotNull
    private JsonNode contenido;

    @Size(max = 120)
    private String autor;

    @Size(max = 500)
    private String observacion;
}
