package pe.centrosalud.adapters.outbound.postgres.billing;

import java.util.*;
import java.util.function.Supplier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import pe.centrosalud.modules.billing.application.ports.RegistroCaja;
import pe.centrosalud.modules.billing.domain.SolicitudReversion.TipoOperacion;

@Component
public class RegistroCajaPostgres implements RegistroCaja {
  private final JdbcTemplate jdbc;
  private final TransactionTemplate tx;

  public RegistroCajaPostgres(JdbcTemplate jdbc, PlatformTransactionManager manager) {
    this.jdbc = jdbc;
    this.tx = new TransactionTemplate(manager);
  }

  public <T> T ejecutar(Supplier<T> trabajo) {
    return tx.execute(
        status -> {
          // Una cola transaccional de caja para el volumen del centro; funciona entre instancias.
          jdbc.queryForObject("select id from control_caja where id=1 for update", Integer.class);
          return trabajo.get();
        });
  }

  public Optional<Reintento> reintento(UUID clave) {
    return jdbc
        .query(
            "select actor, contenido, resultado from reintentos_caja where clave=?",
            (r, n) ->
                new Reintento(
                    r.getObject(1, UUID.class), r.getString(2), r.getObject(3, UUID.class)),
            clave)
        .stream()
        .findFirst();
  }

  public void registrar(UUID clave, UUID actor, String contenido, UUID resultado) {
    jdbc.update(
        "insert into reintentos_caja(clave,actor,contenido,resultado) values (?,?,?,?)",
        clave,
        actor,
        contenido,
        resultado);
  }

  public boolean compensada(UUID original, TipoOperacion tipo) {
    return jdbc.queryForObject(
            "select count(*) from compensaciones_caja where original=? and tipo=?",
            Integer.class,
            original,
            tipo.name())
        > 0;
  }

  public void compensar(UUID original, TipoOperacion tipo, UUID solicitud, UUID movimiento) {
    jdbc.update(
        "insert into compensaciones_caja(original,tipo,solicitud,movimiento) values (?,?,?,?)",
        original,
        tipo.name(),
        solicitud,
        movimiento);
  }
}
