package pe.centrosalud.modules.identity.application.usecases;

import pe.centrosalud.modules.identity.application.ports.ControlIntentos;

/** Verifica identidad bajo límite de intentos; el adaptador HTTP establece la sesión. */
public final class IniciarSesion {
  private final VerificarCredenciales credenciales;
  private final ControlIntentos intentos;

  public IniciarSesion(VerificarCredenciales credenciales, ControlIntentos intentos) {
    this.credenciales = credenciales;
    this.intentos = intentos;
  }

  public AccesoVerificado ejecutar(String identificador, String contrasena) {
    if (identificador == null
        || identificador.isBlank()
        || identificador.length() > 254
        || contrasena == null
        || contrasena.isEmpty()
        || contrasena.codePointCount(0, contrasena.length()) > 128) {
      throw new CredencialesInvalidas();
    }
    return intentos.verificar(
        identificador, () -> credenciales.verificar(identificador, contrasena));
  }
}
