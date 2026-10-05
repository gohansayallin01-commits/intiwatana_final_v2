package com.transporte.sistema.service;

/**
 * Abstracción de almacenamiento de recursos del Portal.
 *
 * No depende de un proveedor concreto. La implementación por defecto usa el
 * sistema de archivos local (válido para desarrollo y para un servidor con
 * disco persistente). Para producción en un PaaS con disco efímero debe
 * implementarse un proveedor de objetos (S3/R2/GCS) — ver limitaciones del
 * informe. Mientras no exista un almacenamiento persistente autorizado, el
 * portal solo administra metadatos y referencias.
 */
public interface PortalStorageService {

    /** Indica si el almacenamiento está configurado y operativo. */
    boolean disponible();

    /** Identificador del tipo de almacenamiento activo (p. ej. "local" o "none"). */
    String tipo();

    /** Persiste el binario y devuelve el nombre interno y la URL pública. */
    StoredFile guardar(byte[] contenido, String extension);

    /** Elimina (best-effort) un recurso por su nombre interno. */
    void eliminar(String nombreInterno);

    record StoredFile(String nombreInterno, String url) {
    }
}
