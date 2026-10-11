package pe.centrosalud.unit;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import pe.centrosalud.adapters.outbound.mock.appointments.AutorizacionCitasMock;
import pe.centrosalud.adapters.outbound.mock.appointments.CatalogoCitasMock;
import pe.centrosalud.modules.appointments.application.ports.Citas;
import pe.centrosalud.modules.appointments.application.usecases.ConsultarAgendaCitas;
import pe.centrosalud.modules.appointments.application.usecases.ConsultarDashboardCitas;
import pe.centrosalud.modules.appointments.application.usecases.FinalizarAtencionCita;
import pe.centrosalud.modules.appointments.application.usecases.IniciarAtencionCita;
import pe.centrosalud.modules.appointments.application.usecases.OperacionCitaInvalida;
import pe.centrosalud.modules.appointments.application.usecases.RegistrarLlegadaCita;
import pe.centrosalud.modules.appointments.domain.Cita;
import pe.centrosalud.modules.appointments.domain.EstadoCita;
import pe.centrosalud.modules.appointments.domain.HorarioCita;
import pe.centrosalud.modules.appointments.domain.Reprogramacion;

class AtencionAppointmentsTest {
  private static final UUID ACTOR = UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");
  private static final LocalDate FECHA = LocalDate.of(2026, 10, 12);
  private final CatalogoCitasMock catalogo = new CatalogoCitasMock();
  private final MemoryCitas citas = new MemoryCitas();
  private final MutableClock clock = new MutableClock(Instant.parse("2026-10-12T13:50:00Z"));
  private final AutorizacionCitasMock autorizacion = new AutorizacionCitasMock();
  private final ConsultarAgendaCitas agenda =
      new ConsultarAgendaCitas(citas, catalogo, autorizacion);

  @Test
  void recorridoCompletoEsIdempotenteYConservaMarcas() {
    var cita = crear(CatalogoCitasMock.PROFESIONAL_1, LocalTime.of(9, 0));
    var llegada = new RegistrarLlegadaCita(citas, agenda, autorizacion, clock);
    var inicio = new IniciarAtencionCita(citas, agenda, autorizacion, citaActual -> false, clock);
    var fin = new FinalizarAtencionCita(citas, agenda, autorizacion, clock);

    clock.set(Instant.parse("2026-10-12T14:05:00Z"));
    var enEspera = llegada.ejecutar(new RegistrarLlegadaCita.Comando(ACTOR, cita.id(), 0)).cita();
    var llegadaOriginal = enEspera.llegadaEn();
    var reintentoLlegada =
        llegada.ejecutar(new RegistrarLlegadaCita.Comando(ACTOR, cita.id(), 0)).cita();
    assertEquals(llegadaOriginal, reintentoLlegada.llegadaEn());
    assertEquals(1, reintentoLlegada.version());

    clock.set(Instant.parse("2026-10-12T14:10:00Z"));
    var enAtencion = inicio.ejecutar(new IniciarAtencionCita.Comando(ACTOR, cita.id(), 1)).cita();
    var inicioOriginal = enAtencion.atencionIniciadaEn();
    assertEquals(EstadoCita.EN_ATENCION, enAtencion.estado());
    assertEquals(
        inicioOriginal,
        inicio
            .ejecutar(new IniciarAtencionCita.Comando(ACTOR, cita.id(), 1))
            .cita()
            .atencionIniciadaEn());

    clock.set(Instant.parse("2026-10-12T14:45:00Z"));
    var atendida = fin.ejecutar(new FinalizarAtencionCita.Comando(ACTOR, cita.id(), 2)).cita();
    var finOriginal = atendida.atencionFinalizadaEn();
    assertEquals(EstadoCita.ATENDIDA, atendida.estado());
    assertEquals(
        finOriginal,
        fin.ejecutar(new FinalizarAtencionCita.Comando(ACTOR, cita.id(), 2))
            .cita()
            .atencionFinalizadaEn());
    assertEquals(3, citas.buscarPorId(cita.id()).orElseThrow().version());
  }

  @Test
  void llegadaSoloSePermiteEnElDiaLocal() {
    var cita = crear(CatalogoCitasMock.PROFESIONAL_1, LocalTime.of(9, 0));
    var llegada = new RegistrarLlegadaCita(citas, agenda, autorizacion, clock);
    clock.set(Instant.parse("2026-10-11T14:05:00Z"));
    assertThrows(
        OperacionCitaInvalida.class,
        () -> llegada.ejecutar(new RegistrarLlegadaCita.Comando(ACTOR, cita.id(), 0)));
    assertEquals(EstadoCita.PROGRAMADA, citas.buscarPorId(cita.id()).orElseThrow().estado());
  }

