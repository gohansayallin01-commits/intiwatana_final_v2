package com.transporte.sistema.config;

import com.transporte.sistema.entity.*;
import com.transporte.sistema.enums.EstadoAsiento;
import com.transporte.sistema.enums.RolNombre;
import com.transporte.sistema.enums.TipoAsiento;
import com.transporte.sistema.enums.EstadoViaje;
import com.transporte.sistema.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Carga datos iniciales al arrancar la aplicacion.
 * - Crea roles, sucursal matriz y admin si no existen.
 * - Si hay buses y rutas en BD pero NO hay viajes activos,
 *   genera viajes de prueba automáticamente (útil en dev).
 */
@Slf4j
@Component
@Profile("!prod")
@RequiredArgsConstructor
@SuppressWarnings("DataFlowIssue")
public class DataInitializer implements CommandLineRunner {

    private final RolRepository      rolRepository;
    private final UsuarioRepository  usuarioRepository;
    private final SucursalRepository sucursalRepository;
    private final BusRepository      busRepository;
    private final RutaRepository     rutaRepository;
    private final ViajeRepository    viajeRepository;
    private final AsientoRepository  asientoRepository;
    private final PasswordEncoder    passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        crearRoles();
        crearSucursalInicial();
        crearAdminInicial();
        crearPersonalInicial();
        crearViajesDePrueba();
    }

    // ── Roles ────────────────────────────────────────────────────────

    private void crearRoles() {
        crearRolSiNoExiste(RolNombre.ROLE_ADMIN, "Administrador del sistema");
        crearRolSiNoExiste(RolNombre.ROLE_CAJERO, "Cajero de sucursal (rol legacy)");
        crearRolSiNoExiste(RolNombre.ROLE_VENDEDOR, "Vendedor de boletos y atención en sucursal");
        crearRolSiNoExiste(RolNombre.ROLE_OPERADOR_ENCOMIENDAS, "Operador de encomiendas");
        crearRolSiNoExiste(RolNombre.ROLE_CHOFER, "Chofer de bus");
        crearRolSiNoExiste(RolNombre.ROLE_CLIENTE, "Cliente portal");
    }

    /** Crea un rol únicamente si no existe aún → ejecución idempotente y segura sobre BD pobladas. */
    private void crearRolSiNoExiste(RolNombre nombre, String descripcion) {
        if (rolRepository.findByNombre(nombre).isEmpty()) {
            rolRepository.save(Rol.builder().nombre(nombre).descripcion(descripcion).build());
            log.info("Rol creado: {}", nombre);
        }
    }

    // ── Sucursal matriz ──────────────────────────────────────────────

    private void crearSucursalInicial() {
        // Crear sucursales de Ayacucho si no existen (por código)
        java.util.List<Object[]> sucData = java.util.List.of(
            new Object[]{"AYA-01","Terminal Huamanga",      "Huamanga",    "Huamanga",      "Ayacucho","Jr. Libertad 120"},
            new Object[]{"VIL-01","Terminal Vilcas Huamán", "Vilcas Huaman","Vilcas Huamán","Ayacucho","Plaza Principal s/n"},
            new Object[]{"ACC-01","Agencia Accomarca",       "Accomarca",   "Vilcas Huamán","Ayacucho","Av. Principal s/n"},
            new Object[]{"HUA-01","Terminal Huarcas",        "Huarcas",     "Huamanga",     "Ayacucho","Jr. San Martín 45"},
            new Object[]{"CAN-01","Agencia Canaria",         "Canaria",     "Víctor Fajardo","Ayacucho","Jr. Grau 12"},
            new Object[]{"FAJ-01","Terminal Huancapi",       "Huancapi",    "Víctor Fajardo","Ayacucho","Plaza de Armas s/n"}
        );
        for (Object[] d : sucData) {
            String codigo = (String) d[0];
            if (!sucursalRepository.existsByCodigo(codigo)) {
                sucursalRepository.save(Sucursal.builder()
                    .codigo(codigo).nombre((String)d[1]).ciudad((String)d[2])
                    .provincia((String)d[3]).departamento((String)d[4])
                    .direccion((String)d[5]).esTerminal(true).activo(true).build());
                log.info("Sucursal creada: {} - {}", codigo, d[1]);
            }
        }
        if (sucursalRepository.count() == 0) {
            sucursalRepository.save(Sucursal.builder()
                    .codigo("MATRIZ")
                    .nombre("Terminal Principal - Huamanga")
                    .ciudad("Huamanga")
                    .departamento("Ayacucho")
                    .esTerminal(true)
                    .activo(true)
                    .build());
            log.info("Sucursal matriz creada");
        }
    }

    // ── Admin ────────────────────────────────────────────────────────

    private void crearAdminInicial() {
        if (!usuarioRepository.existsByUsername("admin")) {
            Rol rolAdmin = rolRepository.findByNombre(RolNombre.ROLE_ADMIN)
                    .orElseThrow(() -> new RuntimeException("Rol ADMIN no encontrado"));
            Sucursal sucursal = sucursalRepository.findAll().stream()
                    .filter(s -> Boolean.TRUE.equals(s.getActivo()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("No hay sucursal disponible"));

            usuarioRepository.save(Usuario.builder()
                    .username("admin")
                    .passwordHash(passwordEncoder.encode("admin123"))
                    .nombres("Administrador")
                    .apellidos("Sistema")
                    .email("admin@transporte.com")
                    .rol(rolAdmin)
                    .sucursal(sucursal)
                    .primerLogin(false)
                    .activo(true)
                    .build());

            log.info("Usuario admin creado (username=admin)");
            log.warn("CAMBIE LA CONTRASENA DEL ADMIN EN PRODUCCION");
        }
    }

    // ── Personal inicial (cajero, vendedor, operador y choferes) ─────────────
    private void crearPersonalInicial() {
        // Sin guard por COUNT: cada usuario se crea con su propio existsByUsername,
        // de modo que los nuevos roles se siembran también sobre BD ya pobladas.
        Rol rolCajero   = rolRepository.findByNombre(RolNombre.ROLE_CAJERO).orElse(null);
        Rol rolChofer   = rolRepository.findByNombre(RolNombre.ROLE_CHOFER).orElse(null);
        Rol rolVendedor = rolRepository.findByNombre(RolNombre.ROLE_VENDEDOR).orElse(null);
        Rol rolOperador = rolRepository.findByNombre(RolNombre.ROLE_OPERADOR_ENCOMIENDAS).orElse(null);
        Sucursal suc    = sucursalRepository.findAll().stream().filter(s -> Boolean.TRUE.equals(s.getActivo())).findFirst().orElse(null);

        crearUsuarioSiNoExiste("cajero1", "cajero123", "María", "Quispe Huamán",
                "cajero@intiwatana.pe", "966001122", "12345678", rolCajero, suc);
        crearUsuarioSiNoExiste("chofer1", "chofer123", "Carlos", "Flores Condori",
                "chofer1@intiwatana.pe", "955002233", "87654321", rolChofer, suc);
        crearUsuarioSiNoExiste("chofer2", "chofer123", "Pedro", "Condori Mamani",
                "chofer2@intiwatana.pe", "944003344", "76543210", rolChofer, suc);
        crearUsuarioSiNoExiste("chofer3", "chofer123", "Luis", "Huamán Palomino",
                "chofer3@intiwatana.pe", "933004455", "65432109", rolChofer, suc);
        crearUsuarioSiNoExiste("vendedor1", "vendedor123", "Rosa", "Prado Quispe",
                "vendedor@intiwatana.pe", "977001122", "11223344", rolVendedor, suc);
        crearUsuarioSiNoExiste("operador1", "operador123", "Jhon", "Ochoa Sáenz",
                "operador@intiwatana.pe", "988002233", "55667788", rolOperador, suc);
        log.info("Personal básico verificado (cajero1, chofer1-3, vendedor1, operador1)");
    }

    private void crearUsuarioSiNoExiste(String username, String password, String nombres,
                                        String apellidos, String email, String telefono,
                                        String dniRuc, Rol rol, Sucursal suc) {
        if (rol == null || suc == null) return;
        if (!usuarioRepository.existsByUsername(username)) {
            usuarioRepository.save(Usuario.builder()
                    .username(username)
                    .passwordHash(passwordEncoder.encode(password))
                    .nombres(nombres)
                    .apellidos(apellidos)
                    .email(email)
                    .telefono(telefono)
                    .dniRuc(dniRuc)
                    .rol(rol)
                    .sucursal(suc)
                    .primerLogin(false)
                    .activo(true)
                    .build());
            log.info("Usuario creado: {}", username);
        }
    }

    // ── Viajes de prueba ─────────────────────────────────────────────

    /**
     * Si hay buses y rutas en BD pero la tabla viajes está vacía,
     * genera viajes de prueba con fechas futuras para que aparezcan
     * en el portal y en el dashboard admin.
     */
    @Transactional
    public void crearViajesDePrueba() {
        long totalViajes = viajeRepository.count();
        if (totalViajes > 0) {
            log.info("Ya existen {} viaje(s) en BD — no se generan viajes de prueba", totalViajes);
            return;
        }

        List<Bus>  buses = busRepository.findAll().stream()
                .filter(b -> !Boolean.FALSE.equals(b.getActivo()))
                .toList();
        List<Ruta> rutas = rutaRepository.findAll().stream()
                .filter(r -> !Boolean.FALSE.equals(r.getActivo()))
                .toList();

        if (buses.isEmpty() || rutas.isEmpty()) {
            log.warn("Sin buses ({}) o rutas ({}) — no se pueden generar viajes de prueba. " +
                     "Crea al menos 1 bus y 1 ruta desde el panel admin para que se generen automáticamente.",
                    buses.size(), rutas.size());
            return;
        }

        // Precios por defecto si la ruta no tiene precio base
        record ViajeConfig(int diasOffset, String hora, BigDecimal precioAdulto, BigDecimal precioNino) {}

        List<ViajeConfig> configs = List.of(
                new ViajeConfig(1, "06:00", null, null),
                new ViajeConfig(1, "14:00", null, null),
                new ViajeConfig(2, "07:30", null, null),
                new ViajeConfig(3, "06:00", null, null),
                new ViajeConfig(4, "08:00", null, null),
                new ViajeConfig(5, "07:00", null, null)
        );

        int busIdx    = 0;
        int configIdx = 0;
        int creados   = 0;

        for (Ruta ruta : rutas) {
            // 1-2 viajes por ruta
            int viajesPorRuta = (creados % 3 == 0) ? 2 : 1;

            for (int v = 0; v < viajesPorRuta; v++) {
                ViajeConfig cfg = configs.get(configIdx % configs.size());
                Bus bus = buses.get(busIdx % buses.size());

                String[] partes   = cfg.hora().split(":");
                LocalDateTime salida = LocalDate.now()
                        .plusDays(cfg.diasOffset() + v)
                        .atTime(Integer.parseInt(partes[0]), Integer.parseInt(partes[1]));

                // Duración estimada basada en la ruta
                double horas = ruta.getDuracionHorasEstimada() != null
                        ? ruta.getDuracionHorasEstimada().doubleValue() : 3.0;
                LocalDateTime llegada = salida.plusMinutes((long)(horas * 60));

                // Precio: usar precio base de la ruta o un valor por defecto
                BigDecimal precioBase = ruta.getPrecioBase() != null
                        ? ruta.getPrecioBase() : BigDecimal.valueOf(20.00);
                BigDecimal precioNino = precioBase.multiply(BigDecimal.valueOf(0.7))
                        .setScale(2, java.math.RoundingMode.HALF_UP);

                Viaje viaje = Viaje.builder()
                        .ruta(ruta)
                        .bus(bus)
                        .chofer(null)
                        .fechaHoraSalida(salida)
                        .fechaHoraLlegadaEstimada(llegada)
                        .precioAdulto(precioBase)
                        .precioNino(precioNino)
                        .estado(EstadoViaje.PROGRAMADO)
                        .activo(true)
                        .build();

                viaje = viajeRepository.save(viaje);
                generarAsientos(viaje, bus);

                busIdx++;
                configIdx++;
                creados++;
            }
        }

        log.info("Viajes de prueba creados: {}", creados);
    }

    private void generarAsientos(Viaje viaje, Bus bus) {
        int total    = bus.getCapacidadAsientos();
        int columnas = 4;
        List<Asiento> lista = new ArrayList<>(total);

        for (int i = 1; i <= total; i++) {
            int fila = (int) Math.ceil((double) i / columnas);
            int col  = ((i - 1) % columnas) + 1;

            TipoAsiento tipo = (col == 1 || col == 4) ? TipoAsiento.VENTANA : TipoAsiento.PASILLO;
            String letra = switch (col) {
                case 1 -> "A"; case 2 -> "B"; case 3 -> "C"; case 4 -> "D";
                default -> String.valueOf(col);
            };

            lista.add(Asiento.builder()
                    .viaje(viaje)
                    .numeroAsiento(fila + letra)
                    .fila(fila)
                    .columna(col)
                    .piso(1)
                    .tipo(tipo)
                    .estado(EstadoAsiento.DISPONIBLE)
                    .activo(true)
                    .build());
        }
        asientoRepository.saveAll(lista);
    }
}
