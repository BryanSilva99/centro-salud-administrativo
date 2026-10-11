package pe.centrosalud.bootstrap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pe.centrosalud.adapters.outbound.mock.appointments.AutorizacionCitasDenegada;
import pe.centrosalud.adapters.outbound.mock.appointments.AutorizacionCitasMock;
import pe.centrosalud.adapters.outbound.mock.appointments.CatalogoCitasDenegado;
import pe.centrosalud.adapters.outbound.mock.appointments.CatalogoCitasMock;
import pe.centrosalud.adapters.outbound.mock.appointments.ConflictosAtencionDenegado;
import pe.centrosalud.adapters.outbound.mock.appointments.ConflictosAtencionMock;
import pe.centrosalud.adapters.outbound.mock.appointments.ImpactoEconomicoCitaDenegado;
import pe.centrosalud.adapters.outbound.mock.appointments.ImpactoEconomicoCitaMock;
import pe.centrosalud.adapters.outbound.mock.appointments.DisponibilidadCitasDenegada;
import pe.centrosalud.adapters.outbound.mock.appointments.DisponibilidadCitasMock;
import pe.centrosalud.modules.appointments.application.ports.AutorizacionCitas;
import pe.centrosalud.modules.appointments.application.ports.CatalogoCitas;
import pe.centrosalud.modules.appointments.application.ports.Citas;
import pe.centrosalud.modules.appointments.application.ports.ConflictosAtencion;
import pe.centrosalud.modules.appointments.application.ports.DisponibilidadCitas;
import pe.centrosalud.modules.appointments.application.ports.ImpactoEconomicoCita;
import pe.centrosalud.modules.appointments.application.usecases.ConsultarAgendaCitas;
import pe.centrosalud.modules.appointments.application.usecases.ConsultarContextoCitas;
import pe.centrosalud.modules.appointments.application.usecases.ConsultarDisponibilidadCitas;
import pe.centrosalud.modules.appointments.application.usecases.ProgramarCita;
import pe.centrosalud.modules.appointments.application.usecases.ReprogramarCita;
import pe.centrosalud.modules.appointments.application.usecases.RegistrarLlegadaCita;
import pe.centrosalud.modules.appointments.application.usecases.ConsultarAtencionCitas;
import pe.centrosalud.modules.appointments.application.usecases.ConsultarDashboardCitas;
import pe.centrosalud.modules.appointments.application.usecases.IniciarAtencionCita;
import pe.centrosalud.modules.appointments.application.usecases.FinalizarAtencionCita;
import pe.centrosalud.modules.appointments.application.usecases.CancelarCita;
import pe.centrosalud.modules.appointments.application.usecases.RegistrarAusenciaCita;
import pe.centrosalud.modules.appointments.application.usecases.RetirarEsperaCita;

@Configuration
public class AppointmentsConfiguration {
    @Bean CatalogoCitas catalogoCitas(@Value("${appointments.mock-enabled:false}") boolean demo) {
        return demo ? new CatalogoCitasMock() : new CatalogoCitasDenegado();
    }

    @Bean DisponibilidadCitas disponibilidadCitas(@Value("${appointments.mock-enabled:false}") boolean demo) {
        return demo ? new DisponibilidadCitasMock() : new DisponibilidadCitasDenegada();
    }

    @Bean AutorizacionCitas autorizacionCitas(@Value("${appointments.mock-enabled:false}") boolean demo) {
        return demo ? new AutorizacionCitasMock() : new AutorizacionCitasDenegada();
    }
    @Bean ConflictosAtencion conflictosAtencion(@Value("${appointments.mock-enabled:false}") boolean demo) {
        return demo ? new ConflictosAtencionMock() : new ConflictosAtencionDenegado();
    }
    @Bean ImpactoEconomicoCita impactoEconomicoCita(@Value("${appointments.mock-enabled:false}") boolean demo) {
        return demo ? new ImpactoEconomicoCitaMock() : new ImpactoEconomicoCitaDenegado();
    }

