package pe.centrosalud.modules.appointments.application.usecases;

import java.time.Clock;
import java.util.Objects;
import java.util.UUID;
import pe.centrosalud.modules.appointments.application.ports.AutorizacionCitas;
import pe.centrosalud.modules.appointments.application.ports.Citas;
import pe.centrosalud.modules.appointments.application.ports.ConflictosAtencion;
import pe.centrosalud.modules.appointments.application.ports.OperacionCita;
import pe.centrosalud.modules.appointments.domain.EstadoCita;

public final class IniciarAtencionCita {
    public record Comando(UUID actorId, UUID citaId, long version) {}
    private final Citas citas;
    private final ConsultarAgendaCitas agenda;
    private final AutorizacionCitas autorizacion;
    private final ConflictosAtencion conflictos;
    private final Clock clock;

    public IniciarAtencionCita(Citas citas, ConsultarAgendaCitas agenda, AutorizacionCitas autorizacion,
                               ConflictosAtencion conflictos, Clock clock) {
        this.citas = Objects.requireNonNull(citas);
        this.agenda = Objects.requireNonNull(agenda);
        this.autorizacion = Objects.requireNonNull(autorizacion);
        this.conflictos = Objects.requireNonNull(conflictos);
        this.clock = Objects.requireNonNull(clock);
    }

    public CitaDetalle ejecutar(Comando comando) {
        Objects.requireNonNull(comando);
        var perfil = autorizacion.perfil(comando.actorId());
        if (!perfil.puede(OperacionCita.INICIAR_ATENCION)) throw new AccesoCitasDenegado();
        var cita = citas.buscarPorId(comando.citaId()).orElseThrow(CitaNoEncontrada::new);
        if (perfil.profesionalAsignadoId() == null || !perfil.profesionalAsignadoId().equals(cita.profesionalId()))
            throw new AccesoCitasDenegado();
        if (cita.atencionIniciadaEn() == null) {
            if (cita.version() != comando.version()) throw new VersionCitaObsoleta();
            if (conflictos.existeConflictoPendiente(cita))
                throw new OperacionCitaInvalida("La cita tiene un conflicto pendiente de disponibilidad");
            if (cita.estado() != EstadoCita.EN_ESPERA)
                throw new OperacionCitaInvalida("Solo una cita en espera puede iniciar atención");
            try {
                cita.iniciarAtencion(comando.actorId(), comando.version(), clock);
            } catch (IllegalArgumentException error) {
                throw new OperacionCitaInvalida(error.getMessage());
            }
            cita = citas.guardarTransicion(cita);
        }
        return agenda.detalle(cita);
    }
}
