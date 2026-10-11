package pe.centrosalud.modules.appointments.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Registro inmutable del cambio de horario de una cita. */
public record Reprogramacion(
        UUID citaId,
        UUID actorId,
        Instant horarioAnteriorInicio,
        Instant horarioAnteriorFin,
        Instant nuevoHorarioInicio,
        Instant nuevoHorarioFin,
        String motivo,
        Instant registradaEn) {

    public Reprogramacion {
        Objects.requireNonNull(citaId, "La cita es obligatoria");
        Objects.requireNonNull(actorId, "El actor es obligatorio");
        Objects.requireNonNull(horarioAnteriorInicio, "El horario anterior es obligatorio");
        Objects.requireNonNull(horarioAnteriorFin, "El horario anterior es obligatorio");
        Objects.requireNonNull(nuevoHorarioInicio, "El nuevo horario es obligatorio");
        Objects.requireNonNull(nuevoHorarioFin, "El nuevo horario es obligatorio");
        Objects.requireNonNull(motivo, "El motivo es obligatorio");
        Objects.requireNonNull(registradaEn, "La fecha de registro es obligatoria");
        if (motivo.isBlank()) throw new IllegalArgumentException("El motivo es obligatorio");
    }
}
