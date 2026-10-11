package pe.centrosalud.modules.appointments.application.usecases;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;
import java.util.UUID;
import java.time.Clock;
import pe.centrosalud.modules.appointments.application.ports.AutorizacionCitas;
import pe.centrosalud.modules.appointments.application.ports.CatalogoCitas;
import pe.centrosalud.modules.appointments.application.ports.Citas;
import pe.centrosalud.modules.appointments.application.ports.DisponibilidadCitas;
import pe.centrosalud.modules.appointments.application.ports.OperacionCita;
import pe.centrosalud.modules.appointments.domain.HorarioCita;

public final class ReprogramarCita {
    public record Comando(UUID actorId, UUID citaId, LocalDate fecha, LocalTime hora, String motivo, long version) {}
    private final Citas citas;
    private final CatalogoCitas catalogo;
    private final DisponibilidadCitas disponibilidad;
    private final AutorizacionCitas autorizacion;
    private final Clock clock;

    public ReprogramarCita(Citas citas, CatalogoCitas catalogo, DisponibilidadCitas disponibilidad,
                           AutorizacionCitas autorizacion, Clock clock) {
        this.citas = Objects.requireNonNull(citas); this.catalogo = Objects.requireNonNull(catalogo);
        this.disponibilidad = Objects.requireNonNull(disponibilidad); this.autorizacion = Objects.requireNonNull(autorizacion);
        this.clock = Objects.requireNonNull(clock);
    }

    public CitaDetalle ejecutar(Comando comando) {
        Objects.requireNonNull(comando);
        autorizacion.verificar(comando.actorId(), OperacionCita.REPROGRAMAR);
        var cita = citas.buscarPorId(comando.citaId()).orElseThrow(CitaNoEncontrada::new);
        if (cita.version() != comando.version()) throw new VersionCitaObsoleta();
        if (cita.estado() != pe.centrosalud.modules.appointments.domain.EstadoCita.PROGRAMADA)
            throw new OperacionCitaInvalida("Solo una cita programada puede reprogramarse");
        var profesional = catalogo.profesional(cita.profesionalId()).filter(CatalogoCitas.Profesional::activo)
                .orElseThrow(() -> new ReferenciaCitaNoEncontrada("profesional"));
        var servicio = catalogo.servicio(cita.servicioId()).filter(CatalogoCitas.Servicio::activo)
                .orElseThrow(() -> new ReferenciaCitaNoEncontrada("servicio"));
        var consultorio = catalogo.consultorio(cita.consultorioId()).filter(CatalogoCitas.Consultorio::activo)
                .orElseThrow(() -> new ReferenciaCitaNoEncontrada("consultorio"));
        var paciente = catalogo.paciente(cita.pacienteId()).filter(CatalogoCitas.Paciente::activo)
                .orElseThrow(() -> new ReferenciaCitaNoEncontrada("paciente"));
        int duracion = cita.duracionMinutos();
        if (comando.fecha() == null || comando.hora() == null) throw new HorarioCitaInvalido("La fecha y hora son obligatorias");
        if (comando.hora().getMinute() % 15 != 0 || comando.hora().getSecond() != 0 || comando.hora().getNano() != 0)
            throw new HorarioCitaInvalido("La hora debe corresponder a un intervalo de 15 minutos");
        var inicio = comando.fecha().atTime(comando.hora()).atZone(ConsultarDisponibilidadCitas.ZONA_CENTRO).toInstant();
        var nuevo = HorarioCita.desde(inicio, duracion);
        if (!inicio.isAfter(clock.instant())) throw new HorarioCitaInvalido("No se pueden reservar horarios pasados");
        if (disponibilidad.intervalosBase(comando.fecha(), cita.profesionalId(), cita.servicioId(), cita.consultorioId()).stream()
                .noneMatch(base -> !nuevo.inicio().isBefore(base.inicio()) && !nuevo.fin().isAfter(base.fin())))
            throw new HorarioCitaInvalido("El horario está fuera de la disponibilidad configurada");
        if (citas.existeConflicto(cita.pacienteId(), cita.profesionalId(), cita.consultorioId(), nuevo, cita.id())) throw new ConflictoCita();
        var historial = cita.reprogramar(nuevo, comando.motivo(), comando.actorId(), comando.version(), clock);
        var guardada = citas.guardarReprogramacion(cita, historial);
        return new CitaDetalle(guardada, paciente, profesional, servicio, consultorio);
    }
}
