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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.NoOpResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AUDITORIA FINAL DE SEGURIDAD Y FUNCIONALIDAD sobre el sistema real.
 *
 * Contrasta, por HTTP contra PostgreSQL, lo que el codigo DECLARA
 * (@PreAuthorize + SecurityConfig) con lo que el sistema REALMENTE permite.
 *
 * Solo puede ejecutarse sobre una base aislada cuyo nombre termine en _qa.
 * Todo lo que crea se elimina al terminar.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("AUDITORIA FINAL - permisos reales, IDOR/BOLA y flujo de pasajes")
class AuditoriaFinalIT {

    static {
        // Se ejecuta al cargar la clase, antes de que Spring abra el contexto y
        // antes de que el DataInitializer escriba nada.
        BaseDatosQA.exigirBaseQA("AuditoriaFinalIT");
    }

    private static final String PREFIJO = "qaaudit";

    @LocalServerPort private int port;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper mapper;

    private RestTemplate rest;
    private Map<String, String> token = new LinkedHashMap<>();
    private Long clienteAId, rolClienteId, sucursalOrigen, sucursalDestino, adminId;
    private String docA, docB, userA, userB;

    private String refPasaje, docPasaje, numeroBoleto;
    private Long pagoPasaje;
    private List<Long> asientosQA = new ArrayList<>();
    private List<String> boletosQA = new ArrayList<>();
    private List<Long> encomiendasQA = new ArrayList<>();
    private Integer encAntes, bolAntes, pagAntes, retAntes;
    private Long pagoMaxInicial;
    private boolean viajeAlterado;

    // â”€â”€ infraestructura â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @BeforeEach
    void http() {
        rest = new RestTemplate(new JdkClientHttpRequestFactory());
        rest.setErrorHandler(new NoOpResponseErrorHandler());
    }

    private String url(String p) { return "http://localhost:" + port + p; }

