package com.transporte.sistema.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * Usuarios del sistema (empleados y clientes con acceso).
 * Implementa Spring Security UserDetails a través del servicio.
 */
@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Usuario extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, unique = true, length = 80)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "nombres", nullable = false, length = 150)
    private String nombres;

    @Column(name = "apellidos", length = 150)
    private String apellidos;

    @Column(name = "email", unique = true, length = 150)
    private String email;

    @Column(name = "telefono", length = 20)
    private String telefono;

    /** Rol del usuario: ADMIN, CAJERO, CHOFER, CLIENTE */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "rol_id", nullable = false)
    private Rol rol;

    /** Sucursal a la que pertenece el usuario (null = todas) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sucursal_id")
    private Sucursal sucursal;

    /** DNI o RUC del usuario (para choferes y cajeros) */
    @Column(name = "dni_ruc", length = 15)
    private String dniRuc;

    @Column(name = "primer_login", nullable = false)
    @Builder.Default
    private Boolean primerLogin = true;
}
