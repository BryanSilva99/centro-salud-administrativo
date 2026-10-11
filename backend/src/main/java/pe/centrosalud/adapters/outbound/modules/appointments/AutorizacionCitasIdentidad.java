package pe.centrosalud.adapters.outbound.modules.appointments;

import java.util.EnumSet;
import java.util.UUID;
import pe.centrosalud.modules.administration.application.ports.FichasProfesionales;
import pe.centrosalud.modules.appointments.application.ports.*;
import pe.centrosalud.modules.appointments.application.usecases.AccesoCitasDenegado;
import pe.centrosalud.modules.identity.application.ports.DirectorioRoles;
import pe.centrosalud.modules.identity.domain.Rol;

/** Lee roles y vínculo actuales por contratos públicos en cada operación. */
public final class AutorizacionCitasIdentidad implements AutorizacionCitas {
  private final DirectorioRoles cuentas;
  private final FichasProfesionales profesionales;

  public AutorizacionCitasIdentidad(DirectorioRoles cuentas, FichasProfesionales profesionales) {
    this.cuentas = cuentas;
    this.profesionales = profesionales;
  }

  public PerfilCitas perfil(UUID id) {
    var cuenta = cuentas.cargar(id);
    if (!cuenta.activa()) throw new AccesoCitasDenegado();
    var operaciones = EnumSet.noneOf(OperacionCita.class);
    UUID profesional = null;
    if (cuenta.roles().contains(Rol.REC) || cuenta.roles().contains(Rol.ADM)) {
      operaciones.addAll(
          EnumSet.of(
              OperacionCita.CONSULTAR,
              OperacionCita.CONSULTAR_ATENCION,
              OperacionCita.CONSULTAR_DASHBOARD,
              OperacionCita.CONSULTAR_GLOBAL));
    }
    if (cuenta.roles().contains(Rol.REC)) {
      operaciones.addAll(
          EnumSet.of(
              OperacionCita.PROGRAMAR,
              OperacionCita.REPROGRAMAR,
              OperacionCita.REGISTRAR_LLEGADA,
              OperacionCita.CANCELAR,
              OperacionCita.REGISTRAR_AUSENCIA,
              OperacionCita.REGISTRAR_RETIRO));
    }
    if (cuenta.roles().contains(Rol.PRO)
        && cuenta.profesionalId() != null
        && profesionales.buscarPorId(cuenta.profesionalId()).filter(p -> p.activa()).isPresent()) {
      profesional = cuenta.profesionalId();
      operaciones.addAll(
          EnumSet.of(
              OperacionCita.CONSULTAR,
              OperacionCita.CONSULTAR_ATENCION,
              OperacionCita.CONSULTAR_DASHBOARD,
              OperacionCita.INICIAR_ATENCION,
              OperacionCita.FINALIZAR_ATENCION));
    }
    return new PerfilCitas(operaciones, profesional);
  }
}
