package pe.centrosalud.modules.appointments.application.ports;

import java.util.UUID;

/** Contrato temporal para autorización; identidad aportará la implementación real después. */
public interface AutorizacionCitas {
    PerfilCitas perfil(UUID cuentaId);

    default void verificar(UUID cuentaId, OperacionCita operacion) {
        if (!perfil(cuentaId).puede(operacion)) {
            throw new pe.centrosalud.modules.appointments.application.usecases.AccesoCitasDenegado();
        }
    }
}
