package pe.centrosalud.modules.administration.application.usecases;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import pe.centrosalud.modules.administration.application.ports.Pacientes;
import pe.centrosalud.modules.administration.domain.Paciente;
import pe.centrosalud.modules.administration.domain.TipoDocumento;

/** RF-04. La autorización (solo REC) se exige en la ruta mediante el permiso PACIENTES_GESTIONAR. */
public final class RegistrarPaciente {
    // La fecha civil del centro, no la de UTC: a las 20:00 en Lima ya es el día siguiente en UTC.
    private static final ZoneId ZONA_CENTRO = ZoneId.of("America/Lima");
    private final Pacientes pacientes;
    private final Clock clock;

    public RegistrarPaciente(Pacientes pacientes, Clock clock) {
        this.pacientes = pacientes;
        this.clock = clock;
    }

    public Paciente registrar(UUID autor, TipoDocumento tipo, String numero, String nombres, String apellidos,
                              LocalDate nacimiento, String telefono, String correo) {
        Paciente.validarNacimiento(nacimiento, LocalDate.now(clock.withZone(ZONA_CENTRO)));
        var nuevo = new Paciente(UUID.randomUUID(), null, tipo, numero, nombres, apellidos, nacimiento, telefono, correo);
        return pacientes.registrar(nuevo, autor);
    }
}
