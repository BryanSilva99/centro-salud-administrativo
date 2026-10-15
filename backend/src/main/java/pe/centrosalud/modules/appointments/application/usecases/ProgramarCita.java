package pe.centrosalud.modules.appointments.application.usecases;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;
import java.util.UUID;
import pe.centrosalud.modules.appointments.application.ports.AutorizacionCitas;
import pe.centrosalud.modules.appointments.application.ports.CatalogoCitas;
import pe.centrosalud.modules.appointments.application.ports.Citas;
import pe.centrosalud.modules.appointments.application.ports.DisponibilidadCitas;
import pe.centrosalud.modules.appointments.application.ports.OperacionCita;
import pe.centrosalud.modules.appointments.domain.Cita;
import pe.centrosalud.modules.appointments.domain.HorarioCita;

public final class ProgramarCita {
    public record Comando(UUID actorId, UUID pacienteId, UUID profesionalId, UUID servicioId,
                          UUID consultorioId, String modalidad, LocalDate fecha, LocalTime hora) {}
    private final Citas citas;
    private final CatalogoCitas catalogo;
    private final DisponibilidadCitas disponibilidad;
    private final AutorizacionCitas autorizacion;
    private final Clock clock;

    public ProgramarCita(Citas citas, CatalogoCitas catalogo, DisponibilidadCitas disponibilidad,
                         AutorizacionCitas autorizacion, Clock clock) {
        this.citas = Objects.requireNonNull(citas); this.catalogo = Objects.requireNonNull(catalogo);
        this.disponibilidad = Objects.requireNonNull(disponibilidad); this.autorizacion = Objects.requireNonNull(autorizacion);
        this.clock = Objects.requireNonNull(clock);
    }

    public CitaDetalle ejecutar(Comando comando) {
        Objects.requireNonNull(comando);
        autorizacion.verificar(comando.actorId(), OperacionCita.PROGRAMAR);
        var paciente = catalogo.paciente(comando.pacienteId()).filter(CatalogoCitas.Paciente::activo)
                .orElseThrow(() -> new ReferenciaCitaNoEncontrada("paciente"));
        var profesional = catalogo.profesional(comando.profesionalId()).filter(CatalogoCitas.Profesional::activo)
                .orElseThrow(() -> new ReferenciaCitaNoEncontrada("profesional"));
        var servicio = catalogo.servicio(comando.servicioId()).filter(CatalogoCitas.Servicio::activo)
                .orElseThrow(() -> new ReferenciaCitaNoEncontrada("servicio"));
        var consultorio = catalogo.consultorio(comando.consultorioId()).filter(CatalogoCitas.Consultorio::activo)
                .orElseThrow(() -> new ReferenciaCitaNoEncontrada("consultorio"));
        if (comando.modalidad() == null || comando.modalidad().isBlank()) throw new HorarioCitaInvalido("La modalidad es obligatoria");
        var modalidad = servicio.modalidad(comando.modalidad()).orElseThrow(() -> new ReferenciaCitaNoEncontrada("modalidad"));
        int duracion = duracion(servicio);
        var horario = horario(comando.fecha(), comando.hora(), duracion);
        validarHorario(horario, comando.fecha());
        if (disponibilidad.intervalosBase(comando.fecha(), profesional.id(), servicio.id(), consultorio.id()).stream()
                .noneMatch(base -> contiene(base, horario))) throw new HorarioCitaInvalido("El horario está fuera de la disponibilidad configurada");
        if (citas.existeConflicto(paciente.id(), profesional.id(), consultorio.id(), horario, null)) throw new ConflictoCita();
        var cita = Cita.programar(UUID.randomUUID(), paciente.id(), profesional.id(), servicio.id(), consultorio.id(),
                horario, duracion, modalidad.codigo(), modalidad.importe(), comando.actorId(), clock);
        var guardada = citas.guardar(cita);
        return new CitaDetalle(guardada, paciente, profesional, servicio, consultorio);
    }

    private HorarioCita horario(LocalDate fecha, LocalTime hora, int duracion) {
        if (fecha == null || hora == null) throw new HorarioCitaInvalido("La fecha y hora son obligatorias");
        if (hora.getMinute() % 15 != 0 || hora.getSecond() != 0 || hora.getNano() != 0)
            throw new HorarioCitaInvalido("La hora debe corresponder a un intervalo de 15 minutos");
        var inicio = fecha.atTime(hora).atZone(ConsultarDisponibilidadCitas.ZONA_CENTRO).toInstant();
        return HorarioCita.desde(inicio, duracion);
    }
    private void validarHorario(HorarioCita horario, LocalDate fecha) {
        if (!horario.inicio().isAfter(clock.instant())) throw new HorarioCitaInvalido("No se pueden reservar horarios pasados");
        if (!fecha.equals(horario.inicio().atZone(ConsultarDisponibilidadCitas.ZONA_CENTRO).toLocalDate())) throw new HorarioCitaInvalido("La fecha no es válida");
    }
    private boolean contiene(HorarioCita base, HorarioCita pedido) { return !pedido.inicio().isBefore(base.inicio()) && !pedido.fin().isAfter(base.fin()); }
    private int duracion(CatalogoCitas.Servicio servicio) {
        return servicio.duracionMinutos();
    }
}
