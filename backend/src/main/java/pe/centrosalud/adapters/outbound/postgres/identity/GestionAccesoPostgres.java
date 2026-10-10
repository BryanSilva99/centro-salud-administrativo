package pe.centrosalud.adapters.outbound.postgres.identity;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import pe.centrosalud.modules.identity.application.ports.*;
import pe.centrosalud.modules.identity.application.roles.*;
import pe.centrosalud.modules.identity.domain.Rol;

@Repository
public class GestionAccesoPostgres implements DirectorioRoles, UnidadAcceso, HistorialCuentas {
    private static final String SELECT = "SELECT c.id, c.identificador, c.activa, c.version_roles, c.profesional_id, ARRAY(SELECT rol FROM cuenta_roles r WHERE r.cuenta_id=c.id ORDER BY rol) AS roles FROM cuentas c ";
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final Clock clock;
    public GestionAccesoPostgres(JdbcTemplate jdbc, PlatformTransactionManager manager, Clock clock) {
        this.jdbc = jdbc; this.tx = new TransactionTemplate(manager); this.clock = clock;
    }
    @Override public <T> T ejecutar(Function<UnidadAcceso.Operacion, T> accion) {
        try { return tx.execute(status -> {
            // Mutex de administración: evita retirar en paralelo a los dos últimos ADM.
            jdbc.queryForObject("SELECT id FROM control_administracion WHERE id=1 FOR UPDATE", Integer.class);
            return accion.apply(new Operacion());
        }); } catch (org.springframework.dao.DuplicateKeyException error) {
            throw new ErrorRoles(ErrorRoles.Codigo.DUPLICADO);
        }
    }
    @Override public CuentaRoles cargar(UUID id) {
        return jdbc.query(SELECT + "WHERE c.id=?", (rs, n) -> cuenta(rs), id).stream().findFirst()
                .orElseThrow(() -> new ErrorRoles(ErrorRoles.Codigo.NO_EXISTE));
    }
    @Override public Pagina<CuentaRoles> listar(String busqueda, int pagina) {
        var items = jdbc.query(SELECT + "WHERE strpos(lower(c.identificador), lower(?)) > 0 ORDER BY c.identificador,c.id LIMIT 21 OFFSET ?",
                (rs, n) -> cuenta(rs), busqueda, pagina * 20);
        return paginar(items);
    }
    @Override public Pagina<Cambio> historial(UUID id, int pagina) {
        cargar(id);
        return paginar(jdbc.query("SELECT * FROM auditoria_roles WHERE cuenta_id=? ORDER BY fecha DESC,id DESC LIMIT 21 OFFSET ?",
                (rs,n) -> new Cambio(rs.getObject("id", UUID.class), rs.getObject("actor_id", UUID.class),
                        id, roles(rs,"anteriores"), roles(rs,"nuevos"), rs.getTimestamp("fecha").toInstant()), id, pagina * 20));
    }
    @Override public Pagina<CambioCuenta> historialCuenta(UUID id, int pagina) {
        cargar(id);
        return paginar(jdbc.query("SELECT * FROM auditoria_cuentas WHERE cuenta_id=? ORDER BY fecha DESC,id DESC LIMIT 21 OFFSET ?",
                (rs,n) -> new CambioCuenta(rs.getObject("id", UUID.class), rs.getObject("actor_id", UUID.class),
                        rs.getString("tipo"), rs.getString("identificador_anterior"), rs.getString("identificador_nuevo"),
                        rs.getObject("activa_anterior", Boolean.class), rs.getBoolean("activa_nueva"), roles(rs,"roles"),
                        rs.getTimestamp("fecha").toInstant(), rs.getObject("profesional_anterior",UUID.class), rs.getObject("profesional_nuevo",UUID.class)), id, pagina * 20));
    }
    private void auditarCuenta(UUID actor, UUID id, String tipo, CuentaRoles anterior, CuentaRoles actual) {
        jdbc.update(connection -> {
            var ps = connection.prepareStatement("INSERT INTO auditoria_cuentas(id,actor_id,cuenta_id,tipo,identificador_anterior,identificador_nuevo,activa_anterior,activa_nueva,roles,fecha,profesional_anterior,profesional_nuevo) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)");
            ps.setObject(1, UUID.randomUUID()); ps.setObject(2, actor); ps.setObject(3, id); ps.setString(4, tipo);
            ps.setString(5, anterior == null ? null : anterior.identificador()); ps.setString(6, actual.identificador());
            ps.setObject(7, anterior == null ? null : anterior.activa()); ps.setBoolean(8, actual.activa());
            ps.setArray(9, connection.createArrayOf("varchar", actual.roles().stream().map(Enum::name).sorted().toArray()));
            ps.setTimestamp(10, java.sql.Timestamp.from(clock.instant()));
            ps.setObject(11, anterior == null ? null : anterior.profesionalId());
            ps.setObject(12, actual.profesionalId());
            return ps;
        });
    }
    private static <T> Pagina<T> paginar(List<T> items) {
        return new Pagina<>(List.copyOf(items.subList(0, Math.min(20,items.size()))),items.size()>20);
    }
    private static Set<Rol> roles(ResultSet rs,String campo) throws SQLException {
        return Arrays.stream((Object[])rs.getArray(campo).getArray()).map(v -> Rol.valueOf(v.toString())).collect(Collectors.toUnmodifiableSet());
    }
    private static CuentaRoles cuenta(ResultSet rs) throws SQLException {
        return new CuentaRoles(rs.getObject("id",UUID.class),rs.getString("identificador"),rs.getBoolean("activa"),roles(rs,"roles"),rs.getLong("version_roles"),rs.getObject("profesional_id",UUID.class));
    }
    private final class Operacion implements UnidadAcceso.Operacion {
        @Override public CuentaRoles cargar(UUID id) { return GestionAccesoPostgres.this.cargar(id); }
        @Override public CuentaRoles crear(UUID actor, String identificador, String hash, Set<Rol> roles) {
            UUID id = UUID.randomUUID();
            jdbc.update("INSERT INTO cuentas(id,identificador,activa,contrasena_hash) VALUES (?,?,true,?)", id, identificador, hash);
            for (Rol rol : roles) jdbc.update("INSERT INTO cuenta_roles(cuenta_id,rol) VALUES (?,?)", id, rol.name());
            CuentaRoles cuenta = cargar(id);
            auditarCuenta(actor, id, "CREACION", null, cuenta);
            return cuenta;
        }
        @Override public CuentaRoles actualizar(UUID actor, CuentaRoles anterior, String identificador, boolean activa) {
            jdbc.update("UPDATE cuentas SET identificador=?,activa=? WHERE id=?", identificador, activa, anterior.id());
            CuentaRoles cuenta = cargar(anterior.id());
            auditarCuenta(actor, anterior.id(), "ACTUALIZACION", anterior, cuenta);
            return cuenta;
        }
        @Override public boolean profesionalOcupado(UUID profesionalId, UUID cuentaId) {
            return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM cuentas WHERE profesional_id=? AND id<>?)",
                    Boolean.class, profesionalId, cuentaId));
        }
        @Override public CuentaRoles vincular(UUID actor, CuentaRoles anterior, UUID profesionalId) {
            jdbc.update("UPDATE cuentas SET profesional_id=? WHERE id=?", profesionalId, anterior.id());
            CuentaRoles actual = cargar(anterior.id());
            auditarCuenta(actor, anterior.id(), "VINCULACION", anterior, actual);
            return actual;
        }
        @Override public boolean existeOtroAdministradorActivo(UUID id) {
            return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM cuentas c JOIN cuenta_roles r ON r.cuenta_id=c.id WHERE c.activa AND r.rol='ADM' AND c.id<>?)",Boolean.class,id));
        }
        @Override public CuentaRoles guardar(UUID actor, CuentaRoles anterior, Set<Rol> nuevos) {
            jdbc.update("DELETE FROM cuenta_roles WHERE cuenta_id=?",anterior.id());
            for (Rol rol : nuevos) jdbc.update("INSERT INTO cuenta_roles(cuenta_id,rol) VALUES (?,?)",anterior.id(),rol.name());
            jdbc.update(connection -> {
                var ps=connection.prepareStatement("INSERT INTO auditoria_roles(id,actor_id,cuenta_id,anteriores,nuevos,fecha) VALUES (?,?,?,?,?,?)");
                ps.setObject(1,UUID.randomUUID()); ps.setObject(2,actor); ps.setObject(3,anterior.id());
                ps.setArray(4,connection.createArrayOf("varchar",anterior.roles().stream().map(Enum::name).sorted().toArray()));
                ps.setArray(5,connection.createArrayOf("varchar",nuevos.stream().map(Enum::name).sorted().toArray()));
                ps.setTimestamp(6,java.sql.Timestamp.from(clock.instant()));
                return ps;
            });
            return cargar(anterior.id());
        }
    }
}
