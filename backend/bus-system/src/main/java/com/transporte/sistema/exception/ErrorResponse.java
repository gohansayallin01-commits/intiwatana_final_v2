package com.transporte.sistema.exception;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
/** Estructura estándar de error devuelto por la API */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ErrorResponse {
    private int status;
    private String error;
    private String mensaje;
    private String path;
    private LocalDateTime timestamp;
    private List<String> errores; // para errores de validación múltiples
}
