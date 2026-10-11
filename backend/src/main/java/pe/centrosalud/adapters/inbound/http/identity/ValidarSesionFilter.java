package pe.centrosalud.adapters.inbound.http.identity;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import pe.centrosalud.modules.identity.application.ports.ConsultarEstadoAcceso;
import pe.centrosalud.modules.identity.application.ports.ConsultarRoles;
import pe.centrosalud.modules.identity.domain.VigenciaSesion;

/** Se registra solo en la cadena de Spring Security, no como filtro servlet global. */
public final class ValidarSesionFilter extends OncePerRequestFilter {
  public static final String INICIO = "IDENTITY_INICIO";
  public static final String ACTIVIDAD = "IDENTITY_ACTIVIDAD";
  private final ConsultarEstadoAcceso estados;
  private final Clock clock;
  private final ConsultarRoles roles;

  public ValidarSesionFilter(ConsultarEstadoAcceso estados, ConsultarRoles roles, Clock clock) {
    this.estados = estados;
    this.roles = roles;
    this.clock = clock;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.isAuthenticated()) {
      var session = request.getSession(false);
      boolean valida = false;
      if (session != null
          && authentication.getPrincipal() instanceof PrincipalSesion principal
          && session.getAttribute(INICIO) instanceof Long inicio
          && session.getAttribute(ACTIVIDAD) instanceof Long actividad
          && VigenciaSesion.vigente(
              Instant.ofEpochMilli(inicio), Instant.ofEpochMilli(actividad), clock.instant())) {
        valida =
            estados
                .buscarEstado(principal.id())
                .filter(e -> e.activa() && e.version() == principal.versionAcceso())
                .isPresent();
        if (valida) {
          // No confiar en los roles serializados al hacer login: leer los vigentes.
          var actual =
              UsernamePasswordAuthenticationToken.authenticated(
                  principal, null, AutorizacionSesion.authorities(roles.deCuenta(principal.id())));
          var context = SecurityContextHolder.createEmptyContext();
          context.setAuthentication(actual);
          SecurityContextHolder.setContext(context);
        }
      }
      if (!valida) {
        if (session != null) session.invalidate();
        SecurityContextHolder.clearContext();
      }
    }
    chain.doFilter(request, response);
  }
}
