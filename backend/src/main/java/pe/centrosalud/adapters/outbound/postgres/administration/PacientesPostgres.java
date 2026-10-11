package pe.centrosalud.adapters.outbound.postgres.administration;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import pe.centrosalud.modules.administration.application.ports.Pacientes;
import pe.centrosalud.modules.administration.application.usecases.ErrorPacientes;
import pe.centrosalud.modules.administration.domain.Paciente;
import pe.centrosalud.modules.administration.domain.TipoDocumento;

@Repository
public class PacientesPostgres implements Pacientes {
  private final JdbcTemplate jdbc;
  private final TransactionTemplate tx;
  private final Clock clock;

  public PacientesPostgres(JdbcTemplate jdbc, PlatformTransactionManager manager, Clock clock) {
    this.jdbc = jdbc;
    this.tx = new TransactionTemplate(manager);
    this.clock = clock;
  }

  @Override
  public Paciente registrar(Paciente nuevo, UUID autor) {
    return escribir(
        () -> {
          // La restricción única resuelve las altas concurrentes; no se consulta antes de insertar.
          String codigo =
              jdbc.queryForObject(
                  """
                    INSERT INTO pacientes(id,tipo_documento,numero_documento,nombres,apellidos,fecha_nacimiento,telefono,correo,creado_por)
                    VALUES (?,?,?,?,?,?,?,?,?) RETURNING codigo""",
                  String.class,
                  nuevo.id(),
                  nuevo.tipoDocumento().name(),
                  nuevo.numeroDocumento(),
                  nuevo.nombres(),
                  nuevo.apellidos(),
                  nuevo.fechaNacimiento(),
                  nuevo.telefono(),
                  nuevo.correo(),
                  autor);
          auditar(autor, nuevo.id(), "CREACION");
          return new Paciente(
              nuevo.id(),
              codigo,
              nuevo.tipoDocumento(),
              nuevo.numeroDocumento(),
              nuevo.nombres(),
              nuevo.apellidos(),
              nuevo.fechaNacimiento(),
              nuevo.telefono(),
              nuevo.correo(),
              0);
        });
  }

  @Override
  public Optional<Paciente> buscarPorId(UUID id) {
    return jdbc
        .query("SELECT * FROM pacientes WHERE id=?", (fila, n) -> paciente(fila), id)
        .stream()
        .findFirst();
  }

  @Override
  public Pagina<Paciente> buscar(String texto, int pagina) {
    var items =
        jdbc.query(
            """
                SELECT * FROM pacientes
                WHERE strpos(lower(codigo || ' ' || numero_documento || ' ' || nombres || ' ' || apellidos), lower(?)) > 0
                ORDER BY nombres, apellidos, id LIMIT 21 OFFSET ?""",
            (fila, n) -> paciente(fila),
            texto,
            pagina * 20);
    return new Pagina<>(
        List.copyOf(items.subList(0, Math.min(20, items.size()))), items.size() > 20);
  }

  @Override
  public Paciente actualizar(Paciente anterior, Paciente nuevo, UUID autor) {
    return escribir(
        () -> {
          int filas =
              jdbc.update(
                  """
                    UPDATE pacientes SET tipo_documento=?, numero_documento=?, nombres=?, apellidos=?,
                        fecha_nacimiento=?, telefono=?, correo=?, version=version+1 WHERE id=? AND version=?""",
                  nuevo.tipoDocumento().name(),
                  nuevo.numeroDocumento(),
                  nuevo.nombres(),
                  nuevo.apellidos(),
                  nuevo.fechaNacimiento(),
                  nuevo.telefono(),
                  nuevo.correo(),
                  anterior.id(),
                  anterior.version());
          if (filas == 0) throw new ErrorPacientes(ErrorPacientes.Codigo.CONFLICTO);
          auditar(autor, anterior.id(), "ACTUALIZACION");
          return cargar(anterior.id());
        });
  }

  private Paciente escribir(java.util.function.Supplier<Paciente> operacion) {
    try {
      return tx.execute(status -> operacion.get());
    } catch (DuplicateKeyException error) {
      if (String.valueOf(error.getMessage()).contains("pacientes_documento_unique")) {
        throw new ErrorPacientes(ErrorPacientes.Codigo.DOCUMENTO_DUPLICADO);
      }
      throw error;
    }
  }

  // La auditoría no copia datos personales (RN-17): solo IDs, tipo y fecha.
  private void auditar(UUID autor, UUID pacienteId, String tipo) {
    jdbc.update(
        "INSERT INTO auditoria_pacientes(id,actor_id,paciente_id,tipo,fecha) VALUES (?,?,?,?,?)",
        UUID.randomUUID(),
        autor,
        pacienteId,
        tipo,
        Timestamp.from(clock.instant()));
  }

  private Paciente cargar(UUID id) {
    return buscarPorId(id).orElseThrow(() -> new ErrorPacientes(ErrorPacientes.Codigo.NO_EXISTE));
  }

  private static Paciente paciente(ResultSet fila) throws SQLException {
    return new Paciente(
        fila.getObject("id", UUID.class),
        fila.getString("codigo"),
        TipoDocumento.valueOf(fila.getString("tipo_documento")),
        fila.getString("numero_documento"),
        fila.getString("nombres"),
        fila.getString("apellidos"),
        fila.getObject("fecha_nacimiento", java.time.LocalDate.class),
        fila.getString("telefono"),
        fila.getString("correo"),
        fila.getLong("version"));
  }
}
