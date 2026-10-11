package pe.centrosalud.modules.administration.application.usecases;

import static pe.centrosalud.modules.administration.application.usecases.ErrorPacientes.Codigo.*;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import pe.centrosalud.modules.administration.application.ports.Pacientes;
import pe.centrosalud.modules.administration.domain.Paciente;
import pe.centrosalud.modules.administration.domain.TipoDocumento;

/**
 * RF-05. La autorización (solo REC) se exige en la ruta mediante PACIENTES_GESTIONAR. Conserva el
 * código interno y las relaciones del paciente; un cambio de documento vuelve a verificar unicidad
 * en la base (RF-05/CA-2).
 */
public final class ActualizarPaciente {
  // La misma zona que RegistrarPaciente: la fecha civil del centro, no la de UTC.
  private static final ZoneId ZONA_CENTRO = ZoneId.of("America/Lima");
  private final Pacientes pacientes;
  private final Clock clock;

  public ActualizarPaciente(Pacientes pacientes, Clock clock) {
    this.pacientes = pacientes;
    this.clock = clock;
  }

  public Paciente actualizar(
      UUID actor,
      UUID id,
      TipoDocumento tipo,
      String numero,
      String nombres,
      String apellidos,
      LocalDate nacimiento,
      String telefono,
      String correo,
      long version) {
    if (version < 0) throw new IllegalArgumentException();
    Paciente.validarNacimiento(nacimiento, LocalDate.now(clock.withZone(ZONA_CENTRO)));
    Paciente anterior = pacientes.buscarPorId(id).orElseThrow(() -> new ErrorPacientes(NO_EXISTE));
    if (anterior.version() != version) throw new ErrorPacientes(CONFLICTO);
    var nuevo =
        new Paciente(
            id,
            anterior.codigo(),
            tipo,
            numero,
            nombres,
            apellidos,
            nacimiento,
            telefono,
            correo,
            version);
    // Un guardado idéntico no incrementa la versión ni genera auditoría artificial.
    if (anterior.equals(nuevo)) return anterior;
    return pacientes.actualizar(anterior, nuevo, actor);
  }
}
