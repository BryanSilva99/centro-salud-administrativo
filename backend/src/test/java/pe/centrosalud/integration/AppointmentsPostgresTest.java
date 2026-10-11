package pe.centrosalud.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import pe.centrosalud.adapters.outbound.mock.appointments.CatalogoCitasMock;
import pe.centrosalud.modules.appointments.application.ports.Citas;
import pe.centrosalud.modules.appointments.application.usecases.CancelarCita;
import pe.centrosalud.modules.appointments.application.usecases.FinalizarAtencionCita;
import pe.centrosalud.modules.appointments.application.usecases.IniciarAtencionCita;
import pe.centrosalud.modules.appointments.application.usecases.ProgramarCita;
import pe.centrosalud.modules.appointments.application.usecases.RegistrarAusenciaCita;
import pe.centrosalud.modules.appointments.application.usecases.RegistrarLlegadaCita;
import pe.centrosalud.modules.appointments.application.usecases.ReprogramarCita;
import pe.centrosalud.modules.appointments.application.usecases.RetirarEsperaCita;
import pe.centrosalud.modules.appointments.domain.Cita;
import pe.centrosalud.modules.appointments.domain.EstadoCita;
import pe.centrosalud.modules.appointments.domain.HorarioCita;
import pe.centrosalud.modules.appointments.domain.TipoCierreCita;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@org.springframework.context.annotation.Import(AppointmentsPostgresTest.Fixtures.class)
@EnabledIfEnvironmentVariable(named = "RUN_POSTGRES_TESTS", matches = "true")
class AppointmentsPostgresTest {
  @org.springframework.boot.test.context.TestConfiguration
  static class Fixtures {
    @org.springframework.context.annotation.Bean
    @org.springframework.context.annotation.Primary
    pe.centrosalud.modules.appointments.application.ports.CatalogoCitas catalogoFixture() {
      return new CatalogoCitasMock();
    }

    @org.springframework.context.annotation.Bean
    @org.springframework.context.annotation.Primary
    pe.centrosalud.modules.appointments.application.ports.DisponibilidadCitas
        disponibilidadFixture() {
      return new pe.centrosalud.adapters.outbound.mock.appointments.DisponibilidadCitasMock();
    }

    @org.springframework.context.annotation.Bean
    @org.springframework.context.annotation.Primary
    pe.centrosalud.modules.appointments.application.ports.AutorizacionCitas autorizacionFixture() {
      return new pe.centrosalud.adapters.outbound.mock.appointments.AutorizacionCitasMock();
    }

    @org.springframework.context.annotation.Bean
    @org.springframework.context.annotation.Primary
    pe.centrosalud.modules.appointments.application.ports.ConflictosAtencion conflictosFixture() {
      return new pe.centrosalud.adapters.outbound.mock.appointments.ConflictosAtencionMock();
    }

    @org.springframework.context.annotation.Bean
    @org.springframework.context.annotation.Primary
    pe.centrosalud.modules.appointments.application.ports.ImpactoEconomicoCita impactoFixture() {
      return new pe.centrosalud.adapters.outbound.mock.appointments.ImpactoEconomicoCitaMock();
    }
  }

  private static final UUID ACTOR = UUID.fromString("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb");
  @Autowired JdbcTemplate jdbc;
  @Autowired ProgramarCita programar;
  @Autowired ReprogramarCita reprogramar;
  @Autowired Citas citas;
  @Autowired RegistrarLlegadaCita registrarLlegada;
  @Autowired IniciarAtencionCita iniciarAtencion;
  @Autowired FinalizarAtencionCita finalizarAtencion;
  @Autowired CancelarCita cancelar;
  @Autowired RegistrarAusenciaCita registrarAusencia;
  @Autowired RetirarEsperaCita retirarEspera;
  @Autowired MockMvc mvc;

  @BeforeEach
  void limpiar() {
    jdbc.update("DELETE FROM reprogramaciones_cita");
    jdbc.update("DELETE FROM citas");
  }

