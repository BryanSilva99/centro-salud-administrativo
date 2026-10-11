package pe.centrosalud.adapters.outbound.postgres.administration;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import pe.centrosalud.modules.administration.application.ports.FichasProfesionales;
import pe.centrosalud.modules.administration.domain.FichaProfesional;

@Repository
public class FichasProfesionalesPostgres implements FichasProfesionales {
  private final JdbcTemplate jdbc;

  public FichasProfesionalesPostgres(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public List<FichaProfesional> buscar(String texto) {
    return jdbc.query(
        "SELECT * FROM profesionales WHERE strpos(lower(nombre || ' ' || identificacion), lower(?)) > 0 ORDER BY nombre,id LIMIT 50",
        (fila, numero) -> ficha(fila),
        texto);
  }

  @Override
  public Optional<FichaProfesional> buscarPorId(UUID id) {
    return jdbc
        .query("SELECT * FROM profesionales WHERE id=?", (fila, numero) -> ficha(fila), id)
        .stream()
        .findFirst();
  }

  @Override
  public FichaProfesional registrar(FichaProfesional ficha, UUID autor) {
    jdbc.update(
        "INSERT INTO profesionales(id,identificacion,nombre,funcion,activa,creado_por) VALUES (?,?,?,?,?,?)",
        ficha.id(),
        ficha.identificacion(),
        ficha.nombre(),
        ficha.funcion(),
        ficha.activa(),
        autor);
    return ficha;
  }

  private FichaProfesional ficha(ResultSet fila) throws SQLException {
    return new FichaProfesional(
        fila.getObject("id", UUID.class),
        fila.getString("identificacion"),
        fila.getString("nombre"),
        fila.getString("funcion"),
        fila.getBoolean("activa"));
  }
}
