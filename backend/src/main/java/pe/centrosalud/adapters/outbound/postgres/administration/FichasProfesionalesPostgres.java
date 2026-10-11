package pe.centrosalud.adapters.outbound.postgres.administration;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import pe.centrosalud.modules.administration.application.ports.FichasProfesionales;
import pe.centrosalud.modules.administration.application.usecases.ErrorProfesionales;
import pe.centrosalud.modules.administration.domain.FichaProfesional;

@Repository
public class FichasProfesionalesPostgres implements FichasProfesionales {
  private static final String RELACIONES = "SELECT * FROM profesional_servicios ";
  // Foto de la ficha para auditoría: sin nombre ni identificación (RF-28/CA-2, RN-17).
  private static final String FOTO =
      """
            SELECT jsonb_build_object('activo', p.activa, 'version', p.version,
                'serviciosHabilitados', (SELECT jsonb_agg(ps.servicio_id)
                    FROM profesional_servicios ps WHERE ps.profesional_id = p.id))::text
            FROM profesionales p WHERE p.id = ?""";
  private final JdbcTemplate jdbc;
  private final TransactionTemplate tx;
  private final Clock clock;

  public FichasProfesionalesPostgres(
      JdbcTemplate jdbc, PlatformTransactionManager manager, Clock clock) {
    this.jdbc = jdbc;
    this.tx = new TransactionTemplate(manager);
    this.clock = clock;
  }

  @Override
  public List<FichaProfesional> buscar(String texto) {
    var ids =
        jdbc.query(
            "SELECT id FROM profesionales WHERE strpos(lower(nombre || ' ' || identificacion), lower(?)) > 0 ORDER BY nombre,id LIMIT 50",
            (fila, n) -> fila.getObject("id", UUID.class),
            texto);
    var relaciones = relaciones(ids);
    return jdbc.query(
        "SELECT * FROM profesionales WHERE strpos(lower(nombre || ' ' || identificacion), lower(?)) > 0 ORDER BY nombre,id LIMIT 50",
        (fila, n) -> ficha(fila, relaciones.get(fila.getObject("id", UUID.class))),
        texto);
  }

  @Override
  public List<FichaProfesional> listar() {
    var ids =
        jdbc.query(
            "SELECT id FROM profesionales ORDER BY nombre,id",
            (fila, n) -> fila.getObject("id", UUID.class));
    var relaciones = relaciones(ids);
    return jdbc.query(
        "SELECT * FROM profesionales ORDER BY nombre,id",
        (fila, n) -> ficha(fila, relaciones.get(fila.getObject("id", UUID.class))));
  }

  @Override
  public Optional<FichaProfesional> buscarPorId(UUID id) {
    var servicios = new HashSet<UUID>();
    jdbc.query(
        RELACIONES + "WHERE profesional_id=?",
        fila -> {
          servicios.add(fila.getObject("servicio_id", UUID.class));
        },
        id);
    return jdbc
        .query("SELECT * FROM profesionales WHERE id=?", (fila, n) -> ficha(fila, servicios), id)
        .stream()
        .findFirst();
  }

  @Override
  public FichaProfesional registrar(FichaProfesional ficha, UUID autor) {
    return tx.execute(
        status -> {
          jdbc.update(
              "INSERT INTO profesionales(id,identificacion,nombre,funcion,activa,creado_por) VALUES (?,?,?,?,?,?)",
              ficha.id(),
              ficha.identificacion(),
              ficha.nombre(),
              ficha.funcion(),
              ficha.activa(),
              autor);
          for (UUID servicioId : ficha.serviciosHabilitados())
            insertarRelacion(ficha.id(), servicioId);
          auditar(autor, ficha.id(), "CREACION", null);
          return cargar(ficha.id());
        });
  }

  @Override
  public FichaProfesional actualizar(
      FichaProfesional anterior, FichaProfesional nuevo, UUID autor) {
    return tx.execute(
        status -> {
          String foto = jdbc.queryForObject(FOTO, String.class, anterior.id());
          int filas =
              jdbc.update(
                  "UPDATE profesionales SET identificacion=?, nombre=?, funcion=?, activa=?, version=version+1 WHERE id=? AND version=?",
                  nuevo.identificacion(),
                  nuevo.nombre(),
                  nuevo.funcion(),
                  nuevo.activa(),
                  anterior.id(),
                  anterior.version());
          if (filas == 0) throw new ErrorProfesionales(ErrorProfesionales.Codigo.CONFLICTO);
          jdbc.update("DELETE FROM profesional_servicios WHERE profesional_id=?", anterior.id());
          for (UUID servicioId : nuevo.serviciosHabilitados())
            insertarRelacion(anterior.id(), servicioId);
          auditar(autor, anterior.id(), "ACTUALIZACION", foto);
          return cargar(anterior.id());
        });
  }

  private void insertarRelacion(UUID profesionalId, UUID servicioId) {
    jdbc.update(
        "INSERT INTO profesional_servicios(profesional_id,servicio_id) VALUES (?,?)",
        profesionalId,
        servicioId);
  }

  private void auditar(UUID autor, UUID profesionalId, String tipo, String anterior) {
    jdbc.update(
        "INSERT INTO auditoria_profesionales(id,actor_id,profesional_id,tipo,anterior,nuevo,fecha) VALUES (?,?,?,?,?::jsonb,("
            + FOTO
            + ")::jsonb,?)",
        UUID.randomUUID(),
        autor,
        profesionalId,
        tipo,
        anterior,
        profesionalId,
        Timestamp.from(clock.instant()));
  }

  private FichaProfesional cargar(UUID id) {
    return buscarPorId(id)
        .orElseThrow(() -> new ErrorProfesionales(ErrorProfesionales.Codigo.NO_EXISTE));
  }

  private Map<UUID, Set<UUID>> relaciones(List<UUID> profesionalIds) {
    var relaciones = new HashMap<UUID, Set<UUID>>();
    if (profesionalIds.isEmpty()) return relaciones;
    jdbc.query(
        RELACIONES
            + "WHERE profesional_id IN ("
            + String.join(",", profesionalIds.stream().map(id -> "?").toList())
            + ")",
        fila -> {
          relaciones
              .computeIfAbsent(fila.getObject("profesional_id", UUID.class), id -> new HashSet<>())
              .add(fila.getObject("servicio_id", UUID.class));
        },
        profesionalIds.toArray());
    return relaciones;
  }

  private static FichaProfesional ficha(ResultSet fila, Set<UUID> servicios) throws SQLException {
    return new FichaProfesional(
        fila.getObject("id", UUID.class),
        fila.getString("identificacion"),
        fila.getString("nombre"),
        fila.getString("funcion"),
        fila.getBoolean("activa"),
        fila.getLong("version"),
        servicios == null ? Set.of() : servicios);
  }
}
