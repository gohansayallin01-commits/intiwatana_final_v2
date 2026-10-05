package com.transporte.sistema.dto.response;
import com.transporte.sistema.enums.EstadoBoleto;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class BoletoResponse {
    private Long id;
    private String numeroBoleto;
    private ViajeResponse viaje;
    private AsientoResponse asiento;
    private ClienteResponse cliente;
    private BigDecimal precioPagado;
    private EstadoBoleto estado;
    private String codigoQr;
    private String qrImagenUrl;
    private LocalDateTime createdAt;
    /** Momento en que el chofer validó el QR (null mientras siga ACTIVO). */
    private LocalDateTime fechaHoraUso;
}