  @Test
  void inicioRechazaProfesionalOcupadoPorConflictoPendiente() {
    var cita = crear(CatalogoCitasMock.PROFESIONAL_1, LocalTime.of(9, 0));
    var llegada = new RegistrarLlegadaCita(citas, agenda, autorizacion, clock);
    clock.set(Instant.parse("2026-10-12T14:05:00Z"));
    llegada.ejecutar(new RegistrarLlegadaCita.Comando(ACTOR, cita.id(), 0));
    var inicio = new IniciarAtencionCita(citas, agenda, autorizacion, ignored -> true, clock);
    assertThrows(
        OperacionCitaInvalida.class,
        () -> inicio.ejecutar(new IniciarAtencionCita.Comando(ACTOR, cita.id(), 1)));
  }

  @Test
  void dashboardAgrupaLosSeisEstadosYRespetaFiltros() {
    var atendida = crear(CatalogoCitasMock.PROFESIONAL_1, LocalTime.of(9, 0));
    var llegada = new RegistrarLlegadaCita(citas, agenda, autorizacion, clock);
    clock.set(Instant.parse("2026-10-12T14:05:00Z"));
    llegada.ejecutar(new RegistrarLlegadaCita.Comando(ACTOR, atendida.id(), 0));
    var inicio = new IniciarAtencionCita(citas, agenda, autorizacion, ignored -> false, clock);
    inicio.ejecutar(new IniciarAtencionCita.Comando(ACTOR, atendida.id(), 1));
    clock.set(Instant.parse("2026-10-12T14:45:00Z"));
    new FinalizarAtencionCita(citas, agenda, autorizacion, clock)
        .ejecutar(new FinalizarAtencionCita.Comando(ACTOR, atendida.id(), 2));
    crear(CatalogoCitasMock.PROFESIONAL_1, LocalTime.of(10, 0));
    var resultado =
        new ConsultarDashboardCitas(citas, autorizacion, clock)
            .ejecutar(ACTOR, FECHA, CatalogoCitasMock.PROFESIONAL_1, null, null);
    assertEquals(2, resultado.total());
    assertEquals(1, resultado.atendidas());
    assertEquals(1, resultado.porEstado().get(EstadoCita.PROGRAMADA));
    assertEquals(0, resultado.enEspera());
  }

  private Cita crear(UUID profesional, LocalTime hora) {
    var cita =
        Cita.programar(
            UUID.randomUUID(),
            CatalogoCitasMock.PACIENTE_1,
            profesional,
            CatalogoCitasMock.SERVICIO_1,
            CatalogoCitasMock.CONSULTORIO_1,
            new HorarioCita(
                FECHA.atTime(hora).atZone(RegistrarLlegadaCita.ZONA_CENTRO).toInstant(),
                FECHA
                    .atTime(hora.plusMinutes(30))
                    .atZone(RegistrarLlegadaCita.ZONA_CENTRO)
                    .toInstant()),
            30,
            "PARTICULAR",
            new BigDecimal("50.00"),
            ACTOR,
            clock);
    citas.guardar(cita);
    return cita;
  }

  private static final class MutableClock extends Clock {
    private Instant instant;

    private MutableClock(Instant instant) {
      this.instant = instant;
    }

    private void set(Instant instant) {
      this.instant = instant;
    }

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return instant;
    }
  }

  private static final class MemoryCitas implements Citas {
    private final List<Cita> guardadas = new ArrayList<>();

    @Override
    public Optional<Cita> buscarPorId(UUID id) {
      return guardadas.stream().filter(x -> x.id().equals(id)).findFirst();
    }

    @Override
    public List<Cita> agenda(
        LocalDate fecha, UUID profesionalId, UUID servicioId, UUID consultorioId) {
      return guardadas.stream()
          .filter(x -> profesionalId == null || x.profesionalId().equals(profesionalId))
          .filter(x -> servicioId == null || x.servicioId().equals(servicioId))
          .filter(x -> consultorioId == null || x.consultorioId().equals(consultorioId))
          .toList();
    }

    @Override
    public boolean existeConflicto(
        UUID pacienteId,
        UUID profesionalId,
        UUID consultorioId,
        HorarioCita horario,
        UUID excluirId) {
      return guardadas.stream()
          .filter(x -> !x.id().equals(excluirId))
          .anyMatch(x -> x.horario().seCruzaCon(horario));
    }

    @Override
    public Cita guardar(Cita cita) {
      guardadas.add(cita);
      return cita;
    }

    @Override
    public Cita guardarTransicion(Cita cita) {
      var indice =
          guardadas.indexOf(
              guardadas.stream().filter(x -> x.id().equals(cita.id())).findFirst().orElseThrow());
      guardadas.set(indice, cita);
      return cita;
    }

    @Override
    public Cita guardarReprogramacion(Cita cita, Reprogramacion cambio) {
      return cita;
    }
  }
}
