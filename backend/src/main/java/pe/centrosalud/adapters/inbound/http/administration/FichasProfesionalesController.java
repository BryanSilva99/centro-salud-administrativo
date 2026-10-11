package pe.centrosalud.adapters.inbound.http.administration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import pe.centrosalud.adapters.inbound.http.identity.PrincipalSesion;
import pe.centrosalud.modules.administration.application.ports.FichasProfesionales;
import pe.centrosalud.modules.administration.application.usecases.ErrorProfesionales;
import pe.centrosalud.modules.administration.application.usecases.GestionarProfesionales;
import pe.centrosalud.modules.administration.domain.FichaProfesional;

@RestController
@RequestMapping("/api/profesionales/fichas")
public class FichasProfesionalesController {
  private final FichasProfesionales fichas;
  private final GestionarProfesionales gestionar;

  public FichasProfesionalesController(
      FichasProfesionales fichas, GestionarProfesionales gestionar) {
    this.fichas = fichas;
    this.gestionar = gestionar;
  }

  @GetMapping
  public List<FichaProfesional> buscar(@RequestParam(defaultValue = "") String busqueda) {
    if (busqueda.length() > 150) throw new IllegalArgumentException();
    return fichas.buscar(busqueda);
  }

  @GetMapping("/{id}")
  public org.springframework.http.ResponseEntity<FichaProfesional> consultar(
      @PathVariable UUID id) {
    return org.springframework.http.ResponseEntity.of(fichas.buscarPorId(id));
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public FichaProfesional registrar(
      @Valid @RequestBody NuevaFicha datos, @AuthenticationPrincipal PrincipalSesion autor) {
    return gestionar.crear(
        autor.id(),
        datos.identificacion(),
        datos.nombre(),
        datos.funcion(),
        datos.serviciosHabilitados());
  }

  @PutMapping("/{id}")
  public FichaProfesional actualizar(
      @PathVariable UUID id,
      @Valid @RequestBody EdicionFicha datos,
      @AuthenticationPrincipal PrincipalSesion autor) {
    return gestionar.actualizar(
        autor.id(),
        id,
        datos.identificacion(),
        datos.nombre(),
        datos.funcion(),
        datos.activa(),
        datos.serviciosHabilitados(),
        datos.version());
  }

  public record NuevaFicha(
      @NotBlank @Size(max = 50) String identificacion,
      @NotBlank @Size(max = 150) String nombre,
      @NotBlank @Size(max = 100) String funcion,
      // Opcional: las solicitudes de RF-02 anteriores a RF-07 no lo envían.
      Set<@NotNull UUID> serviciosHabilitados) {}

  public record EdicionFicha(
      @NotBlank @Size(max = 50) String identificacion,
      @NotBlank @Size(max = 150) String nombre,
      @NotBlank @Size(max = 100) String funcion,
      @NotNull Boolean activa,
      @NotNull Set<@NotNull UUID> serviciosHabilitados,
      @NotNull @PositiveOrZero Long version) {}

  @ExceptionHandler(ErrorProfesionales.class)
  public ProblemDetail error(ErrorProfesionales error) {
    HttpStatus status =
        switch (error.codigo()) {
          case NO_EXISTE -> HttpStatus.NOT_FOUND;
          case SERVICIO_INVALIDO -> HttpStatus.BAD_REQUEST;
          case CONFLICTO -> HttpStatus.CONFLICT;
        };
    String detalle =
        switch (error.codigo()) {
          case NO_EXISTE -> "No se encontró la ficha";
          case SERVICIO_INVALIDO -> "Selecciona un servicio existente";
          case CONFLICTO -> "La ficha cambió. Recárgala antes de guardar";
        };
    var problem = ProblemDetail.forStatusAndDetail(status, detalle);
    problem.setProperty("code", error.codigo().name());
    return problem;
  }

  @ExceptionHandler(DuplicateKeyException.class)
  public ProblemDetail duplicada(DuplicateKeyException error) {
    return ProblemDetail.forStatusAndDetail(
        HttpStatus.CONFLICT, "Ya existe una ficha con esa identificación");
  }

  @ExceptionHandler({
    IllegalArgumentException.class,
    MethodArgumentNotValidException.class,
    org.springframework.http.converter.HttpMessageNotReadableException.class
  })
  public ProblemDetail invalida(Exception error) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Revisa los datos de la ficha");
  }

  @ExceptionHandler(DataAccessException.class)
  public ProblemDetail persistencia(DataAccessException error) {
    return ProblemDetail.forStatusAndDetail(
        HttpStatus.SERVICE_UNAVAILABLE, "No se pudo guardar o consultar la ficha");
  }
}
