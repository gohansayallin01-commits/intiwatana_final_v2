package com.transporte.sistema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.NoOpResponseErrorHandler;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verificacion FUNCIONAL del flujo de encomiendas sobre PostgreSQL real.
 *
 * A diferencia de EncomiendaSolicitudTest (dobles de repositorio), aqui arranca
 * el contexto completo con el perfil "test": Flyway construye el esquema desde
 * V1 a V5 y Hibernate solo VALIDA el modelo, igual que en produccion. Se
 * comprueba el recorrido HTTP real de los dos caminos (solicitud web y registro
 * presencial), la oficializacion unica, la idempotencia y la privacidad del
 * tracking publico.
 *
 * Requiere una base de QA propia. NUNCA debe apuntar a la base de desarrollo:
 * la URL y las credenciales llegan por variables de entorno (DB_URL, DB_USERNAME,
 * DB_PASSWORD, JWT_SECRET) y la prueba aborta si el nombre de la base no acaba
 * en "_qa".
 *
 * Todos los datos que crea se eliminan al finalizar.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Flujo SOL -> recepcion -> tracking (integracion)")
class EncomiendaFlujoIT {

    static {
        // Ver BaseDatosQA: aborta antes de levantar el contexto si la base no es
        // una *_qa, de modo que ninguna prueba de integracion pueda escribir en
        // la base de desarrollo por un fallo de configuracion.
        BaseDatosQA.exigirBaseQA("EncomiendaFlujoIT");
    }

    private static final String MARCA = "QAFLUJO";

    @LocalServerPort private int port;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper mapper;

    private RestTemplate rest;
    private String tokenAdmin;
    private String tokenCliente;
    private Long   clienteId;
    private final List<Long> clientesCreados = new ArrayList<>();
    private Long   idSolicitud;
    private String refSolicitud;
    private String guiaGenerada;
    private Long   idPresencial;
    private String guiaPresencial;
    private Integer totalEncomiendasAntes;

    // ── Utilidades ───────────────────────────────────────────────────────────

    @BeforeEach
    void clienteHttp() {
        // JdkClientHttpRequestFactory admite PATCH, que el cliente por defecto
        // basado en HttpURLConnection no soporta. NoOpResponseErrorHandler evita
        // que un 4xx esperado se lance como excepcion: aqui el codigo de
        // respuesta ES lo que se esta comprobando.
        rest = new RestTemplate(new JdkClientHttpRequestFactory());
        rest.setErrorHandler(new NoOpResponseErrorHandler());
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private void exigirBaseAislada() {
        String url = jdbc.execute((org.springframework.jdbc.core.ConnectionCallback<String>) c ->
                c.getMetaData().getURL());
        String db = jdbc.queryForObject("SELECT current_database()", String.class);
        assertNotNull(url);
        System.out.println("[QA] base de la integracion: " + db);
        assertTrue(db.endsWith("_qa"),
                "La integracion SOLO puede correr sobre una base aislada *_qa. Se abutio: " + db);
    }

    private HttpHeaders jsonConToken(String token) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setBearerAuth(token);
        return h;
    }

    private HttpHeaders jsonSinToken() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    private ResponseEntity<JsonNode> post(String path, String token, Object body) throws Exception {
        return rest.exchange(url(path), HttpMethod.POST,
                new HttpEntity<>(mapper.writeValueAsString(body),
                        token == null ? jsonSinToken() : jsonConToken(token)),
                JsonNode.class);
    }

    private ResponseEntity<JsonNode> method(HttpMethod m, String path, String token, Object body)
            throws Exception {
        HttpEntity<String> entity = new HttpEntity<>(
                body == null ? null : mapper.writeValueAsString(body),
                token == null ? jsonSinToken() : jsonConToken(token));
        return rest.exchange(url(path), m, entity, JsonNode.class);
    }

    private static String texto(JsonNode n, String campo) {
        return n == null || n.get(campo) == null || n.get(campo).isNull() ? null : n.get(campo).asText();
    }

    private void check(String descripcion, boolean condicion) {
        System.out.println((condicion ? "  [OK]    " : "  [FALLA] ") + descripcion);
        assertTrue(condicion, descripcion);
    }