  @Test
  void persisteYReconstruyeUnaCita() {
    var fecha = siguienteDiaHabil();
    var creada =
        programar.ejecutar(
            new ProgramarCita.Comando(
                ACTOR,
                CatalogoCitasMock.PACIENTE_1,
                CatalogoCitasMock.PROFESIONAL_1,
                CatalogoCitasMock.SERVICIO_1,
                CatalogoCitasMock.CONSULTORIO_1,
                "PARTICULAR",
                fecha,
                LocalTime.of(9, 0)));
    var reconstruida = citas.buscarPorId(creada.cita().id()).orElseThrow();
    assertEquals(creada.cita().id(), reconstruida.id());
    assertEquals(50, reconstruida.importe().intValue());
    assertEquals(1, jdbc.queryForObject("select count(*) from citas", Integer.class));
  }

  @Test
  void reprogramaEnUnaTransaccionYConservaHistorial() {
    var fecha = siguienteDiaHabil();
    var creada =
        programar.ejecutar(
            new ProgramarCita.Comando(
                ACTOR,
                CatalogoCitasMock.PACIENTE_2,
                CatalogoCitasMock.PROFESIONAL_1,
                CatalogoCitasMock.SERVICIO_1,
                CatalogoCitasMock.CONSULTORIO_1,
                "SIN_CARGO",
                fecha,
                LocalTime.of(9, 0)));
    var actualizada =
        reprogramar.ejecutar(
            new ReprogramarCita.Comando(
                ACTOR,
                creada.cita().id(),
                fecha,
                LocalTime.of(10, 0),
                "Ajuste de agenda",
                creada.cita().version()));
    assertEquals(creada.cita().id(), actualizada.cita().id());
    assertEquals(
        1, jdbc.queryForObject("select count(*) from reprogramaciones_cita", Integer.class));
    assertEquals(0, actualizada.cita().importe().intValue());
  }

  @Test
  void unaRutaDeCitasSinSesionSigueProtegida() throws Exception {
    mvc.perform(get("/api/citas/contexto")).andExpect(status().isUnauthorized());
  }

  @Test
  void losCierresSinSesionSiguenProtegidos() throws Exception {
    mvc.perform(
            put("/api/citas/{id}/cancelacion", UUID.randomUUID())
                .contentType("application/json")
                .content("{\"motivo\":\"Solicitud\",\"version\":0}"))
        // Unauthenticated unsafe requests are stopped by CSRF before the controller.
        .andExpect(status().isForbidden());
  }

  @Test
  void persisteLlegadaInicioYFinConActoresYVersion() {
    var fecha = LocalDate.now(ZoneId.of("America/Lima"));
    var inicio = fecha.atTime(8, 0).atZone(ZoneId.of("America/Lima")).toInstant();
    var cita =
        Cita.programar(
            UUID.randomUUID(),
            CatalogoCitasMock.PACIENTE_3,
            CatalogoCitasMock.PROFESIONAL_1,
            CatalogoCitasMock.SERVICIO_1,
            CatalogoCitasMock.CONSULTORIO_1,
            new HorarioCita(inicio, inicio.plusSeconds(1800)),
            30,
            "PARTICULAR",
            java.math.BigDecimal.valueOf(50),
            ACTOR,
            java.time.Clock.systemUTC());
    citas.guardar(cita);
    var enEspera =
        registrarLlegada.ejecutar(new RegistrarLlegadaCita.Comando(ACTOR, cita.id(), 0)).cita();
    var enAtencion =
        iniciarAtencion
            .ejecutar(new IniciarAtencionCita.Comando(ACTOR, cita.id(), enEspera.version()))
            .cita();
    var atendida =
        finalizarAtencion
            .ejecutar(new FinalizarAtencionCita.Comando(ACTOR, cita.id(), enAtencion.version()))
            .cita();

    assertEquals(3, atendida.version());
    assertEquals(
        1,
        jdbc.queryForObject(
            "select count(*) from citas where llegada_en is not null and atencion_iniciada_en is not null and atencion_finalizada_en is not null",
            Integer.class));
    assertEquals(
        ACTOR,
        jdbc.queryForObject("select llegada_por from citas where id = ?", UUID.class, cita.id()));
  }