    @Bean ConsultarContextoCitas consultarContextoCitas(CatalogoCitas catalogo, AutorizacionCitas autorizacion) {
        return new ConsultarContextoCitas(catalogo, autorizacion);
    }
    @Bean ConsultarAgendaCitas consultarAgendaCitas(Citas citas, CatalogoCitas catalogo, AutorizacionCitas autorizacion) {
        return new ConsultarAgendaCitas(citas, catalogo, autorizacion);
    }
    @Bean ConsultarDisponibilidadCitas consultarDisponibilidadCitas(CatalogoCitas catalogo, DisponibilidadCitas disponibilidad,
                                                                     Citas citas, AutorizacionCitas autorizacion, java.time.Clock clock) {
        return new ConsultarDisponibilidadCitas(catalogo, disponibilidad, citas, autorizacion, clock);
    }
    @Bean ProgramarCita programarCita(Citas citas, CatalogoCitas catalogo, DisponibilidadCitas disponibilidad,
                                      AutorizacionCitas autorizacion, java.time.Clock clock) {
        return new ProgramarCita(citas, catalogo, disponibilidad, autorizacion, clock);
    }
    @Bean ReprogramarCita reprogramarCita(Citas citas, CatalogoCitas catalogo, DisponibilidadCitas disponibilidad,
                                           AutorizacionCitas autorizacion, java.time.Clock clock) {
        return new ReprogramarCita(citas, catalogo, disponibilidad, autorizacion, clock);
    }
    @Bean RegistrarLlegadaCita registrarLlegadaCita(Citas citas, ConsultarAgendaCitas agenda,
                                                     AutorizacionCitas autorizacion, java.time.Clock clock) {
        return new RegistrarLlegadaCita(citas, agenda, autorizacion, clock);
    }
    @Bean ConsultarAtencionCitas consultarAtencionCitas(Citas citas, ConsultarAgendaCitas agenda,
                                                         AutorizacionCitas autorizacion, java.time.Clock clock) {
        return new ConsultarAtencionCitas(citas, agenda, autorizacion, clock);
    }
    @Bean IniciarAtencionCita iniciarAtencionCita(Citas citas, ConsultarAgendaCitas agenda,
                                                   AutorizacionCitas autorizacion, ConflictosAtencion conflictos,
                                                   java.time.Clock clock) {
        return new IniciarAtencionCita(citas, agenda, autorizacion, conflictos, clock);
    }
    @Bean FinalizarAtencionCita finalizarAtencionCita(Citas citas, ConsultarAgendaCitas agenda,
                                                       AutorizacionCitas autorizacion, java.time.Clock clock) {
        return new FinalizarAtencionCita(citas, agenda, autorizacion, clock);
    }
    @Bean CancelarCita cancelarCita(Citas citas, ConsultarAgendaCitas agenda, AutorizacionCitas autorizacion,
                                    ImpactoEconomicoCita impactoEconomico, java.time.Clock clock) {
        return new CancelarCita(citas, agenda, autorizacion, impactoEconomico, clock);
    }
    @Bean RegistrarAusenciaCita registrarAusenciaCita(Citas citas, ConsultarAgendaCitas agenda,
                                                       AutorizacionCitas autorizacion, java.time.Clock clock) {
        return new RegistrarAusenciaCita(citas, agenda, autorizacion, clock);
    }
    @Bean RetirarEsperaCita retirarEsperaCita(Citas citas, ConsultarAgendaCitas agenda, AutorizacionCitas autorizacion,
                                               ImpactoEconomicoCita impactoEconomico, java.time.Clock clock) {
        return new RetirarEsperaCita(citas, agenda, autorizacion, impactoEconomico, clock);
    }
    @Bean ConsultarDashboardCitas consultarDashboardCitas(Citas citas, AutorizacionCitas autorizacion,
                                                           java.time.Clock clock) {
        return new ConsultarDashboardCitas(citas, autorizacion, clock);
    }
}