    // ── Escenario ────────────────────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("Prepara la base: login de admin, cliente de QA y limpieza previa")
    void preparacion() throws Exception {
        exigirBaseAislada();
        limpiar();

        ResponseEntity<JsonNode> login = post("/api/v1/auth/login", null,
                Map.of("username", "admin", "password", "admin123"));
        assertEquals(HttpStatus.OK, login.getStatusCode(),
                "El admin de QA debe poder iniciar sesion. Cuerpo: " + login.getBody());
        tokenAdmin = login.getBody().get("token").asText();
        assertFalse(tokenAdmin.isBlank());

        // Cliente de QA con cuenta propia para poder registrar la solicitud como
        // el publico lo haria desde el portal.
        ResponseEntity<JsonNode> registro = post("/api/v1/clientes/registro", null, Map.of(
                "nombres", "QA " + MARCA,
                "apellidos", "Solicitud",
                "dniRuc", dniUnico(),
                "telefono", "900000001",
                "email", "qa." + MARCA.toLowerCase() + "@intiwatana.test",
                "username", "qa" + MARCA.toLowerCase() + Math.abs(UUID.randomUUID().hashCode() % 100000),
                "password", "qa123456"));
        assertEquals(HttpStatus.CREATED, registro.getStatusCode(),
                "El registro publico de cliente debe funcionar. Cuerpo: " + registro.getBody());
        tokenCliente = registro.getBody().get("token").asText();
        clienteId = registro.getBody().get("clienteId").asLong();
        clientesCreados.add(clienteId);
        check("cliente de QA creado con cuenta CLIENTE (id=" + clienteId + ")", clienteId != null);

        totalEncomiendasAntes = jdbc.queryForObject("SELECT count(*) FROM encomiendas", Integer.class);
        System.out.println("[QA] encomiendas antes del escenario: " + totalEncomiendasAntes);
    }

    @Test
    @Order(2)
    @DisplayName("La solicitud web nace con referencia SOL y sin ningun dato oficial")
    void solicitudWebSinDatosOficiales() throws Exception {
        ResponseEntity<JsonNode> r = post("/api/v1/encomiendas/solicitar", tokenCliente, Map.of(
                "sucursalOrigenId", 1,
                "sucursalDestinoId", 2,
                "remitenteNombre", "QA Solicitud",
                "remitenteTelefono", "900000001",
                "destinatarioNombre", "Destinatario QA",
                "destinatarioTelefono", "900000002",
                "descripcionContenido", "Caja QA " + MARCA,
                "tipoEnvio", "PAQUETE",
                "cuidadoEspecial", "Fragil"));

        assertEquals(HttpStatus.CREATED, r.getStatusCode(), "Cuerpo: " + r.getBody());
        JsonNode b = r.getBody();
        idSolicitud = b.get("id").asLong();
        refSolicitud = texto(b, "referenciaSolicitud");

        check("referencia SOL con formato SOL-AAAA-NNNNNN: " + refSolicitud,
                refSolicitud != null && refSolicitud.matches("SOL-\\d{4}-\\d{6,}"));
        check("estado SOLICITUD_PENDIENTE", "SOLICITUD_PENDIENTE".equals(texto(b, "estado")));
        check("sin numeroGuia", texto(b, "numeroGuia") == null);
        check("sin pesoKg", b.get("pesoKg") == null || b.get("pesoKg").isNull());
        check("sin medidas", (b.get("largoCm") == null || b.get("largoCm").isNull())
                && (b.get("anchoCm") == null || b.get("anchoCm").isNull())
                && (b.get("altoCm") == null || b.get("altoCm").isNull()));
        check("sin volumenM3", b.get("volumenM3") == null || b.get("volumenM3").isNull());
        check("sin costo", b.get("costo") == null || b.get("costo").isNull());
        check("sin metodoPago", b.get("metodoPago") == null || b.get("metodoPago").isNull());

        Map<String, Object> fila = jdbc.queryForMap(
                "SELECT referencia_solicitud, numero_guia, peso_kg, volumen_m3, costo, metodo_pago, pago_id "
                        + "FROM encomiendas WHERE id = ?", idSolicitud);
        check("la fila en BD tiene referencia y ningun dato oficial: " + fila,
                fila.get("referencia_solicitud") != null && fila.get("numero_guia") == null
                        && fila.get("peso_kg") == null && fila.get("volumen_m3") == null
                        && fila.get("costo") == null && fila.get("metodo_pago") == null
                        && fila.get("pago_id") == null);
    }

