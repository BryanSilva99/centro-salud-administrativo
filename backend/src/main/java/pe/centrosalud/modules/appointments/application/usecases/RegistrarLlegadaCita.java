package pe.centrosalud.modules.appointments.application.usecases;

import java.time.Clock;
import java.time.ZoneId;
import java.util.Objects;
import java.util.UUID;
import pe.centrosalud.modules.appointments.application.ports.AutorizacionCitas;
import pe.centrosalud.modules.appointments.application.ports.Citas;
import pe.centrosalud.modules.appointments.application.ports.OperacionCita;
import pe.centrosalud.modules.appointments.domain.Cita;

public final class RegistrarLlegadaCita {
    public record Comando(UUID actorId, UUID citaId, long version) {}
    public static final ZoneId ZONA_CENTRO = ZoneId.of("America/Lima");
    private final Citas citas;
    private final ConsultarAgendaCitas agenda;
    private final AutorizacionCitas autorizacion;
    private final Clock clock;

    public RegistrarLlegadaCita(Citas citas, ConsultarAgendaCitas agenda,
                                AutorizacionCitas autorizacion, Clock clock) {
        this.citas = Objects.requireNonNull(citas);
        this.agenda = Objects.requireNonNull(agenda);
        this.autorizacion = Objects.requireNonNull(autorizacion);
        this.clock = Objects.requireNonNull(clock);
    }

    public CitaDetalle ejecutar(Comando comando) {
        Objects.requireNonNull(comando);
        autorizacion.verificar(comando.actorId(), OperacionCita.REGISTRAR_LLEGADA);
        var cita = citas.buscarPorId(comando.citaId()).orElseThrow(CitaNoEncontrada::new);
        if (cita.llegadaEn() == null) {
            if (cita.version() != comando.version()) throw new VersionCitaObsoleta();
            try {
                cita.registrarLlegada(comando.actorId(), comando.version(), clock, ZONA_CENTRO);
            } catch (IllegalArgumentException error) {
                throw new OperacionCitaInvalida(error.getMessage());
            }
            cita = citas.guardarTransicion(cita);
        }
        return agenda.detalle(cita);
    }
}
