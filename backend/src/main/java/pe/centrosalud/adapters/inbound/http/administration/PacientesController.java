package pe.centrosalud.adapters.inbound.http.administration;

import java.time.LocalDate;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import pe.centrosalud.adapters.inbound.http.identity.PrincipalSesion;
import pe.centrosalud.modules.administration.application.usecases.PacienteDuplicado;
import pe.centrosalud.modules.administration.application.usecases.RegistrarPaciente;
import pe.centrosalud.modules.administration.domain.Paciente;
import pe.centrosalud.modules.administration.domain.TipoDocumento;

/** Las respuestas de error no repiten datos del paciente. */
@RestController
@RequestMapping("/api/pacientes")
public class PacientesController {
    private final RegistrarPaciente registrar;
    public PacientesController(RegistrarPaciente registrar) { this.registrar = registrar; }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public Paciente registrar(@Valid @RequestBody NuevoPaciente datos, @AuthenticationPrincipal PrincipalSesion autor) {
        return registrar.registrar(autor.id(), datos.tipoDocumento(), datos.numeroDocumento(), datos.nombres(),
                datos.apellidos(), datos.fechaNacimiento(), datos.telefono(), datos.correo());
    }
    public record NuevoPaciente(@NotNull TipoDocumento tipoDocumento, @NotBlank @Size(max=20) String numeroDocumento,
                                @NotBlank @Size(max=100) String nombres, @NotBlank @Size(max=100) String apellidos,
                                @NotNull LocalDate fechaNacimiento, @Size(max=20) String telefono, @Size(max=254) String correo) {}

    @ExceptionHandler(PacienteDuplicado.class) public ProblemDetail duplicado(PacienteDuplicado error) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, error.getMessage());
        problem.setProperty("code", "DOCUMENTO_DUPLICADO");
        return problem;
    }
    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    public ProblemDetail invalido(Exception error) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Revisa los datos del paciente");
    }
    @ExceptionHandler(DataAccessException.class) public ProblemDetail persistencia(DataAccessException error) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, "No se pudo registrar el paciente");
    }
}
