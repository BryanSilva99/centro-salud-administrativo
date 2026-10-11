package pe.centrosalud.unit;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.Test;
import pe.centrosalud.modules.administration.application.ports.FichasProfesionales;
import pe.centrosalud.modules.administration.application.ports.Servicios;
import pe.centrosalud.modules.administration.application.usecases.ErrorProfesionales;
import pe.centrosalud.modules.administration.application.usecases.GestionarProfesionales;
import pe.centrosalud.modules.administration.domain.FichaProfesional;
import pe.centrosalud.modules.administration.domain.Modalidad;
import pe.centrosalud.modules.administration.domain.Servicio;

class GestionarProfesionalesTest {
  private static final UUID ACTOR = UUID.randomUUID();
  private final UUID servicioExistente = UUID.randomUUID();
  private final FichaProfesional existente =
      new FichaProfesional(
          UUID.randomUUID(),
          "P-01",
          "Profesional de prueba",
          "Medicina general",
          true,
          3,
          Set.of(servicioExistente));

  private class FichasFalsas implements FichasProfesionales {
    FichaProfesional creada, actualizada;

    @Override
    public List<FichaProfesional> buscar(String texto) {
      return List.of(existente);
    }

    @Override
    public Optional<FichaProfesional> buscarPorId(UUID id) {
      return Optional.of(existente).filter(f -> f.id().equals(id));
    }

    @Override
    public FichaProfesional registrar(FichaProfesional ficha, UUID autor) {
      creada = ficha;
      return ficha;
    }

    @Override
    public FichaProfesional actualizar(
        FichaProfesional anterior, FichaProfesional nuevo, UUID autor) {
      actualizada = nuevo;
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

  private ErrorProfesionales.Codigo codigo(Runnable accion) {
    return assertThrows(ErrorProfesionales.class, accion::run).codigo();
  }

  @Test
  void creaActivaConVersionInicialYServiciosHabilitados() {
    var puerto = new FichasFalsas();
    new GestionarProfesionales(puerto, new ServiciosFalsos())
        .crear(ACTOR, "P-02", "Otro", "Enfermería", Set.of(servicioExistente));
    assertTrue(puerto.creada.activa());
    assertEquals(0, puerto.creada.version());
    assertEquals(Set.of(servicioExistente), puerto.creada.serviciosHabilitados());
  }

  @Test
  void creaSinServiciosHabilitadosCuandoNoSeEnvianNinguno() {
    var puerto = new FichasFalsas();
    new GestionarProfesionales(puerto, new ServiciosFalsos())
        .crear(ACTOR, "P-02", "Otro", "Enfermería", null);
    assertEquals(Set.of(), puerto.creada.serviciosHabilitados());
  }

  @Test
  void rechazaServicioInexistenteAlCrearYAlActualizar() {
    var gestionar = new GestionarProfesionales(new FichasFalsas(), new ServiciosFalsos());
    UUID ajeno = UUID.randomUUID();
    assertEquals(
        ErrorProfesionales.Codigo.SERVICIO_INVALIDO,
        codigo(() -> gestionar.crear(ACTOR, "P-02", "Otro", "Enfermería", Set.of(ajeno))));
    assertEquals(
        ErrorProfesionales.Codigo.SERVICIO_INVALIDO,
        codigo(
            () ->
                gestionar.actualizar(
                    ACTOR, existente.id(), "P-01", "Nombre", "Funcion", true, Set.of(ajeno), 3)));
  }

  @Test
  void edicionRechazaVersionObsoletaYFichaInexistente() {
    var gestionar = new GestionarProfesionales(new FichasFalsas(), new ServiciosFalsos());
    var servicios = Set.of(servicioExistente);
    assertEquals(
        ErrorProfesionales.Codigo.CONFLICTO,
        codigo(
            () ->
                gestionar.actualizar(
                    ACTOR, existente.id(), "P-01", "Nombre", "Funcion", true, servicios, 2)));
    assertEquals(
        ErrorProfesionales.Codigo.NO_EXISTE,
        codigo(
            () ->
                gestionar.actualizar(
                    ACTOR, UUID.randomUUID(), "P-01", "Nombre", "Funcion", true, Set.of(), 0)));
  }

  @Test
  void guardadoIdenticoNoEscribe() {
    var puerto = new FichasFalsas();
    var resultado =
        new GestionarProfesionales(puerto, new ServiciosFalsos())
            .actualizar(
                ACTOR,
                existente.id(),
                " P-01 ",
                " Profesional de prueba ",
                " Medicina general ",
                true,
                Set.of(servicioExistente),
                3);
    assertEquals(existente, resultado);
    assertNull(puerto.actualizada);
  }

  @Test
  void edicionCambiaDatosYDesactiva() {
    var puerto = new FichasFalsas();
    new GestionarProfesionales(puerto, new ServiciosFalsos())
        .actualizar(
            ACTOR,
            existente.id(),
            "P-01",
            "Nombre renovado",
            "Medicina general",
            false,
            Set.of(),
            3);
    assertEquals("Nombre renovado", puerto.actualizada.nombre());
    assertFalse(puerto.actualizada.activa());
    assertEquals(Set.of(), puerto.actualizada.serviciosHabilitados());
  }
}
