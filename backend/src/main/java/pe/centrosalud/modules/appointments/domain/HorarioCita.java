package pe.centrosalud.modules.appointments.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/** Intervalo de una reserva, cerrado en el inicio y abierto en el final. */
public record HorarioCita(Instant inicio, Instant fin) {
    public HorarioCita {
        Objects.requireNonNull(inicio, "El inicio es obligatorio");
        Objects.requireNonNull(fin, "El fin es obligatorio");
        if (!fin.isAfter(inicio)) {
            throw new IllegalArgumentException("El fin debe ser posterior al inicio");
        }
    }

    public static HorarioCita desde(Instant inicio, int duracionMinutos) {
        if (duracionMinutos <= 0) throw new IllegalArgumentException("La duración debe ser positiva");
        return new HorarioCita(inicio, inicio.plus(Duration.ofMinutes(duracionMinutos)));
    }

    public boolean seCruzaCon(HorarioCita otro) {
        Objects.requireNonNull(otro, "El intervalo es obligatorio");
        return inicio.isBefore(otro.fin) && fin.isAfter(otro.inicio);
    }
}
