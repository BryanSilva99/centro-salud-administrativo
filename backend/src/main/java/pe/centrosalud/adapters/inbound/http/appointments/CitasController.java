package pe.centrosalud.adapters.inbound.http.appointments;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.centrosalud.adapters.inbound.http.identity.PrincipalSesion;
import pe.centrosalud.modules.appointments.application.ports.CatalogoCitas;
import pe.centrosalud.modules.appointments.application.usecases.AccesoCitasDenegado;
import pe.centrosalud.modules.appointments.application.usecases.CitaDetalle;
import pe.centrosalud.modules.appointments.application.usecases.CitaNoEncontrada;
import pe.centrosalud.modules.appointments.application.usecases.ConflictoCita;
import pe.centrosalud.modules.appointments.application.usecases.ConsultarAgendaCitas;
import pe.centrosalud.modules.appointments.application.usecases.ConsultarContextoCitas;
import pe.centrosalud.modules.appointments.application.usecases.ConsultarAtencionCitas;
import pe.centrosalud.modules.appointments.application.usecases.ConsultarDashboardCitas;
import pe.centrosalud.modules.appointments.application.usecases.ConsultarDisponibilidadCitas;
import pe.centrosalud.modules.appointments.application.usecases.HorarioCitaInvalido;
import pe.centrosalud.modules.appointments.application.usecases.OperacionCitaInvalida;
import pe.centrosalud.modules.appointments.application.usecases.ProgramarCita;
import pe.centrosalud.modules.appointments.application.usecases.ReferenciaCitaNoEncontrada;
import pe.centrosalud.modules.appointments.application.usecases.ReprogramarCita;
import pe.centrosalud.modules.appointments.application.usecases.RegistrarLlegadaCita;
import pe.centrosalud.modules.appointments.application.usecases.IniciarAtencionCita;
import pe.centrosalud.modules.appointments.application.usecases.FinalizarAtencionCita;
import pe.centrosalud.modules.appointments.application.usecases.CancelarCita;
import pe.centrosalud.modules.appointments.application.usecases.RegistrarAusenciaCita;
import pe.centrosalud.modules.appointments.application.usecases.RetirarEsperaCita;
import pe.centrosalud.modules.appointments.application.usecases.VersionCitaObsoleta;

@RestController
@RequestMapping("/api/citas")
public class CitasController {
    private final ConsultarContextoCitas contexto;
    private final ConsultarAgendaCitas agenda;
    private final ConsultarDisponibilidadCitas disponibilidad;
    private final ProgramarCita programar;
    private final ReprogramarCita reprogramar;
    private final RegistrarLlegadaCita registrarLlegada;
    private final ConsultarAtencionCitas atencion;
    private final IniciarAtencionCita iniciarAtencion;
    private final FinalizarAtencionCita finalizarAtencion;
    private final ConsultarDashboardCitas dashboard;
    private final CancelarCita cancelar;
    private final RegistrarAusenciaCita registrarAusencia;
    private final RetirarEsperaCita retirarEspera;

    public CitasController(ConsultarContextoCitas contexto, ConsultarAgendaCitas agenda,
                           ConsultarDisponibilidadCitas disponibilidad, ProgramarCita programar,
                           ReprogramarCita reprogramar, RegistrarLlegadaCita registrarLlegada,
                           ConsultarAtencionCitas atencion, IniciarAtencionCita iniciarAtencion,
                           FinalizarAtencionCita finalizarAtencion, ConsultarDashboardCitas dashboard,
                           CancelarCita cancelar, RegistrarAusenciaCita registrarAusencia,
                           RetirarEsperaCita retirarEspera) {
        this.contexto = contexto; this.agenda = agenda; this.disponibilidad = disponibilidad;
        this.programar = programar; this.reprogramar = reprogramar; this.registrarLlegada = registrarLlegada;
        this.atencion = atencion; this.iniciarAtencion = iniciarAtencion; this.finalizarAtencion = finalizarAtencion;
        this.dashboard = dashboard; this.cancelar = cancelar; this.registrarAusencia = registrarAusencia;
        this.retirarEspera = retirarEspera;
    }

    @GetMapping("/contexto")
    public ContextoResponse contexto(@AuthenticationPrincipal PrincipalSesion actor) {
        return ContextoResponse.from(contexto.ejecutar(actor.id()));
    }

