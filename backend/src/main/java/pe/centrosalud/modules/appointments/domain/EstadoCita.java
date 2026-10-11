package pe.centrosalud.modules.appointments.domain;

/** Estados administrativos de una cita. */
public enum EstadoCita {
  PROGRAMADA,
  EN_ESPERA,
  EN_ATENCION,
  ATENDIDA,
  CANCELADA,
  AUSENTE
}