    @Test
    @Order(3)
    @DisplayName("El publico no puede oficializar la solicitud ni alterar el estado")
    void publicoNoPuedeOficializar() throws Exception {
        ResponseEntity<JsonNode> confirmar = post(
                "/api/v1/encomiendas/" + idSolicitud + "/confirmar-recepcion", tokenCliente,
                Map.of("pesoKg", 3.0, "largoCm", 30, "anchoCm", 20, "altoCm", 10,
                        "tarifaOficial", 25, "metodoPago", "EFECTIVO"));
        check("confirmar-recepcion con token CLIENTE -> " + confirmar.getStatusCodeValue()
                + " (403 esperado)", confirmar.getStatusCode() == HttpStatus.FORBIDDEN);

        ResponseEntity<JsonNode> estado = method(HttpMethod.PATCH,
                "/api/v1/encomiendas/" + idSolicitud + "/estado", tokenCliente,
                Map.of("nuevoEstado", "RECIBIDO"));
        check("PATCH estado con token CLIENTE -> " + estado.getStatusCodeValue() + " (403 esperado)",
                estado.getStatusCode() == HttpStatus.FORBIDDEN);
    }

    @Test
    @Order(4)
    @DisplayName("El tracking publico rechaza la referencia SOL sin consultar la guia")
    void trackingRechazaSol() {
        ResponseEntity<JsonNode> r = rest.getForEntity(
                url("/api/v1/encomiendas/tracking/" + refSolicitud), JsonNode.class);
        check("tracking de una referencia SOL -> " + r.getStatusCodeValue()
                        + " (no debe devolver 200 con datos de envio)",
                r.getStatusCode() != HttpStatus.OK);
    }

    @Test
    @Order(5)
    @DisplayName("El panel recupera la solicitud por su referencia SOL con datos enmascarados")
    void panelRecuperaSolicitud() throws Exception {
        ResponseEntity<JsonNode> r = method(HttpMethod.GET,
                "/api/v1/encomiendas/solicitudes-pendientes/" + refSolicitud, tokenAdmin, null);
        assertEquals(HttpStatus.OK, r.getStatusCode(), "Cuerpo: " + r.getBody());
        JsonNode b = r.getBody();
        check("la referencia se recupera tal cual", refSolicitud.equals(texto(b, "referenciaSolicitud")));
        check("documento del remitente enmascarado: " + texto(b, "remitenteDocumento"),
                texto(b, "remitenteDocumento") != null && texto(b, "remitenteDocumento").startsWith("****"));
        check("telefono del destinatario enmascarado: " + texto(b, "destinatarioTelefono"),
                texto(b, "destinatarioTelefono") != null && texto(b, "destinatarioTelefono").startsWith("****"));
        check("el telefono no aparece en observaciones",
                texto(b, "observaciones") == null || !texto(b, "observaciones").contains("900000002"));
        check("sin numeroGuia ni costo en la vista de pendiente",
                texto(b, "numeroGuia") == null && texto(b, "costo") == null);

        ResponseEntity<JsonNode> invalida = method(HttpMethod.GET,
                "/api/v1/encomiendas/solicitudes-pendientes/GUI-20260101-000001", tokenAdmin, null);
        check("una guia no sirve como referencia de solicitud -> "
                + invalida.getStatusCodeValue() + " (400 esperado)",
                invalida.getStatusCode() == HttpStatus.BAD_REQUEST);
    }

