package com.transporte.sistema.dto.request;

import com.transporte.sistema.enums.MetodoPago;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * DTO del REGISTRO PRESENCIAL de encomienda, realizado por el personal
 * autorizado en agencia junto con el remitente fisico.
 *
 * Aqui si se capturan los datos oficiales: peso real, medidas, tarifa y pago
 * recibido. El volumen lo calcula el backend a partir de las medidas, no el
 * cliente. No se admite pago contra entrega: el pago es presencial e inmediato.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EncomiendaRequest {

    @NotNull(message = "El remitente es obligatorio")
    private Long remitenteId;

    @NotNull(message = "El destinatario es obligatorio")
    private Long destinatarioId;

    @NotNull(message = "La sucursal origen es obligatoria")
    private Long sucursalOrigenId;

    @NotNull(message = "La sucursal destino es obligatoria")
    private Long sucursalDestinoId;

    /** Viaje en el que se despachará (puede asignarse después) */
    private Long viajeId;

    @NotBlank(message = "La descripción del contenido es obligatoria")
    @Size(max = 300)
    private String descripcionContenido;

    @NotNull(message = "El peso real es obligatorio")
    @DecimalMin(value = "0.001", message = "El peso debe ser mayor a cero")
    private BigDecimal pesoKg;

    @NotNull(message = "El largo es obligatorio")
    @DecimalMin(value = "0.1", message = "El largo debe ser mayor a cero")
    private BigDecimal largoCm;

    @NotNull(message = "El ancho es obligatorio")
    @DecimalMin(value = "0.1", message = "El ancho debe ser mayor a cero")
    private BigDecimal anchoCm;

    @NotNull(message = "El alto es obligatorio")
    @DecimalMin(value = "0.1", message = "El alto debe ser mayor a cero")
    private BigDecimal altoCm;

    @DecimalMin(value = "0.00")
    private BigDecimal valorDeclarado;

    /**
     * Tarifa oficial del envío. El backend la valida contra el importe del pago
     * registrado; no existe tarifario automático (ver informe de cierre).
     */
    @NotNull(message = "La tarifa oficial es obligatoria")
    @DecimalMin(value = "0.01", message = "La tarifa oficial debe ser mayor a cero")
    private BigDecimal tarifaOficial;

    @NotNull(message = "El método de pago es obligatorio")
    private MetodoPago metodoPago;

    @Size(max = 300)
    private String tipoEnvio;

    @Size(max = 120)
    private String cuidadoEspecial;

    @Size(max = 500)
    private String observaciones;

    /**
     * Clave opcional de idempotencia del registro presencial. Permite que un
     * reintento del panel (doble clic, doble pestaña o error de red) no cree una
     * segunda encomienda con otra guía y otro pago.
     */
    @Size(max = 60)
    private String claveIdempotencia;
}