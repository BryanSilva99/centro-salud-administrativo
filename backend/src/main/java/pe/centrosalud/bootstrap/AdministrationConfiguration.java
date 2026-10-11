package pe.centrosalud.bootstrap;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pe.centrosalud.modules.administration.application.ports.Consultorios;
import pe.centrosalud.modules.administration.application.ports.Disponibilidad;
import pe.centrosalud.modules.administration.application.ports.FichasProfesionales;
import pe.centrosalud.modules.administration.application.ports.Pacientes;
import pe.centrosalud.modules.administration.application.ports.Servicios;
import pe.centrosalud.modules.administration.application.usecases.ActualizarPaciente;
import pe.centrosalud.modules.administration.application.usecases.GestionarConsultorios;
import pe.centrosalud.modules.administration.application.usecases.GestionarDisponibilidad;
import pe.centrosalud.modules.administration.application.usecases.GestionarProfesionales;
import pe.centrosalud.modules.administration.application.usecases.GestionarServicios;
import pe.centrosalud.modules.administration.application.usecases.RegistrarPaciente;

@Configuration
public class AdministrationConfiguration {
  @Bean
  GestionarDisponibilidad gestionarDisponibilidad(
      Disponibilidad disponibilidad, FichasProfesionales fichas, Consultorios consultorios) {
    return new GestionarDisponibilidad(disponibilidad, fichas, consultorios);
  }

  @Bean
  GestionarServicios gestionarServicios(Servicios servicios) {
    return new GestionarServicios(servicios);
  }

  @Bean
  GestionarConsultorios gestionarConsultorios(Consultorios consultorios, Servicios servicios) {
    return new GestionarConsultorios(consultorios, servicios);
  }

  @Bean
  GestionarProfesionales gestionarProfesionales(FichasProfesionales fichas, Servicios servicios) {
    return new GestionarProfesionales(fichas, servicios);
  }

  @Bean
  RegistrarPaciente registrarPaciente(Pacientes pacientes, Clock clock) {
    return new RegistrarPaciente(pacientes, clock);
  }

  @Bean
  ActualizarPaciente actualizarPaciente(Pacientes pacientes, Clock clock) {
    return new ActualizarPaciente(pacientes, clock);
  }
}
