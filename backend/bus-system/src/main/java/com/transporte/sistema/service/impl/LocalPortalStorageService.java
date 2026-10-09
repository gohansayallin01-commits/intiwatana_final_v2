package com.transporte.sistema.service.impl;

import com.transporte.sistema.service.PortalStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Almacenamiento local en disco. Válido para desarrollo y servidores con
 * disco persistente. En un PaaS con filesystem efímero los archivos se pierden
 * al reiniciar: en ese caso debe usarse otro proveedor (ver informe).
 */
@Service
@Slf4j
public class LocalPortalStorageService implements PortalStorageService {

    private final String tipo;
    private final Path baseDir;
    private final String mediaPrefix;

    public LocalPortalStorageService(
            @Value("${app.portal.storage.tipo:local}") String tipo,
            @Value("${app.portal.storage.directorio:storage/portal}") String directorio,
            @Value("${app.portal.media-prefix:/media/portal}") String mediaPrefix) {
        this.tipo = tipo;
        this.mediaPrefix = mediaPrefix;
        this.baseDir = Paths.get(directorio).toAbsolutePath().normalize();
        // El directorio debe existir desde el arranque para que el resource
        // handler /media/portal/** resuelva los archivos; si no, Spring lo
        // cachea como ausente mientras el proceso está vivo.
        try {
            Files.createDirectories(this.baseDir);
        } catch (IOException e) {
            log.warn("No se pudo crear el directorio de almacenamiento del portal: {}", e.getMessage());
        }
    }

    @Override
    public boolean disponible() {
        return "local".equalsIgnoreCase(tipo);
    }

    @Override
    public String tipo() {
        return tipo;
    }

    @Override
    public StoredFile guardar(byte[] contenido, String extension) {
        if (!disponible()) {
            throw new IllegalStateException("Almacenamiento del portal no configurado: " + tipo);
        }
        String nombre = UUID.randomUUID().toString().replace("-", "") + "." + extension.toLowerCase();
        try {
            Files.createDirectories(baseDir);
            Path destino = baseDir.resolve(nombre).normalize();
            // Prevención de path traversal: el destino debe quedar dentro de baseDir.
            if (!destino.startsWith(baseDir)) {
                throw new IOException("Ruta de destino no permitida");
            }
            Files.write(destino, contenido);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo guardar la imagen: " + e.getMessage(), e);
        }
        return new StoredFile(nombre, mediaPrefix + "/" + nombre);
    }

    @Override
    public void eliminar(String nombreInterno) {
        if (nombreInterno == null || nombreInterno.isBlank()) return;
        try {
            Path destino = baseDir.resolve(nombreInterno).normalize();
            if (destino.startsWith(baseDir)) {
                Files.deleteIfExists(destino);
            }
        } catch (IOException e) {
            log.warn("No se pudo eliminar el recurso {}: {}", nombreInterno, e.getMessage());
        }
    }

    /** Directorio base, expuesto para registrar el resource handler HTTP. */
    public Path getBaseDir() {
        return baseDir;
    }
}
