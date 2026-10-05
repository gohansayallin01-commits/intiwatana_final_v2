package com.transporte.sistema.exception;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
/** Error de regla de negocio → HTTP 400 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class NegocioException extends RuntimeException {
    public NegocioException(String mensaje) { super(mensaje); }
}
