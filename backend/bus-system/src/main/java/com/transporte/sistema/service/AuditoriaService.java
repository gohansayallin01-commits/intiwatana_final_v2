package com.transporte.sistema.service;

/**
 * Registro funcional de auditoría. Implementación tolerante a fallos: un problema
 * al auditar jamás debe romper la operación de negocio subyacente.
 */
public interface AuditoriaService {

    void registrar(Long usuarioId, String username, String rol,
                   String accion, String entidad, Long entidadId, String detalle);
}