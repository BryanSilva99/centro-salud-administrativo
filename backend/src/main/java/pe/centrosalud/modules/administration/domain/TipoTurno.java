package pe.centrosalud.modules.administration.domain;

/**
 * RF-10. Un horario sin turno DISPONIBLE ya es "no disponible" por defecto; no existe un tipo
 * BLOQUEO (eso es RF-11).
 */
public enum TipoTurno {
  DISPONIBLE,
  DESCANSO
}
