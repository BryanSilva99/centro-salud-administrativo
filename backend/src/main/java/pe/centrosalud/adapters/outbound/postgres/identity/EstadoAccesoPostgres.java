package pe.centrosalud.adapters.outbound.postgres.identity;

import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import pe.centrosalud.modules.identity.application.ports.ConsultarEstadoAcceso;

@Repository
public class EstadoAccesoPostgres implements ConsultarEstadoAcceso {
    private final JdbcTemplate jdbc;
    public EstadoAccesoPostgres(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    @Override public Optional<Estado> buscarEstado(UUID id) {
        return jdbc.query("SELECT activa, version_acceso FROM cuentas WHERE id = ?",
                (rs, row) -> new Estado(rs.getBoolean(1), rs.getLong(2)), id).stream().findFirst();
    }
}
