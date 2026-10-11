package pe.centrosalud.adapters.inbound.http.identity;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.centrosalud.modules.identity.application.usecases.ConsultarCuenta;
import pe.centrosalud.modules.identity.application.usecases.CuentaNoEncontrada;

@RestController
@RequestMapping("/api/cuentas")
public class CuentaController {
  private final ConsultarCuenta consultarCuenta;

  public CuentaController(ConsultarCuenta consultarCuenta) {
    this.consultarCuenta = consultarCuenta;
  }

  @GetMapping("/{id}")
  public CuentaResponse consultar(@PathVariable("id") UUID id) {
    var cuenta = consultarCuenta.ejecutar(id);
    return new CuentaResponse(cuenta.id(), cuenta.identificador(), cuenta.activa());
  }

  @ExceptionHandler(CuentaNoEncontrada.class)
  public ProblemDetail cuentaNoEncontrada(CuentaNoEncontrada error) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, error.getMessage());
  }

  public record CuentaResponse(UUID id, String identificador, boolean activa) {}
}
