package pe.centrosalud.adapters.outbound.postgres.identity;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.function.Supplier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import pe.centrosalud.modules.identity.application.ports.ControlIntentos;
import pe.centrosalud.modules.identity.application.usecases.*;
import pe.centrosalud.modules.identity.domain.IntentosLogin;

@Repository
public class ControlIntentosPostgres implements ControlIntentos {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaccion;
    private final Clock clock;

    public ControlIntentosPostgres(JdbcTemplate jdbc, PlatformTransactionManager manager, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
        transaccion = new TransactionTemplate(manager);
        transaccion.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override public AccesoVerificado verificar(String identificador, Supplier<AccesoVerificado> verificacion) {
        String clave = clave(identificador);
        AccesoVerificado resultado = transaccion.execute(status -> {
            jdbc.update("INSERT INTO intentos_login (clave) VALUES (?) ON CONFLICT DO NOTHING", clave);
            IntentosLogin estado = jdbc.queryForObject(
                    "SELECT fallos, bloqueado_hasta FROM intentos_login WHERE clave = ? FOR UPDATE",
                    (rs, row) -> {
                        Object[] valores = (Object[]) rs.getArray("fallos").getArray();
                        var fechas = Arrays.stream(valores).map(v -> Instant.ofEpochMilli(((Number) v).longValue())).toList();
                        Long hasta = rs.getObject("bloqueado_hasta", Long.class);
                        return new IntentosLogin(fechas, hasta == null ? null : Instant.ofEpochMilli(hasta));
                    }, clave);
            // Se lee el reloj después de adquirir el bloqueo, no antes de esperar.
            if (estado.bloqueado(clock.instant())) return null;
            try {
                AccesoVerificado acceso = verificacion.get();
                guardar(clave, IntentosLogin.vacio());
                return acceso;
            } catch (CredencialesInvalidas error) {
                guardar(clave, estado.fallar(clock.instant()));
                return null; // El fallo se confirma antes de lanzar la excepción al llamador.
            }
        });
        if (resultado == null) throw new CredencialesInvalidas();
        return resultado;
    }

    private void guardar(String clave, IntentosLogin estado) {
        jdbc.update(connection -> {
            var statement = connection.prepareStatement(
                    "UPDATE intentos_login SET fallos = ?, bloqueado_hasta = ?, actualizado = ? WHERE clave = ?");
            statement.setArray(1, connection.createArrayOf("bigint", estado.fallos().stream().map(Instant::toEpochMilli).toArray()));
            statement.setObject(2, estado.bloqueadoHasta() == null ? null : estado.bloqueadoHasta().toEpochMilli());
            statement.setLong(3, clock.millis());
            statement.setString(4, clave);
            return statement;
        });
    }

    public static String clave(String identificador) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(identificador.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 no disponible", impossible);
        }
    }
}