    @GetMapping("/agenda")
    public List<CitaResponse> agenda(@AuthenticationPrincipal PrincipalSesion actor,
                                     @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                                     @RequestParam(required = false) UUID profesionalId,
                                     @RequestParam(required = false) UUID servicioId,
                                     @RequestParam(required = false) UUID consultorioId) {
        return agenda.ejecutar(actor.id(), fecha, profesionalId, servicioId, consultorioId).stream().map(CitaResponse::from).toList();
    }

    @GetMapping("/disponibilidad")
    public List<HorarioResponse> disponibilidad(@AuthenticationPrincipal PrincipalSesion actor,
                                                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                                                @RequestParam UUID pacienteId, @RequestParam UUID profesionalId,
                                                @RequestParam UUID servicioId, @RequestParam UUID consultorioId,
                                                @RequestParam(required = false) UUID excluirCitaId) {
        return disponibilidad.ejecutar(actor.id(), fecha, pacienteId, profesionalId, servicioId, consultorioId, excluirCitaId)
                .stream().map(HorarioResponse::from).toList();
    }

    @PostMapping
    public ResponseEntity<CitaResponse> programar(@AuthenticationPrincipal PrincipalSesion actor,
                                                   @Valid @RequestBody ProgramarRequest request) {
        var resultado = programar.ejecutar(new ProgramarCita.Comando(actor.id(), request.pacienteId(), request.profesionalId(),
                request.servicioId(), request.consultorioId(), request.modalidad(), request.fecha(), request.hora()));
        return ResponseEntity.status(HttpStatus.CREATED).body(CitaResponse.from(resultado));
    }

    @PostMapping("/{id}/reprogramaciones")
    public CitaResponse reprogramar(@AuthenticationPrincipal PrincipalSesion actor, @PathVariable UUID id,
                                    @Valid @RequestBody ReprogramarRequest request) {
        return CitaResponse.from(reprogramar.ejecutar(new ReprogramarCita.Comando(actor.id(), id, request.fecha(),
                request.hora(), request.motivo(), request.version())));
    }

    @GetMapping("/atencion")
    public AtencionResponse atencion(@AuthenticationPrincipal PrincipalSesion actor,
                                     @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                                     @RequestParam(required = false) UUID profesionalId) {
        var resultado = atencion.ejecutar(actor.id(), fecha, profesionalId);
        return new AtencionResponse(resultado.fecha(), offset(resultado.consultadaEn()),
                resultado.enEspera().stream().map(CitaResponse::from).toList(),
                resultado.enAtencion().stream().map(CitaResponse::from).toList());
    }

    @PutMapping("/{id}/llegada")
    public CitaResponse registrarLlegada(@AuthenticationPrincipal PrincipalSesion actor, @PathVariable UUID id,
                                         @Valid @RequestBody TransicionRequest request) {
        return CitaResponse.from(registrarLlegada.ejecutar(new RegistrarLlegadaCita.Comando(actor.id(), id, request.version())));
    }

    @PutMapping("/{id}/atencion/inicio")
    public CitaResponse iniciarAtencion(@AuthenticationPrincipal PrincipalSesion actor, @PathVariable UUID id,
                                        @Valid @RequestBody TransicionRequest request) {
        return CitaResponse.from(iniciarAtencion.ejecutar(new IniciarAtencionCita.Comando(actor.id(), id, request.version())));
    }

    @PutMapping("/{id}/atencion/finalizacion")
    public CitaResponse finalizarAtencion(@AuthenticationPrincipal PrincipalSesion actor, @PathVariable UUID id,
                                          @Valid @RequestBody TransicionRequest request) {
        return CitaResponse.from(finalizarAtencion.ejecutar(new FinalizarAtencionCita.Comando(actor.id(), id, request.version())));
    }

    @PutMapping("/{id}/cancelacion")
    public CierreResponse cancelar(@AuthenticationPrincipal PrincipalSesion actor, @PathVariable UUID id,
                                   @Valid @RequestBody MotivoTransicionRequest request) {
        var resultado = cancelar.ejecutar(new CancelarCita.Comando(actor.id(), id, request.motivo(), request.version()));
        return new CierreResponse(CitaResponse.from(resultado.detalle()), resultado.gestionEconomicaPendiente());
    }

