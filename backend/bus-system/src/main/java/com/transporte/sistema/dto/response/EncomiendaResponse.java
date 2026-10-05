package com.transporte.sistema.dto.response;
import com.transporte.sistema.enums.EstadoEncomienda;
import com.transporte.sistema.enums.MetodoPago;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class EncomiendaResponse {
    private Long id;
    /** Referencia SOL de la solicitud previa. Solo existe si aún no hay guía oficial. */
    private String referenciaSolicitud;
    /** Guía oficial. Es NULL mientras la encomienda es una SOLICITUD_PENDIENTE. */
    private String numeroGuia;
    private ClienteResponse remitente;
    private ClienteResponse destinatario;
    /** Nombre del destinatario. Para encomiendas del portal web no es un Cliente del
     *  sistema, por lo que sólo se conoce el nombre informado por el remitente. */
    private String destinatarioNombre;
    /** Teléfono de contacto informado por el remitente para este envío. */
    private String remitenteTelefono;
    private SucursalResponse sucursalOrigen;
    private SucursalResponse sucursalDestino;
    private String descripcionContenido;
    private BigDecimal pesoKg;
    private BigDecimal largoCm;
    private BigDecimal anchoCm;
    private BigDecimal altoCm;
    /** Volumen en m3. Lo calcula el backend a partir de las medidas oficiales. */
    private BigDecimal volumenM3;
    private BigDecimal costo;
    private MetodoPago metodoPago;
    private EstadoEncomienda estado;
    private String codigoQr;
    private List<MovimientoEncomiendaResponse> movimientos;
    private LocalDateTime createdAt;
}
