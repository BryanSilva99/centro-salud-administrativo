package pe.centrosalud.adapters.inbound.http.identity;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import pe.centrosalud.modules.identity.application.ports.*;
import pe.centrosalud.modules.identity.application.roles.*;
import pe.centrosalud.modules.identity.application.usecases.GestionarCuentas;
import pe.centrosalud.modules.identity.domain.Rol;

@RestController
@RequestMapping("/api/cuentas")
public class GestionCuentasController {
  private final GestionarCuentas gestionar;
  private final HistorialCuentas historial;

  public GestionCuentasController(GestionarCuentas gestionar, HistorialCuentas historial) {
    this.gestionar = gestionar;
    this.historial = historial;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public CuentaRoles crear(
      @AuthenticationPrincipal PrincipalSesion actor, @Valid @RequestBody CrearRequest body) {
    return gestionar.crear(
        actor.id(), body.getIdentificador(), body.getContrasena(), body.getRoles());
  }

  @PutMapping("/{id}")
  public CuentaRoles actualizar(
      @PathVariable UUID id,
      @AuthenticationPrincipal PrincipalSesion actor,
      @Valid @RequestBody ActualizarRequest body) {
    return gestionar.actualizar(
        actor.id(), id, body.identificador(), body.activa(), body.version());
  }

  @PutMapping("/{id}/profesional")
  public CuentaRoles vincular(
      @PathVariable UUID id,
      @AuthenticationPrincipal PrincipalSesion actor,
      @Valid @RequestBody VinculoRequest body) {
    return gestionar.vincular(actor.id(), id, body.profesionalId(), body.version());
  }

  public record VinculoRequest(UUID profesionalId, @NotNull @PositiveOrZero Long version) {}

  @GetMapping("/{id}/auditoria")
  public DirectorioRoles.Pagina<HistorialCuentas.CambioCuenta> historial(
      @PathVariable UUID id, @RequestParam(defaultValue = "0") int pagina) {
    if (pagina < 0 || pagina > 100000) throw new IllegalArgumentException();
    return historial.historialCuenta(id, pagina);
  }

  // Sin toString generado: nunca incluir la contraseña en representaciones del DTO.
  public static final class CrearRequest {
    @NotBlank
    @Size(max = 254)
    private String identificador;

    @NotBlank
    @Size(max = 256)
    private String contrasena;

    @NotNull private Set<@NotNull Rol> roles;

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

    public Set<Rol> getRoles() {
      return roles;
    }

    public void setRoles(Set<Rol> value) {
      roles = value;
    }
  }

  public record ActualizarRequest(
      @NotBlank @Size(max = 254) String identificador,
      @NotNull Boolean activa,
      @NotNull @PositiveOrZero Long version) {}

  @ExceptionHandler(ErrorRoles.class)
  public ProblemDetail error(ErrorRoles error) {
    HttpStatus status =
        switch (error.codigo()) {
          case NO_EXISTE -> HttpStatus.NOT_FOUND;
          case PROHIBIDO -> HttpStatus.FORBIDDEN;
          default -> HttpStatus.CONFLICT;
        };
    String detalle =
        switch (error.codigo()) {
          case PROFESIONAL_INVALIDO -> "Selecciona una ficha profesional activa existente";
          case VINCULO_OCUPADO -> "La ficha ya está vinculada a otra cuenta";
          case REQUIERE_PRO ->
              "El vínculo profesional requiere el rol PRO; desvincula antes de retirarlo";
          case DUPLICADO -> "El identificador ya está en uso";
          case ULTIMO_ADMIN -> "Debe permanecer un administrador activo";
          case CONFLICTO -> "La cuenta cambió. Recárgala antes de guardar";
          case PROHIBIDO -> "No tienes permiso para administrar cuentas";
          case NO_EXISTE -> "No se encontró la cuenta";
        };
    var problem = ProblemDetail.forStatusAndDetail(status, detalle);
    problem.setProperty("code", error.codigo().name());
    return problem;
  }

  @ExceptionHandler({
    IllegalArgumentException.class,
    MethodArgumentNotValidException.class,
    HttpMessageNotReadableException.class
  })
  public ProblemDetail invalido(Exception error) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Datos de cuenta inválidos");
  }

  @ExceptionHandler(DataAccessException.class)
  public ProblemDetail persistencia(DataAccessException error) {
    return ProblemDetail.forStatusAndDetail(
        HttpStatus.SERVICE_UNAVAILABLE, "No se pudo completar la operación");
  }
}
