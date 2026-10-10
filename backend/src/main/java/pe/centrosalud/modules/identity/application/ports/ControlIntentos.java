package pe.centrosalud.modules.identity.application.ports;

import java.util.function.Supplier;
import pe.centrosalud.modules.identity.application.usecases.AccesoVerificado;

public interface ControlIntentos {
    /** Serializa los intentos de un identificador y conserva fallos aunque se rechace el acceso. */
    AccesoVerificado verificar(String identificador, Supplier<AccesoVerificado> verificacion);
}
