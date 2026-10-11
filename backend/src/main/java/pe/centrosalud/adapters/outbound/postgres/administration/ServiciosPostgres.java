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
import pe.centrosalud.modules.administration.application.ports.Servicios;
import pe.centrosalud.modules.administration.application.usecases.ErrorServicios;
import pe.centrosalud.modules.administration.domain.Modalidad;
import pe.centrosalud.modules.administration.domain.Servicio;

@Repository
public class ServiciosPostgres implements Servicios {
    private static final String MODALIDADES = "SELECT * FROM servicio_modalidades ";
    // Foto del servicio para auditoría, construida en la misma transacción que el cambio.
    private static final String FOTO = """
            SELECT jsonb_build_object('nombre', s.nombre, 'duracionMinutos', s.duracion_minutos, 'activo', s.activo,
                'version', s.version, 'modalidades', (SELECT jsonb_agg(jsonb_build_object('id', m.id, 'nombre', m.nombre,
                    'conCargo', m.con_cargo, 'tarifa', m.tarifa, 'activa', m.activa) ORDER BY m.id)
                    FROM servicio_modalidades m WHERE m.servicio_id = s.id))::text
            FROM servicios s WHERE s.id = ?""";
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final Clock clock;

    public ServiciosPostgres(JdbcTemplate jdbc, PlatformTransactionManager manager, Clock clock) {
        this.jdbc = jdbc; this.tx = new TransactionTemplate(manager); this.clock = clock;
    }

    @Override public List<Servicio> listar() {
        var modalidades = new HashMap<UUID, List<Modalidad>>();
        jdbc.query(MODALIDADES + "ORDER BY lower(nombre), id", fila -> {
            modalidades.computeIfAbsent(fila.getObject("servicio_id", UUID.class), id -> new ArrayList<>()).add(modalidad(fila));
        });
        return jdbc.query("SELECT * FROM servicios ORDER BY lower(nombre), id",
                (fila, n) -> servicio(fila, modalidades.get(fila.getObject("id", UUID.class))));
    }

    @Override public Optional<Servicio> buscarPorId(UUID id) {
        var modalidades = jdbc.query(MODALIDADES + "WHERE servicio_id=? ORDER BY lower(nombre), id", (fila, n) -> modalidad(fila), id);
        return jdbc.query("SELECT * FROM servicios WHERE id=?", (fila, n) -> servicio(fila, modalidades), id).stream().findFirst();
    }

    @Override public Servicio crear(Servicio nuevo, UUID autor) {
        return escribir(() -> {
            jdbc.update("INSERT INTO servicios(id,nombre,duracion_minutos,activo,creado_por) VALUES (?,?,?,?,?)",
                    nuevo.id(), nuevo.nombre(), nuevo.duracionMinutos(), nuevo.activo(), autor);
            for (Modalidad modalidad : nuevo.modalidades()) insertar(nuevo.id(), modalidad);
            auditar(autor, nuevo.id(), "CREACION", null);
            return cargar(nuevo.id());
        });
    }

    @Override public Servicio actualizar(Servicio anterior, Servicio nuevo, UUID autor) {
        return escribir(() -> {
            String foto = jdbc.queryForObject(FOTO, String.class, anterior.id());
            // El UPDATE condicional bloquea la fila: entre ediciones simultáneas con la misma versión, solo una avanza.
            int filas = jdbc.update("UPDATE servicios SET nombre=?, duracion_minutos=?, activo=?, version=version+1 WHERE id=? AND version=?",
                    nuevo.nombre(), nuevo.duracionMinutos(), nuevo.activo(), anterior.id(), anterior.version());
            if (filas == 0) throw new ErrorServicios(ErrorServicios.Codigo.CONFLICTO);
            var existentes = new HashSet<UUID>();
            for (Modalidad modalidad : anterior.modalidades()) existentes.add(modalidad.id());
            for (Modalidad modalidad : nuevo.modalidades()) {
                if (!existentes.contains(modalidad.id())) { insertar(anterior.id(), modalidad); continue; }
                jdbc.update("UPDATE servicio_modalidades SET nombre=?, con_cargo=?, tarifa=?, activa=? WHERE id=? AND servicio_id=?",
                        modalidad.nombre(), modalidad.conCargo(), modalidad.tarifa(), modalidad.activa(), modalidad.id(), anterior.id());
            }
            auditar(autor, anterior.id(), "ACTUALIZACION", foto);
            return cargar(anterior.id());
        });
    }

    private Servicio escribir(java.util.function.Supplier<Servicio> operacion) {
        try {
            return tx.execute(status -> operacion.get());
        } catch (DuplicateKeyException error) {
            if (String.valueOf(error.getMessage()).contains("servicios_nombre_unique")) {
                throw new ErrorServicios(ErrorServicios.Codigo.NOMBRE_DUPLICADO);
            }
            throw error;
        }
    }

    private void insertar(UUID servicioId, Modalidad modalidad) {
        jdbc.update("INSERT INTO servicio_modalidades(id,servicio_id,nombre,con_cargo,tarifa,activa) VALUES (?,?,?,?,?,?)",
                modalidad.id(), servicioId, modalidad.nombre(), modalidad.conCargo(), modalidad.tarifa(), modalidad.activa());
    }

    private void auditar(UUID autor, UUID servicioId, String tipo, String anterior) {
        jdbc.update("INSERT INTO auditoria_servicios(id,actor_id,servicio_id,tipo,anterior,nuevo,fecha) VALUES (?,?,?,?,?::jsonb,(" + FOTO + ")::jsonb,?)",
                UUID.randomUUID(), autor, servicioId, tipo, anterior, servicioId, Timestamp.from(clock.instant()));
    }

    private Servicio cargar(UUID id) {
        return buscarPorId(id).orElseThrow(() -> new ErrorServicios(ErrorServicios.Codigo.NO_EXISTE));
    }

    private static Servicio servicio(ResultSet fila, List<Modalidad> modalidades) throws SQLException {
        return new Servicio(fila.getObject("id", UUID.class), fila.getString("nombre"), fila.getInt("duracion_minutos"),
                fila.getBoolean("activo"), fila.getLong("version"), modalidades);
    }

    private static Modalidad modalidad(ResultSet fila) throws SQLException {
        return new Modalidad(fila.getObject("id", UUID.class), fila.getString("nombre"), fila.getBoolean("con_cargo"),
                fila.getBigDecimal("tarifa"), fila.getBoolean("activa"));
    }
}