    @PutMapping("/{id}/ausencia")
    public CitaResponse registrarAusencia(@AuthenticationPrincipal PrincipalSesion actor, @PathVariable UUID id,
                                          @Valid @RequestBody TransicionRequest request) {
        return CitaResponse.from(registrarAusencia.ejecutar(new RegistrarAusenciaCita.Comando(actor.id(), id, request.version())));
    }

    @PutMapping("/{id}/espera/retiro")
    public CierreResponse retirarEspera(@AuthenticationPrincipal PrincipalSesion actor, @PathVariable UUID id,
                                        @Valid @RequestBody MotivoTransicionRequest request) {
        var resultado = retirarEspera.ejecutar(new RetirarEsperaCita.Comando(actor.id(), id, request.motivo(), request.version()));
        return new CierreResponse(CitaResponse.from(resultado.detalle()), resultado.gestionEconomicaPendiente());
    }

    @GetMapping("/dashboard")
    public DashboardResponse dashboard(@AuthenticationPrincipal PrincipalSesion actor,
                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                                       @RequestParam(required = false) UUID profesionalId,
                                       @RequestParam(required = false) UUID servicioId,
                                       @RequestParam(required = false) UUID consultorioId) {
        var resultado = dashboard.ejecutar(actor.id(), fecha, profesionalId, servicioId, consultorioId);
        var estados = resultado.porEstado().entrySet().stream()
                .collect(Collectors.toMap(entry -> entry.getKey().name(), Map.Entry::getValue));
        return new DashboardResponse(resultado.fecha(), resultado.profesionalId(), resultado.servicioId(),
                resultado.consultorioId(), offset(resultado.actualizadoEn()), estados, resultado.total(),
                resultado.enEspera(), resultado.atendidas(), resultado.ausentes());
    }