    @Test
    @Order(6)
    @DisplayName("El endpoint generico de estado no puede oficializar la solicitud")
    void endpointGenericoNoOficializa() throws Exception {
        ResponseEntity<JsonNode> r = method(HttpMethod.PATCH,
                "/api/v1/encomiendas/" + idSolicitud + "/estado", tokenAdmin,
                Map.of("nuevoEstado", "RECIBIDO", "observacion", "intento por endpoint generico"));
        check("PATCH estado sobre una SOL -> " + r.getStatusCodeValue() + " (400 esperado)",
                r.getStatusCode() == HttpStatus.BAD_REQUEST);
        check("la solicitud sigue pendiente sin guia oficial",
                "SOLICITUD_PENDIENTE".equals(jdbc.queryForObject(
                        "SELECT estado FROM encomiendas WHERE id = ?", String.class, idSolicitud))
                        && jdbc.queryForObject(
                        "SELECT numero_guia FROM encomiendas WHERE id = ?", String.class, idSolicitud) == null);
    }

    @Test
    @Order(7)
    @DisplayName("Confirmar la recepcion oficializa: guia GUI, un pago, un movimiento y volumen calculado")
    void confirmarRecepcion() throws Exception {
        ResponseEntity<JsonNode> r = post(
                "/api/v1/encomiendas/" + idSolicitud + "/confirmar-recepcion", tokenAdmin,
                Map.of("pesoKg", 3.2, "largoCm", 30, "anchoCm", 20, "altoCm", 10,
                        "valorDeclarado", 150, "tarifaOficial", 25, "metodoPago", "EFECTIVO",
                        "observaciones", "Recibido en ventanilla QA"));

        assertEquals(HttpStatus.OK, r.getStatusCode(), "Cuerpo: " + r.getBody());
        JsonNode b = r.getBody();
        guiaGenerada = texto(b, "numeroGuia");

        check("guia oficial con formato GUI-AAAAMMDD-NNNNNN: " + guiaGenerada,
                guiaGenerada != null && guiaGenerada.matches("GUI-\\d{8}-\\d{6,}"));
        check("estado RECIBIDO", "RECIBIDO".equals(texto(b, "estado")));
        check("volumen calculado por el backend: " + texto(b, "volumenM3"),
                b.get("volumenM3") != null && b.get("volumenM3").asDouble() > 0);
        check("peso real 3.2 kg registrado", b.get("pesoKg") != null
                && Math.abs(b.get("pesoKg").asDouble() - 3.2) < 0.001);
        check("tarifa oficial 25.00 registrada", b.get("costo") != null
                && Math.abs(b.get("costo").asDouble() - 25.0) < 0.001);
        check("la referencia SOL se conserva", refSolicitud.equals(texto(b, "referenciaSolicitud")));

        Integer pagos = jdbc.queryForObject(
                "SELECT count(*) FROM pagos WHERE id = (SELECT pago_id FROM encomiendas WHERE id = ?)",
                Integer.class, idSolicitud);
        Integer movimientos = jdbc.queryForObject(
                "SELECT count(*) FROM movimientos_encomienda WHERE encomienda_id = ?", Integer.class, idSolicitud);
        check("exactamente 1 pago oficial (total de la base: " + pagos + ")", pagos == 1);
        check("exactamente 1 movimiento inicial (total: " + movimientos + ")", movimientos == 1);
        check("el pago quedó COMPLETADO",
                "COMPLETADO".equals(jdbc.queryForObject(
                        "SELECT p.estado::text FROM pagos p JOIN encomiendas e ON e.pago_id = p.id "
                                + "WHERE e.id = ?", String.class, idSolicitud)));
    }

    @Test
    @Order(8)
    @DisplayName("Confirmar dos veces devuelve la misma guia y no duplica pago ni movimientos")
    void confirmacionIdempotente() throws Exception {
        ResponseEntity<JsonNode> r = post(
                "/api/v1/encomiendas/" + idSolicitud + "/confirmar-recepcion", tokenAdmin,
                Map.of("pesoKg", 3.2, "largoCm", 30, "anchoCm", 20, "altoCm", 10,
                        "tarifaOficial", 25, "metodoPago", "EFECTIVO"));
        assertEquals(HttpStatus.OK, r.getStatusCode(), "Cuerpo: " + r.getBody());
        check("devuelve la MISMA guia: " + texto(r.getBody(), "numeroGuia"),
                guiaGenerada.equals(texto(r.getBody(), "numeroGuia")));
        check("sigue habiendo 1 pago y 1 movimiento",
                jdbc.queryForObject("SELECT count(*) FROM pagos WHERE id = "
                        + "(SELECT pago_id FROM encomiendas WHERE id = ?)", Integer.class, idSolicitud) == 1
                        && jdbc.queryForObject("SELECT count(*) FROM movimientos_encomienda "
                        + "WHERE encomienda_id = ?", Integer.class, idSolicitud) == 1);
    }

