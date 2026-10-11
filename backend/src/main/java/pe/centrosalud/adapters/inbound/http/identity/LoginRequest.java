package pe.centrosalud.adapters.inbound.http.identity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Clase sin toString automático para no imprimir la contraseña recibida. */
public final class LoginRequest {
  @NotBlank
  @Size(max = 254)
  private String identificador;

  @NotBlank
  @Size(max = 256)
  private String contrasena;

  public String getIdentificador() {
    return identificador;
  }

  public void setIdentificador(String value) {
    identificador = value;
  }

  public String getContrasena() {
    return contrasena;
  }

  public void setContrasena(String value) {
    contrasena = value;
  }
}
