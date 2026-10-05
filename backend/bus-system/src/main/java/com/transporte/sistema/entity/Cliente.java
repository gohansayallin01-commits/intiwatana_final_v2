package com.transporte.sistema.entity;

import com.transporte.sistema.enums.TipoCliente;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
/**
 * Clientes que compran boletos o envían/reciben encomiendas.
 * Puede ser persona natural (DNI) o empresa (RUC).
 */
@Entity
@Table(name = "clientes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Cliente extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** DNI (8 dígitos) o RUC (11 dígitos) */
    @Column(name = "dni_ruc", nullable = false, unique = true, length = 15)
    private String dniRuc;

    /** Solo para personas naturales */
    @Column(name = "nombres", length = 150)
    private String nombres;

    @Column(name = "apellidos", length = 150)
    private String apellidos;

    /** Solo para empresas */
    @Column(name = "razon_social", length = 250)
    private String razonSocial;

    @Column(name = "telefono", length = 20)
    private String telefono;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "direccion", length = 300)
    private String direccion;

    @Column(name = "distrito", length = 100)
    private String distrito;

    @Column(name = "ciudad", length = 100)
    private String ciudad;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_cliente", nullable = false, length = 20)
    private TipoCliente tipoCliente;

    /** Cuenta de usuario vinculada (opcional, para portal self-service) */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    /** Nombre completo calculado según tipo */
    @Transient
    public String getNombreCompleto() {
        if (tipoCliente == TipoCliente.EMPRESA) {
            return razonSocial;
        }
        return nombres + " " + apellidos;
    }
}
