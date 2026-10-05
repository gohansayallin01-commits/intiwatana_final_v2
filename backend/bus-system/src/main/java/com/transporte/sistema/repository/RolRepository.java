package com.transporte.sistema.repository;
import com.transporte.sistema.entity.Rol;
import com.transporte.sistema.enums.RolNombre;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface RolRepository extends JpaRepository<Rol, Long> {
    Optional<Rol> findByNombre(RolNombre nombre);
}
