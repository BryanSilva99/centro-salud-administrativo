package pe.centrosalud.adapters.inbound.http.administration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import pe.centrosalud.adapters.inbound.http.identity.PrincipalSesion;
import pe.centrosalud.modules.administration.application.ports.Disponibilidad;
import pe.centrosalud.modules.administration.application.usecases.ErrorDisponibilidad;
import pe.centrosalud.modules.administration.application.usecases.GestionarDisponibilidad;
import pe.centrosalud.modules.administration.domain.TipoRecurso;
import pe.centrosalud.modules.administration.domain.TipoTurno;
import pe.centrosalud.modules.administration.domain.Turno;

/** {tipo} en la ruta es "profesionales" o "consultorios"; se traduce a TipoRecurso. */
@RestController
@RequestMapping("/api/disponibilidad/{tipo}/{recursoId}")
public class DisponibilidadController {
  private final GestionarDisponibilidad gestionar;
  private final Disponibilidad disponibilidad;

  public DisponibilidadController(
      GestionarDisponibilidad gestionar, Disponibilidad disponibilidad) {
    this.gestionar = gestionar;
    this.disponibilidad = disponibilidad;
  }

  @GetMapping
  public List<Turno> listar(@PathVariable String tipo, @PathVariable UUID recursoId) {
    return disponibilidad.listar(tipoRecurso(tipo), recursoId);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Turno crear(
      @PathVariable String tipo,
      @PathVariable UUID recursoId,
      @Valid @RequestBody NuevoTurno datos,
      @AuthenticationPrincipal PrincipalSesion autor) {
    return gestionar.crear(
        autor.id(),
        tipoRecurso(tipo),
        recursoId,
        datos.fecha(),
        datos.horaInicio(),
        datos.horaFin(),
        datos.tipo());
  }

  @PutMapping("/{turnoId}")
  public void desactivar(
      @PathVariable String tipo,
      @PathVariable UUID recursoId,
      @PathVariable UUID turnoId,
      @Valid @RequestBody Desactivacion datos,
      @AuthenticationPrincipal PrincipalSesion autor) {
    if (datos.activo()) throw new IllegalArgumentException();
    gestionar.desactivar(autor.id(), tipoRecurso(tipo), turnoId);
  }

  public record NuevoTurno(
      @NotNull LocalDate fecha,
      @NotNull LocalTime horaInicio,
      @NotNull LocalTime horaFin,
      @NotNull TipoTurno tipo) {}

  public record Desactivacion(@NotNull Boolean activo) {}

  private static TipoRecurso tipoRecurso(String valor) {
    return switch (valor) {
      case "profesionales" -> TipoRecurso.PROFESIONAL;
      case "consultorios" -> TipoRecurso.CONSULTORIO;
      default -> throw new IllegalArgumentException();
    };
  }

  @ExceptionHandler(ErrorDisponibilidad.class)
  public ProblemDetail error(ErrorDisponibilidad error) {
    HttpStatus status =
        switch (error.codigo()) {
          case NO_EXISTE -> HttpStatus.NOT_FOUND;
          case RECURSO_INVALIDO -> HttpStatus.BAD_REQUEST;
          case SUPERPUESTO -> HttpStatus.CONFLICT;
        };
    String detalle =
        switch (error.codigo()) {
          case NO_EXISTE -> "No se encontró el turno";
          case RECURSO_INVALIDO -> "Selecciona un profesional o consultorio existente";
          case SUPERPUESTO -> "El intervalo se cruza con otro turno activo del mismo recurso";
        };
    var problem = ProblemDetail.forStatusAndDetail(status, detalle);
    problem.setProperty("code", error.codigo().name());
    return problem;
  }

  @ExceptionHandler({
    IllegalArgumentException.class,
    MethodArgumentNotValidException.class,
    org.springframework.http.converter.HttpMessageNotReadableException.class
  })
  public ProblemDetail invalido(Exception error) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Revisa los datos del turno");
  }

  @ExceptionHandler(DataAccessException.class)
  public ProblemDetail persistencia(DataAccessException error) {
    return ProblemDetail.forStatusAndDetail(
        HttpStatus.SERVICE_UNAVAILABLE, "No se pudo completar la operación");
  }
}
