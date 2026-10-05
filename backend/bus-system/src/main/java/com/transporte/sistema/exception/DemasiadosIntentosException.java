package com.transporte.sistema.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Demasiados intentos (H-06). Se responde 429 Too Many Requests en el login
 * cuando se supera el limite de intentos fallidos por usuario o por IP.
 */
@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
public class DemasiadosIntentosException extends RuntimeException {

    public DemasiadosIntentosException(String message) {
        super(message);
    }
}
