package pe.centrosalud.modules.identity.application.usecases;

import java.util.Objects;
import pe.centrosalud.modules.identity.application.ports.ConsultarCredenciales;
import pe.centrosalud.modules.identity.application.ports.HashContrasenas;
import pe.centrosalud.modules.identity.domain.Cuenta;
import pe.centrosalud.modules.identity.domain.PoliticaContrasenas;

/** Verifica identidad; todavía no crea una sesión ni concede permisos. */
public final class VerificarCredenciales {
  private final ConsultarCredenciales credenciales;
  private final HashContrasenas hashes;

  public VerificarCredenciales(ConsultarCredenciales credenciales, HashContrasenas hashes) {
    this.credenciales = Objects.requireNonNull(credenciales);
    this.hashes = Objects.requireNonNull(hashes);
  }

  public Cuenta ejecutar(String identificador, String contrasena) {
    return verificar(identificador, contrasena).cuenta();
  }

  public AccesoVerificado verificar(String identificador, String contrasena) {
    if (identificador == null
        || identificador.isBlank()
        || contrasena == null
        || contrasena.isEmpty()
        || contrasena.codePointCount(0, contrasena.length()) > PoliticaContrasenas.MAXIMO) {
      throw new CredencialesInvalidas();
    }
    var encontrada = credenciales.buscarPorIdentificador(identificador);
    // Verificar también cuando no existe o está inactiva, reduciendo diferencias de tiempo.
    boolean coincide = hashes.coincide(contrasena, encontrada.map(c -> c.hash()).orElse(null));
    if (encontrada.isEmpty() || !coincide || !encontrada.get().cuenta().activa()) {
      throw new CredencialesInvalidas();
    }
    return new AccesoVerificado(encontrada.get().cuenta(), encontrada.get().versionAcceso());
  }
}
