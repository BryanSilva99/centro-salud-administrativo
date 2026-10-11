package pe.centrosalud.unit;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.Test;
import pe.centrosalud.modules.administration.application.ports.Consultorios;
import pe.centrosalud.modules.administration.application.ports.Servicios;
import pe.centrosalud.modules.administration.application.usecases.ErrorConsultorios;
import pe.centrosalud.modules.administration.application.usecases.GestionarConsultorios;
import pe.centrosalud.modules.administration.domain.Consultorio;
import pe.centrosalud.modules.administration.domain.Modalidad;
import pe.centrosalud.modules.administration.domain.Servicio;

class GestionarConsultoriosTest {
  private static final UUID ACTOR = UUID.randomUUID();
  private final UUID servicioExistente = UUID.randomUUID();
  private final Consultorio existente =
      new Consultorio(
          UUID.randomUUID(), "C-1", "Consultorio 1", true, 3, Set.of(servicioExistente));

  private class ConsultoriosFalsos implements Consultorios {
    Consultorio creado, actualizado;

    @Override
    public List<Consultorio> listar() {
      return List.of(existente);
    }

    @Override
    public Optional<Consultorio> buscarPorId(UUID id) {
      return Optional.of(existente).filter(c -> c.id().equals(id));
    }

    @Override
    public Consultorio crear(Consultorio nuevo, UUID autor) {
      creado = nuevo;
      return nuevo;
    }

    @Override
    public Consultorio actualizar(Consultorio anterior, Consultorio nuevo, UUID autor) {
      actualizado = nuevo;
      return nuevo;
    }
  }

  private class ServiciosFalsos implements Servicios {
    @Override
    public List<Servicio> listar() {
      return List.of();
    }

    @Override
    public Optional<Servicio> buscarPorId(UUID id) {
      if (!id.equals(servicioExistente)) return Optional.empty();
      var modalidad =
          new Modalidad(
              UUID.randomUUID(), "Particular", true, new java.math.BigDecimal("40.00"), true);
      return Optional.of(new Servicio(id, "Consulta", 30, true, 0, List.of(modalidad)));
    }

    @Override
    public Servicio crear(Servicio nuevo, UUID autor) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Servicio actualizar(Servicio anterior, Servicio nuevo, UUID autor) {
      throw new UnsupportedOperationException();
    }
  }

  private ErrorConsultorios.Codigo codigo(Runnable accion) {
    return assertThrows(ErrorConsultorios.class, accion::run).codigo();
  }

  @Test
  void creaActivoConVersionInicial() {
    var puerto = new ConsultoriosFalsos();
    new GestionarConsultorios(puerto, new ServiciosFalsos())
        .crear(ACTOR, "C-2", "Consultorio 2", Set.of(servicioExistente));
    assertTrue(puerto.creado.activo());
    assertEquals(0, puerto.creado.version());
    assertEquals(Set.of(servicioExistente), puerto.creado.serviciosCompatibles());
  }

  @Test
  void rechazaServicioInexistenteAlCrearYAlActualizar() {
    var gestionar = new GestionarConsultorios(new ConsultoriosFalsos(), new ServiciosFalsos());
    UUID ajeno = UUID.randomUUID();
    assertEquals(
        ErrorConsultorios.Codigo.SERVICIO_INVALIDO,
        codigo(() -> gestionar.crear(ACTOR, "C-2", "Consultorio 2", Set.of(ajeno))));
    assertEquals(
        ErrorConsultorios.Codigo.SERVICIO_INVALIDO,
        codigo(
            () ->
                gestionar.actualizar(
                    ACTOR, existente.id(), "C-1", "Consultorio 1", true, Set.of(ajeno), 3)));
  }

  @Test
  void edicionRechazaVersionObsoletaYConsultorioInexistente() {
    var gestionar = new GestionarConsultorios(new ConsultoriosFalsos(), new ServiciosFalsos());
    assertEquals(
        ErrorConsultorios.Codigo.CONFLICTO,
        codigo(
            () ->
                gestionar.actualizar(
                    ACTOR,
                    existente.id(),
                    "C-1",
                    "Consultorio 1",
                    true,
                    Set.of(servicioExistente),
                    2)));
    assertEquals(
        ErrorConsultorios.Codigo.NO_EXISTE,
        codigo(
            () ->
                gestionar.actualizar(
                    ACTOR, UUID.randomUUID(), "C-1", "Consultorio 1", true, Set.of(), 0)));
  }

  @Test
  void guardadoIdenticoNoEscribe() {
    var puerto = new ConsultoriosFalsos();
    var resultado =
        new GestionarConsultorios(puerto, new ServiciosFalsos())
            .actualizar(
                ACTOR,
                existente.id(),
                " C-1 ",
                " Consultorio 1 ",
                true,
                Set.of(servicioExistente),
                3);
    assertEquals(existente, resultado);
    assertNull(puerto.actualizado);
  }

  @Test
  void edicionCambiaDatosYDesactiva() {
    var puerto = new ConsultoriosFalsos();
    new GestionarConsultorios(puerto, new ServiciosFalsos())
        .actualizar(ACTOR, existente.id(), "C-1", "Consultorio renombrado", false, Set.of(), 3);
    assertEquals("Consultorio renombrado", puerto.actualizado.nombre());
    assertFalse(puerto.actualizado.activo());
    assertEquals(Set.of(), puerto.actualizado.serviciosCompatibles());
  }
}
