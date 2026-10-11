package pe.centrosalud.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
import org.junit.jupiter.api.Test;
import pe.centrosalud.adapters.outbound.mock.appointments.AutorizacionCitasMock;
import pe.centrosalud.adapters.outbound.mock.appointments.CatalogoCitasMock;
import pe.centrosalud.modules.appointments.application.ports.Citas;
import pe.centrosalud.modules.appointments.application.usecases.CancelarCita;
import pe.centrosalud.modules.appointments.application.usecases.ConsultarAgendaCitas;
import pe.centrosalud.modules.appointments.application.usecases.OperacionCitaInvalida;
import pe.centrosalud.modules.appointments.application.usecases.RegistrarAusenciaCita;
import pe.centrosalud.modules.appointments.application.usecases.RegistrarLlegadaCita;
import pe.centrosalud.modules.appointments.application.usecases.RetirarEsperaCita;
import pe.centrosalud.modules.appointments.application.usecases.VersionCitaObsoleta;
import pe.centrosalud.modules.appointments.domain.Cita;
import pe.centrosalud.modules.appointments.domain.EstadoCita;
import pe.centrosalud.modules.appointments.domain.HorarioCita;
import pe.centrosalud.modules.appointments.domain.Reprogramacion;
import pe.centrosalud.modules.appointments.domain.TipoCierreCita;

class CierreAppointmentsTest {
    private static final UUID ACTOR = UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");
    private static final LocalDate FECHA = LocalDate.of(2026, 10, 12);
    private static final Instant MOMENTO = Instant.parse("2026-10-12T15:00:00Z");
    private final CatalogoCitasMock catalogo = new CatalogoCitasMock();
    private final AutorizacionCitasMock autorizacion = new AutorizacionCitasMock();
    private final MemoryCitas citas = new MemoryCitas();
    private final ConsultarAgendaCitas agenda = new ConsultarAgendaCitas(citas, catalogo, autorizacion);

    @Test
    void cancelacionConservaMotivoMomentoYEsIdempotente() {
        var cita = crear(FECHA, LocalTime.of(9, 0), Clock.fixed(Instant.parse("2026-10-10T18:00:00Z"), ZoneOffset.UTC));
        var cierre = new CancelarCita(citas, agenda, autorizacion, ignored -> true,
                Clock.fixed(MOMENTO, ZoneOffset.UTC));

        var primera = cierre.ejecutar(new CancelarCita.Comando(ACTOR, cita.id(), "Solicitud del paciente", 0));
        var segunda = cierre.ejecutar(new CancelarCita.Comando(ACTOR, cita.id(), "Otro motivo ignorado", 0));

        assertEquals(EstadoCita.CANCELADA, primera.detalle().cita().estado());
        assertEquals(TipoCierreCita.CANCELACION, primera.detalle().cita().tipoCierre());
        assertEquals("Solicitud del paciente", primera.detalle().cita().motivoCierre());
        assertEquals(MOMENTO, primera.detalle().cita().cerradaEn());
        assertEquals(1, primera.detalle().cita().version());
        assertEquals(1, segunda.detalle().cita().version());
        assertEquals(MOMENTO, segunda.detalle().cita().cerradaEn());
        assertEquals("Solicitud del paciente", segunda.detalle().cita().motivoCierre());
        assertEquals(true, primera.gestionEconomicaPendiente());
    }

    @Test
    void ausenciaSoloSeRegistraCuandoTerminoElIntervalo() {
        var clock = Clock.fixed(MOMENTO, ZoneOffset.UTC);
        var cita = crear(FECHA, LocalTime.of(9, 0), Clock.fixed(Instant.parse("2026-10-10T18:00:00Z"), ZoneOffset.UTC));
        var ausencia = new RegistrarAusenciaCita(citas, agenda, autorizacion, clock);

        var resultado = ausencia.ejecutar(new RegistrarAusenciaCita.Comando(ACTOR, cita.id(), 0));

        assertEquals(EstadoCita.AUSENTE, resultado.cita().estado());
        assertEquals(TipoCierreCita.AUSENCIA, resultado.cita().tipoCierre());
        assertEquals(1, resultado.cita().version());
        assertNotNull(resultado.cita().cerradaEn());

        var segunda = ausencia.ejecutar(new RegistrarAusenciaCita.Comando(ACTOR, cita.id(), 0));
        assertEquals(resultado.cita().cerradaEn(), segunda.cita().cerradaEn());
        assertEquals(1, segunda.cita().version());
    }

