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

@Configuration
public class SecurityConfiguration {
    @Bean SecurityContextRepository securityContextRepository() {
        var repository = new HttpSessionSecurityContextRepository();
        repository.setDisableUrlRewriting(true);
        return repository;
    }
    @Bean HttpSessionCsrfTokenRepository csrfTokenRepository() { return new HttpSessionCsrfTokenRepository(); }
    @Bean SessionAuthenticationStrategy sessionAuthenticationStrategy(HttpSessionCsrfTokenRepository csrf) {
        return new CompositeSessionAuthenticationStrategy(List.of(
                new ChangeSessionIdAuthenticationStrategy(), new CsrfAuthenticationStrategy(csrf)));
    }

    @Bean SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityContextRepository contexts,
            HttpSessionCsrfTokenRepository csrf, ConsultarEstadoAcceso estados, Clock clock) throws Exception {
        return http
                .securityContext(context -> context.securityContextRepository(contexts).requireExplicitSave(true))
                .csrf(config -> config.csrfTokenRepository(csrf))
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(HttpMethod.GET, "/api/auth/csrf").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/me").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/auth/activity").authenticated()
                        .anyRequest().denyAll())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, exception) -> {
                            response.setStatus(401);
                            response.setContentType("application/problem+json");
                            response.getWriter().write("{\"status\":401,\"detail\":\"Se requiere una sesión válida\"}");
                        })
                        .accessDeniedHandler((request, response, exception) -> {
                            response.setStatus(403);
                            response.setContentType("application/problem+json");
                            response.getWriter().write("{\"status\":403,\"detail\":\"Solicitud no permitida\"}");
                        }))
                .requestCache(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(logout -> logout.logoutUrl("/api/auth/logout")
                        .invalidateHttpSession(true).clearAuthentication(true).deleteCookies("SESSION")
                        .logoutSuccessHandler((request, response, authentication) -> response.setStatus(204)))
                .addFilterBefore(new ValidarSesionFilter(estados, clock), AnonymousAuthenticationFilter.class)
                .build();
    }
}
