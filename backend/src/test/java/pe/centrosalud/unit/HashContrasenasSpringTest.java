package pe.centrosalud.unit;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import pe.centrosalud.adapters.outbound.security.HashContrasenasSpring;
import static org.junit.jupiter.api.Assertions.*;

class HashContrasenasSpringTest {
    private static final HashContrasenasSpring hashes = new HashContrasenasSpring();

    @Test void usaSalAleatoriaYVerificaSinGuardarTextoPlano() {
        String clave = UUID.randomUUID().toString();
        String uno = hashes.generar(clave), dos = hashes.generar(clave);
        assertNotEquals(uno, dos);
        assertFalse(uno.contains(clave));
        assertTrue(hashes.coincide(clave, uno));
        assertTrue(hashes.coincide(clave, dos));
        assertFalse(hashes.coincide(clave + "x", uno));
    }

    @Test void rechazaHashesAusentesMalformadosODeOtroAlgoritmo() {
        for (String hash : new String[]{null, "", "texto-plano", "{otro}123", "{pbkdf2-sha256-v1}zz"}) {
            assertFalse(hashes.coincide("entrada-de-prueba", hash));
        }
    }

    @Test void validaLongitudesYNoRecortaLaContrasena() {
        assertThrows(IllegalArgumentException.class, () -> hashes.generar("x".repeat(11)));
        assertThrows(IllegalArgumentException.class, () -> hashes.generar("x".repeat(129)));
        assertThrows(IllegalArgumentException.class, () -> hashes.generar(" ".repeat(12)));
        assertThrows(IllegalArgumentException.class, () -> hashes.generar(null));
        String clave = " " + UUID.randomUUID() + " ";
        String hash = hashes.generar(clave);
        assertTrue(hashes.coincide(clave, hash));
        assertFalse(hashes.coincide(clave.strip(), hash));
        assertFalse(hashes.coincide(null, hash));
        assertFalse(hashes.coincide("x".repeat(129), hash));
    }
}
