package pe.centrosalud.adapters.inbound.http.administration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import pe.centrosalud.adapters.inbound.http.identity.PrincipalSesion;
import pe.centrosalud.modules.administration.application.ports.Pacientes;
import pe.centrosalud.modules.administration.application.usecases.ActualizarPaciente;
import pe.centrosalud.modules.administration.application.usecases.ErrorPacientes;
import pe.centrosalud.modules.administration.application.usecases.RegistrarPaciente;
import pe.centrosalud.modules.administration.domain.Paciente;
import pe.centrosalud.modules.administration.domain.TipoDocumento;

/** Las respuestas de error no repiten datos del paciente. */
@RestController
@RequestMapping("/api/pacientes")
public class PacientesController {
  private final RegistrarPaciente registrar;
  private final ActualizarPaciente actualizar;
  private final Pacientes pacientes;

  public PacientesController(
      RegistrarPaciente registrar, ActualizarPaciente actualizar, Pacientes pacientes) {
    this.registrar = registrar;
    this.actualizar = actualizar;
    this.pacientes = pacientes;
  }

  // Búsqueda por código, documento o nombre, paginada; sin caso de uso propio (solo lectura).
  @GetMapping
  public Pacientes.Pagina<Paciente> buscar(
      @RequestParam(defaultValue = "") String busqueda,
      @RequestParam(defaultValue = "0") int pagina) {
    if (busqueda.length() > 150 || pagina < 0 || pagina > 100000)
      throw new IllegalArgumentException();
    return pacientes.buscar(busqueda, pagina);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Paciente registrar(
      @Valid @RequestBody NuevoPaciente datos, @AuthenticationPrincipal PrincipalSesion autor) {
    return registrar.registrar(
        autor.id(),
        datos.tipoDocumento(),
        datos.numeroDocumento(),
        datos.nombres(),
        datos.apellidos(),
        datos.fechaNacimiento(),
        datos.telefono(),
        datos.correo());
  }

  @PutMapping("/{id}")
  public Paciente actualizar(
      @PathVariable UUID id,
      @Valid @RequestBody EdicionPaciente datos,
      @AuthenticationPrincipal PrincipalSesion autor) {
    return actualizar.actualizar(
        autor.id(),
        id,
        datos.tipoDocumento(),
        datos.numeroDocumento(),
        datos.nombres(),
        datos.apellidos(),
        datos.fechaNacimiento(),
        datos.telefono(),
        datos.correo(),
        datos.version());
  }

  public record NuevoPaciente(
      @NotNull TipoDocumento tipoDocumento,
      @NotBlank @Size(max = 20) String numeroDocumento,
      @NotBlank @Size(max = 100) String nombres,
      @NotBlank @Size(max = 100) String apellidos,
      @NotNull LocalDate fechaNacimiento,
      @Size(max = 20) String telefono,
      @Size(max = 254) String correo) {}

  public record EdicionPaciente(
      @NotNull TipoDocumento tipoDocumento,
      @NotBlank @Size(max = 20) String numeroDocumento,
      @NotBlank @Size(max = 100) String nombres,
      @NotBlank @Size(max = 100) String apellidos,
      @NotNull LocalDate fechaNacimiento,
      @Size(max = 20) String telefono,
      @Size(max = 254) String correo,
      @NotNull @PositiveOrZero Long version) {}

  @ExceptionHandler(ErrorPacientes.class)
  public ProblemDetail error(ErrorPacientes error) {
    HttpStatus status =
        switch (error.codigo()) {
          case NO_EXISTE -> HttpStatus.NOT_FOUND;
          case CONFLICTO, DOCUMENTO_DUPLICADO -> HttpStatus.CONFLICT;
        };
    String detalle =
        switch (error.codigo()) {
          case NO_EXISTE -> "No se encontró el paciente";
          case CONFLICTO -> "El paciente cambió. Recárgalo antes de guardar";
          case DOCUMENTO_DUPLICADO -> "Ya existe un paciente con ese documento";
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
        HttpStatus.BAD_REQUEST, "Revisa los datos del paciente");
  }

  @ExceptionHandler(DataAccessException.class)
  public ProblemDetail persistencia(DataAccessException error) {
    return ProblemDetail.forStatusAndDetail(
        HttpStatus.SERVICE_UNAVAILABLE, "No se pudo completar la operación");
  }
}
