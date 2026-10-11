package pe.centrosalud.bootstrap;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pe.centrosalud.modules.administration.application.ports.Pacientes;
import pe.centrosalud.modules.administration.application.usecases.RegistrarPaciente;

@Configuration
public class AdministrationConfiguration {
  @Bean
  RegistrarPaciente registrarPaciente(Pacientes pacientes, Clock clock) {
    return new RegistrarPaciente(pacientes, clock);
  }
}
