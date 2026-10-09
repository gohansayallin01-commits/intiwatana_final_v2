package com.transporte.sistema.service;

import com.transporte.sistema.dto.request.DeclararPagoYapeRequest;
import com.transporte.sistema.dto.request.RechazarPagoYapeRequest;
import com.transporte.sistema.dto.response.BoletoResponse;
import com.transporte.sistema.dto.response.CierreViajeResponse;
import com.transporte.sistema.dto.response.DeclaracionYapeResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Flujo Yape: declaración del cliente + verificación/aprobación en caja.
 *
 * La declaración NO genera boleto ni asiento vendido. Aprobar reutiliza el
 * {@code confirmarPago} comercial existente (asiento VENDIDO + boleto + QR).
 */
public interface PagoYapeService {

    DeclaracionYapeResponse declarar(String referencia, String documento,
                                     DeclararPagoYapeRequest request, MultipartFile comprobante);

    List<DeclaracionYapeResponse> listarPendientes();

    List<DeclaracionYapeResponse> historialDelDia();

    List<BoletoResponse> aprobar(Long declaracionId, String username);

    DeclaracionYapeResponse rechazar(Long declaracionId, RechazarPagoYapeRequest request, String username);

    CierreViajeResponse cierrePorViaje(Long viajeId);
}