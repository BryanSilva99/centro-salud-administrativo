package pe.centrosalud.adapters.inbound.http.administration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
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
import pe.centrosalud.modules.administration.domain.FichaProfesional;

@RestController
@RequestMapping("/api/profesionales/fichas")
public class FichasProfesionalesController {
  private final FichasProfesionales fichas;

  public FichasProfesionalesController(FichasProfesionales fichas) {
    this.fichas = fichas;
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
    return fichas.registrar(
        new FichaProfesional(
            UUID.randomUUID(), datos.identificacion(), datos.nombre(), datos.funcion(), true),
        autor.id());
  }

  public record NuevaFicha(
      @NotBlank @Size(max = 50) String identificacion,
      @NotBlank @Size(max = 150) String nombre,
      @NotBlank @Size(max = 100) String funcion) {}

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
