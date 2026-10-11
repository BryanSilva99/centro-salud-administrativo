package pe.centrosalud.adapters.outbound.postgres.administration;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import pe.centrosalud.modules.administration.application.ports.Consultorios;
import pe.centrosalud.modules.administration.application.usecases.ErrorConsultorios;
import pe.centrosalud.modules.administration.domain.Consultorio;

@Repository
public class ConsultoriosPostgres implements Consultorios {
  private static final String RELACIONES = "SELECT * FROM consultorio_servicios ";
  // Foto del consultorio para auditoría, construida en la misma transacción que el cambio.
  private static final String FOTO =
      """
            SELECT jsonb_build_object('codigo', c.codigo, 'nombre', c.nombre, 'activo', c.activo,
                'version', c.version, 'serviciosCompatibles', (SELECT jsonb_agg(cs.servicio_id)
                    FROM consultorio_servicios cs WHERE cs.consultorio_id = c.id))::text
            FROM consultorios c WHERE c.id = ?""";
  private final JdbcTemplate jdbc;
  private final TransactionTemplate tx;
  private final Clock clock;

  public ConsultoriosPostgres(JdbcTemplate jdbc, PlatformTransactionManager manager, Clock clock) {
    this.jdbc = jdbc;
    this.tx = new TransactionTemplate(manager);
    this.clock = clock;
  }

  @Override
  public List<Consultorio> listar() {
    var relaciones = new HashMap<UUID, Set<UUID>>();
    jdbc.query(
        RELACIONES + "ORDER BY consultorio_id, servicio_id",
        fila -> {
          relaciones
              .computeIfAbsent(fila.getObject("consultorio_id", UUID.class), id -> new HashSet<>())
              .add(fila.getObject("servicio_id", UUID.class));
        });
    return jdbc.query(
        "SELECT * FROM consultorios ORDER BY lower(codigo), id",
        (fila, n) -> consultorio(fila, relaciones.get(fila.getObject("id", UUID.class))));
  }

  @Override
  public Optional<Consultorio> buscarPorId(UUID id) {
    var servicios = new HashSet<UUID>();
    jdbc.query(
        RELACIONES + "WHERE consultorio_id=?",
        fila -> {
          servicios.add(fila.getObject("servicio_id", UUID.class));
        },
        id);
    return jdbc
        .query(
            "SELECT * FROM consultorios WHERE id=?", (fila, n) -> consultorio(fila, servicios), id)
        .stream()
        .findFirst();
  }

  @Override
  public Consultorio crear(Consultorio nuevo, UUID autor) {
    return escribir(
        () -> {
          jdbc.update(
              "INSERT INTO consultorios(id,codigo,nombre,activo,creado_por) VALUES (?,?,?,?,?)",
              nuevo.id(),
              nuevo.codigo(),
              nuevo.nombre(),
              nuevo.activo(),
              autor);
          for (UUID servicioId : nuevo.serviciosCompatibles())
            insertarRelacion(nuevo.id(), servicioId);
          auditar(autor, nuevo.id(), "CREACION", null);
          return cargar(nuevo.id());
        });
  }

  @Override
  public Consultorio actualizar(Consultorio anterior, Consultorio nuevo, UUID autor) {
    return escribir(
        () -> {
          String foto = jdbc.queryForObject(FOTO, String.class, anterior.id());
          int filas =
              jdbc.update(
                  "UPDATE consultorios SET codigo=?, nombre=?, activo=?, version=version+1 WHERE id=? AND version=?",
                  nuevo.codigo(),
                  nuevo.nombre(),
                  nuevo.activo(),
                  anterior.id(),
                  anterior.version());
          if (filas == 0) throw new ErrorConsultorios(ErrorConsultorios.Codigo.CONFLICTO);
          jdbc.update("DELETE FROM consultorio_servicios WHERE consultorio_id=?", anterior.id());
          for (UUID servicioId : nuevo.serviciosCompatibles())
            insertarRelacion(anterior.id(), servicioId);
          auditar(autor, anterior.id(), "ACTUALIZACION", foto);
          return cargar(anterior.id());
        });
  }

  private Consultorio escribir(java.util.function.Supplier<Consultorio> operacion) {
    try {
      return tx.execute(status -> operacion.get());
    } catch (DuplicateKeyException error) {
      if (String.valueOf(error.getMessage()).contains("consultorios_codigo_unique")) {
        throw new ErrorConsultorios(ErrorConsultorios.Codigo.CODIGO_DUPLICADO);
      }
      throw error;
    }
  }

  private void insertarRelacion(UUID consultorioId, UUID servicioId) {
    jdbc.update(
        "INSERT INTO consultorio_servicios(consultorio_id,servicio_id) VALUES (?,?)",
        consultorioId,
        servicioId);
  }

  private void auditar(UUID autor, UUID consultorioId, String tipo, String anterior) {
    jdbc.update(
        "INSERT INTO auditoria_consultorios(id,actor_id,consultorio_id,tipo,anterior,nuevo,fecha) VALUES (?,?,?,?,?::jsonb,("
            + FOTO
            + ")::jsonb,?)",
        UUID.randomUUID(),
        autor,
        consultorioId,
        tipo,
        anterior,
        consultorioId,
        Timestamp.from(clock.instant()));
  }

  private Consultorio cargar(UUID id) {
    return buscarPorId(id)
        .orElseThrow(() -> new ErrorConsultorios(ErrorConsultorios.Codigo.NO_EXISTE));
  }

  private static Consultorio consultorio(ResultSet fila, Set<UUID> servicios) throws SQLException {
    return new Consultorio(
        fila.getObject("id", UUID.class),
        fila.getString("codigo"),
        fila.getString("nombre"),
        fila.getBoolean("activo"),
        fila.getLong("version"),
        servicios == null ? Set.of() : servicios);
  }
}
