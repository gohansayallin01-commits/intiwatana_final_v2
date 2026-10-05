package com.transporte.sistema.service.impl;

import com.transporte.sistema.dto.request.CrearPagoRequest;
import com.transporte.sistema.dto.response.PagoResponse;
import com.transporte.sistema.entity.Boleto;
import com.transporte.sistema.entity.Encomienda;
import com.transporte.sistema.entity.Pago;
import com.transporte.sistema.enums.EstadoPago;
import com.transporte.sistema.exception.NegocioException;
import com.transporte.sistema.repository.BoletoRepository;
import com.transporte.sistema.repository.EncomiendaRepository;
import com.transporte.sistema.repository.PagoRepository;
import com.transporte.sistema.service.PagoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PagoServiceImpl implements PagoService {

    private final PagoRepository pagoRepository;
    private final BoletoRepository boletoRepository;
    private final EncomiendaRepository encomiendaRepository;

    @Override
    @Transactional
    public PagoResponse procesarPago(CrearPagoRequest request) {
        if (request.getMonto() == null || request.getMonto().compareTo(BigDecimal.ZERO) <= 0) {
            throw new NegocioException("El monto debe ser mayor a cero");
        }

        if (request.getMetodo() != null) {
            switch (request.getMetodo()) {
                case TARJETA_CREDITO, TARJETA_DEBITO, TRANSFERENCIA, YAPE, PLIN -> {
                    if (request.getReferencia() == null || request.getReferencia().isBlank()) {
                        throw new NegocioException("La referencia es obligatoria para métodos electrónicos");
                    }
                }
                default -> {}
            }
        }

        Pago pago = Pago.builder()
                .monto(request.getMonto())
                .fechaPago(LocalDateTime.now())
                .metodo(request.getMetodo())
                .estado(request.getEstado() != null ? request.getEstado() : EstadoPago.COMPLETADO)
                .referencia(request.getReferencia())
                .observacion(request.getObservacion())
                .activo(true)
                .build();

        return toResponse(pagoRepository.save(pago));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PagoResponse> listarTodos() {
        return pagoRepository.findByActivoTrueOrderByFechaPagoDesc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PagoResponse obtenerPorId(Long id) {
        return pagoRepository.findByIdAndActivoTrue(id)
                .map(this::toResponse)
                .orElseThrow(() -> new NegocioException("Pago no encontrado"));
    }

    @Override
    @Transactional
    public PagoResponse actualizarPago(Long id, CrearPagoRequest request) {
        Pago pago = pagoRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new NegocioException("Pago no encontrado"));

        pago.setMonto(request.getMonto());
        pago.setMetodo(request.getMetodo());
        pago.setReferencia(request.getReferencia());
        pago.setObservacion(request.getObservacion());
        if (request.getEstado() != null) pago.setEstado(request.getEstado());

        return toResponse(pagoRepository.save(pago));
    }

    @Override
    @Transactional
    public void eliminarPago(Long id) {
        Pago pago = pagoRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new NegocioException("Pago no encontrado"));
        pago.setActivo(false);
        pagoRepository.save(pago);
    }

    private PagoResponse toResponse(Pago p) {
        PagoResponse resp = PagoResponse.builder()
                .id(p.getId())
                .monto(p.getMonto())
                .fechaPago(p.getFechaPago())
                .metodo(p.getMetodo())
                .estado(p.getEstado())
                .referencia(p.getReferencia())
                .build();

        // Intentar obtener cliente desde la relación directa en Pago (si la tiene)
        if (p.getCliente() != null) {
            resp.setCliente(p.getCliente().getNombreCompleto());
        } else {
            // Buscar en boletos o encomiendas
            boletoRepository.findByPagoId(p.getId()).ifPresent(b -> {
                resp.setCliente(b.getCliente().getNombreCompleto());
                resp.setReferenciaEntidad("Boleto: " + b.getNumeroBoleto());
            });

            if (resp.getCliente() == null) {
                encomiendaRepository.findByPagoId(p.getId()).ifPresent(e -> {
                    resp.setCliente(e.getRemitente().getNombreCompleto());
                    resp.setReferenciaEntidad("Encomienda: " + e.getNumeroGuia());
                });
            }
        }

        return resp;
    }
}