  @Test
  void persisteCierreYLiberaElHorarioCancelado() {
    var fecha = siguienteDiaHabil();
    var creada =
        programar.ejecutar(
            new ProgramarCita.Comando(
                ACTOR,
                CatalogoCitasMock.PACIENTE_1,
                CatalogoCitasMock.PROFESIONAL_1,
                CatalogoCitasMock.SERVICIO_1,
                CatalogoCitasMock.CONSULTORIO_1,
                "PARTICULAR",
                fecha,
                LocalTime.of(9, 0)));

    var cerrada =
        cancelar.ejecutar(
            new CancelarCita.Comando(
                ACTOR, creada.cita().id(), "Solicitud del paciente", creada.cita().version()));
    var reconstruida = citas.buscarPorId(creada.cita().id()).orElseThrow();

    assertEquals(EstadoCita.CANCELADA, cerrada.detalle().cita().estado());
    assertEquals(TipoCierreCita.CANCELACION, reconstruida.tipoCierre());
    assertEquals("Solicitud del paciente", reconstruida.motivoCierre());
    assertNotNull(reconstruida.cerradaEn());
    assertEquals(ACTOR, reconstruida.cerradaPor());

    var reutilizada =
        programar.ejecutar(
            new ProgramarCita.Comando(
                ACTOR,
                CatalogoCitasMock.PACIENTE_2,
                CatalogoCitasMock.PROFESIONAL_1,
                CatalogoCitasMock.SERVICIO_1,
                CatalogoCitasMock.CONSULTORIO_1,
                "SIN_CARGO",
                fecha,
                LocalTime.of(9, 0)));
    assertEquals(0, reutilizada.cita().importe().intValue());
  }

  @Test
  void persisteAusenciaYRetiroConservandoLaLlegada() {
    var fecha = LocalDate.now(ZoneId.of("America/Lima"));
    var inicio = fecha.atTime(8, 0).atZone(ZoneId.of("America/Lima")).toInstant();
    var ausencia =
        Cita.programar(
            UUID.randomUUID(),
            CatalogoCitasMock.PACIENTE_3,
            CatalogoCitasMock.PROFESIONAL_1,
            CatalogoCitasMock.SERVICIO_1,
            CatalogoCitasMock.CONSULTORIO_1,
            new HorarioCita(inicio, inicio.plusSeconds(1800)),
            30,
            "PARTICULAR",
            java.math.BigDecimal.valueOf(50),
            ACTOR,
            java.time.Clock.systemUTC());
    citas.guardar(ausencia);
    var marcada =
        registrarAusencia.ejecutar(new RegistrarAusenciaCita.Comando(ACTOR, ausencia.id(), 0));
    assertEquals(EstadoCita.AUSENTE, marcada.cita().estado());
    assertEquals(
        TipoCierreCita.AUSENCIA, citas.buscarPorId(ausencia.id()).orElseThrow().tipoCierre());

    var esperaInicio = fecha.atTime(9, 0).atZone(ZoneId.of("America/Lima")).toInstant();
    var espera =
        Cita.programar(
            UUID.randomUUID(),
            CatalogoCitasMock.PACIENTE_2,
            CatalogoCitasMock.PROFESIONAL_1,
            CatalogoCitasMock.SERVICIO_1,
            CatalogoCitasMock.CONSULTORIO_1,
            new HorarioCita(esperaInicio, esperaInicio.plusSeconds(1800)),
            30,
            "PARTICULAR",
            java.math.BigDecimal.valueOf(50),
            ACTOR,
            java.time.Clock.systemUTC());
    citas.guardar(espera);
    var enEspera =
        registrarLlegada.ejecutar(new RegistrarLlegadaCita.Comando(ACTOR, espera.id(), 0)).cita();
    var retirado =
        retirarEspera.ejecutar(
            new RetirarEsperaCita.Comando(
                ACTOR, espera.id(), "Paciente se retira", enEspera.version()));
    var persistido = citas.buscarPorId(espera.id()).orElseThrow();
    assertEquals(EstadoCita.CANCELADA, retirado.detalle().cita().estado());
    assertEquals(TipoCierreCita.RETIRO_ESPERA, persistido.tipoCierre());
    assertEquals(enEspera.llegadaEn(), persistido.llegadaEn());
    assertNull(persistido.atencionIniciadaEn());
  }

  private LocalDate siguienteDiaHabil() {
    var fecha = LocalDate.now(ZoneId.of("America/Lima")).plusDays(1);
    while (fecha.getDayOfWeek() == DayOfWeek.SATURDAY || fecha.getDayOfWeek() == DayOfWeek.SUNDAY)
      fecha = fecha.plusDays(1);
    return fecha;
  }
}
