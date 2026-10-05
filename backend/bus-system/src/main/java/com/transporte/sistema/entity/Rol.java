package com.transporte.sistema.entity;

import com.transporte.sistema.enums.RolNombre;
import jakarta.persistence.*;
import lombok.*;

/**
 * Roles del sistema. Se cargan al iniciar (data.sql o DataInitializer).
 */
@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Rol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "nombre", nullable = false, unique = true, length = 30)
    private RolNombre nombre;

    @Column(name = "descripcion", length = 200)
    private String descripcion;
}
