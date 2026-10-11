package pe.centrosalud.unit;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import org.junit.jupiter.api.Test;
import pe.centrosalud.modules.administration.application.ports.Consultorios;
import pe.centrosalud.modules.administration.application.ports.Disponibilidad;
import pe.centrosalud.modules.administration.application.ports.FichasProfesionales;
import pe.centrosalud.modules.administration.application.usecases.ErrorDisponibilidad;
import pe.centrosalud.modules.administration.application.usecases.GestionarDisponibilidad;
import pe.centrosalud.modules.administration.domain.*;

class GestionarDisponibilidadTest {
  private static final UUID ACTOR = UUID.randomUUID();
  private final UUID profesionalExistente = UUID.randomUUID();
  private final UUID consultorioExistente = UUID.randomUUID();

  private class DisponibilidadFalsa implements Disponibilidad {
    Turno creado;
    UUID desactivado;

    @Override
    public List<Turno> listar(TipoRecurso tipo, UUID recursoId) {
      return List.of();
    }

    @Override
    public Turno crear(TipoRecurso tipo, Turno nuevo, UUID autor) {
      creado = nuevo;
      return nuevo;
    }

    @Override
    public void desactivar(TipoRecurso tipo, UUID turnoId, UUID autor) {
      desactivado = turnoId;
    }
  }

  private class FichasFalsas implements FichasProfesionales {
    @Override
    public List<FichaProfesional> buscar(String texto) {
      return List.of();
    }

    @Override
    public Optional<FichaProfesional> buscarPorId(UUID id) {
      return id.equals(profesionalExistente)
          ? Optional.of(new FichaProfesional(id, "P-01", "Nombre", "Funcion", true, 0, Set.of()))
          : Optional.empty();
    }

    @Override
    public FichaProfesional registrar(FichaProfesional ficha, UUID autor) {
      throw new UnsupportedOperationException();
    }

    @Override
    public FichaProfesional actualizar(FichaProfesional a, FichaProfesional n, UUID autor) {
      throw new UnsupportedOperationException();
    }
  }

  private class ConsultoriosFalsos implements Consultorios {
    @Override
    public List<Consultorio> listar() {
      return List.of();
    }

    @Override
    public Optional<Consultorio> buscarPorId(UUID id) {
      return id.equals(consultorioExistente)
          ? Optional.of(new Consultorio(id, "C-1", "Consultorio", true, 0, Set.of()))
          : Optional.empty();
    }

    @Override
    public Consultorio crear(Consultorio nuevo, UUID autor) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Consultorio actualizar(Consultorio a, Consultorio n, UUID autor) {
      throw new UnsupportedOperationException();
    }
  }

  @Test
  void creaUnTurnoParaUnProfesionalExistente() {
    var puerto = new DisponibilidadFalsa();
    var gestionar =
        new GestionarDisponibilidad(puerto, new FichasFalsas(), new ConsultoriosFalsos());
    gestionar.crear(
        ACTOR,
        TipoRecurso.PROFESIONAL,
        profesionalExistente,
        LocalDate.of(2026, 10, 15),
        LocalTime.of(8, 0),
        LocalTime.of(12, 0),
        TipoTurno.DISPONIBLE);
    assertEquals(profesionalExistente, puerto.creado.recursoId());
    assertTrue(puerto.creado.activo());
  }

  @Test
  void creaUnTurnoParaUnConsultorioExistente() {
    var puerto = new DisponibilidadFalsa();
    var gestionar =
        new GestionarDisponibilidad(puerto, new FichasFalsas(), new ConsultoriosFalsos());
    gestionar.crear(
        ACTOR,
        TipoRecurso.CONSULTORIO,
        consultorioExistente,
        LocalDate.of(2026, 10, 15),
        LocalTime.of(8, 0),
        LocalTime.of(12, 0),
        TipoTurno.DESCANSO);
    assertEquals(consultorioExistente, puerto.creado.recursoId());
  }

  @Test
  void rechazaRecursoInexistenteAntesDeEscribir() {
    var puerto = new DisponibilidadFalsa();
    var gestionar =
        new GestionarDisponibilidad(puerto, new FichasFalsas(), new ConsultoriosFalsos());
    var ajeno = UUID.randomUUID();
    var error =
        assertThrows(
            ErrorDisponibilidad.class,
            () ->
                gestionar.crear(
                    ACTOR,
                    TipoRecurso.PROFESIONAL,
                    ajeno,
                    LocalDate.of(2026, 10, 15),
                    LocalTime.of(8, 0),
                    LocalTime.of(12, 0),
                    TipoTurno.DISPONIBLE));
    assertEquals(ErrorDisponibilidad.Codigo.RECURSO_INVALIDO, error.codigo());
    assertNull(puerto.creado, "No debe llegar a persistencia");
  }

  @Test
  void desactivarDelegaEnElPuerto() {
    var puerto = new DisponibilidadFalsa();
    var gestionar =
        new GestionarDisponibilidad(puerto, new FichasFalsas(), new ConsultoriosFalsos());
    UUID turnoId = UUID.randomUUID();
    gestionar.desactivar(ACTOR, TipoRecurso.CONSULTORIO, turnoId);
    assertEquals(turnoId, puerto.desactivado);
  }
}
