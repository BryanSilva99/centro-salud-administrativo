package pe.centrosalud.adapters.outbound.modules.appointments;

import static pe.centrosalud.modules.appointments.application.usecases.ConsultarDisponibilidadCitas.ZONA_CENTRO;

import java.time.LocalDate;
import java.util.*;
import pe.centrosalud.modules.administration.application.ports.*;
import pe.centrosalud.modules.administration.domain.*;
import pe.centrosalud.modules.appointments.application.ports.DisponibilidadCitas;
import pe.centrosalud.modules.appointments.domain.HorarioCita;

/** Intersección de turnos activos y habilitación del servicio en ambos recursos. */
public final class DisponibilidadCitasAdministracion implements DisponibilidadCitas {
  private final Disponibilidad turnos;
  private final FichasProfesionales profesionales;
  private final Consultorios consultorios;
  private final Servicios servicios;

  public DisponibilidadCitasAdministracion(
      Disponibilidad t, FichasProfesionales p, Consultorios c, Servicios s) {
    turnos = t;
    profesionales = p;
    consultorios = c;
    servicios = s;
  }

  public List<HorarioCita> intervalosBase(
      LocalDate fecha, UUID profesional, UUID servicio, UUID consultorio) {
    if (profesionales
            .buscarPorId(profesional)
            .filter(p -> p.activa() && p.serviciosHabilitados().contains(servicio))
            .isEmpty()
        || consultorios
            .buscarPorId(consultorio)
            .filter(c -> c.activo() && c.serviciosCompatibles().contains(servicio))
            .isEmpty()
        || servicios.buscarPorId(servicio).filter(s -> s.activo()).isEmpty()) return List.of();
    var resultado = new ArrayList<HorarioCita>();
    for (var p : intervalos(TipoRecurso.PROFESIONAL, profesional, fecha)) {
      for (var c : intervalos(TipoRecurso.CONSULTORIO, consultorio, fecha)) {
        var inicio = p.inicio().isAfter(c.inicio()) ? p.inicio() : c.inicio();
        var fin = p.fin().isBefore(c.fin()) ? p.fin() : c.fin();
        if (fin.isAfter(inicio)) resultado.add(new HorarioCita(inicio, fin));
      }
    }
    return resultado.stream().sorted(Comparator.comparing(HorarioCita::inicio)).toList();
  }

  private List<HorarioCita> intervalos(TipoRecurso tipo, UUID id, LocalDate fecha) {
    // Administración impide solapar DISPONIBLE y DESCANSO: solo los primeros son reservables.
    var ordenados =
        turnos.listar(tipo, id).stream()
            .filter(t -> t.activo() && t.fecha().equals(fecha) && t.tipo() == TipoTurno.DISPONIBLE)
            .map(
                t ->
                    new HorarioCita(
                        fecha.atTime(t.horaInicio()).atZone(ZONA_CENTRO).toInstant(),
                        fecha.atTime(t.horaFin()).atZone(ZONA_CENTRO).toInstant()))
            .sorted(Comparator.comparing(HorarioCita::inicio))
            .toList();
    var unidos = new ArrayList<HorarioCita>();
    for (var intervalo : ordenados) {
      if (!unidos.isEmpty() && unidos.getLast().fin().equals(intervalo.inicio())) {
        var anterior = unidos.removeLast();
        unidos.add(new HorarioCita(anterior.inicio(), intervalo.fin()));
      } else unidos.add(intervalo);
    }
    return unidos;
  }
}
