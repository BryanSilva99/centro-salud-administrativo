package pe.centrosalud.adapters.outbound.postgres.identity;

import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import pe.centrosalud.modules.identity.application.ports.ConsultarRoles;
import pe.centrosalud.modules.identity.domain.Rol;

@Repository
public class ConsultarRolesPostgres implements ConsultarRoles {
  private final JdbcTemplate jdbc;

  public ConsultarRolesPostgres(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public Set<Rol> deCuenta(UUID id) {
    return Set.copyOf(
        jdbc.query(
            "SELECT rol FROM cuenta_roles WHERE cuenta_id = ?",
            (rs, row) -> Rol.valueOf(rs.getString(1)),
            id));
  }
}
