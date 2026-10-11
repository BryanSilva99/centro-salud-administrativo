package pe.centrosalud.modules.appointments.application.ports;

import java.util.UUID;

/** Consulta reemplazable del estado económico, sin acoplar appointments a caja. */
public interface ImpactoEconomicoCita {
    boolean requiereGestion(UUID citaId);
}
