package pe.centrosalud.modules.appointments.application.usecases;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import pe.centrosalud.modules.appointments.application.ports.AutorizacionCitas;
import pe.centrosalud.modules.appointments.application.ports.Citas;
import pe.centrosalud.modules.appointments.application.ports.OperacionCita;
import pe.centrosalud.modules.appointments.domain.EstadoCita;

public final class ConsultarAtencionCitas {
    public record Resultado(LocalDate fecha, Instant consultadaEn, List<CitaDetalle> enEspera,
                            List<CitaDetalle> enAtencion) {}
    private final Citas citas;
    private final ConsultarAgendaCitas agenda;
    private final AutorizacionCitas autorizacion;
    private final Clock clock;

    public ConsultarAtencionCitas(Citas citas, ConsultarAgendaCitas agenda,
                                  AutorizacionCitas autorizacion, Clock clock) {
        this.citas = Objects.requireNonNull(citas);
        this.agenda = Objects.requireNonNull(agenda);
        this.autorizacion = Objects.requireNonNull(autorizacion);
        this.clock = Objects.requireNonNull(clock);
    }

    public Resultado ejecutar(UUID actorId, LocalDate fecha, UUID profesionalId) {
        Objects.requireNonNull(fecha, "La fecha es obligatoria");
        var perfil = autorizacion.perfil(actorId);
        if (!perfil.puede(OperacionCita.CONSULTAR_ATENCION)) throw new AccesoCitasDenegado();
        UUID filtro = alcanceProfesional(perfil, profesionalId);
        var detalles = citas.atencion(fecha, filtro).stream().map(agenda::detalle).toList();
        return new Resultado(fecha, clock.instant(),
                detalles.stream().filter(x -> x.cita().estado() == EstadoCita.EN_ESPERA).toList(),
                detalles.stream().filter(x -> x.cita().estado() == EstadoCita.EN_ATENCION).toList());
    }

    static UUID alcanceProfesional(pe.centrosalud.modules.appointments.application.ports.PerfilCitas perfil,
                                   UUID profesionalSolicitado) {
        // Una cuenta sin capacidad de recepción nunca puede ampliar su alcance por un parámetro HTTP.
        if (!perfil.puede(OperacionCita.PROGRAMAR)) {
            if (perfil.profesionalAsignadoId() == null) throw new AccesoCitasDenegado();
            return perfil.profesionalAsignadoId();
        }
        return profesionalSolicitado;
    }
}
