package pe.centrosalud.adapters.inbound.http.billing;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pe.centrosalud.adapters.inbound.http.identity.PrincipalSesion;
import pe.centrosalud.modules.billing.application.usecases.*;
import pe.centrosalud.modules.billing.domain.*;

@RestController
@RequestMapping("/api/billing")
public class BillingController {
  private final CajaIntegrada caja;

  public BillingController(CajaIntegrada caja) {
    this.caja = caja;
  }

  @GetMapping("/contexto")
  public CajaIntegrada.Contexto contexto(
      @AuthenticationPrincipal PrincipalSesion actor, @RequestParam("fecha") LocalDate fecha) {
    return caja.contexto(actor.id(), fecha);
  }

  @PostMapping("/cargos")
  public Cargo cargo(@AuthenticationPrincipal PrincipalSesion actor, @RequestBody CargoRequest r) {
    return caja.cargo(actor.id(), r.citaId());
  }

  @PostMapping("/sesiones")
  public SesionCaja abrir(
      @AuthenticationPrincipal PrincipalSesion actor, @RequestBody AbrirRequest r) {
    return caja.abrir(actor.id(), r.fondoInicial(), r.clave());
  }

  @PostMapping("/pagos")
  public Pago pagar(@AuthenticationPrincipal PrincipalSesion actor, @RequestBody PagoRequest r) {
    return caja.pagar(
        actor.id(),
        r.cargoId(),
        r.sesionCajaId(),
        r.importe(),
        r.medio(),
        r.referencia(),
        r.clave());
  }

  @GetMapping("/cargos/{id}/saldo")
  public ConsultarSaldo.SaldoCargo saldo(
      @AuthenticationPrincipal PrincipalSesion actor, @PathVariable("id") UUID id) {
    return caja.saldo(actor.id(), id);
  }

  @PostMapping("/reversiones")
  public SolicitudReversion solicitar(
      @AuthenticationPrincipal PrincipalSesion actor, @RequestBody SolicitarRequest r) {
    return caja.solicitar(actor.id(), r.operacionId(), r.tipo(), r.motivo());
  }

  @PostMapping("/reversiones/{id}/resolver")
  public SolicitudReversion resolver(
      @AuthenticationPrincipal PrincipalSesion actor,
      @PathVariable("id") UUID id,
      @RequestBody ResolverRequest r) {
    return caja.resolver(actor.id(), id, r.aprobar(), r.referencia());
  }

  @PostMapping("/sesiones/{id}/cerrar")
  public CerrarCaja.CierreCaja cerrar(
      @AuthenticationPrincipal PrincipalSesion actor,
      @PathVariable("id") UUID id,
      @RequestBody CerrarRequest r) {
    return caja.cerrar(actor.id(), id, r.efectivoDeclarado(), r.motivoDiferencia());
  }

  @GetMapping("/sesiones/{id}")
  public CerrarCaja.CierreCaja cierre(
      @AuthenticationPrincipal PrincipalSesion actor, @PathVariable("id") UUID id) {
    return caja.cierre(actor.id(), id);
  }

  @GetMapping("/reportes/economico")
  public ConsultarReporteEconomico.ReporteEconomico reporte(
      @AuthenticationPrincipal PrincipalSesion actor,
      @RequestParam("fechaInicio") LocalDate desde,
      @RequestParam("fechaFin") LocalDate hasta) {
    return caja.reporte(actor.id(), desde, hasta);
  }

  @ExceptionHandler(AccesoCajaDenegado.class)
  public ProblemDetail denegado(RuntimeException e) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, e.getMessage());
  }

  @ExceptionHandler({
    CargoNoEncontrado.class,
    SesionCajaNoEncontrada.class,
    SolicitudNoEncontrada.class
  })
  public ProblemDetail noEncontrado(RuntimeException e) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
  }

  @ExceptionHandler({IllegalArgumentException.class, NullPointerException.class})
  public ProblemDetail invalido(RuntimeException e) {
    return ProblemDetail.forStatusAndDetail(
        HttpStatus.BAD_REQUEST,
        e instanceof NullPointerException ? "Faltan datos obligatorios" : e.getMessage());
  }

  @ExceptionHandler(IllegalStateException.class)
  public ProblemDetail conflicto(RuntimeException e) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
  }

  @ExceptionHandler(org.springframework.dao.DataAccessException.class)
  public ProblemDetail persistencia(org.springframework.dao.DataAccessException e) {
    return ProblemDetail.forStatusAndDetail(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "No se pudo guardar la operación. Consulta el estado antes de reintentar con la misma clave.");
  }

  public record CargoRequest(UUID citaId) {}

  public record AbrirRequest(BigDecimal fondoInicial, UUID clave) {}

  public record PagoRequest(
      UUID cargoId,
      UUID sesionCajaId,
      BigDecimal importe,
      MedioPago medio,
      String referencia,
      UUID clave) {}

  public record SolicitarRequest(
      UUID operacionId, SolicitudReversion.TipoOperacion tipo, String motivo) {}

  public record ResolverRequest(boolean aprobar, String referencia) {}

  public record CerrarRequest(BigDecimal efectivoDeclarado, String motivoDiferencia) {}
}
