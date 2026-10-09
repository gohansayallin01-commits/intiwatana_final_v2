package com.transporte.sistema.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transporte.sistema.dto.ConfiguracionAsientos;
import com.transporte.sistema.enums.TipoAsiento;
import com.transporte.sistema.exception.NegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de la derivacion y validacion de la configuracion de asientos por
 * vehiculo: la cantidad de asientos generada debe coincidir siempre con la
 * capacidad declarada del bus.
 */
class ConfiguracionAsientosServiceTest {

    private final ConfiguracionAsientosService service =
            new ConfiguracionAsientosService(new ObjectMapper());

    private ConfiguracionAsientos de40() {
        return new ConfiguracionAsientos("BUS_40", 2,
                List.of(new ConfiguracionAsientos.PisoConfig(1,
                        List.of(4, 4, 4, 4, 4, 4, 4, 4, 4, 4))));
    }

    @Test
    @DisplayName("Un bus de 40 genera exactamente 40 asientos con filas y columnas coherentes")
    void generaCuarentaAsientos() {
        List<ConfiguracionAsientosService.PosicionAsiento> posiciones =
                service.generarPosiciones(de40());

        assertEquals(40, posiciones.size());

        ConfiguracionAsientosService.PosicionAsiento primera = posiciones.get(0);
        assertEquals(1, primera.fila());
        assertEquals(1, primera.columna());
        assertEquals("1A", primera.numeroAsiento());
        assertEquals(TipoAsiento.VENTANA, primera.tipo());

        ConfiguracionAsientosService.PosicionAsiento ultima = posiciones.get(39);
        assertEquals(10, ultima.fila());
        assertEquals(4, ultima.columna());
        assertEquals("10D", ultima.numeroAsiento());
        assertEquals(TipoAsiento.VENTANA, ultima.tipo());
    }

    @Test
    @DisplayName("Las columnas interiores son pasillo y las exteriores ventana")
    void clasificaVentanaYPasillo() {
        List<ConfiguracionAsientosService.PosicionAsiento> posiciones =
                service.generarPosiciones(de40());

        assertEquals(TipoAsiento.VENTANA, posiciones.get(0).tipo());
        assertEquals(TipoAsiento.PASILLO, posiciones.get(1).tipo());
        assertEquals(TipoAsiento.PASILLO, posiciones.get(2).tipo());
        assertEquals(TipoAsiento.VENTANA, posiciones.get(3).tipo());
    }

    @Test
    @DisplayName("La numeracion de fila continua entre pisos para no repetir numero de asiento")
    void numeraFilasGlobalmenteEntrePisos() {
        ConfiguracionAsientos config = service.generarPorCapacidad(28, 2, "AUTO");
        List<ConfiguracionAsientosService.PosicionAsiento> posiciones =
                service.generarPosiciones(config);

        assertEquals(28, posiciones.size());
        assertEquals(2, config.numeroPisos());
        assertEquals(1, posiciones.get(0).piso());
        assertEquals(1, posiciones.get(13).piso());
        assertEquals(2, posiciones.get(14).piso());
        assertEquals(2, posiciones.get(posiciones.size() - 1).piso());
        long filasDistintas = posiciones.stream()
                .map(ConfiguracionAsientosService.PosicionAsiento::numeroAsiento).distinct().count();
        assertEquals(28, filasDistintas);
    }

    @Test
    @DisplayName("Se considera COMPLETO solo si los asientos coinciden con la capacidad")
    void validaCompletitud() {
        assertTrue(service.estaCompleta(de40(), 40));
        assertEquals(ConfiguracionAsientosService.ESTADO_COMPLETO, service.estado(de40(), 40));

        assertFalse(service.estaCompleta(de40(), 36));
        assertEquals(ConfiguracionAsientosService.ESTADO_INCOMPLETO, service.estado(de40(), 36));
        assertFalse(service.estaCompleta(null, 40));
    }

    @Test
    @DisplayName("Guardar una configuracion que no coincide con la capacidad es rechazado")
    void rechazaConfiguracionIncoherente() {
        assertThrows(NegocioException.class,
                () -> service.validarContraCapacidad(de40(), 36));
    }

    @Test
    @DisplayName("Una fila con mas de 6 asientos o sin asientos es invalida")
    void rechazaEstructuraInvalida() {
        ConfiguracionAsientos demasiadasColumnas = new ConfiguracionAsientos("X", 2,
                List.of(new ConfiguracionAsientos.PisoConfig(1, List.of(7, 1))));
        assertThrows(NegocioException.class,
                () -> service.validarEstructura(demasiadasColumnas));

        ConfiguracionAsientos filaVacia = new ConfiguracionAsientos("X", 2,
                List.of(new ConfiguracionAsientos.PisoConfig(1, List.of(0))));
        assertThrows(NegocioException.class,
                () -> service.validarEstructura(filaVacia));
    }

    @Test
    @DisplayName("La configuracion se serializa y se vuelve a leer sin perdida")
    void serializaYParsea() {
        String json = service.serialize(de40());
        ConfiguracionAsientos leida = service.parse(json);

        assertNotNull(leida);
        assertEquals(40, leida.totalAsientos());
        assertEquals("BUS_40", leida.plantilla());
        assertEquals(2, leida.pasillo());
    }
}
