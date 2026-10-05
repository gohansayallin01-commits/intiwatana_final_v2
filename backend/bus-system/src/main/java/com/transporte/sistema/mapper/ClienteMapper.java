package com.transporte.sistema.mapper;

import com.transporte.sistema.dto.request.ClienteRequest;
import com.transporte.sistema.dto.response.ClienteResponse;
import com.transporte.sistema.entity.Cliente;
import org.mapstruct.*;

/**
 * Mapper MapStruct para la entidad Cliente.
 * MapStruct genera automáticamente la implementación en tiempo de compilación.
 *
 * Nota: componentModel = "spring" (configurado globalmente en pom.xml) permite
 * inyectarlo como @Autowired / constructor injection.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ClienteMapper {

    /**
     * Convierte ClienteRequest → Cliente entity.
     * Los campos no presentes en el request (id, createdAt, etc.) son ignorados.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "activo", ignore = true)
    @Mapping(target = "usuario", ignore = true)
    Cliente toEntity(ClienteRequest request);

    /**
     * Convierte Cliente entity → ClienteResponse DTO.
     * El campo nombreCompleto no existe en la entidad como columna,
     * sino como método @Transient, por lo que se mapea explícitamente.
     */
    @Mapping(target = "nombreCompleto", expression = "java(cliente.getNombreCompleto())")
    ClienteResponse toResponse(Cliente cliente);

    /**
     * Actualiza una entidad existente desde un request (útil para PUT).
     * @MappingTarget indica que el parámetro es el objeto a modificar.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "activo", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "usuario", ignore = true)
    void updateEntityFromRequest(ClienteRequest request, @MappingTarget Cliente cliente);
}
