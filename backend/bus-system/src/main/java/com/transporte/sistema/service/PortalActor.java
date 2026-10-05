package com.transporte.sistema.service;

/** Identidad del usuario que ejecuta una acción editorial (desde el JWT). */
public record PortalActor(Long usuarioId, String username, String rol) {
}
