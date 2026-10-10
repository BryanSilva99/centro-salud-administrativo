package pe.centrosalud.adapters.inbound.http.identity;
import java.util.Set;
import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.dao.DataAccessException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import pe.centrosalud.modules.identity.application.ports.DirectorioRoles;
import pe.centrosalud.modules.identity.application.roles.*;
import pe.centrosalud.modules.identity.domain.Rol;

@RestController
@RequestMapping("/api/cuentas")
public class RolesController {
    private final DirectorioRoles directorio;
    private final CambiarRoles cambiar;
    public RolesController(DirectorioRoles directorio, CambiarRoles cambiar) { this.directorio=directorio;this.cambiar=cambiar; }
    @GetMapping public DirectorioRoles.Pagina<CuentaRoles> listar(@RequestParam(defaultValue="") String busqueda, @RequestParam(defaultValue="0") int pagina) {
        validar(pagina); if(busqueda.length()>254) throw new IllegalArgumentException();
        return directorio.listar(busqueda,pagina);
    }
    @GetMapping("/{id}/roles") public CuentaRoles consultar(@PathVariable UUID id) { return directorio.cargar(id); }
    @PutMapping("/{id}/roles") public CuentaRoles guardar(@PathVariable UUID id, @Valid @RequestBody CambioRequest body,
            @AuthenticationPrincipal PrincipalSesion actor) {
        return cambiar.ejecutar(actor.id(),id,body.roles(),body.version());
    }
    @GetMapping("/{id}/roles/auditoria") public DirectorioRoles.Pagina<DirectorioRoles.Cambio> historial(@PathVariable UUID id,@RequestParam(defaultValue="0") int pagina) {
        validar(pagina);return directorio.historial(id,pagina);
    }
    private void validar(int pagina) { if(pagina<0 || pagina>100000) throw new IllegalArgumentException(); }
    public record CambioRequest(@NotNull Set<@NotNull Rol> roles, @NotNull @PositiveOrZero Long version) {}
    @ExceptionHandler(ErrorRoles.class) public ProblemDetail error(ErrorRoles error) {
        var status=switch(error.codigo()) {case NO_EXISTE -> HttpStatus.NOT_FOUND;case PROHIBIDO -> HttpStatus.FORBIDDEN;default -> HttpStatus.CONFLICT;};
        var detalle=switch(error.codigo()) {
            case NO_EXISTE -> "No se encontró la cuenta";
            case PROHIBIDO -> "Ya no tienes permiso para administrar roles";
            case CONFLICTO -> "Otra persona modificó los roles. Recarga la cuenta antes de guardar";
            case ULTIMO_ADMIN -> "Debe permanecer un administrador activo";
            case PROFESIONAL_INVALIDO -> "Selecciona una ficha profesional activa existente";
            case VINCULO_OCUPADO -> "La ficha ya está vinculada a otra cuenta";
            case REQUIERE_PRO -> "El vínculo profesional requiere el rol PRO; desvincula antes de retirarlo";
            case DUPLICADO -> "El identificador ya está en uso";
        };
        var problem=ProblemDetail.forStatusAndDetail(status,detalle);problem.setProperty("code",error.codigo().name());return problem;
    }
    @ExceptionHandler({IllegalArgumentException.class,MethodArgumentNotValidException.class,HttpMessageNotReadableException.class})
    public ProblemDetail invalido(Exception error) { return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,"Solicitud de roles inválida"); }
    @ExceptionHandler(DataAccessException.class) public ProblemDetail persistencia(DataAccessException error) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,"No se pudo guardar o consultar. Inténtalo nuevamente");
    }
}