    @Test
    @Order(9)
    @DisplayName("La referencia SOL ya oficializada responde con su guia, no como pendiente")
    void referenciaYaOficializada() throws Exception {
        ResponseEntity<JsonNode> r = method(HttpMethod.GET,
                "/api/v1/encomiendas/solicitudes-pendientes/" + refSolicitud, tokenAdmin, null);
        check("-> " + r.getStatusCodeValue() + " (400 esperado)", r.getStatusCode() == HttpStatus.BAD_REQUEST);
        String mensaje = texto(r.getBody(), "mensaje");
        check("el mensaje incluye la guia oficial: " + mensaje,
                mensaje != null && mensaje.contains(guiaGenerada));
    }

    @Test
    @Order(10)
    @DisplayName("El tracking publico de la guia no expone datos personales ni detalles internos")
    void trackingPublicoSinPii() throws Exception {
        ResponseEntity<JsonNode> r = rest.getForEntity(
                url("/api/v1/encomiendas/tracking/" + guiaGenerada), JsonNode.class);
        assertEquals(HttpStatus.OK, r.getStatusCode(), "Cuerpo: " + r.getBody());
        String json = r.getBody().toString();

        for (String prohibido : new String[]{"destinatario", "remitente", "telefono", "dniRuc",
                "costo", "tarifa", "usuario", "cajero", "auditoria"}) {
            check("el tracking no contiene '" + prohibido + "'", !json.contains(prohibido));
        }
        check("expone el estado publico del envio", texto(r.getBody(), "estado") != null);
        check("expone la sucursal de origen", r.getBody().get("sucursalOrigen") != null);
        check("no filtra el nombre del remitente", !json.contains("QA Solicitud"));
    }

