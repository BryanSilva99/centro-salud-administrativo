package pe.centrosalud.modules.administration.application.usecases;

import static pe.centrosalud.modules.administration.application.usecases.ErrorDisponibilidad.Codigo.RECURSO_INVALIDO;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import pe.centrosalud.modules.administration.application.ports.Consultorios;
import pe.centrosalud.modules.administration.application.ports.Disponibilidad;
import pe.centrosalud.modules.administration.application.ports.FichasProfesionales;
import pe.centrosalud.modules.administration.domain.TipoRecurso;
import pe.centrosalud.modules.administration.domain.TipoTurno;
import pe.centrosalud.modules.administration.domain.Turno;

/**
 * RF-10. La autorización (solo ADM) se exige en la ruta mediante DISPONIBILIDAD_GESTIONAR. Los
 * turnos no se editan: se crean o se desactivan; para cambiar un horario se desactiva el turno
 * anterior y se crea uno nuevo.
 */
public final class GestionarDisponibilidad {
  private final Disponibilidad disponibilidad;
  private final FichasProfesionales fichas;
  private final Consultorios consultorios;

  public GestionarDisponibilidad(
      Disponibilidad disponibilidad, FichasProfesionales fichas, Consultorios consultorios) {
    this.disponibilidad = disponibilidad;
    this.fichas = fichas;
    this.consultorios = consultorios;
  }

  public Turno crear(
      UUID actor,
      TipoRecurso tipo,
      UUID recursoId,
      LocalDate fecha,
      LocalTime horaInicio,
      LocalTime horaFin,
      TipoTurno tipoTurno) {
    validarRecurso(tipo, recursoId);
    var nuevo =
        new Turno(UUID.randomUUID(), recursoId, fecha, horaInicio, horaFin, tipoTurno, true);
    return disponibilidad.crear(tipo, nuevo, actor);
  }

  public void desactivar(UUID actor, TipoRecurso tipo, UUID turnoId) {
    disponibilidad.desactivar(tipo, turnoId, actor);
  }

  /** La ruta debe identificar al recurso propietario del turno, incluso si ya está inactivo. */
  public void desactivar(UUID actor, TipoRecurso tipo, UUID recursoId, UUID turnoId) {
    if (disponibilidad.listar(tipo, recursoId).stream()
        .noneMatch(turno -> turno.id().equals(turnoId)))
      throw new ErrorDisponibilidad(ErrorDisponibilidad.Codigo.NO_EXISTE);
    desactivar(actor, tipo, turnoId);
  }

  private void validarRecurso(TipoRecurso tipo, UUID recursoId) {
    boolean existe =
        switch (tipo) {
          case PROFESIONAL -> fichas.buscarPorId(recursoId).isPresent();
          case CONSULTORIO -> consultorios.buscarPorId(recursoId).isPresent();
        };
    if (!existe) throw new ErrorDisponibilidad(RECURSO_INVALIDO);
  }
}
