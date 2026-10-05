package com.transporte.sistema.service;

import com.transporte.sistema.dto.request.LoginRequest;
import com.transporte.sistema.dto.response.LoginResponse;

public interface AuthService {

    /**
     * Autentica al usuario. Aplica el limite de intentos (H-06):
     * un login correcto limpia el contador y uno fallido lo incrementa.
     *
     * @param ipOrigen IP del solicitante, para el limite por IP
     */
    LoginResponse login(LoginRequest request, String ipOrigen);

    /**
     * Revoca el token de acceso actual (logout). Tras la llamada, el token deja de
     * autenticar aunque no haya expirado.
     *
     * @return el username del sesion cerrada, o null si el token no es utilizable
     */
    String logout(String token);

    /**
     * Renueva un token de acceso a partir de un refresh token vigente.
     * El refresh token se rota: el usado queda revocado y no sirve de nuevo.
     */
    LoginResponse refresh(String refreshToken);
}
