package pe.centrosalud.adapters.inbound.http.administration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import pe.centrosalud.adapters.inbound.http.identity.PrincipalSesion;
import pe.centrosalud.modules.administration.application.ports.Consultorios;
import pe.centrosalud.modules.administration.application.usecases.ErrorConsultorios;
import pe.centrosalud.modules.administration.application.usecases.GestionarConsultorios;
import pe.centrosalud.modules.administration.domain.Consultorio;

@RestController
@RequestMapping("/api/consultorios")
public class ConsultoriosController {
  private final GestionarConsultorios gestionar;
  private final Consultorios consultorios;

  public ConsultoriosController(GestionarConsultorios gestionar, Consultorios consultorios) {
    this.gestionar = gestionar;
    this.consultorios = consultorios;
  }

  // El catálogo es pequeño: se devuelve completo, activos e inactivos, ordenado por código.
  @GetMapping
  public List<Consultorio> listar() {
    return consultorios.listar();
  }

  @GetMapping("/{id}")
  public ResponseEntity<Consultorio> consultar(@PathVariable UUID id) {
    return ResponseEntity.of(consultorios.buscarPorId(id));
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Consultorio crear(
      @Valid @RequestBody NuevoConsultorio datos, @AuthenticationPrincipal PrincipalSesion autor) {
    return gestionar.crear(
        autor.id(), datos.codigo(), datos.nombre(), datos.serviciosCompatibles());
  }

  @PutMapping("/{id}")
  public Consultorio actualizar(
      @PathVariable UUID id,
      @Valid @RequestBody EdicionConsultorio datos,
      @AuthenticationPrincipal PrincipalSesion autor) {
    return gestionar.actualizar(
        autor.id(),
        id,
        datos.codigo(),
        datos.nombre(),
        datos.activo(),
        datos.serviciosCompatibles(),
        datos.version());
  }

  public record NuevoConsultorio(
      @NotBlank @Size(max = 20) String codigo,
      @NotBlank @Size(max = 150) String nombre,
      @NotNull Set<@NotNull UUID> serviciosCompatibles) {}

  public record EdicionConsultorio(
      @NotBlank @Size(max = 20) String codigo,
      @NotBlank @Size(max = 150) String nombre,
      @NotNull Boolean activo,
      @NotNull Set<@NotNull UUID> serviciosCompatibles,
      @NotNull @PositiveOrZero Long version) {}

  @ExceptionHandler(ErrorConsultorios.class)
  public ProblemDetail error(ErrorConsultorios error) {
    HttpStatus status =
        switch (error.codigo()) {
          case NO_EXISTE -> HttpStatus.NOT_FOUND;
          case SERVICIO_INVALIDO -> HttpStatus.BAD_REQUEST;
          case CONFLICTO, CODIGO_DUPLICADO -> HttpStatus.CONFLICT;
        };
    String detalle =
        switch (error.codigo()) {
          case NO_EXISTE -> "No se encontró el consultorio";
          case SERVICIO_INVALIDO -> "Selecciona un servicio existente";
          case CONFLICTO -> "El consultorio cambió. Recárgalo antes de guardar";
          case CODIGO_DUPLICADO -> "Ya existe un consultorio con ese código";
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
    return ProblemDetail.forStatusAndDetail(
        HttpStatus.BAD_REQUEST, "Revisa los datos del consultorio");
  }

  @ExceptionHandler(DataAccessException.class)
  public ProblemDetail persistencia(DataAccessException error) {
    return ProblemDetail.forStatusAndDetail(
        HttpStatus.SERVICE_UNAVAILABLE, "No se pudo completar la operación");
  }
}
