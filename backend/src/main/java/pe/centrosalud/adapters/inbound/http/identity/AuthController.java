package pe.centrosalud.adapters.inbound.http.identity;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.Clock;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import pe.centrosalud.modules.identity.application.ports.ConsultarRoles;
import pe.centrosalud.modules.identity.application.usecases.*;
import pe.centrosalud.modules.identity.domain.VigenciaSesion;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
  private final IniciarSesion iniciar;
  private final ConsultarCuenta consultar;
  private final SecurityContextRepository contexts;
  private final SessionAuthenticationStrategy sessionStrategy;
  private final Clock clock;
  private final ConsultarRoles roles;

  public AuthController(
      IniciarSesion iniciar,
      ConsultarCuenta consultar,
      SecurityContextRepository contexts,
      SessionAuthenticationStrategy sessionStrategy,
      Clock clock,
      ConsultarRoles roles) {
    this.iniciar = iniciar;
    this.consultar = consultar;
    this.contexts = contexts;
    this.sessionStrategy = sessionStrategy;
    this.clock = clock;
    this.roles = roles;
  }

  @GetMapping("/csrf")
  public TokenCsrf csrf(CsrfToken token) {
    return new TokenCsrf(token.getHeaderName(), token.getToken());
  }

  @PostMapping("/login")
  public SesionResponse login(
      @Valid @RequestBody LoginRequest body,
      HttpServletRequest request,
      HttpServletResponse response) {
    var acceso = iniciar.ejecutar(body.getIdentificador(), body.getContrasena());
    var principal = new PrincipalSesion(acceso.cuenta().id(), acceso.versionAcceso());
    var authentication =
        UsernamePasswordAuthenticationToken.authenticated(
            principal, null, AutorizacionSesion.authorities(roles.deCuenta(principal.id())));
    sessionStrategy.onAuthentication(authentication, request, response);
    var session = request.getSession(true);
    session.setMaxInactiveInterval((int) VigenciaSesion.INACTIVIDAD.toSeconds());
    session.setAttribute(ValidarSesionFilter.INICIO, clock.millis());
    session.setAttribute(ValidarSesionFilter.ACTIVIDAD, clock.millis());
    var context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(authentication);
    SecurityContextHolder.setContext(context);
    contexts.saveContext(context, request, response);
    var cuenta = acceso.cuenta();
    return SesionResponse.de(cuenta, authentication);
  }

  @GetMapping("/me")
  public SesionResponse me(
      @AuthenticationPrincipal PrincipalSesion principal, Authentication authentication) {
    var cuenta = consultar.ejecutar(principal.id());
    return SesionResponse.de(cuenta, authentication);
  }

  /** El futuro frontend invocará esto por interacción, nunca por un temporizador automático. */
  @PostMapping("/activity")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void activity(HttpServletRequest request) {
    request.getSession(false).setAttribute(ValidarSesionFilter.ACTIVIDAD, clock.millis());
  }

  @ExceptionHandler(CredencialesInvalidas.class)
  public ProblemDetail credencialesInvalidas(CredencialesInvalidas error) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, error.getMessage());
  }

  @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
  public ProblemDetail solicitudInvalida(Exception error) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Solicitud de acceso inválida");
  }

  public record TokenCsrf(String headerName, String token) {}
}
