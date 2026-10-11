package pe.centrosalud.unit;

import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import pe.centrosalud.modules.administration.application.ports.Servicios;
import pe.centrosalud.modules.administration.application.usecases.ErrorServicios;
import pe.centrosalud.modules.administration.application.usecases.GestionarServicios;
import pe.centrosalud.modules.administration.application.usecases.GestionarServicios.DatosModalidad;
import pe.centrosalud.modules.administration.domain.Modalidad;
import pe.centrosalud.modules.administration.domain.Servicio;
import static org.junit.jupiter.api.Assertions.*;

class GestionarServiciosTest {
    private static final UUID ACTOR = UUID.randomUUID();
    private final Modalidad particular = new Modalidad(UUID.randomUUID(), "Particular", true, new BigDecimal("40.00"), true);
    private final Servicio existente = new Servicio(UUID.randomUUID(), "Consulta", 30, true, 3, List.of(particular));

    private class ServiciosFalsos implements Servicios {
        Servicio creado, actualizado;
        @Override public List<Servicio> listar() { return List.of(existente); }
        @Override public Optional<Servicio> buscarPorId(UUID id) { return Optional.of(existente).filter(s -> s.id().equals(id)); }
        @Override public Servicio crear(Servicio nuevo, UUID autor) { creado = nuevo; return nuevo; }
        @Override public Servicio actualizar(Servicio anterior, Servicio nuevo, UUID autor) { actualizado = nuevo; return nuevo; }
    }
    private static DatosModalidad datos(UUID id, String nombre, boolean conCargo, String tarifa, boolean activa) {
        return new DatosModalidad(id, nombre, conCargo, new BigDecimal(tarifa), activa);
    }
    private ErrorServicios.Codigo codigo(Runnable accion) {
        return assertThrows(ErrorServicios.class, accion::run).codigo();
    }

    @Test void creaActivoConVersionInicialYModalidadesNuevas() {
        var puerto = new ServiciosFalsos();
        new GestionarServicios(puerto).crear(ACTOR, "Consulta", 30,
                List.of(datos(null, "Particular", true, "40", true), datos(null, "Campaña", false, "0", true)));
        assertTrue(puerto.creado.activo());
        assertEquals(0, puerto.creado.version());
        assertEquals(2, puerto.creado.modalidades().size());
        assertEquals(ErrorServicios.Codigo.MODALIDAD_INVALIDA, codigo(() -> new GestionarServicios(puerto).crear(ACTOR, "Otro", 30,
                List.of(datos(UUID.randomUUID(), "Particular", true, "40", true)))));
    }

    @Test void edicionRechazaVersionObsoletaYServicioInexistente() {
        var gestionar = new GestionarServicios(new ServiciosFalsos());
        var modalidades = List.of(datos(particular.id(), "Particular", true, "45", true));
        assertEquals(ErrorServicios.Codigo.CONFLICTO, codigo(() -> gestionar.actualizar(ACTOR, existente.id(), "Consulta", 30, true, modalidades, 2)));
        assertEquals(ErrorServicios.Codigo.NO_EXISTE, codigo(() -> gestionar.actualizar(ACTOR, UUID.randomUUID(), "Consulta", 30, true, modalidades, 3)));
    }

    @Test void noPermiteQuitarRepetirNiTomarModalidadesAjenas() {
        var gestionar = new GestionarServicios(new ServiciosFalsos());
        assertEquals(ErrorServicios.Codigo.MODALIDAD_INVALIDA, codigo(() -> gestionar.actualizar(ACTOR, existente.id(), "Consulta", 30, true,
                List.of(datos(null, "Nueva", true, "40", true)), 3)));
        assertEquals(ErrorServicios.Codigo.MODALIDAD_INVALIDA, codigo(() -> gestionar.actualizar(ACTOR, existente.id(), "Consulta", 30, true,
                List.of(datos(particular.id(), "Particular", true, "40", true), datos(UUID.randomUUID(), "Ajena", true, "40", true)), 3)));
        assertEquals(ErrorServicios.Codigo.MODALIDAD_INVALIDA, codigo(() -> gestionar.actualizar(ACTOR, existente.id(), "Consulta", 30, true,
                List.of(datos(particular.id(), "Particular", true, "40", true), datos(particular.id(), "Otra", true, "40", true)), 3)));
    }

    @Test void desactivaModalidadAgregaOtraYCambiaTarifa() {
        var puerto = new ServiciosFalsos();
        new GestionarServicios(puerto).actualizar(ACTOR, existente.id(), "Consulta", 45, true,
                List.of(datos(particular.id(), "Particular", true, "40", false), datos(null, "Convenio", true, "35.5", true)), 3);
        assertEquals(45, puerto.actualizado.duracionMinutos());
        var convenio = puerto.actualizado.modalidades().stream().filter(m -> m.nombre().equals("Convenio")).findFirst().orElseThrow();
        assertEquals(new BigDecimal("35.50"), convenio.tarifa());
        assertNotNull(convenio.id());
    }

    @Test void guardadoIdenticoNoEscribe() {
        var puerto = new ServiciosFalsos();
        var resultado = new GestionarServicios(puerto).actualizar(ACTOR, existente.id(), " Consulta ", 30, true,
                List.of(datos(particular.id(), "Particular", true, "40.0", true)), 3);
        assertSame(existente, resultado);
        assertNull(puerto.actualizado);
    }
}
