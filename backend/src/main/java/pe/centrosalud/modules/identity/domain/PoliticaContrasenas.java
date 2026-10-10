package pe.centrosalud.modules.identity.domain;

/** RNF-02 exige un mínimo de 12; se fija 128 como límite de entrada del sistema. */
public final class PoliticaContrasenas {
  public static final int MINIMO = 12;
  public static final int MAXIMO = 128;

  private PoliticaContrasenas() {}

  public static void validarNueva(String contrasena) {
    if (contrasena == null || contrasena.isBlank()) throw invalida();
    int longitud = contrasena.codePointCount(0, contrasena.length());
    if (longitud < MINIMO || longitud > MAXIMO) throw invalida();
  }

  private static IllegalArgumentException invalida() {
    return new IllegalArgumentException("La contraseña debe tener entre 12 y 128 caracteres");
  }
}