    @ExceptionHandler(AccesoCitasDenegado.class)
    ProblemDetail acceso(AccesoCitasDenegado error) { return problem(HttpStatus.FORBIDDEN, error.getMessage()); }
    @ExceptionHandler({CitaNoEncontrada.class, ReferenciaCitaNoEncontrada.class})
    ProblemDetail noEncontrado(RuntimeException error) { return problem(HttpStatus.NOT_FOUND, error.getMessage()); }
    @ExceptionHandler({ConflictoCita.class, VersionCitaObsoleta.class})
    ProblemDetail conflicto(RuntimeException error) { return problem(HttpStatus.CONFLICT, error.getMessage()); }
    @ExceptionHandler({HorarioCitaInvalido.class, OperacionCitaInvalida.class})
    ProblemDetail regla(RuntimeException error) { return problem(HttpStatus.UNPROCESSABLE_ENTITY, error.getMessage()); }
    @ExceptionHandler(IllegalStateException.class)
    ProblemDetail estado(IllegalStateException error) { return problem(HttpStatus.UNPROCESSABLE_ENTITY, error.getMessage()); }
    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class})
    ProblemDetail solicitud(Exception error) { return problem(HttpStatus.BAD_REQUEST, "La solicitud de cita no es válida"); }

    private ProblemDetail problem(HttpStatus status, String detail) { return ProblemDetail.forStatusAndDetail(status, detail); }
    private static String offset(java.time.Instant value) {
        return value.atZone(ConsultarDisponibilidadCitas.ZONA_CENTRO).toOffsetDateTime().toString();
    }

    public record ProgramarRequest(@NotNull UUID pacienteId, @NotNull UUID profesionalId, @NotNull UUID servicioId,
                                   @NotNull UUID consultorioId, @NotBlank String modalidad, @NotNull LocalDate fecha,
                                   @NotNull LocalTime hora) {}
    public record ReprogramarRequest(@NotNull LocalDate fecha, @NotNull LocalTime hora, @NotBlank String motivo,
                                     @NotNull @Min(0) Long version) {}
    public record TransicionRequest(@NotNull @Min(0) Long version) {}
    public record MotivoTransicionRequest(@NotBlank @Size(max = 500) String motivo, @NotNull @Min(0) Long version) {}
    public record CierreResponse(CitaResponse cita, boolean gestionEconomicaPendiente) {}
    public record HorarioResponse(String inicio, String fin) {
        static HorarioResponse from(pe.centrosalud.modules.appointments.domain.HorarioCita horario) {
            return new HorarioResponse(horario.inicio().atZone(ConsultarDisponibilidadCitas.ZONA_CENTRO).toOffsetDateTime().toString(),
                    horario.fin().atZone(ConsultarDisponibilidadCitas.ZONA_CENTRO).toOffsetDateTime().toString());
        }
    }
    public record CitaResponse(UUID id, UUID pacienteId, String paciente, UUID profesionalId, String profesional,
                               String especialidad, UUID servicioId, String servicio, UUID consultorioId,
                               String consultorio, String inicio, String fin, String estado, int duracionMinutos,
                               String modalidad, String modalidadNombre, BigDecimal importe, String llegadaEn,
                               String atencionIniciadaEn, String atencionFinalizadaEn, String tipoCierre,
                               String motivoCierre, String cerradaEn, long version) {
        static CitaResponse from(CitaDetalle detalle) {
            var cita = detalle.cita();
            var modalidad = detalle.servicio().modalidad(cita.modalidad()).orElse(null);
            return new CitaResponse(cita.id(), cita.pacienteId(), detalle.paciente().nombre(), cita.profesionalId(),
                    detalle.profesional().nombre(), detalle.profesional().especialidad(), cita.servicioId(),
                    detalle.servicio().nombre(), cita.consultorioId(), detalle.consultorio().nombre(),
                    cita.horario().inicio().atZone(ConsultarDisponibilidadCitas.ZONA_CENTRO).toOffsetDateTime().toString(),
                    cita.horario().fin().atZone(ConsultarDisponibilidadCitas.ZONA_CENTRO).toOffsetDateTime().toString(),
                    cita.estado().name(), cita.duracionMinutos(), cita.modalidad(), modalidad == null ? cita.modalidad() : modalidad.nombre(),
                    cita.importe(), instante(cita.llegadaEn()), instante(cita.atencionIniciadaEn()),
                    instante(cita.atencionFinalizadaEn()), cita.tipoCierre() == null ? null : cita.tipoCierre().name(),
                    cita.motivoCierre(), instante(cita.cerradaEn()), cita.version());
        }
        private static String instante(java.time.Instant value) {
            return value == null ? null : value.atZone(ConsultarDisponibilidadCitas.ZONA_CENTRO).toOffsetDateTime().toString();
        }
    }
    public record ContextoResponse(List<PacienteResponse> pacientes, List<ProfesionalResponse> profesionales,
                                   List<ServicioResponse> servicios, List<ConsultorioResponse> consultorios,
                                   List<String> capacidades, UUID profesionalAsignadoId) {
        static ContextoResponse from(pe.centrosalud.modules.appointments.application.usecases.ContextoCitas resultado) {
            var contexto = resultado.catalogo();
            return new ContextoResponse(contexto.pacientes().stream().map(x -> new PacienteResponse(x.id(), x.nombre(), x.activo())).toList(),
                    contexto.profesionales().stream().map(x -> new ProfesionalResponse(x.id(), x.nombre(), x.especialidad(), x.activo())).toList(),
                    contexto.servicios().stream().map(x -> new ServicioResponse(x.id(), x.nombre(), x.activo(), x.duracionMinutos(),
                            x.modalidades().stream().map(m -> new ModalidadResponse(m.codigo(), m.nombre(), m.importe())).toList())).toList(),
                    contexto.consultorios().stream().map(x -> new ConsultorioResponse(x.id(), x.nombre(), x.activo())).toList(),
                    resultado.perfil().operaciones().stream().map(Enum::name).sorted().toList(),
                    resultado.perfil().profesionalAsignadoId());
        }
    }
    public record PacienteResponse(UUID id, String nombre, boolean activo) {}
    public record ProfesionalResponse(UUID id, String nombre, String especialidad, boolean activo) {}
    public record ConsultorioResponse(UUID id, String nombre, boolean activo) {}
    public record ModalidadResponse(String codigo, String nombre, BigDecimal importe) {}
    public record ServicioResponse(UUID id, String nombre, boolean activo, int duracionMinutos, List<ModalidadResponse> modalidades) {}
    public record AtencionResponse(LocalDate fecha, String consultadaEn, List<CitaResponse> enEspera,
                                   List<CitaResponse> enAtencion) {}
    public record DashboardResponse(LocalDate fecha, UUID profesionalId, UUID servicioId, UUID consultorioId,
                                    String actualizadoEn, Map<String, Long> porEstado, long total,
                                    long enEspera, long atendidas, long ausentes) {}
}
