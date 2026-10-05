package com.transporte.sistema.exception;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
/** Conflicto de datos (duplicado) → HTTP 409 */
@ResponseStatus(HttpStatus.CONFLICT)
public class ConflictoException extends RuntimeException {
    public ConflictoException(String mensaje) { super(mensaje); }
}
