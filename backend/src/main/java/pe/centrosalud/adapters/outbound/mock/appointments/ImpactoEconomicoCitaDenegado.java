package pe.centrosalud.adapters.outbound.mock.appointments;

import java.util.UUID;
import pe.centrosalud.modules.appointments.application.ports.ImpactoEconomicoCita;

/** Placeholder seguro hasta que caja publique su contrato. */
public final class ImpactoEconomicoCitaDenegado implements ImpactoEconomicoCita {
    @Override public boolean requiereGestion(UUID citaId) { return false; }
}