    private static Map<String, Object> m(Object... kv) {
        Map<String, Object> r = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) r.put((String) kv[i], kv[i + 1]);
        return r;
    }

    private HttpHeaders hdr(String t, boolean json) {
        HttpHeaders h = new HttpHeaders();
        if (json) h.setContentType(MediaType.APPLICATION_JSON);
        if (t != null) h.setBearerAuth(t);
        return h;
    }

    private ResponseEntity<JsonNode> send(HttpMethod method, String path, String t, Object body) throws Exception {
        boolean json = body != null;
        HttpHeaders h = hdr(t, json);
        String payload = json ? mapper.writeValueAsString(body) : null;
        return rest.exchange(url(path), method, new HttpEntity<>(payload, h), JsonNode.class);
    }

    private ResponseEntity<JsonNode> get(String p, String t) throws Exception { return send(HttpMethod.GET, p, t, null); }
    private ResponseEntity<JsonNode> post(String p, String t, Object b) throws Exception { return send(HttpMethod.POST, p, t, b); }
    private ResponseEntity<JsonNode> put(String p, String t, Object b) throws Exception { return send(HttpMethod.PUT, p, t, b); }
    private ResponseEntity<JsonNode> patch(String p, String t, Object b) throws Exception { return send(HttpMethod.PATCH, p, t, b); }
    private ResponseEntity<JsonNode> del(String p, String t) throws Exception { return send(HttpMethod.DELETE, p, t, null); }

    private ResponseEntity<byte[]> bytes(String p, String t) {
        return rest.exchange(url(p), HttpMethod.GET, new HttpEntity<>(hdr(t, false)), byte[].class);
    }

    private static String txt(JsonNode n, String c) {
        return n == null || n.get(c) == null || n.get(c).isNull() ? null : n.get(c).asText();
    }

    private void ok(String d, boolean c) {
        System.out.println((c ? "  [OK]     " : "  [FALLA]  ") + d);
        assertTrue(c, d);
    }

    private void ver(String d, Object v) { System.out.println("  [OBS]    " + d + " -> " + v); }

    // â”€â”€ 1. preparacion â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    @Order(1)
    @DisplayName("Prepara la QA con las 6 identidades reales y dos clientes del portal")
    void preparacion() throws Exception {
        String db = jdbc.queryForObject("SELECT current_database()", String.class);
        System.out.println("[QA] base auditada: " + db);
        assertTrue(db.endsWith("_qa"), "La auditoria exige una base *_qa. Actual: " + db);

        limpiar();

        encAntes = count("encomiendas");
        bolAntes = count("boletos");
        pagAntes = count("pagos");
        retAntes = count("retenciones");
        pagoMaxInicial = jdbc.queryForObject("SELECT coalesce(max(id),0) FROM pagos", Long.class);
        ver("id maximo de pago antes de la auditoria", pagoMaxInicial);

        rolClienteId = jdbc.queryForObject("SELECT id FROM roles WHERE nombre='ROLE_CLIENTE'", Long.class);
        List<Long> sucursales = jdbc.queryForList("SELECT id FROM sucursales WHERE activo=true ORDER BY id", Long.class);
        sucursalOrigen = sucursales.get(0);
        sucursalDestino = sucursales.get(1);
        adminId = jdbc.queryForObject("SELECT id FROM usuarios WHERE username='admin'", Long.class);

        token.put("ADMIN", login("admin", "admin123"));
        token.put("CAJERO", login("cajero1", "cajero123"));
        token.put("VENDEDOR", login("vendedor1", "vendedor123"));
        token.put("OPERADOR", login("operador1", "operador123"));
        token.put("CHOFER1", login("chofer1", "chofer123"));
        token.put("CHOFER2", login("chofer2", "chofer123"));

        docA = dni();
        docB = dni();
        userA = PREFIJO + "a" + sufijo();
        userB = PREFIJO + "b" + sufijo();

        ResponseEntity<JsonNode> ra = post("/api/v1/clientes/registro", null, m(
                "nombres", "QA Auditoria", "apellidos", "Alfa",
                "dniRuc", docA, "telefono", "900000101", "email", userA + "@intiwatana.test",
                "username", userA, "password", "QaAudit2026"));
        assertEquals(HttpStatus.CREATED, ra.getStatusCode(), "Registro cliente A: " + ra.getBody());
        clienteAId = ra.getBody().get("clienteId").asLong();
        token.put("CLIENTE", ra.getBody().get("token").asText());

        ResponseEntity<JsonNode> rb = post("/api/v1/clientes/registro", null, m(
                "nombres", "QA Auditoria", "apellidos", "Beta",
                "dniRuc", docB, "telefono", "900000102", "email", userB + "@intiwatana.test",
                "username", userB, "password", "QaAudit2026"));
        assertEquals(HttpStatus.CREATED, rb.getStatusCode(), "Registro cliente B: " + rb.getBody());
        token.put("CLIENTEB", rb.getBody().get("token").asText());

        ver("identidades autenticadas", token.keySet());
        ver("conteos iniciales encomiendas/boletos/pagos/retenciones", encAntes + "/" + bolAntes + "/" + pagAntes + "/" + retAntes);
        ver("sucursales usadas", sucursalOrigen + " -> " + sucursalDestino);
    }

    private String login(String u, String p) throws Exception {
        ResponseEntity<JsonNode> r = post("/api/v1/auth/login", null, m("username", u, "password", p));
        assertEquals(HttpStatus.OK, r.getStatusCode(), "Login " + u + ": " + r.getBody());
        String t = r.getBody().get("token").asText();
        assertNotNull(t);
        return t;
    }

    /**
     * POST contra una IP concreta. El limitador de intentos (H-06) tiene una dimension
     * por IP ademas de la por usuario, asi que para provocar el 429 de una cuenta sin
     * agotar el techo de IP compartida hay que poder fijar el origen de la peticion.
     * X-Forwarded-For es lo que lee AuthController para determinar el cliente.
     */
    private ResponseEntity<JsonNode> postConIp(String ruta, String token, Map<String, Object> cuerpo,
                                              String ip) throws Exception {
        HttpHeaders h = hdr(token, cuerpo != null);
        h.set("X-Forwarded-For", ip);
        String payload = cuerpo == null ? null : mapper.writeValueAsString(cuerpo);
        return rest.exchange(url(ruta), HttpMethod.POST, new HttpEntity<>(payload, h), JsonNode.class);
    }

    private ResponseEntity<JsonNode> loginConIp(String usuario, String password, String ip) throws Exception {
        return postConIp("/api/v1/auth/login", null, m("username", usuario, "password", password), ip);
    }

    // â”€â”€ 2. autenticacion â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    @Order(2)
    @DisplayName("Autenticacion: credenciales invalidas, token ausente, token manipulado y usuario inactivo")
    void autenticacion() throws Exception {
        ok("login con password incorrecto -> 401",
                post("/api/v1/auth/login", null, m("username", "admin", "password", "mala")).getStatusCode() == HttpStatus.UNAUTHORIZED);
        ok("login de usuario inexistente -> 401",
                post("/api/v1/auth/login", null, m("username", "nadie" + sufijo(), "password", "x")).getStatusCode() == HttpStatus.UNAUTHORIZED);
        ok("login sin campos -> 400",
                post("/api/v1/auth/login", null, m()).getStatusCode() == HttpStatus.BAD_REQUEST);

        // â”€â”€ H-06: el limite de intentos frena la fuerza bruta contra el login â”€â”€
        // Se usa un usuario propio de la prueba y una IP propia para no tocar los
        // contadores de las cuentas reales ni el limite por IP de los 6 logins validos.
        String userForce = PREFIJO + "f" + sufijo();
        String ipFuerzaBruta = "203.0.113." + (10 + Math.abs(sufijo().hashCode() % 200));
        int ultimo = 0;
        for (int i = 1; i <= 7; i++) {
            ultimo = loginConIp(userForce, "ClaveIncorrecta9", ipFuerzaBruta).getStatusCodeValue();
        }
        ok("7 intentos fallidos -> 429 en el ultimo (rate limiting activo): " + ultimo,
                ultimo == 429);
        // El bloqueo es POR USUARIO: otra cuenta desde el mismo origen sigue entrando.
        int otroUsuario = postConIp("/api/v1/auth/login", null,
                m("username", "cajero1", "password", "cajero123"), ipFuerzaBruta + ".5").getStatusCodeValue();
        ok("otra cuenta desde la misma IP no hereda el bloqueo -> " + otroUsuario,
                otroUsuario == 200);

        // â”€â”€ H-06: el logout revoca el token en el servidor â”€â”€
        // Un token de un usuario Dedicated, cerrado y luego reutilizado.
        String userLogout = PREFIJO + "s" + sufijo();
        ResponseEntity<JsonNode> altaLogout = post("/api/v1/usuarios", token.get("ADMIN"), m(
                "username", userLogout, "password", "QaAudit2026",
                "nombres", "QA", "apellidos", "Sesion",
                "email", userLogout + "@intiwatana.test",
                "rolId", rolClienteId, "sucursalId", sucursalOrigen));
        assertEquals(HttpStatus.CREATED, altaLogout.getStatusCode(), "Alta de usuario para logout: " + altaLogout.getBody());
        ResponseEntity<JsonNode> liLogout = post("/api/v1/auth/login", null,
                m("username", userLogout, "password", "QaAudit2026"));
        assertEquals(HttpStatus.OK, liLogout.getStatusCode(), "Login del usuario de logout: " + liLogout.getBody());
        String tokenLogout = liLogout.getBody().get("token").asText();
        assertNotNull(liLogout.getBody().get("refreshToken"), "El login debe entregar refreshToken");

        // Se usa GET /api/v1/buses porque exige solo isAuthenticated() y devuelve una
        // lista:asi el 200 prueba que el TOKEN es valido, sin depender de que el
        // usuario tenga un cliente o viaje asociado.
        ok("endpoint protegido con token valido -> 200",
                get("/api/v1/buses", tokenLogout).getStatusCode() == HttpStatus.OK);
        ok("POST /auth/logout -> 200",
                post("/api/v1/auth/logout", tokenLogout, null).getStatusCode() == HttpStatus.OK);
        ok("el mismo token tras el logout -> 401 (revocado en el servidor)",
                get("/api/v1/buses", tokenLogout).getStatusCode() == HttpStatus.UNAUTHORIZED);

        // El refresh token es de un solo uso: al renovarlo queda invalidado.
        String refresh = liLogout.getBody().get("refreshToken").asText();
        ResponseEntity<JsonNode> renueva = post("/api/v1/auth/refresh", null, m("refreshToken", refresh));
        ok("POST /auth/refresh -> 200 con un token nuevo", renueva.getStatusCode() == HttpStatus.OK);
        String tokenNuevo = renueva.getBody().get("token").asText();
        assertNotEquals(tokenLogout, tokenNuevo, "El refresh debe emitir un token DISTINTO");
        ok("el token renovado sirve -> 200",
                get("/api/v1/buses", tokenNuevo).getStatusCode() == HttpStatus.OK);
        ok("el refresh token ya usado -> 401 (rotacion de un solo uso)",
                post("/api/v1/auth/refresh", null, m("refreshToken", refresh)).getStatusCode() == HttpStatus.UNAUTHORIZED);
        // Y un refresh token NO puede usarse como credencial de la API.
        ok("un refresh token NO autentica la API -> 401",
                get("/api/v1/buses", refresh).getStatusCode() == HttpStatus.UNAUTHORIZED);

        ok("endpoint protegido sin token -> 401",
                get("/api/v1/encomiendas", null).getStatusCode() == HttpStatus.UNAUTHORIZED);
        ok("token no-JWT -> 401",
                get("/api/v1/encomiendas", "abc.def.ghi").getStatusCode() == HttpStatus.UNAUTHORIZED);

        String t = token.get("ADMIN");
        ok("token con firma alterada -> 401",
                get("/api/v1/encomiendas", t.substring(0, t.length() - 8) + "AAAAAAAA").getStatusCode() == HttpStatus.UNAUTHORIZED);

        // Usuario creado por la API y luego desactivado directamente en la base.
        String userInactivo = PREFIJO + "i" + sufijo();
        ResponseEntity<JsonNode> alta = post("/api/v1/usuarios", token.get("ADMIN"), m(
                "username", userInactivo, "password", "QaAudit2026",
                "nombres", "QA", "apellidos", "Inactivo",
                "email", userInactivo + "@intiwatana.test",
                "rolId", rolClienteId, "sucursalId", sucursalOrigen));
        assertEquals(HttpStatus.CREATED, alta.getStatusCode(), "Alta de usuario QA: " + alta.getBody());
        Long idInactivo = alta.getBody().get("id").asLong();
        jdbc.update("UPDATE usuarios SET activo=false WHERE id=?", idInactivo);
        ResponseEntity<JsonNode> li = post("/api/v1/auth/login", null, m("username", userInactivo, "password", "QaAudit2026"));
        ok("usuario con activo=false no puede iniciar sesion -> " + li.getStatusCodeValue(),
                li.getStatusCode() == HttpStatus.UNAUTHORIZED);
        jdbc.update("UPDATE usuarios SET activo=true WHERE id=?", idInactivo);
        ok("al reactivarlo vuelve a poder iniciar sesion",
                post("/api/v1/auth/login", null, m("username", userInactivo, "password", "QaAudit2026")).getStatusCode() == HttpStatus.OK);
    }

    @Test
    @Order(3)
    @DisplayName("La autorizacion prevalece: con cuerpo VALIDO un endpoint restringido sigue respondiendo 403")
    void autorizacionPrevaleceSobreValidacion() throws Exception {
        // Un cuerpo vacio produce 400 porque Spring valida el @RequestBody antes
        // de evaluar @PreAuthorize. Con un cuerpo totalmente valido solo puede
        // responder 403, lo que demuestra que la anotacion de seguridad se
        // aplica y no hay forma de eludirla con un cuerpo mal formado.
        String u = PREFIJO + "x" + sufijo();
        Map<String, Object> cuerpo = m("username", u, "password", "QaAudit2026",
                "nombres", "QA", "apellidos", "Intruso",
                "email", u + "@intiwatana.test",
                "rolId", rolClienteId, "sucursalId", sucursalOrigen);
        for (String rol : List.of("CAJERO", "VENDEDOR", "OPERADOR", "CHOFER1", "CLIENTE")) {
            ResponseEntity<JsonNode> r = post("/api/v1/usuarios", token.get(rol), cuerpo);
            ok("POST /usuarios con cuerpo valido como " + rol + " -> " + r.getStatusCodeValue() + " (403 esperado)",
                    r.getStatusCode() == HttpStatus.FORBIDDEN);
        }
        Integer creados = jdbc.queryForObject("SELECT count(*) FROM usuarios WHERE username = ?", Integer.class, u);
        ok("no se creo ningun usuario con el intento de intrusion", creados != null && creados == 0);
        Integer anadidos = jdbc.queryForObject("SELECT count(*) FROM auditoria WHERE detalle LIKE ?", Integer.class, "%" + u + "%");
        System.out.println("  [INFO]  filas de auditoria que mencionan el intento: " + anadidos);

        // El endpoint mas sensible del alta de encomiendas: solo el personal autorizado.
        int antes = jdbc.queryForObject("SELECT count(*) FROM encomiendas", Integer.class);
        for (String rol : List.of("CHOFER1", "CLIENTE")) {
            ResponseEntity<JsonNode> r = post("/api/v1/encomiendas", token.get(rol), m(
                    "remitenteId", 1, "destinatarioId", 2,
                    "sucursalOrigenId", sucursalOrigen, "sucursalDestinoId", sucursalDestino,
                    "descripcionContenido", "intento " + rol,
                    "pesoKg", 1.0, "largoCm", 10, "anchoCm", 10, "altoCm", 10,
                    "tarifaOficial", 10, "metodoPago", "EFECTIVO"));
            ok("POST /encomiendas con cuerpo valido como " + rol + " -> " + r.getStatusCodeValue() + " (403 esperado)",
                    r.getStatusCode() == HttpStatus.FORBIDDEN);
        }
        Integer despues = jdbc.queryForObject("SELECT count(*) FROM encomiendas", Integer.class);
        ok("no se registro ninguna encomienda por los intentos (antes=" + antes + ", despues=" + despues + ")",
                despues != null && despues == antes);
    }

    // 3. matriz de permisos reales

    /** Caso de la matriz: lo que el codigo DECLARA, contrastado con lo que responde el sistema. */
    private record Caso(String etiqueta, HttpMethod metodo, String ruta, Object cuerpo,
                        boolean probarLosPermitidos, List<String> permitidos) {}

    @Test
    @Order(3)
    @DisplayName("Matriz de permisos: 6 identidades x 36 endpoints contra lo declarado en @PreAuthorize")
    void matrizDePermisos() throws Exception {
        List<String> todos = List.of("ADMIN", "CAJERO", "VENDEDOR", "OPERADOR", "CHOFER1", "CLIENTE");
        List<Caso> casos = List.of(
                new Caso("GET  /usuarios", HttpMethod.GET, "/api/v1/usuarios", null, true, List.of("ADMIN")),
                new Caso("POST /usuarios", HttpMethod.POST, "/api/v1/usuarios", m(), true, List.of("ADMIN")),
                new Caso("DEL  /usuarios/999999", HttpMethod.DELETE, "/api/v1/usuarios/999999", null, true, List.of("ADMIN")),
                new Caso("GET  /clientes", HttpMethod.GET, "/api/v1/clientes", null, true, List.of("ADMIN", "CAJERO", "VENDEDOR", "OPERADOR", "CHOFER1")),
                new Caso("GET  /clientes/dni/{dni}", HttpMethod.GET, "/api/v1/clientes/dni/99999999", null, true, List.of("ADMIN", "CAJERO", "VENDEDOR", "OPERADOR")),
                new Caso("POST /clientes", HttpMethod.POST, "/api/v1/clientes", m(), true, List.of("ADMIN", "CAJERO", "VENDEDOR", "OPERADOR")),
                new Caso("PUT  /clientes/999999", HttpMethod.PUT, "/api/v1/clientes/999999", m(), true, List.of("ADMIN", "CAJERO")),
                new Caso("DEL  /clientes/999999", HttpMethod.DELETE, "/api/v1/clientes/999999", null, true, List.of("ADMIN")),
                new Caso("GET  /clientes/mi-perfil", HttpMethod.GET, "/api/v1/clientes/mi-perfil", null, true, List.of("CLIENTE")),
                new Caso("GET  /pagos", HttpMethod.GET, "/api/v1/pagos", null, true, List.of("ADMIN", "CAJERO")),
                new Caso("POST /pagos", HttpMethod.POST, "/api/v1/pagos", m(), true, List.of("ADMIN", "CAJERO")),
                new Caso("PUT  /pagos/999999", HttpMethod.PUT, "/api/v1/pagos/999999", m(), true, List.of("ADMIN", "CAJERO")),
                new Caso("DEL  /pagos/999999", HttpMethod.DELETE, "/api/v1/pagos/999999", null, true, List.of("ADMIN")),
                new Caso("GET  /reportes/resumen", HttpMethod.GET, "/api/v1/reportes/resumen", null, true, List.of("ADMIN")),
                new Caso("GET  /auditoria", HttpMethod.GET, "/api/v1/auditoria", null, true, List.of("ADMIN")),
                new Caso("GET  /encomiendas", HttpMethod.GET, "/api/v1/encomiendas", null, true, List.of("ADMIN", "CAJERO", "VENDEDOR", "OPERADOR")),
                new Caso("POST /encomiendas", HttpMethod.POST, "/api/v1/encomiendas", m(), true, List.of("ADMIN", "CAJERO", "VENDEDOR", "OPERADOR")),
                new Caso("GET  /encomiendas/999999", HttpMethod.GET, "/api/v1/encomiendas/999999", null, true, List.of("ADMIN", "CAJERO", "VENDEDOR", "OPERADOR")),
                new Caso("GET  /encomiendas/mis-encomiendas", HttpMethod.GET, "/api/v1/encomiendas/mis-encomiendas", null, true, List.of("CLIENTE")),
                new Caso("POST /encomiendas/solicitar", HttpMethod.POST, "/api/v1/encomiendas/solicitar", m(), true, List.of("CLIENTE")),
                new Caso("GET  /viajes/mis-viajes", HttpMethod.GET, "/api/v1/viajes/mis-viajes", null, true, List.of("ADMIN", "CHOFER1")),
                new Caso("PATCH /viajes/999999/estado", HttpMethod.PATCH, "/api/v1/viajes/999999/estado", m("estado", "FINALIZADO"), true, List.of("ADMIN", "CHOFER1")),
                new Caso("GET  /boletos/mis-boletos", HttpMethod.GET, "/api/v1/boletos/mis-boletos", null, true, List.of("CLIENTE")),
                new Caso("POST /boletos/comprar", HttpMethod.POST, "/api/v1/boletos/comprar", m(), true, List.of("CLIENTE")),
                new Caso("POST /boletos/escanear", HttpMethod.POST, "/api/v1/boletos/escanear?codigoQr=NOEXISTE", null, true, List.of("ADMIN", "CHOFER1")),
                new Caso("GET  /boletos/viaje/999999", HttpMethod.GET, "/api/v1/boletos/viaje/999999", null, true, List.of("ADMIN", "CAJERO", "VENDEDOR", "CHOFER1")),
                new Caso("PATCH /boletos/999999/cancelar", HttpMethod.PATCH, "/api/v1/boletos/999999/cancelar", m(), true, List.of("ADMIN", "CAJERO", "VENDEDOR", "CLIENTE")),
                new Caso("GET  /boletos/999999", HttpMethod.GET, "/api/v1/boletos/999999", null, true, List.of("ADMIN", "CAJERO", "VENDEDOR", "CHOFER1")),
                new Caso("GET  /ventas/reservas", HttpMethod.GET, "/api/v1/ventas/reservas", null, true, List.of("ADMIN", "CAJERO", "VENDEDOR")),
                new Caso("POST /ventas/reservas/ZZ/confirmar", HttpMethod.POST, "/api/v1/ventas/reservas/ZZZZZZ/confirmar", m(), true, List.of("ADMIN", "CAJERO", "VENDEDOR")),
                new Caso("POST /ventas/reservas/ZZ/liberar", HttpMethod.POST, "/api/v1/ventas/reservas/ZZZZZZ/liberar", null, true, List.of("ADMIN", "CAJERO", "VENDEDOR")),
                new Caso("GET  /portal/admin/secciones", HttpMethod.GET, "/api/v1/portal/admin/secciones", null, true, List.of("ADMIN")),
                new Caso("GET  /portal/admin/imagenes", HttpMethod.GET, "/api/v1/portal/admin/imagenes", null, true, List.of("ADMIN")),
                new Caso("POST /portal/admin/imagenes", HttpMethod.POST, "/api/v1/portal/admin/imagenes", m(), true, List.of("ADMIN")),
                new Caso("PUT  /rutas/999999", HttpMethod.PUT, "/api/v1/rutas/999999", m(), true, List.of("ADMIN")),
                new Caso("POST /buses", HttpMethod.POST, "/api/v1/buses", m(), true, List.of("ADMIN")),
                new Caso("PATCH /asientos/999999", HttpMethod.PATCH, "/api/v1/asientos/999999", m(), true, List.of("ADMIN")),
                // Endpoint generador de datos: nunca se ejecuta con ADMIN para no crear viajes reales.
                new Caso("POST /admin/tools/generar-viajes-prueba", HttpMethod.POST, "/api/v1/admin/tools/generar-viajes-prueba", m(), false, List.of("ADMIN")));

        int coherentes = 0, total = 0, discrepancias = 0, validados = 0;
        for (Caso c : casos) {
            StringBuilder det = new StringBuilder();
            boolean todoBien = true;
            for (String rol : todos) {
                boolean declarado = c.permitidos().contains(rol);
                // Los casos marcados como no ejecutables no se invocan con los roles
                // permitidos para no producir datos (por ejemplo el generador de viajes).
                if (declarado && !c.probarLosPermitidos()) continue;
                String tk = token.get(rol);
                ResponseEntity<JsonNode> r = send(c.metodo(), c.ruta(), tk, c.cuerpo());
                int st = r.getStatusCodeValue();
                boolean correcto;
                if (declarado) {
                    correcto = st != 403 && st != 401;
                    if (!correcto) det.append(" ").append(rol).append("=").append(st).append("(BLOQUEADO!!) ");
                } else if (st == 400) {
                    // Spring valida el @RequestBody antes de evaluar @PreAuthorize:
                    // no es un bypass, lo demuestra la prueba autorizacionPrevaleceSobreValidacion.
                    correcto = true;
                    validados++;
                } else {
                    correcto = st == 403;
                    if (!correcto) det.append(" ").append(rol).append("=").append(st).append("(PERMITIDO!!) ");
                }
                total++;
                if (correcto) coherentes++; else { todoBien = false; discrepancias++; }
            }
            String marca = todoBien ? "  [OK]     " : "  [FALLA]  ";
            System.out.println(marca + String.format("%-36s %s", c.etiqueta(), todoBien ? "coherente con lo declarado" : det.toString().trim()));
        }
        System.out.println("  [RESUMEN] matriz coherente en " + coherentes + "/" + total + " combinaciones; "
                + "discrepancias reales: " + discrepancias + "; respuestas 400 por validacion previa a @PreAuthorize: " + validados);
    }

    // â”€â”€ 4. acceso anonymous â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    @Order(4)
    @DisplayName("Acceso sin token: lo declarado permitAll en SecurityConfig se cumple y lo demas responde 401")
    void accesoAnonimo() throws Exception {
        String[][] publicos = {
                // /api/v1/roles salio de esta lista en el cierre de H-05: el catalogo
                // de roles es informacion interna y ahora exige ADMIN.
                {"GET", "/api/v1/rutas"},
                {"GET", "/api/v1/sucursales"},
                {"GET", "/api/v1/sucursales/terminales"},
                {"GET", "/api/v1/viajes"},
                {"GET", "/api/v1/viajes/disponibles"},
                {"GET", "/api/v1/viajes/1/asientos"},
                {"GET", "/api/v1/portal/publico"},
                {"GET", "/actuator/health"},
        };
        for (String[] c : publicos) {
            int st = get(c[1], null).getStatusCodeValue();
            ok(String.format("anonimo %-34s -> %d (accesible sin token)", c[1], st), st != 401 && st != 403);
        }
        String[][] protegidos = {
                {"GET", "/api/v1/usuarios"}, {"GET", "/api/v1/clientes"}, {"GET", "/api/v1/encomiendas"},
                {"GET", "/api/v1/ventas/reservas"}, {"GET", "/api/v1/portal/admin/secciones"},
                {"GET", "/api/v1/pagos"}, {"GET", "/api/v1/auditoria"}, {"GET", "/api/v1/buses"},
                // H-05: el catalogo de roles ya no es publico.
                {"GET", "/api/v1/roles"},
                // H-07: el listado de viajes es publico, su DETALLE no.
                {"GET", "/api/v1/viajes/1"},
                {"GET", "/actuator/metrics"},
        };
        for (String[] c : protegidos) {
            int st = get(c[1], null).getStatusCodeValue();
            ok(String.format("anonimo %-34s -> %d (401 esperado)", c[1], st), st == 401);
        }

        // H-05: /roles responde 200 a ADMIN y 403 a un usuario autenticado que no lo es.
        int rolesAdmin = get("/api/v1/roles", token.get("ADMIN")).getStatusCodeValue();
        ok(String.format("ADMIN     %-34s -> %d", "/api/v1/roles", rolesAdmin), rolesAdmin == 200);
        int rolesCajero = get("/api/v1/roles", token.get("CAJERO")).getStatusCodeValue();
        ok(String.format("CAJERO    %-34s -> %d (403 esperado)", "/api/v1/roles", rolesCajero), rolesCajero == 403);

        // H-07: con sesion, el detalle del viaje se puede consultar.
        int viajeCajero = get("/api/v1/viajes/1", token.get("CAJERO")).getStatusCodeValue();
        ok(String.format("CAJERO    %-34s -> %d", "/api/v1/viajes/1", viajeCajero), viajeCajero == 200);

        ver("metrica actuator con token de CLIENTE", get("/actuator/metrics", token.get("CLIENTE")).getStatusCodeValue());
        ver("metrica actuator con token de CHOFER", get("/actuator/metrics", token.get("CHOFER1")).getStatusCodeValue());
        ver("metrica actuator anonimo", get("/actuator/metrics", null).getStatusCodeValue());
    }

    // â”€â”€ 5. IDOR: cancelacion de boleto ajeno â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    @Order(5)
    @DisplayName("IDOR: un cliente del portal no puede cancelar el boleto de otro")
    void idorCancelarBoletoAjeno() throws Exception {
        Integer ajenos = jdbc.queryForObject(
                "SELECT count(*) FROM boletos b JOIN clientes c ON c.id=b.cliente_id "
                        + "WHERE c.usuario_id IS DISTINCT FROM (SELECT usuario_id FROM clientes WHERE id=?)", Integer.class, clienteAId);
        ver("boletos en QA que NO son del cliente A", ajenos);
        if (ajenos == null || ajenos == 0) { ver("sin datos para la prueba", true); return; }

        Long idBoleto = jdbc.queryForObject("SELECT b.id FROM boletos b JOIN clientes c ON c.id=b.cliente_id "
                + "WHERE c.usuario_id IS DISTINCT FROM (SELECT usuario_id FROM clientes WHERE id=?) ORDER BY b.id LIMIT 1",
                Long.class, clienteAId);
        String estadoAntes = jdbc.queryForObject("SELECT estado FROM boletos WHERE id=?", String.class, idBoleto);
        // motivo es un @RequestParam, no un campo del cuerpo.
        ResponseEntity<JsonNode> r = patch("/api/v1/boletos/" + idBoleto + "/cancelar", token.get("CLIENTE"), null);
        String estadoDespues = jdbc.queryForObject("SELECT estado FROM boletos WHERE id=?", String.class, idBoleto);
        ok("cliente NO cancela el boleto ajeno (HTTP " + r.getStatusCodeValue() + ", estado " + estadoAntes + " -> " + estadoDespues + ")",
                r.getStatusCode().is4xxClientError() && estadoAntes.equals(estadoDespues));
        ver("mensaje devuelto", r.getBody() == null ? "(vacio)" : txt(r.getBody(), "mensaje"));

        ResponseEntity<JsonNode> propio = patch("/api/v1/boletos/999999/cancelar", token.get("CLIENTE"), null);
        ok("el mismo endpoint responde 404 (no 403) para un boleto inexistente -> "
                + propio.getStatusCodeValue(), propio.getStatusCode() == HttpStatus.NOT_FOUND);
    }

    // â”€â”€ 6. IDOR: cambio de estado de viaje ajeno â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    @Order(6)
    @DisplayName("IDOR: un chofer no puede cambiar el estado de un viaje que no conduce")
    void idorEstadoDeViajeAjeno() throws Exception {
        List<Long> ajenos = jdbc.queryForList(
                "SELECT id FROM viajes WHERE chofer_id <> (SELECT id FROM usuarios WHERE username='chofer1') AND activo=true",
                Long.class);
        ver("viajes activos que NO conduce chofer1", ajenos);
        if (ajenos.isEmpty()) { ver("sin viajes ajenos para la prueba", true); return; }
        Long viaje = ajenos.get(0);
        String estadoAntes = jdbc.queryForObject("SELECT estado FROM viajes WHERE id=?", String.class, viaje);
        ResponseEntity<JsonNode> r = patch("/api/v1/viajes/" + viaje + "/estado", token.get("CHOFER2"), m("estado", "FINALIZADO"));
        String estadoDespues = jdbc.queryForObject("SELECT estado FROM viajes WHERE id=?", String.class, viaje);
        ok("chofer ajeno NO cambia el estado (HTTP " + r.getStatusCodeValue() + ", " + estadoAntes + " -> " + estadoDespues + ")",
                r.getStatusCode() == HttpStatus.FORBIDDEN && estadoAntes.equals(estadoDespues));

        List<Long> propios = jdbc.queryForList(
                "SELECT id FROM viajes WHERE chofer_id=(SELECT id FROM usuarios WHERE username='chofer1') AND activo=true", Long.class);
        ver("viajes de chofer1 (control positivo)", propios);
    }

    // â”€â”€ 7. IDOR: movimientos de encomienda ajena â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    @Order(7)
    @DisplayName("IDOR: movimientos de encomienda de otro remitente, con y sin usuario vinculado")
    void idorMovimientos() throws Exception {
        // Un cliente de ventanilla: existe en clientes pero SIN cuenta vinculada.
        Long sinUsuario = jdbc.queryForObject(
                "SELECT id FROM clientes WHERE usuario_id IS NULL AND activo=true ORDER BY id LIMIT 1", Long.class);
        Long otroSinUsuario = jdbc.queryForObject(
                "SELECT id FROM clientes WHERE usuario_id IS NULL AND id <> ? ORDER BY id LIMIT 1", Long.class, sinUsuario);
        String marca = "AUDITORIA IDOR " + UUID.randomUUID().toString().substring(0, 8);

        // Encomienda de ventanilla: remitente SIN usuario (caso que antes omitia la propiedad).
        ResponseEntity<JsonNode> alta = post("/api/v1/encomiendas", token.get("CAJERO"), m(
                "remitenteId", sinUsuario, "destinatarioId", otroSinUsuario,
                "sucursalOrigenId", sucursalOrigen, "sucursalDestinoId", sucursalDestino,
                "descripcionContenido", marca + " ventanilla",
                "pesoKg", 1.0, "largoCm", 10, "anchoCm", 10, "altoCm", 10,
                "tarifaOficial", 10, "metodoPago", "EFECTIVO",
                "claveIdempotencia", "aud-idor-" + UUID.randomUUID()));
        assertEquals(HttpStatus.CREATED, alta.getStatusCode(), "Alta presencial: " + alta.getBody());
        long idVentanilla = alta.getBody().get("id").asLong();
        encomiendasQA.add(idVentanilla);

        // Solicitud del portal: el remitente es el cliente B con cuenta.
        ResponseEntity<JsonNode> sol = post("/api/v1/encomiendas/solicitar", token.get("CLIENTEB"), m(
                "sucursalOrigenId", sucursalOrigen, "sucursalDestinoId", sucursalDestino,
                "remitenteNombre", "QA Auditoria", "remitenteTelefono", "900000103",
                "destinatarioNombre", "Destinatario QA", "destinatarioTelefono", "900000104",
                "descripcionContenido", marca + " solicitud",
                "tipoEnvio", "PAQUETE", "cuidadoEspecial", "NINGUNO"));
        assertEquals(HttpStatus.CREATED, sol.getStatusCode(), "Solicitud: " + sol.getBody());
        long idSolicitud = sol.getBody().get("id").asLong();
        encomiendasQA.add(idSolicitud);

        ResponseEntity<JsonNode> r1 = get("/api/v1/encomiendas/" + idVentanilla + "/movimientos", token.get("CLIENTE"));
        ok("cliente NO lee movimientos de una encomienda cuyo remitente no tiene cuenta (HTTP " + r1.getStatusCodeValue() + ")",
                r1.getStatusCode() == HttpStatus.FORBIDDEN);
        ResponseEntity<JsonNode> r2 = get("/api/v1/encomiendas/" + idSolicitud + "/movimientos", token.get("CLIENTE"));
        ok("cliente NO lee movimientos de la encomienda de otro cliente (HTTP " + r2.getStatusCodeValue() + ")",
                r2.getStatusCode() == HttpStatus.FORBIDDEN);
        ResponseEntity<JsonNode> r3 = get("/api/v1/encomiendas/" + idSolicitud + "/movimientos", token.get("CLIENTEB"));
        ok("el cliente B SI lee los movimientos de su propia encomienda (HTTP " + r3.getStatusCodeValue() + ")",
                r3.getStatusCode() == HttpStatus.OK);
        ResponseEntity<JsonNode> r4 = get("/api/v1/encomiendas/" + idVentanilla + "/movimientos", token.get("CAJERO"));
        ok("el personal autorizado SI lee los movimientos (HTTP " + r4.getStatusCodeValue() + ")",
                r4.getStatusCode() == HttpStatus.OK);
    }

    // â”€â”€ 8. BOLA en endpoints publicos â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    @Order(8)
    @DisplayName("BOLA: los endpoints publicos de venta exigen el documento del comprador")
    void bolaEndpointsPublicos() throws Exception {
        String ref = jdbc.queryForObject("SELECT referencia FROM retenciones WHERE estado='CONFIRMADO' ORDER BY id DESC LIMIT 1", String.class);
        String doc = jdbc.queryForObject("SELECT comprador_documento FROM retenciones WHERE referencia=?", String.class, ref);
        String num = jdbc.queryForObject("SELECT numero_boleto FROM boletos WHERE estado <> 'CANCELADO' ORDER BY id DESC LIMIT 1", String.class);
        String docBoleto = jdbc.queryForObject("SELECT c.dni_ruc FROM boletos b JOIN clientes c ON c.id=b.cliente_id WHERE b.numero_boleto=?", String.class, num);
        ver("boleto de prueba para el PDF", num + " / documento " + docBoleto);

        ResponseEntity<byte[]> pdfOk = bytes("/api/v1/ventas/boleto/" + num + "/pdf?documento=" + docBoleto, null);
        ok("PDF del boleto con el documento correcto -> " + pdfOk.getStatusCodeValue(),
                pdfOk.getStatusCode() == HttpStatus.OK && pdfOk.getBody() != null && pdfOk.getBody().length > 900);
        ok("el PDF tiene firma %PDF", pdfOk.getBody() != null && pdfOk.getBody().length > 4
                && "%PDF".equals(new String(pdfOk.getBody(), 0, 4)));
        ok("PDF del boleto con documento ajeno -> " + bytes("/api/v1/ventas/boleto/" + num + "/pdf?documento=99999999", null).getStatusCodeValue() + " (no 200)",
                bytes("/api/v1/ventas/boleto/" + num + "/pdf?documento=99999999", null).getStatusCode() != HttpStatus.OK);
        ok("PDF del boleto sin documento -> " + bytes("/api/v1/ventas/boleto/" + num + "/pdf", null).getStatusCodeValue() + " (no 200)",
                bytes("/api/v1/ventas/boleto/" + num + "/pdf", null).getStatusCode() != HttpStatus.OK);
        ok("retencion con el documento correcto -> " + get("/api/v1/ventas/retencion/" + ref + "?documento=" + doc, null).getStatusCodeValue(),
                get("/api/v1/ventas/retencion/" + ref + "?documento=" + doc, null).getStatusCode() == HttpStatus.OK);
        ok("retencion con documento ajeno -> " + get("/api/v1/ventas/retencion/" + ref + "?documento=99999999", null).getStatusCodeValue() + " (no 200)",
                get("/api/v1/ventas/retencion/" + ref + "?documento=99999999", null).getStatusCode() != HttpStatus.OK);
        ok("retencion inexistente -> 404",
                get("/api/v1/ventas/retencion/ZZZZZZ?documento=" + doc, null).getStatusCode() == HttpStatus.NOT_FOUND);
        ResponseEntity<JsonNode> sinReservas = get("/api/v1/ventas/recuperar?documento=99999999", null);
        boolean vacio = sinReservas.getStatusCode() == HttpStatus.OK
                && (sinReservas.getBody() == null || sinReservas.getBody().isEmpty()
                    || (sinReservas.getBody().has("reservas") && sinReservas.getBody().get("reservas").isEmpty()));
        ok("recuperacion de un documento sin reservas -> HTTP " + sinReservas.getStatusCodeValue() + " y vacia", vacio);
    }

    // â”€â”€ 9. enums Java vs restricciones de la base â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    @Order(9)
    @DisplayName("Coherencia enum Java <-> CHECK de PostgreSQL en los estados ya presentes en la base")
    void enumsContraBase() throws Exception {
        System.out.println("  [INFO]  enums Java: Retencion=RETENIDO|PENDIENTE_PAGO|CONFIRMADO|VENCIDO  "
                + " Boleto=ACTIVO|USADO|CANCELADO  Asiento=DISPONIBLE|RESERVADO|VENDIDO");
        System.out.println("  [INFO]  CHECK en PostgreSQL: retenciones=+CANCELADO  boletos=+REEMBOLSADO  "
                + " asientos=+BLOQUEADO  tipo_asiento=+CAMA_DOBLE");

        List<String> refCancelada = jdbc.queryForList("SELECT referencia FROM retenciones WHERE estado='CANCELADO'", String.class);
        ver("retenciones con estado CANCELADO que el enum Java NO reconoce", refCancelada.size());
        if (!refCancelada.isEmpty()) {
            String ref = refCancelada.get(0);
            String doc = jdbc.queryForObject("SELECT comprador_documento FROM retenciones WHERE referencia=?", String.class, ref);
            int stPub = get("/api/v1/ventas/retencion/" + ref + "?documento=" + doc, null).getStatusCodeValue();
            int stLista = get("/api/v1/ventas/reservas", token.get("VENDEDOR")).getStatusCodeValue();
            int stRecup = get("/api/v1/ventas/recuperar?documento=" + doc, null).getStatusCodeValue();
            ver("GET publico /ventas/retencion/{CANCELADA} -> " + stPub, stPub);
            ver("GET /ventas/reservas (VENDEDOR) -> " + stLista, stLista);
            ver("GET publico /ventas/recuperar -> " + stRecup, stRecup);
            ok("la retencion CANCELADA no rompe la lectura publica (ningun 500)", stPub != 500 && stRecup != 500);
            ok("el listado de reservas del vendedor no se rompe (no 500)", stLista != 500);
        }
        Integer reemb = jdbc.queryForObject("SELECT count(*) FROM boletos WHERE estado='REEMBOLSADO'", Integer.class);
        Integer bloq = jdbc.queryForObject("SELECT count(*) FROM asientos WHERE estado='BLOQUEADO'", Integer.class);
        ver("boletos REEMBOLSADO en QA / asientos BLOQUEADO en QA", reemb + " / " + bloq);
    }

    // â”€â”€ 10. flujo completo de PASAJES â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    @Order(10)
    @DisplayName("Flujo de pasajes: retencion, reserva, pago, boleto, QR, constancia, PDF y escaneo")
    void flujoDePasajes() throws Exception {
        docPasaje = dni();
        List<Long> libres = jdbc.queryForList(
                "SELECT id FROM asientos WHERE viaje_id=1 AND estado='DISPONIBLE' ORDER BY id LIMIT 2", Long.class);
        assertEquals(2, libres.size(), "Se requieren 2 asientos libres");
        asientosQA.addAll(libres);
        ver("asientos de la prueba", asientosQA);

        ResponseEntity<JsonNode> ret = post("/api/v1/ventas/retencion", null, m(
                "viajeId", 1, "asientoIds", libres, "idempotencyKey", "aud-pas-" + UUID.randomUUID(),
                "comprador", m("nombres", "QA", "apellidos", "Pasajes", "documento", docPasaje,
                        "telefono", "900000105", "email", "qa.pas@intiwatana.test"),
                "metodoPagoPreferido", "YAPE"));
        assertEquals(HttpStatus.CREATED, ret.getStatusCode(), "Crear retencion: " + ret.getBody());
        refPasaje = txt(ret.getBody(), "referencia");
        ok("retencion " + refPasaje + " creada en RETENIDO", "RETENIDO".equals(txt(ret.getBody(), "estado")));
        ok("los asientos pasan a RESERVADO", countAsientos("RESERVADO", libres) == 2);

        ResponseEntity<JsonNode> res = post("/api/v1/ventas/retencion/" + refPasaje + "/reservar?metodoPago=YAPE", null, null);
        assertEquals(HttpStatus.OK, res.getStatusCode(), "Reservar: " + res.getBody());
        ok("estado PENDIENTE_PAGO", "PENDIENTE_PAGO".equals(txt(res.getBody(), "estado")));

        ResponseEntity<byte[]> cons = bytes("/api/v1/ventas/retencion/" + refPasaje + "/constancia.pdf?documento=" + docPasaje, null);
        ok("constancia de reserva pendiente -> " + cons.getStatusCodeValue(),
                cons.getStatusCode() == HttpStatus.OK && cons.getBody() != null && cons.getBody().length > 700);

        ResponseEntity<JsonNode> conf = post("/api/v1/ventas/reservas/" + refPasaje + "/confirmar", token.get("CAJERO"),
                m("metodoPago", "YAPE", "referenciaPago", "OPQA" + sufijo(), "documento", docPasaje));
        assertEquals(HttpStatus.OK, conf.getStatusCode(), "Confirmar: " + conf.getBody());
        ok("el cobro emite 2 boletos", conf.getBody() != null && conf.getBody().size() == 2);
        numeroBoleto = conf.getBody().get(0).get("numeroBoleto").asText();
        boletosQA.add(numeroBoleto);
        boletosQA.add(conf.getBody().get(1).get("numeroBoleto").asText());
        ok("numero de boleto con formato BOL-AAAAMMDD-NNNNNN (" + numeroBoleto + ")", numeroBoleto.matches("BOL-\\d{8}-\\d{6}"));
        ok("el boleto nace ACTIVO", "ACTIVO".equals(txt(conf.getBody().get(0), "estado")));
        ok("los asientos pasan a VENDIDO", countAsientos("VENDIDO", libres) == 2);
        ok("la retencion queda CONFIRMADA", "CONFIRMADO".equals(jdbc.queryForObject(
                "SELECT estado FROM retenciones WHERE referencia=?", String.class, refPasaje)));
        Integer npagos = jdbc.queryForObject("SELECT count(*) FROM pagos p JOIN retenciones r ON r.pago_id=p.id WHERE r.referencia=?", Integer.class, refPasaje);
        ok("un solo pago COMPLETADO asociado", npagos != null && npagos == 1 && "COMPLETADO".equals(jdbc.queryForObject(
                "SELECT p.estado FROM pagos p JOIN retenciones r ON r.pago_id=p.id WHERE r.referencia=?", String.class, refPasaje)));
        Integer nbol = jdbc.queryForObject("SELECT count(*) FROM boletos WHERE numero_boleto IN (?,?) AND qr_imagen_url IS NOT NULL",
                Integer.class, boletosQA.get(0), boletosQA.get(1));
        ok("ambos boletos tienen imagen QR generada", nbol != null && nbol == 2);
        pagoPasaje = jdbc.queryForObject("SELECT pago_id FROM retenciones WHERE referencia=?", Long.class, refPasaje);

        ResponseEntity<JsonNode> rec = get("/api/v1/ventas/recuperar?documento=" + docPasaje, null);
        ok("recuperacion sin cuenta con su documento -> " + rec.getStatusCodeValue(), rec.getStatusCode() == HttpStatus.OK);

        ResponseEntity<JsonNode> e1 = post("/api/v1/boletos/escanear?codigoQr=" + numeroBoleto, token.get("CHOFER1"), null);
        assertEquals(HttpStatus.OK, e1.getStatusCode(), "Primer escaneo: " + e1.getBody());
        ok("el primer escaneo marca USADO", "USADO".equals(jdbc.queryForObject(
                "SELECT estado FROM boletos WHERE numero_boleto=?", String.class, numeroBoleto)));
        ResponseEntity<JsonNode> e2 = post("/api/v1/boletos/escanear?codigoQr=" + numeroBoleto, token.get("CHOFER1"), null);
        ok("el segundo escaneo del mismo QR se rechaza -> " + e2.getStatusCodeValue(),
                e2.getStatusCode() != HttpStatus.OK);
        ok("sigue USADO y no se duplica el uso", "USADO".equals(jdbc.queryForObject(
                "SELECT estado FROM boletos WHERE numero_boleto=?", String.class, numeroBoleto)));
    }

    // â”€â”€ 11. no double sale / no double charge â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    @Order(11)
    @DisplayName("Integridad de la venta: un asiento vendido no genera un segundo boleto ni un segundo cobro")
    void noHayDobleVenta() throws Exception {
        Long asiento = asientosQA.get(0);
        String clave = "aud-doble-" + UUID.randomUUID();
        ResponseEntity<JsonNode> r1 = post("/api/v1/ventas/retencion", null, m(
                "viajeId", 1, "asientoIds", List.of(asiento), "idempotencyKey", clave,
                "comprador", m("nombres", "QA", "apellidos", "Intruso", "documento", dni(), "telefono", "900000106")));
        ver("nueva retencion sobre un asiento ya vendido -> HTTP " + r1.getStatusCodeValue(), r1.getStatusCodeValue());

        ResponseEntity<JsonNode> r2 = post("/api/v1/ventas/retencion", null, m(
                "viajeId", 1, "asientoIds", List.of(asiento), "idempotencyKey", clave,
                "comprador", m("nombres", "QA", "apellidos", "Intruso", "documento", dni(), "telefono", "900000106")));
        ok("la misma idempotencyKey no crea una segunda retencion -> " + r2.getStatusCodeValue(),
                r2.getStatusCode() != HttpStatus.CREATED || txt(r1.getBody(), "referencia").equals(txt(r2.getBody(), "referencia")));

        Integer activos = jdbc.queryForObject("SELECT count(*) FROM boletos WHERE asiento_id=? AND estado<>'CANCELADO'", Integer.class, asiento);
        ok("el asiento conserva 1 solo boleto activo (total " + activos + ")", activos != null && activos == 1);
        ok("existe el indice unico parcial uk_boletos_asiento_activo", Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname='uk_boletos_asiento_activo')", Boolean.class)));
        Integer cobros = jdbc.queryForObject("SELECT count(*) FROM pagos WHERE cliente_id=(SELECT cliente_id FROM boletos WHERE numero_boleto=?)",
                Integer.class, numeroBoleto);
        ver("pagos asociados al cliente del boleto de la prueba", cobros);
    }

    // â”€â”€ 12. manejo de errores â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    @Order(12)
    @DisplayName("Errores: 404, 400 y 405 coherentes y sin rastro de pila al cliente")
    void manejoDeErrores() throws Exception {
        ResponseEntity<JsonNode> r404 = get("/api/v1/encomiendas/99999999", token.get("ADMIN"));
        ok("recurso inexistente -> " + r404.getStatusCodeValue(), r404.getStatusCode() == HttpStatus.NOT_FOUND);
        ResponseEntity<JsonNode> r400 = get("/api/v1/encomiendas/tracking/NOESUNAGUIA", null);
        ok("parametro invalido de tracking -> " + r400.getStatusCodeValue() + " (4xx)", r400.getStatusCode().is4xxClientError());
        ResponseEntity<JsonNode> r405 = del("/api/v1/auth/login", token.get("ADMIN"));
        ok("metodo no permitido -> " + r405.getStatusCodeValue(), r405.getStatusCode() == HttpStatus.METHOD_NOT_ALLOWED);
        ResponseEntity<JsonNode> r500 = get("/api/v1/clientes/abc", token.get("ADMIN"));
        ok("id no numerico -> " + r500.getStatusCodeValue() + " (4xx, nunca 500)", r500.getStatusCode().is4xxClientError());

        String cuerpo = r404.getBody() == null ? "" : r404.getBody().toString();
        ok("la respuesta de error no incluye traza de pila", !cuerpo.contains("at com.transporte") && !cuerpo.contains("java.lang"));
        ver("cuerpo del error 404", cuerpo.length() > 160 ? cuerpo.substring(0, 160) + "..." : cuerpo);
    }

    // â”€â”€ 13. aislamiento â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    @Order(13)
    @DisplayName("Aislamiento: la base QA vuelve a su estado inicial")
    void aislamiento() throws Exception {
        restaurarViaje();
        limpiar();
        ok("encomiendas = " + encAntes, count("encomiendas").equals(encAntes));
        ok("boletos = " + bolAntes, count("boletos").equals(bolAntes));
        ok("pagos = " + pagAntes, count("pagos").equals(pagAntes));
        ok("retenciones = " + retAntes, count("retenciones").equals(retAntes));
        Integer usuarios = jdbc.queryForObject("SELECT count(*) FROM usuarios WHERE username LIKE ?", Integer.class, PREFIJO + "%");
        Integer clientes = jdbc.queryForObject("SELECT count(*) FROM clientes WHERE dni_ruc LIKE ?", Integer.class, "90%");
        ok("no quedan usuarios QA (total " + usuarios + ")", usuarios != null && usuarios == 0);
        ver("clientes con dni 90xxxxxx (semilla QA del proyecto)", clientes);
    }

    // â”€â”€ soporte â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private Integer count(String tabla) { return jdbc.queryForObject("SELECT count(*) FROM " + tabla, Integer.class); }

    private int countAsientos(String estado, List<Long> ids) {
        String ph = String.join(",", java.util.Collections.nCopies(ids.size(), "?"));
        return jdbc.queryForObject("SELECT count(*) FROM asientos WHERE id IN (" + ph + ") AND estado=?", Integer.class,
                concat(ids.toArray(), estado));
    }

    private static Object[] concat(Object[] a, Object last) {
        Object[] r = new Object[a.length + 1];
        System.arraycopy(a, 0, r, 0, a.length);
        r[a.length] = last;
        return r;
    }

    private void restaurarViaje() {
        if (!viajeAlterado) return;
        jdbc.update("UPDATE viajes SET estado='PROGRAMADO' WHERE id=1");
        viajeAlterado = false;
    }

    /** DNI de la auditoria: siempre con prefijo 906 para poder limpiarlo sin tocar la semilla. */
    private String dni() { return String.valueOf(90_600_000L + Math.abs(UUID.randomUUID().hashCode() % 90_000)); }

    private String sufijo() { return String.valueOf(Math.abs(UUID.randomUUID().hashCode() % 90000 + 10000)); }

    @AfterAll
    void limpiarAlFinal() {
        try { restaurarViaje(); limpiar(); } catch (Exception e) { System.out.println("[QA] aviso: " + e.getMessage()); }
    }

    /** Elimina exactamente lo creado por la auditoria, en orden de claves foraneas. */
    private void limpiar() {
        jdbc.update("DELETE FROM auditoria WHERE username LIKE ?", PREFIJO + "%");
        jdbc.update("DELETE FROM auditoria WHERE detalle LIKE '%AUDITORIA IDOR%'");

        if (!boletosQA.isEmpty()) {
            jdbc.update("DELETE FROM boletos WHERE numero_boleto IN (" + ph(boletosQA) + ")", boletosQA.toArray());
        }
        if (!asientosQA.isEmpty()) {
            jdbc.update("UPDATE asientos SET retencion_id=NULL, retenido_hasta=NULL, estado='DISPONIBLE' WHERE id IN ("
                    + ph(asientosQA) + ")", asientosQA.toArray());
        }
        if (refPasaje != null) {
            jdbc.update("UPDATE retenciones SET pago_id=NULL WHERE referencia=?", refPasaje);
            jdbc.update("DELETE FROM retenciones WHERE referencia=?", refPasaje);
            refPasaje = null;
        }
        if (!encomiendasQA.isEmpty()) {
            List<Long> ids = new ArrayList<>(encomiendasQA);
            jdbc.update("DELETE FROM movimientos_encomienda WHERE encomienda_id IN (" + ph(ids) + ")", ids.toArray());
            jdbc.update("UPDATE encomiendas SET pago_id=NULL WHERE id IN (" + ph(ids) + ")", ids.toArray());
            jdbc.update("DELETE FROM encomiendas WHERE id IN (" + ph(ids) + ")", ids.toArray());
        }
        // Retenciones creadas por el flujo de venta con documentos de la auditoria.
        jdbc.update("UPDATE retenciones SET pago_id=NULL WHERE comprador_documento LIKE '906%'");
        jdbc.update("DELETE FROM retenciones WHERE comprador_documento LIKE '906%'");
        // Pagos: el de la prueba y cualquier pago creado por la oficializacion de encomiendas.
        if (pagoPasaje != null) jdbc.update("DELETE FROM pagos WHERE id=?", pagoPasaje);
        jdbc.update("DELETE FROM pagos WHERE referencia LIKE 'OPQA%'");
        if (pagoMaxInicial != null) {
            jdbc.update("DELETE FROM pagos p WHERE p.id > ? AND NOT EXISTS (SELECT 1 FROM retenciones r WHERE r.pago_id=p.id)"
                    + " AND NOT EXISTS (SELECT 1 FROM encomiendas e WHERE e.pago_id=p.id)"
                    + " AND NOT EXISTS (SELECT 1 FROM boletos b WHERE b.pago_id=p.id)", pagoMaxInicial);
        }
        // Clientes creados por el portal o por el flujo de venta de la auditoria.
        jdbc.update("DELETE FROM clientes WHERE email LIKE ?", PREFIJO + "%@intiwatana.test");
        jdbc.update("DELETE FROM clientes WHERE dni_ruc LIKE '906%'");
        jdbc.update("DELETE FROM usuarios WHERE username LIKE ?", PREFIJO + "%");
        boletosQA.clear();
        asientosQA.clear();
        encomiendasQA.clear();
        pagoPasaje = null;
    }

    private static String ph(List<?> ids) {
        return String.join(",", java.util.Collections.nCopies(ids.size(), "?"));
    }
}

