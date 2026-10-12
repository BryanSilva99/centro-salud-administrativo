package pe.centrosalud.bootstrap;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pe.centrosalud.adapters.outbound.modules.appointments.*;
import pe.centrosalud.modules.administration.application.ports.*;
import pe.centrosalud.modules.appointments.application.ports.AutorizacionCitas;
import pe.centrosalud.modules.appointments.application.ports.CatalogoCitas;
import pe.centrosalud.modules.appointments.application.ports.Citas;
import pe.centrosalud.modules.appointments.application.ports.ConflictosAtencion;
import pe.centrosalud.modules.appointments.application.ports.DisponibilidadCitas;
import pe.centrosalud.modules.appointments.application.ports.ImpactoEconomicoCita;
import pe.centrosalud.modules.appointments.application.usecases.CancelarCita;
import pe.centrosalud.modules.appointments.application.usecases.ConsultarAgendaCitas;
import pe.centrosalud.modules.appointments.application.usecases.ConsultarAtencionCitas;
import pe.centrosalud.modules.appointments.application.usecases.ConsultarContextoCitas;
import pe.centrosalud.modules.appointments.application.usecases.ConsultarDashboardCitas;
import pe.centrosalud.modules.appointments.application.usecases.ConsultarDisponibilidadCitas;
import pe.centrosalud.modules.appointments.application.usecases.FinalizarAtencionCita;
import pe.centrosalud.modules.appointments.application.usecases.IniciarAtencionCita;
import pe.centrosalud.modules.appointments.application.usecases.ProgramarCita;
import pe.centrosalud.modules.appointments.application.usecases.RegistrarAusenciaCita;
import pe.centrosalud.modules.appointments.application.usecases.RegistrarLlegadaCita;
import pe.centrosalud.modules.appointments.application.usecases.ReprogramarCita;
import pe.centrosalud.modules.appointments.application.usecases.RetirarEsperaCita;
import pe.centrosalud.modules.identity.application.ports.DirectorioRoles;

@Configuration
public class AppointmentsConfiguration {
  @Bean
  CatalogoCitas catalogoCitas(Pacientes p, FichasProfesionales f, Servicios s, Consultorios c) {
    return new CatalogoCitasAdministracion(p, f, s, c);
  }

  @Bean
  DisponibilidadCitas disponibilidadCitas(
      Disponibilidad t, FichasProfesionales p, Consultorios c, Servicios s) {
    return new DisponibilidadCitasAdministracion(t, p, c, s);
  }

  @Bean
  AutorizacionCitas autorizacionCitas(DirectorioRoles c, FichasProfesionales p) {
    return new AutorizacionCitasIdentidad(c, p);
  }

  @Bean
  ConflictosAtencion conflictosAtencion(DisponibilidadCitas disponibilidad) {
    return cita ->
        disponibilidad
            .intervalosBase(
                cita.horario()
                    .inicio()
                    .atZone(ConsultarDisponibilidadCitas.ZONA_CENTRO)
                    .toLocalDate(),
                cita.profesionalId(),
                cita.servicioId(),
                cita.consultorioId())
            .stream()
            .noneMatch(
                base ->
                    !cita.horario().inicio().isBefore(base.inicio())
                        && !cita.horario().fin().isAfter(base.fin()));
  }

  @Bean
  ImpactoEconomicoCita impactoEconomicoCita(
      pe.centrosalud.modules.billing.application.ports.CargoRepository cargos) {
    return id ->
        cargos.buscarPorCita(id).stream()
            .anyMatch(pe.centrosalud.modules.billing.domain.Cargo::estaVigente);
  }

  @Bean
  ConsultarContextoCitas consultarContextoCitas(
      CatalogoCitas catalogo, AutorizacionCitas autorizacion) {
    return new ConsultarContextoCitas(catalogo, autorizacion);
  }

  @Bean
  ConsultarAgendaCitas consultarAgendaCitas(
      Citas citas, CatalogoCitas catalogo, AutorizacionCitas autorizacion) {
    return new ConsultarAgendaCitas(citas, catalogo, autorizacion);
  }

  @Bean
  ConsultarDisponibilidadCitas consultarDisponibilidadCitas(
      CatalogoCitas catalogo,
      DisponibilidadCitas disponibilidad,
      Citas citas,
      AutorizacionCitas autorizacion,
      java.time.Clock clock) {
    return new ConsultarDisponibilidadCitas(catalogo, disponibilidad, citas, autorizacion, clock);
  }

  @Bean
  ProgramarCita programarCita(
      Citas citas,
      CatalogoCitas catalogo,
      DisponibilidadCitas disponibilidad,
      AutorizacionCitas autorizacion,
      java.time.Clock clock) {
    return new ProgramarCita(citas, catalogo, disponibilidad, autorizacion, clock);
  }

  @Bean
  ReprogramarCita reprogramarCita(
      Citas citas,
      CatalogoCitas catalogo,
      DisponibilidadCitas disponibilidad,
      AutorizacionCitas autorizacion,
      java.time.Clock clock) {
    return new ReprogramarCita(citas, catalogo, disponibilidad, autorizacion, clock);
  }

  @Bean
  RegistrarLlegadaCita registrarLlegadaCita(
      Citas citas,
      ConsultarAgendaCitas agenda,
      AutorizacionCitas autorizacion,
      java.time.Clock clock) {
    return new RegistrarLlegadaCita(citas, agenda, autorizacion, clock);
  }

  @Bean
  ConsultarAtencionCitas consultarAtencionCitas(
      Citas citas,
      ConsultarAgendaCitas agenda,
      AutorizacionCitas autorizacion,
      java.time.Clock clock) {
    return new ConsultarAtencionCitas(citas, agenda, autorizacion, clock);
  }

  @Bean
  IniciarAtencionCita iniciarAtencionCita(
      Citas citas,
      ConsultarAgendaCitas agenda,
      AutorizacionCitas autorizacion,
      ConflictosAtencion conflictos,
      java.time.Clock clock) {
    return new IniciarAtencionCita(citas, agenda, autorizacion, conflictos, clock);
  }

  @Bean
  FinalizarAtencionCita finalizarAtencionCita(
      Citas citas,
      ConsultarAgendaCitas agenda,
      AutorizacionCitas autorizacion,
      java.time.Clock clock) {
    return new FinalizarAtencionCita(citas, agenda, autorizacion, clock);
  }

  @Bean
  CancelarCita cancelarCita(
      Citas citas,
      ConsultarAgendaCitas agenda,
      AutorizacionCitas autorizacion,
      ImpactoEconomicoCita impactoEconomico,
      java.time.Clock clock) {
    return new CancelarCita(citas, agenda, autorizacion, impactoEconomico, clock);
  }

  @Bean
  RegistrarAusenciaCita registrarAusenciaCita(
      Citas citas,
      ConsultarAgendaCitas agenda,
      AutorizacionCitas autorizacion,
      java.time.Clock clock) {
    return new RegistrarAusenciaCita(citas, agenda, autorizacion, clock);
  }

  @Bean
  RetirarEsperaCita retirarEsperaCita(
      Citas citas,
      ConsultarAgendaCitas agenda,
      AutorizacionCitas autorizacion,
      ImpactoEconomicoCita impactoEconomico,
      java.time.Clock clock) {
    return new RetirarEsperaCita(citas, agenda, autorizacion, impactoEconomico, clock);
  }

  @Bean
  ConsultarDashboardCitas consultarDashboardCitas(
      Citas citas, AutorizacionCitas autorizacion, java.time.Clock clock) {
    return new ConsultarDashboardCitas(citas, autorizacion, clock);
  }
}
