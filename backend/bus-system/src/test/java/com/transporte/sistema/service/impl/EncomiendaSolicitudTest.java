package com.transporte.sistema.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transporte.sistema.dto.request.ActualizarEstadoEncomiendaRequest;
import com.transporte.sistema.dto.request.ConfirmarRecepcionEncomiendaRequest;
import com.transporte.sistema.dto.request.EncomiendaClienteRequest;
import com.transporte.sistema.dto.request.EncomiendaRequest;
import com.transporte.sistema.dto.response.EncomiendaResponse;
import com.transporte.sistema.dto.response.SolicitudPendienteResponse;
import com.transporte.sistema.dto.response.TrackingEncomiendaResponse;
import com.transporte.sistema.entity.Cliente;
import com.transporte.sistema.entity.Encomienda;
import com.transporte.sistema.entity.MovimientoEncomienda;
import com.transporte.sistema.entity.Pago;
import com.transporte.sistema.entity.Sucursal;
import com.transporte.sistema.entity.Usuario;
import com.transporte.sistema.enums.EstadoEncomienda;
import com.transporte.sistema.enums.EstadoPago;
import com.transporte.sistema.enums.MetodoPago;
import com.transporte.sistema.enums.TipoCliente;
import com.transporte.sistema.exception.NegocioException;
import com.transporte.sistema.repository.ClienteRepository;
import com.transporte.sistema.repository.EncomiendaRepository;
import com.transporte.sistema.repository.MovimientoEncomiendaRepository;
import com.transporte.sistema.repository.PagoRepository;
import com.transporte.sistema.repository.SucursalRepository;
import com.transporte.sistema.repository.UsuarioRepository;
import com.transporte.sistema.repository.ViajeRepository;
import com.transporte.sistema.service.AuditoriaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas de la SOLICITUD previa de encomienda (SOL-AAAA-NNNNNN) y de su
 * oficializacion unica (GUI) tanto desde el portal web como desde el registro
 * presencial en agencia.
 *
 * Los repositorios se sustituyen por dobles: aqui se comprueba la reglas de
 * negocio (que datos nacen, que datos solo existen tras la recepcion fisica,
 * idempotencia, transiciones y privacidad del tracking), no la integridad de la
 * base de datos, que exige PostgreSQL y se cubre en la QA aislada.
 */
class EncomiendaSolicitudTest {

    private EncomiendaRepository           encomiendaRepository;
    private ClienteRepository              clienteRepository;
    private SucursalRepository             sucursalRepository;
    private ViajeRepository                viajeRepository;
    private MovimientoEncomiendaRepository movimientoRepository;
    private UsuarioRepository              usuarioRepository;
    private PagoRepository                 pagoRepository;
    private AuditoriaService               auditoriaService;
    private EncomiendaServiceImpl          servicio;

    private static final long ID_ENCOMIENDA = 42L;

