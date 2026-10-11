package pe.centrosalud.bootstrap;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pe.centrosalud.adapters.outbound.postgres.billing.CargoRepositoryPostgres;
import pe.centrosalud.adapters.outbound.postgres.billing.PagoRepositoryPostgres;
import pe.centrosalud.adapters.outbound.postgres.billing.SesionCajaRepositoryPostgres;
import pe.centrosalud.adapters.outbound.postgres.billing.SolicitudReversionRepositoryPostgres;
import pe.centrosalud.modules.billing.application.ports.CargoRepository;
import pe.centrosalud.modules.billing.application.ports.PagoRepository;
import pe.centrosalud.modules.billing.application.ports.SesionCajaRepository;
import pe.centrosalud.modules.billing.application.ports.SolicitudReversionRepository;
import pe.centrosalud.modules.billing.application.usecases.AbrirSesionCaja;
import pe.centrosalud.modules.billing.application.usecases.AprobarReversion;
import pe.centrosalud.modules.billing.application.usecases.CerrarCaja;
import pe.centrosalud.modules.billing.application.usecases.ConsultarReporteEconomico;
import pe.centrosalud.modules.billing.application.usecases.ConsultarSaldo;
import pe.centrosalud.modules.billing.application.usecases.RegistrarCargo;
import pe.centrosalud.modules.billing.application.usecases.RegistrarPago;
import pe.centrosalud.modules.billing.application.usecases.SolicitarReversion;

/** Configuración de beans del módulo de caja. */
@Configuration
public class BillingConfiguration {

    @Bean
    CargoRepository cargoRepository(CargoRepositoryPostgres impl) {
        return impl;
    }

    @Bean
    PagoRepository pagoRepository(PagoRepositoryPostgres impl) {
        return impl;
    }

    @Bean
    SesionCajaRepository sesionCajaRepository(SesionCajaRepositoryPostgres impl) {
        return impl;
    }

    @Bean
    SolicitudReversionRepository solicitudReversionRepository(SolicitudReversionRepositoryPostgres impl) {
        return impl;
    }

    @Bean
    RegistrarCargo registrarCargo(CargoRepository cargos) {
        return new RegistrarCargo(cargos);
    }

    @Bean
    AbrirSesionCaja abrirSesionCaja(SesionCajaRepository sesiones) {
        return new AbrirSesionCaja(sesiones);
    }

    @Bean
    RegistrarPago registrarPago(PagoRepository pagos, CargoRepository cargos, SesionCajaRepository sesiones) {
        return new RegistrarPago(pagos, cargos, sesiones);
    }

    @Bean
    ConsultarSaldo consultarSaldo(CargoRepository cargos, PagoRepository pagos) {
        return new ConsultarSaldo(cargos, pagos);
    }

    @Bean
    SolicitarReversion solicitarReversion(SolicitudReversionRepository solicitudes) {
        return new SolicitarReversion(solicitudes);
    }

    @Bean
    AprobarReversion aprobarReversion(SolicitudReversionRepository solicitudes) {
        return new AprobarReversion(solicitudes);
    }

    @Bean
    CerrarCaja cerrarCaja(SesionCajaRepository sesiones, PagoRepository pagos) {
        return new CerrarCaja(sesiones, pagos);
    }

    @Bean
    ConsultarReporteEconomico consultarReporteEconomico(PagoRepository pagos) {
        return new ConsultarReporteEconomico(pagos);
    }
}
