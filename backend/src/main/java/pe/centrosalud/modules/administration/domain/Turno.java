package pe.centrosalud.modules.administration.domain;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Intervalo de un recurso (profesional o consultorio) en una fecha civil de América/Lima (RN-11).
 * No representa un instante: no tiene zona horaria, igual que un horario de atención.
 */
public record Turno(
    UUID id,
    UUID recursoId,
    LocalDate fecha,
    LocalTime horaInicio,
    LocalTime horaFin,
    TipoTurno tipo,
    boolean activo) {
  public Turno {
    Objects.requireNonNull(id);
    Objects.requireNonNull(recursoId);
    Objects.requireNonNull(fecha);
    Objects.requireNonNull(tipo);
    if (horaInicio == null || horaFin == null || !horaFin.isAfter(horaInicio)) {
      throw new IllegalArgumentException("El intervalo del turno no es válido");
    }
  }

  /**
   * Dos intervalos del mismo recurso y fecha se superponen si [inicio,fin) se cruza, incluso entre
   * tipos distintos.
   */
  public boolean seSuperponeCon(Turno otro) {
    return horaInicio.isBefore(otro.horaFin) && otro.horaInicio.isBefore(horaFin);
  }
}
