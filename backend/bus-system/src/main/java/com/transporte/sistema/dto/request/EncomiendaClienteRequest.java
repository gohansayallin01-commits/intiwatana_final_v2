package com.transporte.sistema.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * DTO de la SOLICITUD previa de encomienda enviada desde el portal web.
 *
 * El publico unicamente declara origen, destino, remitente, destinatario y
 * descripcion. NO admite peso, dimensiones, volumen, valor, tarifa, metodo de
 * pago, pago ni numero de guia: esos datos los determina el personal autorizado
 * cuando el paquete es recibido fisicamente en una agencia INTIWATANA.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EncomiendaClienteRequest {

    @NotNull(message = "La sucursal de origen es obligatoria")
    private Long sucursalOrigenId;

    @NotNull(message = "La sucursal de destino es obligatoria")
    private Long sucursalDestinoId;

    @NotBlank(message = "El nombre del remitente es obligatorio")
    @Size(max = 150, message = "El nombre del remitente es demasiado largo")
    private String remitenteNombre;

    @NotBlank(message = "El telefono del remitente es obligatorio")
    @Size(max = 20, message = "El telefono del remitente es demasiado largo")
    private String remitenteTelefono;

    @NotBlank(message = "El nombre del destinatario es obligatorio")
    @Size(max = 200, message = "El nombre del destinatario es demasiado largo")
    private String destinatarioNombre;

    @NotBlank(message = "El telefono del destinatario es obligatorio")
    @Size(max = 20, message = "El telefono del destinatario es demasiado largo")
    private String destinatarioTelefono;

    @NotBlank(message = "La descripcion del contenido es obligatoria")
    @Size(max = 300, message = "La descripcion es demasiado larga")
    private String descripcionContenido;

    /** Tipo de envio declarado por el cliente (solo referencia). */
    @Size(max = 40)
    private String tipoEnvio;

    /** Cuidado especial solicitado por el cliente (solo referencia). */
    @Size(max = 120)
    private String cuidadoEspecial;

    @Size(max = 300)
    private String observaciones;
}