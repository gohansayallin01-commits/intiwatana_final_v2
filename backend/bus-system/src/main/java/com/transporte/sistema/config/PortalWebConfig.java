package com.transporte.sistema.config;

import com.transporte.sistema.service.impl.LocalPortalStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sirve los recursos del Portal almacenados en disco bajo /media/portal/**.
 *
 * Solo se registra cuando el almacenamiento local está disponible. La lista de
 * tipos MIME servidos se restringe a imágenes para evitar exponer otros
 * archivos.
 */
@Configuration
@RequiredArgsConstructor
public class PortalWebConfig implements WebMvcConfigurer {

    private final LocalPortalStorageService localPortalStorageService;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        if (!localPortalStorageService.disponible()) {
            return;
        }
        String base = localPortalStorageService.getBaseDir().toUri().toString();
        registry.addResourceHandler("/media/portal/**")
                .addResourceLocations(base)
                .setCachePeriod(3600);
    }
}
