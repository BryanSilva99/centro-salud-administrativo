package pe.centrosalud.unit;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pe.centrosalud.adapters.outbound.mock.appointments.AutorizacionCitasMock;
import pe.centrosalud.adapters.outbound.mock.appointments.CatalogoCitasMock;
import pe.centrosalud.adapters.outbound.mock.appointments.DisponibilidadCitasMock;
import pe.centrosalud.modules.appointments.application.ports.Citas;
import pe.centrosalud.modules.appointments.application.usecases.ConflictoCita;
import pe.centrosalud.modules.appointments.application.usecases.ProgramarCita;
import pe.centrosalud.modules.appointments.application.usecases.ReprogramarCita;
import pe.centrosalud.modules.appointments.domain.Cita;
import pe.centrosalud.modules.appointments.domain.EstadoCita;
import pe.centrosalud.modules.appointments.domain.HorarioCita;
import pe.centrosalud.modules.appointments.domain.Reprogramacion;

class CitaAppointmentsTest {
    private static final UUID ACTOR = UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");
    private static final Instant AHORA = Instant.parse("2026-10-10T18:00:00Z");
    private final CatalogoCitasMock catalogo = new CatalogoCitasMock();
    private final DisponibilidadCitasMock horarios = new DisponibilidadCitasMock();
    private final MemoryCitas citas = new MemoryCitas();
    private final Clock clock = Clock.fixed(AHORA, ZoneOffset.UTC);
    private ProgramarCita programar;

    @BeforeEach void preparar() {
        programar = new ProgramarCita(citas, catalogo, horarios, new AutorizacionCitasMock(), clock);
    }

    @Test void programaConservandoDuracionYCondicionEconomica() {
        var resultado = programar.ejecutar(new ProgramarCita.Comando(ACTOR, CatalogoCitasMock.PACIENTE_1,
                CatalogoCitasMock.PROFESIONAL_1, CatalogoCitasMock.SERVICIO_1, CatalogoCitasMock.CONSULTORIO_1,
                "PARTICULAR", LocalDate.of(2026, 10, 12), LocalTime.of(9, 0)));

        assertEquals(EstadoCita.PROGRAMADA, resultado.cita().estado());
        assertEquals(30, resultado.cita().duracionMinutos());
        assertEquals(new BigDecimal("50.00"), resultado.cita().importe());
        assertEquals(1, citas.guardadas.size());
    }

    @Test void rechazaCrucePeroPermiteIntervalosContiguos() {
        var fecha = LocalDate.of(2026, 10, 12);
        var primero = programar.ejecutar(new ProgramarCita.Comando(ACTOR, CatalogoCitasMock.PACIENTE_1,
                CatalogoCitasMock.PROFESIONAL_1, CatalogoCitasMock.SERVICIO_1, CatalogoCitasMock.CONSULTORIO_1,
                "PARTICULAR", fecha, LocalTime.of(9, 0)));
        assertThrows(ConflictoCita.class, () -> programar.ejecutar(new ProgramarCita.Comando(ACTOR,
                CatalogoCitasMock.PACIENTE_1, CatalogoCitasMock.PROFESIONAL_1, CatalogoCitasMock.SERVICIO_1,
                CatalogoCitasMock.CONSULTORIO_1, "PARTICULAR", fecha, LocalTime.of(9, 15))));
        var contigua = programar.ejecutar(new ProgramarCita.Comando(ACTOR, CatalogoCitasMock.PACIENTE_2,
                CatalogoCitasMock.PROFESIONAL_1, CatalogoCitasMock.SERVICIO_1, CatalogoCitasMock.CONSULTORIO_1,
                "SIN_CARGO", fecha, LocalTime.of(9, 30)));
        assertNotEquals(primero.cita().id(), contigua.cita().id());
    }

    @Test void reprogramaConservandoIdentidadYRegistraHistorial() {
        var fecha = LocalDate.of(2026, 10, 12);
        var creada = programar.ejecutar(new ProgramarCita.Comando(ACTOR, CatalogoCitasMock.PACIENTE_1,
                CatalogoCitasMock.PROFESIONAL_1, CatalogoCitasMock.SERVICIO_1, CatalogoCitasMock.CONSULTORIO_1,
                "PARTICULAR", fecha, LocalTime.of(9, 0)));
        var reprogramar = new ReprogramarCita(citas, catalogo, horarios, new AutorizacionCitasMock(), clock);
        var actualizada = reprogramar.ejecutar(new ReprogramarCita.Comando(ACTOR, creada.cita().id(), fecha,
                LocalTime.of(10, 0), "Solicitud del paciente", creada.cita().version()));

        assertEquals(creada.cita().id(), actualizada.cita().id());
        assertEquals(LocalTime.of(10, 0), actualizada.cita().horario().inicio()
                .atZone(pe.centrosalud.modules.appointments.application.usecases.ConsultarDisponibilidadCitas.ZONA_CENTRO).toLocalTime());
        assertEquals(1, citas.historial.size());
        assertEquals("Solicitud del paciente", citas.historial.getFirst().motivo());
        assertEquals(50, actualizada.cita().importe().intValue());
    }

    @Test void unaReprogramacionInvalidaNoCambiaLaCita() {
        var fecha = LocalDate.of(2026, 10, 12);
        var creada = programar.ejecutar(new ProgramarCita.Comando(ACTOR, CatalogoCitasMock.PACIENTE_1,
                CatalogoCitasMock.PROFESIONAL_1, CatalogoCitasMock.SERVICIO_1, CatalogoCitasMock.CONSULTORIO_1,
                "PARTICULAR", fecha, LocalTime.of(9, 0)));
        var reprogramar = new ReprogramarCita(citas, catalogo, horarios, new AutorizacionCitasMock(), clock);
        assertThrows(RuntimeException.class, () -> reprogramar.ejecutar(new ReprogramarCita.Comando(ACTOR,
                creada.cita().id(), fecha, LocalTime.of(11, 45), "Fuera de disponibilidad", creada.cita().version())));
        assertEquals(LocalTime.of(9, 0), citas.buscarPorId(creada.cita().id()).orElseThrow().horario().inicio()
                .atZone(pe.centrosalud.modules.appointments.application.usecases.ConsultarDisponibilidadCitas.ZONA_CENTRO).toLocalTime());
        assertTrue(citas.historial.isEmpty());
    }

    private static final class MemoryCitas implements Citas {
        private final List<Cita> guardadas = new ArrayList<>();
        private final List<Reprogramacion> historial = new ArrayList<>();
        @Override public Optional<Cita> buscarPorId(UUID id) { return guardadas.stream().filter(x -> x.id().equals(id)).findFirst(); }
        @Override public List<Cita> agenda(LocalDate fecha, UUID profesionalId, UUID servicioId, UUID consultorioId) { return List.copyOf(guardadas); }
        @Override public boolean existeConflicto(UUID pacienteId, UUID profesionalId, UUID consultorioId, HorarioCita horario, UUID excluirId) {
            return guardadas.stream().filter(x -> !x.id().equals(excluirId)).filter(x -> x.estado() != EstadoCita.CANCELADA && x.estado() != EstadoCita.AUSENTE)
                    .anyMatch(x -> (x.pacienteId().equals(pacienteId) || x.profesionalId().equals(profesionalId) || x.consultorioId().equals(consultorioId)) && x.horario().seCruzaCon(horario));
        }
        @Override public Cita guardar(Cita cita) { guardadas.add(cita); return cita; }
        @Override public Cita guardarReprogramacion(Cita cita, Reprogramacion cambio) { historial.add(cambio); return cita; }
    }
}
