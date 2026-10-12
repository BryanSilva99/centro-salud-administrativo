package pe.centrosalud.bootstrap;

import org.springframework.context.annotation.*;
import pe.centrosalud.adapters.outbound.modules.billing.*;
import pe.centrosalud.modules.administration.application.ports.Pacientes;
import pe.centrosalud.modules.appointments.application.ports.Citas;
import pe.centrosalud.modules.billing.application.ports.*;
import pe.centrosalud.modules.billing.application.usecases.CajaIntegrada;
import pe.centrosalud.modules.identity.application.ports.DirectorioRoles;

@Configuration
public class BillingConfiguration {
  @Bean
  CajaIntegrada cajaIntegrada(
      CargoRepository cargos,
      PagoRepository pagos,
      SesionCajaRepository sesiones,
      SolicitudReversionRepository solicitudes,
      RegistroCaja registro,
      DirectorioRoles cuentas,
      Citas citas,
      Pacientes pacientes) {
    return new CajaIntegrada(
        cargos,
        pagos,
        sesiones,
        solicitudes,
        registro,
        new AccesoCajaIdentidad(cuentas),
        new ReservasCajaCitas(citas, pacientes));
  }
}
