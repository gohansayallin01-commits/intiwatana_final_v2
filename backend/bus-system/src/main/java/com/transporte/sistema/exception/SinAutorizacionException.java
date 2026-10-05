package com.transporte.sistema.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Usuario autenticado sin autorización sobre el recurso solicitado → HTTP 403 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class SinAutorizacionException extends RuntimeException {
    public SinAutorizacionException(String mensaje) { super(mensaje); }
}
