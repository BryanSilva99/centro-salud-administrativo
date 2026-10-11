package pe.centrosalud.bootstrap;

import java.time.Clock;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import pe.centrosalud.adapters.inbound.http.identity.ValidarSesionFilter;
import pe.centrosalud.modules.identity.application.ports.ConsultarEstadoAcceso;
import pe.centrosalud.modules.identity.application.ports.ConsultarRoles;

@Configuration
public class SecurityConfiguration {
  @Bean
  SecurityContextRepository securityContextRepository() {
    var repository = new HttpSessionSecurityContextRepository();
    repository.setDisableUrlRewriting(true);
    return repository;
  }

  @Bean
  HttpSessionCsrfTokenRepository csrfTokenRepository() {
    return new HttpSessionCsrfTokenRepository();
  }

  @Bean
  SessionAuthenticationStrategy sessionAuthenticationStrategy(HttpSessionCsrfTokenRepository csrf) {
    return new CompositeSessionAuthenticationStrategy(
        List.of(new ChangeSessionIdAuthenticationStrategy(), new CsrfAuthenticationStrategy(csrf)));
  }

  @Bean
  SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      SecurityContextRepository contexts,
      HttpSessionCsrfTokenRepository csrf,
      ConsultarEstadoAcceso estados,
      ConsultarRoles roles,
      Clock clock)
      throws Exception {
    return http.securityContext(
            context -> context.securityContextRepository(contexts).requireExplicitSave(true))
        .csrf(config -> config.csrfTokenRepository(csrf))
        .authorizeHttpRequests(
            requests ->
                requests
                    .requestMatchers(HttpMethod.GET, "/api/auth/csrf")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/auth/login")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/auth/me")
                    .authenticated()
                    .requestMatchers(HttpMethod.POST, "/api/auth/activity")
                    .authenticated()
                    .requestMatchers(
                        HttpMethod.GET,
                        "/api/cuentas",
                        "/api/cuentas/{id}/roles",
                        "/api/cuentas/{id}/roles/auditoria")
                    .hasAuthority("ROLES_GESTIONAR")
                    .requestMatchers(
                        HttpMethod.GET,
                        "/api/profesionales/fichas",
                        "/api/profesionales/fichas/{id}")
                    .hasAuthority("PROFESIONALES_GESTIONAR")
                    .requestMatchers(HttpMethod.POST, "/api/profesionales/fichas")
                    .hasAuthority("PROFESIONALES_GESTIONAR")
                    .requestMatchers(HttpMethod.PUT, "/api/profesionales/fichas/{id}")
                    .hasAuthority("PROFESIONALES_GESTIONAR")
                    .requestMatchers(HttpMethod.PUT, "/api/cuentas/{id}/profesional")
                    .hasAuthority("CUENTAS_GESTIONAR")
                    .requestMatchers(HttpMethod.POST, "/api/cuentas")
                    .hasAuthority("CUENTAS_GESTIONAR")
                    .requestMatchers(HttpMethod.PUT, "/api/cuentas/{id}")
                    .hasAuthority("CUENTAS_GESTIONAR")
                    .requestMatchers(HttpMethod.GET, "/api/cuentas/{id}/auditoria")
                    .hasAuthority("CUENTAS_GESTIONAR")
                    .requestMatchers(HttpMethod.PUT, "/api/cuentas/{id}/roles")
                    .hasAuthority("ROLES_GESTIONAR")
                    .requestMatchers(HttpMethod.GET, "/api/cuentas/{id}")
                    .hasAuthority("CUENTAS_CONSULTAR")
<<<<<<< HEAD
                    .requestMatchers(HttpMethod.GET, "/api/pacientes")
                    .hasAuthority("PACIENTES_GESTIONAR")
                    .requestMatchers(HttpMethod.POST, "/api/pacientes")
                    .hasAuthority("PACIENTES_GESTIONAR")
                    .requestMatchers(HttpMethod.PUT, "/api/pacientes/{id}")
                    .hasAuthority("PACIENTES_GESTIONAR")
                    .requestMatchers(HttpMethod.GET, "/api/servicios", "/api/servicios/{id}")
                    .hasAuthority("SERVICIOS_GESTIONAR")
                    .requestMatchers(HttpMethod.POST, "/api/servicios")
                    .hasAuthority("SERVICIOS_GESTIONAR")
                    .requestMatchers(HttpMethod.PUT, "/api/servicios/{id}")
                    .hasAuthority("SERVICIOS_GESTIONAR")
                    .requestMatchers(HttpMethod.GET, "/api/consultorios", "/api/consultorios/{id}")
                    .hasAuthority("CONSULTORIOS_GESTIONAR")
                    .requestMatchers(HttpMethod.POST, "/api/consultorios")
                    .hasAuthority("CONSULTORIOS_GESTIONAR")
                    .requestMatchers(HttpMethod.PUT, "/api/consultorios/{id}")
                    .hasAuthority("CONSULTORIOS_GESTIONAR")
                    .requestMatchers(HttpMethod.GET, "/api/disponibilidad/{tipo}/{recursoId}")
                    .hasAuthority("DISPONIBILIDAD_GESTIONAR")
                    .requestMatchers(HttpMethod.POST, "/api/disponibilidad/{tipo}/{recursoId}")
                    .hasAuthority("DISPONIBILIDAD_GESTIONAR")
                    .requestMatchers(
                        HttpMethod.PUT, "/api/disponibilidad/{tipo}/{recursoId}/{turnoId}")
                    .hasAuthority("DISPONIBILIDAD_GESTIONAR")
                    .requestMatchers("/api/citas/**")
                    .authenticated()
                    .requestMatchers("/api/billing/**")
                    .hasAnyAuthority("CAJ", "ADM")
                    .anyRequest()
                    .denyAll())
        .exceptionHandling(
            errors ->
                errors
                    .authenticationEntryPoint(
                        (request, response, exception) -> {
                          response.setStatus(401);
                          response.setContentType("application/problem+json");
                          response
                              .getWriter()
                              .write(
                                  "{\"status\":401,\"detail\":\"Se requiere una sesión válida\"}");
                        })
                    .accessDeniedHandler(
                        (request, response, exception) -> {
                          response.setStatus(403);
                          response.setContentType("application/problem+json");
                          response
                              .getWriter()
                              .write("{\"status\":403,\"detail\":\"Solicitud no permitida\"}");
                        }))
        .requestCache(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .logout(
            logout ->
                logout
                    .logoutUrl("/api/auth/logout")
                    .invalidateHttpSession(true)
                    .clearAuthentication(true)
                    .deleteCookies("SESSION")
                    .logoutSuccessHandler(
                        (request, response, authentication) -> response.setStatus(204)))
        .addFilterBefore(
            new ValidarSesionFilter(estados, roles, clock), AnonymousAuthenticationFilter.class)
        .build();
  }
}
