package com.transporte.sistema.repository;

import com.transporte.sistema.entity.Usuario;
import com.transporte.sistema.enums.RolNombre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByUsername(String username);

    Optional<Usuario> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM Usuario u JOIN u.rol r WHERE r.nombre = :rolNombre AND u.activo = true")
    List<Usuario> findByRolNombre(@Param("rolNombre") RolNombre rolNombre);

    List<Usuario> findBySucursalIdAndActivoTrue(Long sucursalId);
}
