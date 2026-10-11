package pe.centrosalud.modules.appointments.application.ports;

import java.util.UUID;

/** Autorización de citas basada en los roles y vínculo profesional vigentes. */
public interface AutorizacionCitas {
  PerfilCitas perfil(UUID cuentaId);

  default void verificar(UUID cuentaId, OperacionCita operacion) {
    if (!perfil(cuentaId).puede(operacion)) {
      throw new pe.centrosalud.modules.appointments.application.usecases.AccesoCitasDenegado();
    }
  }
}
