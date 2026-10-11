package pe.centrosalud.bootstrap;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pe.centrosalud.modules.administration.application.ports.Pacientes;
import pe.centrosalud.modules.administration.application.ports.Servicios;
import pe.centrosalud.modules.administration.application.usecases.GestionarServicios;
import pe.centrosalud.modules.administration.application.usecases.RegistrarPaciente;

@Configuration
public class AdministrationConfiguration {
    @Bean
    GestionarServicios gestionarServicios(Servicios servicios) {
        return new GestionarServicios(servicios);
    }

    @Bean
    RegistrarPaciente registrarPaciente(Pacientes pacientes, Clock clock) {
        return new RegistrarPaciente(pacientes, clock);
    }
}
