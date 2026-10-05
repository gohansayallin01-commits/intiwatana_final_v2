package com.transporte.sistema.service.impl;

import com.transporte.sistema.entity.Auditoria;
import com.transporte.sistema.repository.AuditoriaRepository;
import com.transporte.sistema.service.AuditoriaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditoriaServiceImpl implements AuditoriaService {

    private final AuditoriaRepository auditoriaRepository;

    /** REQUIRES_NEW: el fallo de auditoría NO debe revertir la transacción de negocio. */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(Long usuarioId, String username, String rol,
                          String accion, String entidad, Long entidadId, String detalle) {
        try {
            auditoriaRepository.save(Auditoria.builder()
                    .usuarioId(usuarioId)
                    .username(username)
                    .rol(rol)
                    .accion(accion)
                    .entidad(entidad)
                    .entidadId(entidadId)
                    .detalle(detalle)
                    .build());
        } catch (Exception e) {
            log.error("No se pudo registrar auditoría (accion={}): {}", accion, e.getMessage());
        }
    }
}