    @Test
    void retiroConservaLlegadaYNoIniciaAtencion() {
        var clock = Clock.fixed(Instant.parse("2026-10-12T14:05:00Z"), ZoneOffset.UTC);
        var cita = crear(FECHA, LocalTime.of(9, 0), Clock.fixed(Instant.parse("2026-10-10T18:00:00Z"), ZoneOffset.UTC));
        var llegada = new RegistrarLlegadaCita(citas, agenda, autorizacion, clock);
        var enEspera = llegada.ejecutar(new RegistrarLlegadaCita.Comando(ACTOR, cita.id(), 0)).cita();
        var llegadaOriginal = enEspera.llegadaEn();
        var retiro = new RetirarEsperaCita(citas, agenda, autorizacion, ignored -> false, clock);

        var cerrado = retiro.ejecutar(new RetirarEsperaCita.Comando(ACTOR, cita.id(), "Paciente se retira", 1));

        assertEquals(EstadoCita.CANCELADA, cerrado.detalle().cita().estado());
        assertEquals(TipoCierreCita.RETIRO_ESPERA, cerrado.detalle().cita().tipoCierre());
        assertEquals(llegadaOriginal, cerrado.detalle().cita().llegadaEn());
        assertEquals(null, cerrado.detalle().cita().atencionIniciadaEn());
        assertEquals(2, cerrado.detalle().cita().version());
    }

    @Test
    void rechazaAusenciaAntesDelFinYCancelacionDesdeEspera() {
        var futuro = Clock.fixed(Instant.parse("2026-10-12T13:50:00Z"), ZoneOffset.UTC);
        var citaAusencia = crear(FECHA, LocalTime.of(9, 0), futuro);
        var ausencia = new RegistrarAusenciaCita(citas, agenda, autorizacion, futuro);
        assertThrows(OperacionCitaInvalida.class,
                () -> ausencia.ejecutar(new RegistrarAusenciaCita.Comando(ACTOR, citaAusencia.id(), 0)));

        var llegada = new RegistrarLlegadaCita(citas, agenda, autorizacion,
                Clock.fixed(Instant.parse("2026-10-12T14:05:00Z"), ZoneOffset.UTC));
        var citaEspera = crear(FECHA, LocalTime.of(10, 0), futuro);
        llegada.ejecutar(new RegistrarLlegadaCita.Comando(ACTOR, citaEspera.id(), 0));
        var cancelacion = new CancelarCita(citas, agenda, autorizacion, ignored -> false, futuro);
        assertThrows(IllegalStateException.class,
                () -> cancelacion.ejecutar(new CancelarCita.Comando(ACTOR, citaEspera.id(), "No puede", 1)));
    }

    @Test
    void rechazaVersionObsoletaAntesDeCerrar() {
        var cita = crear(FECHA, LocalTime.of(11, 0), Clock.fixed(Instant.parse("2026-10-10T18:00:00Z"), ZoneOffset.UTC));
        var cierre = new CancelarCita(citas, agenda, autorizacion, ignored -> false,
                Clock.fixed(MOMENTO, ZoneOffset.UTC));

        assertThrows(VersionCitaObsoleta.class,
                () -> cierre.ejecutar(new CancelarCita.Comando(ACTOR, cita.id(), "Motivo", 4)));
        assertEquals(EstadoCita.PROGRAMADA, citas.buscarPorId(cita.id()).orElseThrow().estado());
    }

    private Cita crear(LocalDate fecha, LocalTime hora, Clock clock) {
        var inicio = fecha.atTime(hora).atZone(RegistrarLlegadaCita.ZONA_CENTRO).toInstant();
        var cita = Cita.programar(UUID.randomUUID(), CatalogoCitasMock.PACIENTE_1,
                CatalogoCitasMock.PROFESIONAL_1, CatalogoCitasMock.SERVICIO_1,
                CatalogoCitasMock.CONSULTORIO_1, new HorarioCita(inicio, inicio.plusSeconds(1800)),
                30, "PARTICULAR", BigDecimal.valueOf(50), ACTOR, clock);
        citas.guardar(cita);
        return cita;
    }

    private static final class MemoryCitas implements Citas {
        private final List<Cita> guardadas = new ArrayList<>();

        @Override public Optional<Cita> buscarPorId(UUID id) {
            return guardadas.stream().filter(cita -> cita.id().equals(id)).findFirst();
        }
        @Override public List<Cita> agenda(LocalDate fecha, UUID profesionalId, UUID servicioId, UUID consultorioId) {
            return guardadas.stream().filter(cita -> profesionalId == null || cita.profesionalId().equals(profesionalId)).toList();
        }
        @Override public boolean existeConflicto(UUID pacienteId, UUID profesionalId, UUID consultorioId,
                                                  HorarioCita horario, UUID excluirId) { return false; }
        @Override public Cita guardar(Cita cita) { guardadas.add(cita); return cita; }
        @Override public Cita guardarTransicion(Cita cita) { return cita; }
        @Override public Cita guardarReprogramacion(Cita cita, Reprogramacion cambio) { return cita; }
    }
}
