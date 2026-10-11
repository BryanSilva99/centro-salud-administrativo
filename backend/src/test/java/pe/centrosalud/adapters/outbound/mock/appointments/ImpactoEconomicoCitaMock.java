package pe.centrosalud.adapters.outbound.mock.appointments;

import java.util.UUID;
import pe.centrosalud.modules.appointments.application.ports.ImpactoEconomicoCita;

/** En el demo no existen tablas de caja ni movimientos económicos. */
public final class ImpactoEconomicoCitaMock implements ImpactoEconomicoCita {
  @Override
  public boolean requiereGestion(UUID citaId) {
    return false;
  }
}
