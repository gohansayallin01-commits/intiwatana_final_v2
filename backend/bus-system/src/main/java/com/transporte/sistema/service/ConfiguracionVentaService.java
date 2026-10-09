package com.transporte.sistema.service;

import com.transporte.sistema.dto.request.ConfigurarTiempoRequest;
import com.transporte.sistema.dto.request.ConfiguracionYapeRequest;
import com.transporte.sistema.dto.response.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Configuración administrativa del flujo de reserva y pago.
 *
 * Sólo ROLE_ADMIN puede modificar tiempos y el QR oficial Yape. El cajero no
 * puede cambiar duraciones, vencimientos ni la operación vinculada.
 */
public interface ConfiguracionVentaService {

    // ── Tiempos (leídos por el backend como fuente de verdad) ───────────────

    long holdMinutos();

    long pagoPendienteMinutos();

    long cierreWebMinutos();

    long toleranciaYapeMinutos();

    List<ParametroVentaResponse> listarTiempos();

    ParametroVentaResponse actualizarTiempo(ConfigurarTiempoRequest request, String username);

    List<ParametroVentaHistorialResponse> historialTiempos(String parametro);

    // ── QR oficial Yape (configurado por ADMIN) ─────────────────────────────

    ConfiguracionYapeResponse guardarConfiguracionYape(ConfiguracionYapeRequest request,
                                                       MultipartFile imagen, String username);

    ConfiguracionYapeResponse yapeActiva();

    ConfiguracionYapePublicaResponse yapeActivaPublica();

    List<ConfiguracionYapeResponse> listarYapeHistorico();

    List<ConfiguracionYapeHistorialResponse> historialYape();
}