package pe.centrosalud.adapters.outbound.postgres.administration;

import static pe.centrosalud.modules.administration.application.usecases.ErrorDisponibilidad.Codigo.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import pe.centrosalud.modules.administration.application.ports.Disponibilidad;
import pe.centrosalud.modules.administration.application.usecases.ErrorDisponibilidad;
import pe.centrosalud.modules.administration.domain.TipoRecurso;
import pe.centrosalud.modules.administration.domain.TipoTurno;
import pe.centrosalud.modules.administration.domain.Turno;

/**
 * Un único adaptador para los dos recursos: la tabla y la columna del recurso cambian según {@link
 * TipoRecurso}, pero la lógica de bloqueo, superposición y auditoría es la misma.
 */
@Repository
public class DisponibilidadPostgres implements Disponibilidad {
  private final JdbcTemplate jdbc;
  private final TransactionTemplate tx;
  private final Clock clock;

  public DisponibilidadPostgres(
      JdbcTemplate jdbc, PlatformTransactionManager manager, Clock clock) {
    this.jdbc = jdbc;
    this.tx = new TransactionTemplate(manager);
    this.clock = clock;
  }

  @Override
  public List<Turno> listar(TipoRecurso tipo, UUID recursoId) {
    String tabla = tabla(tipo);
    return jdbc.query(
        "SELECT * FROM " + tabla + " WHERE " + columna(tipo) + "=? ORDER BY fecha, hora_inicio",
        (fila, n) -> turno(fila, recursoId),
        recursoId);
  }

  @Override
  public Turno crear(TipoRecurso tipo, Turno nuevo, UUID autor) {
    String tabla = tabla(tipo);
    String columna = columna(tipo);
    return tx.execute(
        status -> {
          // Bloquea la fila del recurso: dos altas concurrentes del mismo recurso se serializan
          // aquí.
          var filas =
              jdbc.queryForList(
                  "SELECT id FROM " + tablaRecurso(tipo) + " WHERE id=? FOR UPDATE",
                  UUID.class,
                  nuevo.recursoId());
          if (filas.isEmpty()) throw new ErrorDisponibilidad(RECURSO_INVALIDO);
          boolean superpuesto =
              Boolean.TRUE.equals(
                  jdbc.queryForObject(
                      "SELECT EXISTS(SELECT 1 FROM "
                          + tabla
                          + " WHERE "
                          + columna
                          + "=? AND fecha=? AND activo AND hora_inicio < ? AND hora_fin > ?)",
                      Boolean.class,
                      nuevo.recursoId(),
                      nuevo.fecha(),
                      nuevo.horaFin(),
                      nuevo.horaInicio()));
          if (superpuesto) throw new ErrorDisponibilidad(SUPERPUESTO);
          jdbc.update(
              "INSERT INTO "
                  + tabla
                  + "(id,"
                  + columna
                  + ",fecha,hora_inicio,hora_fin,tipo,creado_por) VALUES (?,?,?,?,?,?,?)",
              nuevo.id(),
              nuevo.recursoId(),
              nuevo.fecha(),
              nuevo.horaInicio(),
              nuevo.horaFin(),
              nuevo.tipo().name(),
              autor);
          auditar(tipo, autor, nuevo.id(), "CREACION");
          return nuevo;
        });
  }

  @Override
  public void desactivar(TipoRecurso tipo, UUID turnoId, UUID autor) {
    String tabla = tabla(tipo);
    tx.execute(
        status -> {
          var activos =
              jdbc.queryForList(
                  "SELECT activo FROM " + tabla + " WHERE id=?", Boolean.class, turnoId);
          if (activos.isEmpty()) throw new ErrorDisponibilidad(NO_EXISTE);
          // Idempotente: desactivar un turno ya inactivo no vuelve a auditar.
          if (!activos.getFirst()) return null;
          jdbc.update("UPDATE " + tabla + " SET activo=false WHERE id=?", turnoId);
          auditar(tipo, autor, turnoId, "DESACTIVACION");
          return null;
        });
  }

  private void auditar(TipoRecurso tipo, UUID autor, UUID turnoId, String tipoEvento) {
    jdbc.update(
        "INSERT INTO auditoria_turnos(id,actor_id,tipo_recurso,turno_id,tipo,fecha) VALUES (?,?,?,?,?,?)",
        UUID.randomUUID(),
        autor,
        tipo.name(),
        turnoId,
        tipoEvento,
        Timestamp.from(clock.instant()));
  }

  private static String tabla(TipoRecurso tipo) {
    return tipo == TipoRecurso.PROFESIONAL ? "turnos_profesional" : "turnos_consultorio";
  }

  private static String columna(TipoRecurso tipo) {
    return tipo == TipoRecurso.PROFESIONAL ? "profesional_id" : "consultorio_id";
  }

  private static String tablaRecurso(TipoRecurso tipo) {
    return tipo == TipoRecurso.PROFESIONAL ? "profesionales" : "consultorios";
  }

  private static Turno turno(ResultSet fila, UUID recursoId) throws SQLException {
    return new Turno(
        fila.getObject("id", UUID.class),
        recursoId,
        fila.getObject("fecha", java.time.LocalDate.class),
        fila.getObject("hora_inicio", java.time.LocalTime.class),
        fila.getObject("hora_fin", java.time.LocalTime.class),
        TipoTurno.valueOf(fila.getString("tipo")),
        fila.getBoolean("activo"));
  }
}
