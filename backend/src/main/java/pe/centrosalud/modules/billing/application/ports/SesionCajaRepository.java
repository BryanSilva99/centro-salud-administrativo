package pe.centrosalud.modules.billing.application.ports;

import java.util.Optional;
import java.util.UUID;
import pe.centrosalud.modules.billing.domain.SesionCaja;

/** Contrato de persistencia para sesiones de caja; implementado por PostgreSQL. */
public interface SesionCajaRepository {
    SesionCaja guardar(SesionCaja sesion);
    Optional<SesionCaja> buscarPorId(UUID id);
    Optional<SesionCaja> buscarSesionAbiertaPorCajero(UUID cajeroId);
}
