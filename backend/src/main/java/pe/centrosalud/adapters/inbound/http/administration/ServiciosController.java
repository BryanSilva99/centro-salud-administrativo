package pe.centrosalud.adapters.inbound.http.administration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;
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
import pe.centrosalud.modules.administration.application.ports.Servicios;
import pe.centrosalud.modules.administration.application.usecases.ErrorServicios;
import pe.centrosalud.modules.administration.application.usecases.GestionarServicios;
import pe.centrosalud.modules.administration.domain.Servicio;

@RestController
@RequestMapping("/api/servicios")
public class ServiciosController {
  private final GestionarServicios gestionar;
  private final Servicios servicios;

  public ServiciosController(GestionarServicios gestionar, Servicios servicios) {
    this.gestionar = gestionar;
    this.servicios = servicios;
  }

  // El catálogo es pequeño: se devuelve completo, activos e inactivos, ordenado por nombre.
  @GetMapping
  public List<Servicio> listar() {
    return servicios.listar();
  }

  @GetMapping("/{id}")
  public ResponseEntity<Servicio> consultar(@PathVariable UUID id) {
    return ResponseEntity.of(servicios.buscarPorId(id));
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Servicio crear(
      @Valid @RequestBody NuevoServicio datos, @AuthenticationPrincipal PrincipalSesion autor) {
    return gestionar.crear(
        autor.id(), datos.nombre(), datos.duracionMinutos(), modalidades(datos.modalidades()));
  }

  @PutMapping("/{id}")
  public Servicio actualizar(
      @PathVariable UUID id,
      @Valid @RequestBody EdicionServicio datos,
      @AuthenticationPrincipal PrincipalSesion autor) {
    return gestionar.actualizar(
        autor.id(),
        id,
        datos.nombre(),
        datos.duracionMinutos(),
        datos.activo(),
        modalidades(datos.modalidades()),
        datos.version());
  }

  /**
   * La tarifa se acepta como cadena ("40.00") o número y se convierte a BigDecimal sin pasar por
   * double.
   */
  public record ModalidadSolicitud(
      UUID id,
      @NotBlank @Size(max = 60) String nombre,
      @NotNull Boolean conCargo,
      @NotNull BigDecimal tarifa,
      @NotNull Boolean activa) {}

  public record NuevoServicio(
      @NotBlank @Size(max = 100) String nombre,
      @NotNull Integer duracionMinutos,
      @NotNull @Size(min = 1, max = 50) List<@NotNull @Valid ModalidadSolicitud> modalidades) {}

  public record EdicionServicio(
      @NotBlank @Size(max = 100) String nombre,
      @NotNull Integer duracionMinutos,
      @NotNull Boolean activo,
      @NotNull @Size(min = 1, max = 50) List<@NotNull @Valid ModalidadSolicitud> modalidades,
      @NotNull @PositiveOrZero Long version) {}

  private static List<GestionarServicios.DatosModalidad> modalidades(
      List<ModalidadSolicitud> solicitudes) {
    return solicitudes.stream()
        .map(
            m ->
                new GestionarServicios.DatosModalidad(
                    m.id(), m.nombre(), m.conCargo(), m.tarifa(), m.activa()))
        .toList();
  }

  @ExceptionHandler(ErrorServicios.class)
  public ProblemDetail error(ErrorServicios error) {
    HttpStatus status =
        switch (error.codigo()) {
          case NO_EXISTE -> HttpStatus.NOT_FOUND;
          case MODALIDAD_INVALIDA -> HttpStatus.BAD_REQUEST;
          case CONFLICTO, NOMBRE_DUPLICADO -> HttpStatus.CONFLICT;
        };
    String detalle =
        switch (error.codigo()) {
          case NO_EXISTE -> "No se encontró el servicio";
          case MODALIDAD_INVALIDA ->
              "Las modalidades no corresponden al servicio; desactívalas en lugar de quitarlas";
          case CONFLICTO -> "El servicio cambió. Recárgalo antes de guardar";
          case NOMBRE_DUPLICADO -> "Ya existe un servicio con ese nombre";
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
        HttpStatus.BAD_REQUEST, "Revisa los datos del servicio");
  }

  @ExceptionHandler(DataAccessException.class)
  public ProblemDetail persistencia(DataAccessException error) {
    return ProblemDetail.forStatusAndDetail(
        HttpStatus.SERVICE_UNAVAILABLE, "No se pudo completar la operación");
  }
}