    @Test
    @Order(11)
    @DisplayName("El registro presencial oficializa en el mismo paso y respeta la clave de idempotencia")
    void registroPresencialIdempotente() throws Exception {
        ResponseEntity<JsonNode> alta = post("/api/v1/clientes", tokenAdmin, Map.of(
                "dniRuc", dniUnico(), "nombres", "QA", "apellidos", "Presencial",
                "telefono", "900000003", "tipoCliente", "PERSONA"));
        assertEquals(HttpStatus.CREATED, alta.getStatusCode(), "Cuerpo: " + alta.getBody());
        Long remitente = alta.getBody().get("id").asLong();
        clientesCreados.add(remitente);

        ResponseEntity<JsonNode> dest = post("/api/v1/clientes", tokenAdmin, Map.of(
                "dniRuc", dniUnico(), "nombres", "QA", "apellidos", "Destinatario",
                "telefono", "900000004", "tipoCliente", "PERSONA"));
        assertEquals(HttpStatus.CREATED, dest.getStatusCode(), "Cuerpo: " + dest.getBody());
        Long destinatario = dest.getBody().get("id").asLong();
        clientesCreados.add(destinatario);

        String clave = "qa-" + UUID.randomUUID();
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("remitenteId", remitente);
        cuerpo.put("destinatarioId", destinatario);
        cuerpo.put("sucursalOrigenId", 1);
        cuerpo.put("sucursalDestinoId", 3);
        cuerpo.put("descripcionContenido", "Presencial QA " + MARCA);
        cuerpo.put("pesoKg", 1.5);
        cuerpo.put("largoCm", 20);
        cuerpo.put("anchoCm", 15);
        cuerpo.put("altoCm", 10);
        cuerpo.put("tarifaOficial", 18);
        cuerpo.put("metodoPago", "YAPE");
        cuerpo.put("claveIdempotencia", clave);

        ResponseEntity<JsonNode> r1 = post("/api/v1/encomiendas", tokenAdmin, cuerpo);
        assertEquals(HttpStatus.CREATED, r1.getStatusCode(), "Cuerpo: " + r1.getBody());
        idPresencial = r1.getBody().get("id").asLong();
        guiaPresencial = texto(r1.getBody(), "numeroGuia");

        check("la alta presencial nace con referencia SOL: " + texto(r1.getBody(), "referenciaSolicitud"),
                texto(r1.getBody(), "referenciaSolicitud") != null
                        && texto(r1.getBody(), "referenciaSolicitud").matches("SOL-\\d{4}-\\d{6,}"));
        check("la alta presencial queda oficializada con guia " + guiaPresencial,
                guiaPresencial != null && guiaPresencial.matches("GUI-\\d{8}-\\d{6,}"));
        check("estado RECIBIDO y tarifa 18.00",
                "RECIBIDO".equals(texto(r1.getBody(), "estado"))
                        && Math.abs(r1.getBody().get("costo").asDouble() - 18.0) < 0.001);

        // Reintento con la MISMA clave: debe devolver la encomienda ya creada.
        ResponseEntity<JsonNode> r2 = post("/api/v1/encomiendas", tokenAdmin, cuerpo);
        check("el reintento con la misma clave devuelve la misma encomienda",
                r1.getBody().get("id").asLong() == r2.getBody().get("id").asLong());
        check("y la misma guia " + guiaPresencial,
                guiaPresencial.equals(texto(r2.getBody(), "numeroGuia")));
        check("no se creo una segunda encomienda con esa clave",
                jdbc.queryForObject("SELECT count(*) FROM encomiendas WHERE clave_idempotencia = ?",
                        Integer.class, clave) == 1);

        // Una clave distinta SI crea otra encomienda: la idempotencia es por clave,
        // no un bloqueo global del alta presencial.
        String claveParalela = "qa-paralela-" + UUID.randomUUID();
        Map<String, Object> cuerpo2 = new LinkedHashMap<>(cuerpo);
        cuerpo2.put("claveIdempotencia", claveParalela);
        cuerpo2.put("descripcionContenido", "Presencial QA paralela " + MARCA);
        ResponseEntity<JsonNode> r3 = post("/api/v1/encomiendas", tokenAdmin, cuerpo2);
        check("una clave distinta crea una encomienda distinta (id "
                        + (r3.getBody() != null ? r3.getBody().get("id").asText() : "?") + ")",
                r3.getStatusCode() == HttpStatus.CREATED
                        && r3.getBody().get("id").asLong() != idPresencial);
        check("las dos encomiendas tienen guias distintas",
                guiaPresencial != null && !guiaPresencial.equals(texto(r3.getBody(), "numeroGuia")));
    }

