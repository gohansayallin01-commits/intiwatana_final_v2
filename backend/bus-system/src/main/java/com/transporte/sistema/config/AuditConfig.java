package com.transporte.sistema.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Habilita la auditoría automática de JPA.
 * Necesario para que @CreatedDate y @LastModifiedDate funcionen en BaseEntity.
 */
@Configuration
@EnableJpaAuditing
public class AuditConfig {
    // Spring Data JPA se encarga del resto al detectar @EntityListeners(AuditingEntityListener.class)
}