    @BeforeEach
    void setUp() {
        encomiendaRepository = mock(EncomiendaRepository.class);
        clienteRepository = mock(ClienteRepository.class);
        sucursalRepository = mock(SucursalRepository.class);
        viajeRepository = mock(ViajeRepository.class);
        movimientoRepository = mock(MovimientoEncomiendaRepository.class);
        usuarioRepository = mock(UsuarioRepository.class);
        pagoRepository = mock(PagoRepository.class);
        auditoriaService = mock(AuditoriaService.class);

        // Los saves devuelven la misma entidad: sin esto el servicio recibiría null
        // y no podría construir la respuesta oficial.
        when(encomiendaRepository.save(any(Encomienda.class))).thenAnswer(inv -> {
            Encomienda e = inv.getArgument(0);
            if (e.getId() == null) e.setId(ID_ENCOMIENDA);
            return e;
        });
        when(pagoRepository.save(any(Pago.class))).thenAnswer(inv -> inv.getArgument(0));
        when(movimientoRepository.save(any(MovimientoEncomienda.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Correlativo de la secuencia seq_encomienda_sol.
        when(encomiendaRepository.siguienteCorrelativoSolicitud())
                .thenReturn(7L, 8L, 9L, 10L, 11L, 12L);

        servicio = new EncomiendaServiceImpl(encomiendaRepository, clienteRepository,
                sucursalRepository, viajeRepository, movimientoRepository, usuarioRepository,
                pagoRepository, auditoriaService, transaccionSinRecursos());
    }

    /**
     * TransactionTemplate que ejecuta el trabajo sin gestionar recursos: en esta
     * prueba no se verifican las fronteras de transaccion (eso lo cubre
     * EncomiendaFlujoIT contra PostgreSQL), solo hace falta que el servicio
     * pueda ejecutar su bloque transaccional.
     */
    private static TransactionTemplate transaccionSinRecursos() {
        PlatformTransactionManager gestor = new PlatformTransactionManager() {
            @Override
            public TransactionStatus getTransaction(TransactionDefinition definicion) {
                return new SimpleTransactionStatus();
            }

            @Override
            public void commit(TransactionStatus estado) { }

            @Override
            public void rollback(TransactionStatus estado) { }
        };
        return new TransactionTemplate(gestor);
    }

    // ── Escenarios de prueba ──────────────────────────────────────────────────

    private Cliente remitente() {
        return Cliente.builder().id(1L).dniRuc("70123456")
                .nombres("Ana").apellidos("Quispe")
                .telefono("999888777").tipoCliente(TipoCliente.PERSONA).build();
    }

    private Cliente destinatario() {
        return Cliente.builder().id(2L).dniRuc("70999999")
                .nombres("Luis").apellidos("Ramos")
                .telefono("988777666").tipoCliente(TipoCliente.PERSONA).build();
    }

    private Sucursal sucursal(long id, String nombre) {
        return Sucursal.builder().id(id).codigo("SUC" + id).nombre(nombre).ciudad(nombre).build();
    }

    private Usuario cajero() {
        return Usuario.builder().id(3L).username("cajero1").build();
    }

    private Encomienda solicitudPendiente() {
        return Encomienda.builder()
                .id(ID_ENCOMIENDA)
                .referenciaSolicitud("SOL-2026-000007")
                .remitente(remitente())
                .destinatario(null)
                .destinatarioNombre("Luis Ramos")
                .remitenteTelefono("999888777")
                .sucursalOrigen(sucursal(1L, "Ayacucho"))
                .sucursalDestino(sucursal(2L, "Lima"))
                .descripcionContenido("Caja con documentos")
                .estado(EstadoEncomienda.SOLICITUD_PENDIENTE)
                .observaciones("Tipo: PAQUETE | Tel. contacto: 988777666 | Fragil")
                .build();
    }

    private ConfirmarRecepcionEncomiendaRequest recepcionValida() {
        return ConfirmarRecepcionEncomiendaRequest.builder()
                .pesoKg(new BigDecimal("2.500"))
                .largoCm(new BigDecimal("30"))
                .anchoCm(new BigDecimal("20"))
                .altoCm(new BigDecimal("10"))
                .valorDeclarado(new BigDecimal("100.00"))
                .tarifaOficial(new BigDecimal("25.00"))
                .metodoPago(MetodoPago.YAPE)
                .observaciones("Recibido en ventanilla")
                .build();
    }

    private EncomiendaRequest registroPresencial(String claveIdempotencia) {
        return EncomiendaRequest.builder()
                .remitenteId(1L).destinatarioId(2L)
                .sucursalOrigenId(1L).sucursalDestinoId(2L)
                .descripcionContenido("Caja con documentos")
                .pesoKg(new BigDecimal("2.500"))
                .largoCm(new BigDecimal("30")).anchoCm(new BigDecimal("20")).altoCm(new BigDecimal("10"))
                .tarifaOficial(new BigDecimal("25.00"))
                .metodoPago(MetodoPago.EFECTIVO)
                .claveIdempotencia(claveIdempotencia)
                .build();
    }

    private void escenarioPresencial() {
        when(clienteRepository.findById(1L)).thenReturn(java.util.Optional.of(remitente()));
        when(clienteRepository.findById(2L)).thenReturn(java.util.Optional.of(destinatario()));
        when(sucursalRepository.findById(1L)).thenReturn(java.util.Optional.of(sucursal(1L, "Ayacucho")));
        when(sucursalRepository.findById(2L)).thenReturn(java.util.Optional.of(sucursal(2L, "Lima")));
        when(encomiendaRepository.findByClaveIdempotencia(anyString()))
                .thenReturn(java.util.Optional.empty());
    }

    // ── Solicitud web ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Solicitud web (SOL)")
    class SolicitudWeb {

        private EncomiendaClienteRequest solicitudPublica() {
            return EncomiendaClienteRequest.builder()
                    .sucursalOrigenId(1L).sucursalDestinoId(2L)
                    .remitenteNombre("Ana Quispe").remitenteTelefono(" 999888777 ")
                    .destinatarioNombre(" Luis Ramos ").destinatarioTelefono("988777666")
                    .descripcionContenido("Caja con documentos")
                    .tipoEnvio("PAQUETE").cuidadoEspecial("Fragil")
                    .observaciones("Sinapidermible")
                    .build();
        }

        @Test
        @DisplayName("Asigna la referencia SOL antes del INSERT, con el formato exigido")
        void referenciaSolAntesDelInsert() {
            when(clienteRepository.findByUsuarioUsername("ana"))
                    .thenReturn(java.util.Optional.of(remitente()));
            when(sucursalRepository.findById(1L)).thenReturn(java.util.Optional.of(sucursal(1L, "Ayacucho")));
            when(sucursalRepository.findById(2L)).thenReturn(java.util.Optional.of(sucursal(2L, "Lima")));

            EncomiendaResponse r = servicio.solicitarEncomienda(solicitudPublica(), "ana");

            assertNotNull(r.getReferenciaSolicitud(), "La solicitud debe tener referencia propia");
            assertTrue(r.getReferenciaSolicitud().matches("SOL-\\d{4}-\\d{6,}"),
                    "Formato de referencia inválido: " + r.getReferenciaSolicitud());

            ArgumentCaptor<Encomienda> guardada = ArgumentCaptor.forClass(Encomienda.class);
            verify(encomiendaRepository).save(guardada.capture());
            assertNotNull(guardada.getValue().getReferenciaSolicitud(),
                    "La referencia debe existir ya en la fila insertada, no asignarse despues");
            verify(encomiendaRepository).siguienteCorrelativoSolicitud();
        }

        @Test
        @DisplayName("La solicitud nace sin guia, peso, medidas, tarifa, metodo de pago ni pago")
        void naceSinDatosOficiales() {
            when(clienteRepository.findByUsuarioUsername("ana"))
                    .thenReturn(java.util.Optional.of(remitente()));
            when(sucursalRepository.findById(1L)).thenReturn(java.util.Optional.of(sucursal(1L, "Ayacucho")));
            when(sucursalRepository.findById(2L)).thenReturn(java.util.Optional.of(sucursal(2L, "Lima")));

            EncomiendaResponse r = servicio.solicitarEncomienda(solicitudPublica(), "ana");

            assertEquals(EstadoEncomienda.SOLICITUD_PENDIENTE, r.getEstado());
            assertNull(r.getNumeroGuia(), "Una solicitud no tiene guia oficial");
            assertNull(r.getPesoKg(), "El peso lo mide el personal autorizado");
            assertNull(r.getLargoCm());
            assertNull(r.getAnchoCm());
            assertNull(r.getAltoCm());
            assertNull(r.getVolumenM3(), "El volumen se calcula tras la recepcion");
            assertNull(r.getCosto(), "La tarifa la fija el personal autorizado");
            assertNull(r.getMetodoPago(), "El publico no elige metodo de pago");
            assertEquals("Luis Ramos", r.getDestinatarioNombre());
            assertEquals("999888777", r.getRemitenteTelefono(), "Se guarda el telefono declarado, sin espacios");
        }

        @Test
        @DisplayName("Rechaza origen y destino iguales")
        void rechazaRutaCircular() {
            when(clienteRepository.findByUsuarioUsername("ana"))
                    .thenReturn(java.util.Optional.of(remitente()));
            when(sucursalRepository.findById(1L)).thenReturn(java.util.Optional.of(sucursal(1L, "Ayacucho")));
            when(sucursalRepository.findById(2L)).thenReturn(java.util.Optional.of(sucursal(1L, "Ayacucho")));

            EncomiendaClienteRequest request = solicitudPublica();
            request.setSucursalDestinoId(1L);

            assertThrows(NegocioException.class, () -> servicio.solicitarEncomienda(request, "ana"));
            verify(encomiendaRepository, never()).save(any(Encomienda.class));
        }

        @Test
        @DisplayName("Un cliente sin perfil activo recibe un mensaje claro y no crea filas")
        void exigePerfilDeCliente() {
            when(clienteRepository.findByUsuarioUsername("desconocido"))
                    .thenReturn(java.util.Optional.empty());

            NegocioException e = assertThrows(NegocioException.class,
                    () -> servicio.solicitarEncomienda(solicitudPublica(), "desconocido"));

            assertTrue(e.getMessage().toLowerCase().contains("perfil de cliente"),
                    "El mensaje debe explicar que falta el perfil. Fue: " + e.getMessage());
            verify(encomiendaRepository, never()).save(any(Encomienda.class));
        }

        @Test
        @DisplayName("Sin correlativo de la secuencia falla de forma explicita y no inventa una referencia")
        void sinCorrelativoNoInventaReferencia() {
            when(clienteRepository.findByUsuarioUsername("ana"))
                    .thenReturn(java.util.Optional.of(remitente()));
            when(sucursalRepository.findById(1L)).thenReturn(java.util.Optional.of(sucursal(1L, "Ayacucho")));
            when(sucursalRepository.findById(2L)).thenReturn(java.util.Optional.of(sucursal(2L, "Lima")));
            when(encomiendaRepository.siguienteCorrelativoSolicitud()).thenReturn(null);

            NegocioException e = assertThrows(NegocioException.class,
                    () -> servicio.solicitarEncomienda(solicitudPublica(), "ana"));

            assertTrue(e.getMessage().toLowerCase().contains("referencia"),
                    "Fue: " + e.getMessage());
            verify(encomiendaRepository, never()).save(any(Encomienda.class));
        }
    }

    // ── Oficialización (confirmación de recepción) ─────────────────────────────

    @Nested
    @DisplayName("Confirmación de recepción en agencia")
    class ConfirmacionRecepcion {

        @Test
        @DisplayName("Convierte la SOL en una encomienda oficial con un solo pago, guia y movimiento")
        void oficializaEnUnaOperacion() {
            when(encomiendaRepository.findByIdForUpdate(ID_ENCOMIENDA))
                    .thenReturn(java.util.Optional.of(solicitudPendiente()));

            EncomiendaResponse r = servicio.confirmarRecepcion(ID_ENCOMIENDA, recepcionValida());

            assertEquals(EstadoEncomienda.RECIBIDO, r.getEstado());
            assertNotNull(r.getNumeroGuia());
            assertTrue(r.getNumeroGuia().matches("GUI-\\d{8}-\\d{6,}"),
                    "Formato de guia oficial inválido: " + r.getNumeroGuia());
            assertEquals(0, new BigDecimal("2.500").compareTo(r.getPesoKg()), "peso real");
            assertEquals(0, new BigDecimal("0.0060").compareTo(r.getVolumenM3()),
                    "El backend calcula el volumen: 30x20x10 cm = 0.0060 m3. Fue: " + r.getVolumenM3());
            assertEquals(0, new BigDecimal("25.00").compareTo(r.getCosto()), "tarifa oficial");
            assertEquals(MetodoPago.YAPE, r.getMetodoPago());

            ArgumentCaptor<Pago> pago = ArgumentCaptor.forClass(Pago.class);
            verify(pagoRepository, times(1)).save(pago.capture());
            assertEquals(EstadoPago.COMPLETADO, pago.getValue().getEstado(),
                    "El pago es presencial e inmediato");
            assertEquals(0, new BigDecimal("25.00").compareTo(pago.getValue().getMonto()));

            ArgumentCaptor<MovimientoEncomienda> movimiento = ArgumentCaptor.forClass(MovimientoEncomienda.class);
            verify(movimientoRepository, times(1)).save(movimiento.capture());
            assertEquals(EstadoEncomienda.SOLICITUD_PENDIENTE, movimiento.getValue().getEstadoAnterior());
            assertEquals(EstadoEncomienda.RECIBIDO, movimiento.getValue().getEstadoNuevo());

            verify(auditoriaService).registrar(any(), any(), any(),
                    eq("FIJAR_TARIFA_ENCOMIENDA"), eq("Encomienda"), eq(ID_ENCOMIENDA), anyString());
        }

        @Test
        @DisplayName("Una segunda confirmación devuelve la misma guia sin duplicar pago ni movimiento")
        void confirmacionIdempotente() {
            Encomienda yaOficializada = solicitudPendiente();
            yaOficializada.setEstado(EstadoEncomienda.RECIBIDO);
            yaOficializada.setNumeroGuia("GUI-20260101-000042");
            yaOficializada.setPago(Pago.builder().estado(EstadoPago.COMPLETADO)
                    .monto(new BigDecimal("25.00")).build());
            when(encomiendaRepository.findByIdForUpdate(ID_ENCOMIENDA))
                    .thenReturn(java.util.Optional.of(yaOficializada));

            EncomiendaResponse r = servicio.confirmarRecepcion(ID_ENCOMIENDA, recepcionValida());

            assertEquals("GUI-20260101-000042", r.getNumeroGuia());
            verify(pagoRepository, never()).save(any(Pago.class));
            verify(movimientoRepository, never()).save(any(MovimientoEncomienda.class));
        }

        @Test
        @DisplayName("Una encomienda no pendiente ni oficializada no se puede confirmar")
        void exigeSolicitudPendiente() {
            Encomienda enAlmacen = solicitudPendiente();
            enAlmacen.setEstado(EstadoEncomienda.EN_ALMACEN);
            when(encomiendaRepository.findByIdForUpdate(ID_ENCOMIENDA))
                    .thenReturn(java.util.Optional.of(enAlmacen));

            NegocioException e = assertThrows(NegocioException.class,
                    () -> servicio.confirmarRecepcion(ID_ENCOMIENDA, recepcionValida()));

            assertTrue(e.getMessage().contains("EN_ALMACEN"),
                    "El mensaje debe mostrar el estado actual. Fue: " + e.getMessage());
        }

        @Test
        @DisplayName("Rechaza pago contra entrega: el cobro de encomiendas es presencial e inmediato")
        void rechazaContraEntrega() {
            when(encomiendaRepository.findByIdForUpdate(ID_ENCOMIENDA))
                    .thenReturn(java.util.Optional.of(solicitudPendiente()));
            ConfirmarRecepcionEncomiendaRequest request = recepcionValida();
            request.setMetodoPago(MetodoPago.CONTRA_ENTREGA);

            NegocioException e = assertThrows(NegocioException.class,
                    () -> servicio.confirmarRecepcion(ID_ENCOMIENDA, request));

            assertTrue(e.getMessage().toLowerCase().contains("contra entrega"),
                    "Fue: " + e.getMessage());
            verify(pagoRepository, never()).save(any(Pago.class));
        }

        @Test
        @DisplayName("Rechaza una tarifa oficial de cero aunque el panel la envíe")
        void rechazaTarifaNoValida() {
            when(encomiendaRepository.findByIdForUpdate(ID_ENCOMIENDA))
                    .thenReturn(java.util.Optional.of(solicitudPendiente()));
            ConfirmarRecepcionEncomiendaRequest request = recepcionValida();
            request.setTarifaOficial(BigDecimal.ZERO);

            NegocioException e = assertThrows(NegocioException.class,
                    () -> servicio.confirmarRecepcion(ID_ENCOMIENDA, request));

            assertTrue(e.getMessage().toLowerCase().contains("tarifa"),
                    "Fue: " + e.getMessage());
        }
    }

    // ── Registro presencial ───────────────────────────────────────────────────

    @Nested
    @DisplayName("Registro presencial en agencia")
    class RegistroPresencial {

        @Test
        @DisplayName("Nace con referencia SOL y termina oficializado por la misma operación")
        void registroPresencialSeOficializa() {
            escenarioPresencial();

            EncomiendaResponse r = servicio.registrar(registroPresencial("clave-abc"));

            assertTrue(r.getReferenciaSolicitud().matches("SOL-\\d{4}-\\d{6,}"),
                    "El alta presencial también debe tener referencia SOL. Fue: " + r.getReferenciaSolicitud());
            assertEquals(EstadoEncomienda.RECIBIDO, r.getEstado());
            assertTrue(r.getNumeroGuia().matches("GUI-\\d{8}-\\d{6,}"));
            assertEquals(0, new BigDecimal("25.00").compareTo(r.getCosto()));
            verify(pagoRepository, times(1)).save(any(Pago.class));
            verify(movimientoRepository, times(1)).save(any(MovimientoEncomienda.class));
        }

        @Test
        @DisplayName("La misma clave de idempotencia devuelve la encomienda ya creada")
        void idempotenciaPorClave() {
            Encomienda previa = solicitudPendiente();
            previa.setEstado(EstadoEncomienda.RECIBIDO);
            previa.setNumeroGuia("GUI-20260101-000042");
            escenarioPresencial();
            when(encomiendaRepository.findByClaveIdempotencia("clave-abc"))
                    .thenReturn(java.util.Optional.of(previa));

            EncomiendaResponse r = servicio.registrar(registroPresencial("clave-abc"));

            assertEquals("GUI-20260101-000042", r.getNumeroGuia(), "Debe devolver la encomienda ya creada");
            verify(encomiendaRepository, never()).save(any(Encomienda.class));
            verify(pagoRepository, never()).save(any(Pago.class));
            verify(movimientoRepository, never()).save(any(MovimientoEncomienda.class));
        }

        @Test
        @DisplayName("Si dos agentes registran a la vez, el que pierde devuelve la encomienda ganadora")
        void carreraDeIdempotencia() {
            Encomienda ganadora = solicitudPendiente();
            ganadora.setEstado(EstadoEncomienda.RECIBIDO);
            ganadora.setClaveIdempotencia("clave-carrera");
            ganadora.setNumeroGuia("GUI-20260101-000099");
            escenarioPresencial();

            // La consulta previa no encuentra nada (las dos peticiones son
            // simultaneas) y el INSERT del perdedor choca con el indice unico.
            when(encomiendaRepository.findByClaveIdempotencia("clave-carrera"))
                    .thenReturn(java.util.Optional.empty())
                    .thenReturn(java.util.Optional.of(ganadora));
            when(encomiendaRepository.save(any(Encomienda.class)))
                    .thenAnswer(inv -> {
                        Encomienda e = inv.getArgument(0);
                        if (e.getId() == null) e.setId(ID_ENCOMIENDA);
                        throw new org.springframework.dao.DataIntegrityViolationException(
                                "Ya existe la llave (clave_idempotencia)=(clave-carrera).");
                    });

            EncomiendaResponse r = servicio.registrar(registroPresencial("clave-carrera"));

            assertEquals("GUI-20260101-000099", r.getNumeroGuia(),
                    "La carrera debe resolverse con la encomienda ya creada, no con un error 500");
            assertEquals(EstadoEncomienda.RECIBIDO, r.getEstado());
            verify(pagoRepository, never()).save(any(Pago.class));
            verify(movimientoRepository, never()).save(any(MovimientoEncomienda.class));
        }

        @Test
        @DisplayName("La clave se normaliza antes de buscar, para que un reintento con espacios no duplique")
        void normalizaClaveIdempotencia() {
            Encomienda previa = solicitudPendiente();
            previa.setEstado(EstadoEncomienda.RECIBIDO);
            previa.setNumeroGuia("GUI-20260101-000042");
            escenarioPresencial();
            when(encomiendaRepository.findByClaveIdempotencia("clave-abc"))
                    .thenReturn(java.util.Optional.of(previa));

            EncomiendaResponse r = servicio.registrar(registroPresencial("  clave-abc  "));

            verify(encomiendaRepository).findByClaveIdempotencia("clave-abc");
            assertEquals("GUI-20260101-000042", r.getNumeroGuia());
        }

        @Test
        @DisplayName("Sin clave de idempotencia el alta no consulta el indice único")
        void sinClaveNoBusca() {
            escenarioPresencial();

            servicio.registrar(registroPresencial(null));

            verify(encomiendaRepository, never()).findByClaveIdempotencia(anyString());
        }

        @Test
        @DisplayName("Rechaza remitente y destinatario iguales")
        void rechazaMismoRemitenteYDestinatario() {
            EncomiendaRequest request = registroPresencial("clave-abc");
            request.setDestinatarioId(1L);

            NegocioException e = assertThrows(NegocioException.class, () -> servicio.registrar(request));

            assertTrue(e.getMessage().contains("misma persona"), "Fue: " + e.getMessage());
            verify(encomiendaRepository, never()).save(any(Encomienda.class));
        }
    }

    // ── Transiciones de estado ────────────────────────────────────────────────

    @Nested
    @DisplayName("Transiciones de estado")
    class Transiciones {

        @Test
        @DisplayName("Una SOL no se oficializa por el endpoint genérico de cambio de estado")
        void pendienteNoSeCambiaPorEndpointGenerico() {
            when(encomiendaRepository.findById(ID_ENCOMIENDA))
                    .thenReturn(java.util.Optional.of(solicitudPendiente()));

            NegocioException e = assertThrows(NegocioException.class,
                    () -> servicio.actualizarEstado(ID_ENCOMIENDA, ActualizarEstadoEncomiendaRequest.builder()
                            .nuevoEstado(EstadoEncomienda.RECIBIDO)
                            .observacion("Intento por endpoint generico")
                            .build()));

            assertTrue(e.getMessage().toLowerCase().contains("confirmar"),
                    "Debe indicar la via correcta. Fue: " + e.getMessage());
            verify(encomiendaRepository, never()).save(any(Encomienda.class));
            verify(movimientoRepository, never()).save(any(MovimientoEncomienda.class));
        }

        @Test
        @DisplayName("Una encomienda recibida si puede pasar a almacen")
        void recibidoAAlmacen() {
            Encomienda recibida = solicitudPendiente();
            recibida.setEstado(EstadoEncomienda.RECIBIDO);
            recibida.setNumeroGuia("GUI-20260101-000042");
            recibida.setPago(Pago.builder().estado(EstadoPago.COMPLETADO).build());
            when(encomiendaRepository.findById(ID_ENCOMIENDA))
                    .thenReturn(java.util.Optional.of(recibida));

            EncomiendaResponse r = servicio.actualizarEstado(ID_ENCOMIENDA,
                    ActualizarEstadoEncomiendaRequest.builder()
                            .nuevoEstado(EstadoEncomienda.EN_ALMACEN)
                            .observacion("En espera de despacho")
                            .build());

            assertEquals(EstadoEncomienda.EN_ALMACEN, r.getEstado());
            verify(movimientoRepository).save(any(MovimientoEncomienda.class));
        }

        @Test
        @DisplayName("Rechaza saltarse la recepcion oficial e ir directo a entregado")
        void rechazaSaltoDeEstados() {
            Encomienda recibida = solicitudPendiente();
            recibida.setEstado(EstadoEncomienda.RECIBIDO);
            recibida.setNumeroGuia("GUI-20260101-000042");
            when(encomiendaRepository.findById(ID_ENCOMIENDA))
                    .thenReturn(java.util.Optional.of(recibida));

            assertThrows(NegocioException.class, () -> servicio.actualizarEstado(ID_ENCOMIENDA,
                    ActualizarEstadoEncomiendaRequest.builder()
                            .nuevoEstado(EstadoEncomienda.ENTREGADO).build()));
        }
    }

    // ── Panel del personal autorizado ─────────────────────────────────────────

    @Nested
    @DisplayName("Panel de solicitudes pendientes")
    class PanelSolicitudes {

        @Test
        @DisplayName("Enmascara documento y telefono, y no expone el telefono en observaciones")
        void protegeDatosPersonales() {
            when(encomiendaRepository.findByReferenciaSolicitudIgnoreCase("SOL-2026-000007"))
                    .thenReturn(java.util.Optional.of(solicitudPendiente()));

            SolicitudPendienteResponse r = servicio.buscarSolicitudPorReferencia("SOL-2026-000007");

            assertEquals("SOL-2026-000007", r.getReferenciaSolicitud());
            assertEquals("****3456", r.getRemitenteDocumento(), "El documento va enmascarado");
            assertEquals("****7666", r.getDestinatarioTelefono(), "El telefono va enmascarado");
            assertFalse(r.getObservaciones().contains("988777666"),
                    "El telefono no debe aparecer en texto libre. Fue: " + r.getObservaciones());
            assertTrue(r.getObservaciones().contains("Fragil"), "El resto de observaciones se conserva");
            assertEquals("PAQUETE", r.getTipoEnvio());
            assertNull(r.getNumeroGuia(), "Una solicitud pendiente no tiene guia");
            assertNull(r.getCosto(), "Una solicitud pendiente no tiene tarifa");
        }

        @Test
        @DisplayName("Rechaza referencias con formato invalido")
        void rechazaFormatoInvalido() {
            assertThrows(NegocioException.class, () -> servicio.buscarSolicitudPorReferencia("GUI-20260101-000042"));
            assertThrows(NegocioException.class, () -> servicio.buscarSolicitudPorReferencia("SOL-26-1"));
            assertThrows(NegocioException.class, () -> servicio.buscarSolicitudPorReferencia("  "));
            verify(encomiendaRepository, never()).findByReferenciaSolicitudIgnoreCase(anyString());
        }

        @Test
        @DisplayName("Una referencia ya oficializada responde con su guia, no como pendiente")
        void referenciaYaOficializada() {
            Encomienda oficializada = solicitudPendiente();
            oficializada.setEstado(EstadoEncomienda.RECIBIDO);
            oficializada.setNumeroGuia("GUI-20260101-000042");
            oficializada.setPago(Pago.builder().estado(EstadoPago.COMPLETADO).build());
            when(encomiendaRepository.findByReferenciaSolicitudIgnoreCase("SOL-2026-000007"))
                    .thenReturn(java.util.Optional.of(oficializada));

            NegocioException e = assertThrows(NegocioException.class,
                    () -> servicio.buscarSolicitudPorReferencia("SOL-2026-000007"));

            assertTrue(e.getMessage().contains("GUI-20260101-000042"),
                    "Debe indicar la guia oficial. Fue: " + e.getMessage());
        }

        @Test
        @DisplayName("El listado filtra siempre por el estado SOLICITUD_PENDIENTE")
        void listadoFiltraPorEstado() {
            when(encomiendaRepository.buscarSolicitudesPendientes(
                    eq(EstadoEncomienda.SOLICITUD_PENDIENTE), any(), any(), any(), any(), any(), any()))
                    .thenReturn(new PageImpl<>(java.util.List.of(solicitudPendiente())));

            var pagina = servicio.listarSolicitudesPendientes("SOL", null, null, null, null,
                    PageRequest.of(0, 20));

            assertEquals(1, pagina.getTotalElements());
            assertEquals(EstadoEncomienda.SOLICITUD_PENDIENTE, pagina.getContent().get(0).getEstado());
        }
    }

    // ── Tracking público ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("Tracking público")
    class TrackingPublico {

        private Encomienda encomiendaOficializada() {
            Encomienda e = solicitudPendiente();
            e.setEstado(EstadoEncomienda.EN_TRANSITO);
            e.setNumeroGuia("GUI-20260101-000042");
            e.setPesoKg(new BigDecimal("2.500"));
            e.setCosto(new BigDecimal("25.00"));
            e.getMovimientos().add(MovimientoEncomienda.builder()
                    .estadoAnterior(EstadoEncomienda.EN_ALMACEN)
                    .estadoNuevo(EstadoEncomienda.EN_TRANSITO)
                    .observacion("Tarifa S/ 25.00 cobrada por el cajero Juan Perez")
                    .usuarioResponsable(cajero())
                    .sucursalActual(sucursal(1L, "Ayacucho"))
                    .build());
            return e;
        }

        @Test
        @DisplayName("Una referencia SOL no habilita el tracking público")
        void rechazaReferenciaSol() {
            NegocioException e = assertThrows(NegocioException.class,
                    () -> servicio.consultarTracking("SOL-2026-000007"));

            assertTrue(e.getMessage().toLowerCase().contains("solicitud"),
                    "Debe explicar que es una referencia de solicitud. Fue: " + e.getMessage());
            verify(encomiendaRepository, never()).findByNumeroGuia(anyString());
        }

        @Test
        @DisplayName("La referencia SOL se rechaza aunque venga en minusculas")
        void rechazaSolEnMinusculas() {
            assertThrows(NegocioException.class, () -> servicio.consultarTracking("  sol-2026-000007  "));
            verify(encomiendaRepository, never()).findByNumeroGuia(anyString());
        }

        @Test
        @DisplayName("No expone remitente, destinatario, telefono, documento, tarifa ni personal")
        void noExponeDatosSensibles() throws Exception {
            when(encomiendaRepository.findByNumeroGuia("GUI-20260101-000042"))
                    .thenReturn(java.util.Optional.of(encomiendaOficializada()));

            var r = servicio.consultarTracking("GUI-20260101-000042");
            String json = new ObjectMapper().writeValueAsString(r);

            for (String prohibido : new String[]{"destinatario", "remitente", "telefono", "dniRuc",
                    "costo", "tarifa", "usuario", "auditoria", "cajero"}) {
                assertFalse(json.contains(prohibido),
                        "El tracking público no debe incluir '" + prohibido + "'. JSON: " + json);
            }
            assertTrue(r.getMovimientos().get(0).getObservacion().contains("en tránsito"),
                    "El movimiento público debe usar el mensaje genérico. Fue: "
                            + r.getMovimientos().get(0).getObservacion());
        }

        @Test
        @DisplayName("La respuesta publica no tiene ninguna propiedad de persona o de personal")
        void contratoPublicoSinPii() {
            boolean exponePii = java.util.Arrays.stream(TrackingEncomiendaResponse.class.getMethods())
                    .map(java.lang.reflect.Method::getName)
                    .anyMatch(n -> n.toLowerCase().contains("destinatario")
                            || n.toLowerCase().contains("remitente")
                            || n.toLowerCase().contains("telefono")
                            || n.toLowerCase().contains("usuario")
                            || n.toLowerCase().contains("costo"));
            assertFalse(exponePii,
                    "TrackingEncomiendaResponse expone un dato personal o interno; debe reducirse a estado, "
                            + "descripcion, peso, sucursal y movimientos publicos");
        }
    }
}
