package pe.centrosalud.modules.identity.application.ports;

import java.util.Optional;

public interface ConsultarCredenciales {
    Optional<CredencialesCuenta> buscarPorIdentificador(String identificador);
}
