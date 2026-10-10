package pe.centrosalud.adapters.outbound.security;

import java.util.UUID;
import pe.centrosalud.modules.identity.domain.PoliticaContrasenas;
import java.util.regex.Pattern;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.stereotype.Component;
import pe.centrosalud.modules.identity.application.ports.HashContrasenas;

@Component
public final class HashContrasenasSpring implements HashContrasenas {
    private static final String PREFIJO = "{pbkdf2-sha256-v1}";
    private static final Pattern FORMATO = Pattern.compile("[0-9a-f]{96}");
    private final Pbkdf2PasswordEncoder encoder;
    private final String hashSimulado;

    public HashContrasenasSpring() {
        encoder = new Pbkdf2PasswordEncoder("", 16, 600_000, 256);
        encoder.setAlgorithm(Pbkdf2PasswordEncoder.SecretKeyFactoryAlgorithm.PBKDF2WithHmacSHA256);
        hashSimulado = encoder.encode(UUID.randomUUID().toString());
    }

    @Override public String generar(String contrasena) {
        PoliticaContrasenas.validarNueva(contrasena);
        return PREFIJO + encoder.encode(contrasena);
    }

    @Override public boolean coincide(String contrasena, String hash) {
        boolean formatoValido = hash != null && hash.startsWith(PREFIJO)
                && FORMATO.matcher(hash.substring(PREFIJO.length())).matches();
        boolean entradaValida = contrasena != null
                && contrasena.codePointCount(0, contrasena.length()) <= PoliticaContrasenas.MAXIMO;
        String entrada = entradaValida ? contrasena : "";
        String valor = formatoValido ? hash.substring(PREFIJO.length()) : hashSimulado;
        boolean resultado = encoder.matches(entrada, valor);
        return formatoValido && entradaValida && resultado;
    }
}
