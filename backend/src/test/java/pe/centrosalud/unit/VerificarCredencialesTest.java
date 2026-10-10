package pe.centrosalud.unit;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import pe.centrosalud.modules.identity.application.ports.*;
import pe.centrosalud.modules.identity.application.usecases.*;
import pe.centrosalud.modules.identity.domain.Cuenta;
import static org.junit.jupiter.api.Assertions.*;

class VerificarCredencialesTest {
    private static class HashFalso implements HashContrasenas {
        int llamadas;
        String hashRecibido;
        boolean resultado;
        @Override public String generar(String valor) { throw new UnsupportedOperationException(); }
        @Override public boolean coincide(String valor, String hash) {
            llamadas++; hashRecibido = hash; return resultado;
        }
    }

    @Test void devuelveCuentaActivaConContrasenaCorrecta() {
        var cuenta = new Cuenta(UUID.randomUUID(), "bryan", true);
        var hashes = new HashFalso(); hashes.resultado = true;
        var verificar = new VerificarCredenciales(id -> Optional.of(new CredencialesCuenta(cuenta, "hash")), hashes);
        assertEquals(cuenta, verificar.ejecutar("bryan", "entrada-de-prueba"));
    }

    @Test void ausenciaInactividadYContrasenaIncorrectaTienenElMismoError() {
        for (int escenario = 0; escenario < 3; escenario++) {
            var hashes = new HashFalso(); hashes.resultado = escenario == 1;
            Optional<CredencialesCuenta> datos = escenario == 0 ? Optional.empty()
                    : Optional.of(new CredencialesCuenta(new Cuenta(UUID.randomUUID(), "bryan", escenario == 2), "hash"));
            var verificar = new VerificarCredenciales(id -> datos, hashes);
            var error = assertThrows(CredencialesInvalidas.class, () -> verificar.ejecutar("bryan", "entrada-de-prueba"));
            assertEquals("Identificador o contraseña incorrectos", error.getMessage());
            assertEquals(1, hashes.llamadas, "También se verifica si no existe o está inactiva");
            if (escenario == 0) assertNull(hashes.hashRecibido);
        }
    }

    @Test void entradasInvalidasNoConsultanLaBase() {
        var verificar = new VerificarCredenciales(id -> { throw new AssertionError("No debe consultar"); }, new HashFalso());
        assertThrows(CredencialesInvalidas.class, () -> verificar.ejecutar(null, "prueba"));
        assertThrows(CredencialesInvalidas.class, () -> verificar.ejecutar("  ", "prueba"));
        assertThrows(CredencialesInvalidas.class, () -> verificar.ejecutar("bryan", null));
        assertThrows(CredencialesInvalidas.class, () -> verificar.ejecutar("bryan", ""));
        assertThrows(CredencialesInvalidas.class, () -> verificar.ejecutar("bryan", "x".repeat(129)));
    }

    @Test void credencialesNoImprimenElHash() {
        var datos = new CredencialesCuenta(new Cuenta(UUID.randomUUID(), "bryan", true), "hash-sensible");
        assertFalse(datos.toString().contains("hash-sensible"));
    }
}