    @Test
    @Order(12)
    @DisplayName("Dos peticiones simultaneas con la misma clave no duplican la encomienda")
    void concurrenciaMismaClave() throws Exception {
        ResponseEntity<JsonNode> rem = post("/api/v1/clientes", tokenAdmin, Map.of(
                "dniRuc", dniUnico(), "nombres", "QA", "apellidos", "Carrera",
                "telefono", "900000005", "tipoCliente", "PERSONA"));
        assertEquals(HttpStatus.CREATED, rem.getStatusCode(), "Cuerpo: " + rem.getBody());
        Long remitente = rem.getBody().get("id").asLong();
        clientesCreados.add(remitente);

        ResponseEntity<JsonNode> des = post("/api/v1/clientes", tokenAdmin, Map.of(
                "dniRuc", dniUnico(), "nombres", "QA", "apellidos", "Carrera Destino",
                "telefono", "900000006", "tipoCliente", "PERSONA"));
        assertEquals(HttpStatus.CREATED, des.getStatusCode(), "Cuerpo: " + des.getBody());
        Long destinatario = des.getBody().get("id").asLong();
        clientesCreados.add(destinatario);

        final String claveCarrera = "qa-carrera-" + UUID.randomUUID();
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("remitenteId", remitente);
        cuerpo.put("destinatarioId", destinatario);
        cuerpo.put("sucursalOrigenId", 1);
        cuerpo.put("sucursalDestinoId", 3);
        cuerpo.put("descripcionContenido", "Carrera QA " + MARCA);
        cuerpo.put("pesoKg", 1.0);
        cuerpo.put("largoCm", 10);
        cuerpo.put("anchoCm", 10);
        cuerpo.put("altoCm", 10);
        cuerpo.put("tarifaOficial", 10);
        cuerpo.put("metodoPago", "EFECTIVO");
        cuerpo.put("claveIdempotencia", claveCarrera);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch listos = new CountDownLatch(2);
        CountDownLatch disparo = new CountDownLatch(1);
        Callable<ResponseEntity<JsonNode>> tarea = () -> {
            listos.countDown();
            disparo.await(30, TimeUnit.SECONDS);
            return post("/api/v1/encomiendas", tokenAdmin, cuerpo);
        };
        try {
            Future<ResponseEntity<JsonNode>> f1 = pool.submit(tarea);
            Future<ResponseEntity<JsonNode>> f2 = pool.submit(tarea);
            assertTrue(listos.await(20, TimeUnit.SECONDS), "Los dos hilos deben quedar listos");
            disparo.countDown();
            ResponseEntity<JsonNode> a = f1.get(60, TimeUnit.SECONDS);
            ResponseEntity<JsonNode> b = f2.get(60, TimeUnit.SECONDS);

            check("las dos peticiones saline con exito o con conflicto controlado ("
                            + a.getStatusCodeValue() + " y " + b.getStatusCodeValue() + ")",
                    !a.getStatusCode().is5xxServerError() && !b.getStatusCode().is5xxServerError());

            Integer filas = jdbc.queryForObject(
                    "SELECT count(*) FROM encomiendas WHERE clave_idempotencia = ?", Integer.class, claveCarrera);
            check("la carrera con la misma clave deja UNA sola encomienda (filas=" + filas + ")",
                    filas != null && filas == 1);

            Integer guias = jdbc.queryForObject(
                    "SELECT count(DISTINCT numero_guia) FROM encomiendas WHERE clave_idempotencia = ?",
                    Integer.class, claveCarrera);
            check("y una sola guia oficial", guias != null && guias == 1);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    @Order(13)
    @DisplayName("Dos confirmaciones simultaneas de la misma solicitud no duplican pago ni guia")
    void concurrenciaConfirmacion() throws Exception {
        // Se crea una solicitud nueva para esta carrera.
        ResponseEntity<JsonNode> sol = post("/api/v1/encomiendas/solicitar", tokenCliente, Map.of(
                "sucursalOrigenId", 1, "sucursalDestinoId", 3,
                "remitenteNombre", "QA Solicitud",
                "remitenteTelefono", "900000001",
                "destinatarioNombre", "Destinatario Carrera " + MARCA,
                "destinatarioTelefono", "900000009",
                "descripcionContenido", "Solicitud carrera " + MARCA,
                "tipoEnvio", "PAQUETE", "cuidadoEspecial", "FRAGIL"));
        assertEquals(HttpStatus.CREATED, sol.getStatusCode(), "Cuerpo: " + sol.getBody());
        long idCarrera = sol.getBody().get("id").asLong();

        Map<String, Object> cuerpo = Map.of(
                "pesoKg", 2.0, "largoCm", 25, "anchoCm", 15, "altoCm", 12,
                "valorDeclarado", 90, "tarifaOficial", 20, "metodoPago", "YAPE",
                "observaciones", "Carrera de confirmacion QA");

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch listos = new CountDownLatch(2);
        CountDownLatch disparo = new CountDownLatch(1);
        Callable<ResponseEntity<JsonNode>> tarea = () -> {
            listos.countDown();
            disparo.await(30, TimeUnit.SECONDS);
            return post("/api/v1/encomiendas/" + idCarrera + "/confirmar-recepcion", tokenAdmin, cuerpo);
        };
        try {
            Future<ResponseEntity<JsonNode>> f1 = pool.submit(tarea);
            Future<ResponseEntity<JsonNode>> f2 = pool.submit(tarea);
            assertTrue(listos.await(20, TimeUnit.SECONDS), "Los dos hilos deben quedar listos");
            disparo.countDown();
            ResponseEntity<JsonNode> a = f1.get(60, TimeUnit.SECONDS);
            ResponseEntity<JsonNode> b = f2.get(60, TimeUnit.SECONDS);

            check("ninguna confirmacion simultanea devolvio 5xx ("
                            + a.getStatusCodeValue() + " y " + b.getStatusCodeValue() + ")",
                    !a.getStatusCode().is5xxServerError() && !b.getStatusCode().is5xxServerError());

            Integer pagos = jdbc.queryForObject(
                    "SELECT count(*) FROM pagos WHERE id = (SELECT pago_id FROM encomiendas WHERE id = ?)",
                    Integer.class, idCarrera);
            Integer movimientos = jdbc.queryForObject(
                    "SELECT count(*) FROM movimientos_encomienda WHERE encomienda_id = ?", Integer.class, idCarrera);
            check("la carrera de confirmacion deja UN solo pago (pagos=" + pagos + ")", pagos == 1);
            check("y UN solo movimiento inicial (movimientos=" + movimientos + ")", movimientos == 1);

            Integer guias = jdbc.queryForObject(
                    "SELECT count(DISTINCT numero_guia) FROM encomiendas WHERE id = ?", Integer.class, idCarrera);
            check("y una sola guia oficial para la solicitud", guias != null && guias == 1);

            String guiaA = texto(a.getBody(), "numeroGuia");
            String guiaB = texto(b.getBody(), "numeroGuia");
            check("las dos respuestas apuntan a la misma guia (" + guiaA + " / " + guiaB + ")",
                    guiaA != null && guiaA.equals(guiaB));
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    @Order(14)
    @DisplayName("Limpia todos los datos creados y comprueba que la base quedo como estaba")
    void limpiezaYAislamiento() {
        limpiar();
        Integer restantes = jdbc.queryForObject("SELECT count(*) FROM encomiendas", Integer.class);
        check("no quedan encomiendas de la QA (antes hubo " + totalEncomiendasAntes + ")",
                totalEncomiendasAntes == null || restantes.equals(totalEncomiendasAntes));
    }

    @AfterAll
    void limpiezaFinal() {
        try {
            limpiar();
        } catch (Exception e) {
            System.out.println("[QA] aviso al limpiar: " + e.getMessage());
        }
    }

    /**
     * Borra lo creado por la prueba respetando el orden de las claves foraneas:
     * los movimientos apuntan a la encomienda y la encomienda al pago, asi que
     * primero se borran los movimientos, luego se suelta el pago de la encomienda,
     * luego la encomienda y por ultimo el pago.
     */
    private void limpiar() {
        String patron = "%" + MARCA + "%";
        List<Long> encomiendas = jdbc.queryForList(
                "SELECT id FROM encomiendas WHERE descripcion_contenido LIKE ?", Long.class, patron);
        if (!encomiendas.isEmpty()) {
            List<Long> pagos = jdbc.queryForList(
                    "SELECT pago_id FROM encomiendas WHERE descripcion_contenido LIKE ? AND pago_id IS NOT NULL",
                    Long.class, patron);
            jdbc.update("DELETE FROM movimientos_encomienda WHERE encomienda_id IN "
                    + "(SELECT id FROM encomiendas WHERE descripcion_contenido LIKE ?)", patron);
            jdbc.update("UPDATE encomiendas SET pago_id = NULL WHERE descripcion_contenido LIKE ?", patron);
            jdbc.update("DELETE FROM encomiendas WHERE descripcion_contenido LIKE ?", patron);
            if (!pagos.isEmpty()) {
                jdbc.update("DELETE FROM pagos WHERE id IN (" + placeholders(pagos.size()) + ")",
                        pagos.toArray());
            }
        }
        for (Long id : clientesCreados) {
            jdbc.update("DELETE FROM clientes WHERE id = ?", id);
        }
        jdbc.update("DELETE FROM usuarios WHERE username LIKE ?", "qa" + MARCA.toLowerCase() + "%");
        clientesCreados.clear();
    }

    private static String placeholders(int n) {
        return String.join(",", java.util.Collections.nCopies(n, "?"));
    }

    private String dniUnico() {
        return String.valueOf(90_000_000L + Math.abs(UUID.randomUUID().hashCode() % 9_999_999));
    }
}
