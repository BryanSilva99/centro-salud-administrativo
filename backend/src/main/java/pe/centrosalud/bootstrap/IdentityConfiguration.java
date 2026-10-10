package pe.centrosalud.bootstrap;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pe.centrosalud.modules.identity.application.ports.ConsultarCuentas;
import pe.centrosalud.modules.identity.application.usecases.ConsultarCuenta;
import pe.centrosalud.modules.identity.application.usecases.VerificarCredenciales;
import pe.centrosalud.modules.identity.application.ports.ConsultarCredenciales;
import pe.centrosalud.modules.identity.application.ports.HashContrasenas;

@Configuration
public class IdentityConfiguration {
    @Bean
    java.time.Clock clock() { return java.time.Clock.systemUTC(); }

    @Bean
    pe.centrosalud.modules.identity.application.usecases.IniciarSesion iniciarSesion(
            VerificarCredenciales verificar, pe.centrosalud.modules.identity.application.ports.ControlIntentos intentos) {
        return new pe.centrosalud.modules.identity.application.usecases.IniciarSesion(verificar, intentos);
    }

    @Bean
    VerificarCredenciales verificarCredenciales(ConsultarCredenciales credenciales, HashContrasenas hashes) {
        return new VerificarCredenciales(credenciales, hashes);
    }

    @Bean
    ConsultarCuenta consultarCuenta(ConsultarCuentas cuentas) {
        return new ConsultarCuenta(cuentas);
    }
}
