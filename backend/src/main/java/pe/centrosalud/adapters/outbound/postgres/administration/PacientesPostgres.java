package pe.centrosalud.adapters.outbound.postgres.administration;

import java.sql.Timestamp;
import java.time.Clock;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import pe.centrosalud.modules.administration.application.ports.Pacientes;
import pe.centrosalud.modules.administration.application.usecases.PacienteDuplicado;
import pe.centrosalud.modules.administration.domain.Paciente;

@Repository
public class PacientesPostgres implements Pacientes {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final Clock clock;

    public PacientesPostgres(JdbcTemplate jdbc, PlatformTransactionManager manager, Clock clock) {
        this.jdbc = jdbc; this.tx = new TransactionTemplate(manager); this.clock = clock;
    }

    @Override public Paciente registrar(Paciente nuevo, UUID autor) {
        try {
            return tx.execute(status -> {
                // La restricción única resuelve las altas concurrentes; no se consulta antes de insertar.
                String codigo = jdbc.queryForObject("""
                        INSERT INTO pacientes(id,tipo_documento,numero_documento,nombres,apellidos,fecha_nacimiento,telefono,correo,creado_por)
                        VALUES (?,?,?,?,?,?,?,?,?) RETURNING codigo""", String.class,
                        nuevo.id(), nuevo.tipoDocumento().name(), nuevo.numeroDocumento(), nuevo.nombres(),
                        nuevo.apellidos(), nuevo.fechaNacimiento(), nuevo.telefono(), nuevo.correo(), autor);
                jdbc.update("INSERT INTO auditoria_pacientes(id,actor_id,paciente_id,tipo,fecha) VALUES (?,?,?,'CREACION',?)",
                        UUID.randomUUID(), autor, nuevo.id(), Timestamp.from(clock.instant()));
                return new Paciente(nuevo.id(), codigo, nuevo.tipoDocumento(), nuevo.numeroDocumento(), nuevo.nombres(),
                        nuevo.apellidos(), nuevo.fechaNacimiento(), nuevo.telefono(), nuevo.correo());
            });
        } catch (DuplicateKeyException error) {
            if (String.valueOf(error.getMessage()).contains("pacientes_documento_unique")) throw new PacienteDuplicado();
            throw error;
        }
    }
}
