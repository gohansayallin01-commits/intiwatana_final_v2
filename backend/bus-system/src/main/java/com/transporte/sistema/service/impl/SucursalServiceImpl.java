package com.transporte.sistema.service.impl;

import com.transporte.sistema.dto.request.SucursalRequest;
import com.transporte.sistema.dto.response.SucursalResponse;
import com.transporte.sistema.entity.Sucursal;
import com.transporte.sistema.exception.ConflictoException;
import com.transporte.sistema.exception.RecursoNoEncontradoException;
import com.transporte.sistema.repository.SucursalRepository;
import com.transporte.sistema.service.SucursalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@SuppressWarnings("DataFlowIssue")
public class SucursalServiceImpl implements SucursalService {

    private final SucursalRepository sucursalRepository;

    @Override
    @Transactional
    public SucursalResponse crear(SucursalRequest request) {
        if (sucursalRepository.existsByCodigo(request.getCodigo()))
            throw new ConflictoException("Ya existe una sucursal con código: " + request.getCodigo());
        Sucursal sucursal = toEntity(request);
        return toResponse(sucursalRepository.save(sucursal));
    }

    @Override
    @Transactional
    public SucursalResponse actualizar(Long id, SucursalRequest request) {
        Sucursal sucursal = obtenerEntidad(id);
        sucursal.setCodigo(request.getCodigo());
        sucursal.setNombre(request.getNombre());
        sucursal.setDireccion(request.getDireccion());
        sucursal.setCiudad(request.getCiudad());
        sucursal.setProvincia(request.getProvincia());
        sucursal.setDepartamento(request.getDepartamento());
        sucursal.setTelefono(request.getTelefono());
        sucursal.setEmail(request.getEmail());
        sucursal.setEsTerminal(request.getEsTerminal());
        sucursal.setLatitud(request.getLatitud());
        sucursal.setLongitud(request.getLongitud());
        return toResponse(sucursalRepository.save(sucursal));
    }

    @Override
    @Transactional(readOnly = true)
    public SucursalResponse obtenerPorId(Long id) {
        return toResponse(obtenerEntidad(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SucursalResponse> listarActivas() {
        return sucursalRepository.findByActivoTrue().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SucursalResponse> listarTerminales() {
        List<SucursalResponse> terminales = sucursalRepository.findByEsTerminalTrueAndActivoTrue()
                .stream().map(this::toResponse).toList();
        // Si no hay sucursales marcadas como terminal, devolver todas las activas
        // (evita lista vacía en el selector público de pasajes)
        if (terminales.isEmpty()) {
            return sucursalRepository.findByActivoTrue().stream().map(this::toResponse).toList();
        }
        return terminales;
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Sucursal s = obtenerEntidad(id);
        s.softDelete();
        sucursalRepository.save(s);
    }

    private Sucursal obtenerEntidad(Long id) {
        return sucursalRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Sucursal", id));
    }

    private Sucursal toEntity(SucursalRequest r) {
        return Sucursal.builder()
                .codigo(r.getCodigo()).nombre(r.getNombre()).direccion(r.getDireccion())
                .ciudad(r.getCiudad()).provincia(r.getProvincia())
                .departamento(r.getDepartamento()).telefono(r.getTelefono())
                .email(r.getEmail()).esTerminal(r.getEsTerminal() != null ? r.getEsTerminal() : false)
                .latitud(r.getLatitud()).longitud(r.getLongitud())
                .activo(true)
                .build();
    }

    public SucursalResponse toResponse(Sucursal s) {
        return SucursalResponse.builder()
                .id(s.getId()).codigo(s.getCodigo()).nombre(s.getNombre())
                .ciudad(s.getCiudad()).provincia(s.getProvincia())
                .departamento(s.getDepartamento()).direccion(s.getDireccion())
                .telefono(s.getTelefono()).esTerminal(s.getEsTerminal())
                .activo(s.getActivo()).build();
    }
}
