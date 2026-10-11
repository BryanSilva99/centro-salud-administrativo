package pe.centrosalud.adapters.inbound.http.billing;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.centrosalud.modules.billing.application.usecases.AbrirSesionCaja;
import pe.centrosalud.modules.billing.application.usecases.AprobarReversion;
import pe.centrosalud.modules.billing.application.usecases.CargoNoEncontrado;
import pe.centrosalud.modules.billing.application.usecases.CerrarCaja;
import pe.centrosalud.modules.billing.application.usecases.ConsultarReporteEconomico;
import pe.centrosalud.modules.billing.application.usecases.ConsultarSaldo;
import pe.centrosalud.modules.billing.application.usecases.RegistrarCargo;
import pe.centrosalud.modules.billing.application.usecases.RegistrarPago;
import pe.centrosalud.modules.billing.application.usecases.SesionCajaNoEncontrada;
import pe.centrosalud.modules.billing.application.usecases.SolicitarReversion;
import pe.centrosalud.modules.billing.domain.MedioPago;
import pe.centrosalud.modules.billing.domain.SolicitudReversion;

/**
 * Controlador HTTP del módulo de caja.
 * RF-19 a RF-24, RF-27. Requiere rol CAJ o ADM.
 */
@RestController
@RequestMapping("/api/billing")
public class BillingController {
    private final RegistrarCargo registrarCargo;
    private final AbrirSesionCaja abrirSesionCaja;
    private final RegistrarPago registrarPago;
    private final ConsultarSaldo consultarSaldo;
    private final SolicitarReversion solicitarReversion;
    private final AprobarReversion aprobarReversion;
    private final CerrarCaja cerrarCaja;
    private final ConsultarReporteEconomico consultarReporteEconomico;

    public BillingController(
            RegistrarCargo registrarCargo,
            AbrirSesionCaja abrirSesionCaja,
            RegistrarPago registrarPago,
            ConsultarSaldo consultarSaldo,
            SolicitarReversion solicitarReversion,
            AprobarReversion aprobarReversion,
            CerrarCaja cerrarCaja,
            ConsultarReporteEconomico consultarReporteEconomico) {
        this.registrarCargo = registrarCargo;
        this.abrirSesionCaja = abrirSesionCaja;
        this.registrarPago = registrarPago;
        this.consultarSaldo = consultarSaldo;
        this.solicitarReversion = solicitarReversion;
        this.aprobarReversion = aprobarReversion;
        this.cerrarCaja = cerrarCaja;
        this.consultarReporteEconomico = consultarReporteEconomico;
    }

    // RF-19: Registrar cargo
    @PostMapping("/cargos")
    public CargoResponse registrarCargo(@RequestBody RegistrarCargoRequest request) {
        var cargo = registrarCargo.ejecutar(request.citaId(), request.pacienteId(), request.importe());
        return new CargoResponse(cargo.id(), cargo.citaId(), cargo.pacienteId(), cargo.importe(), cargo.estado().name());
    }

    // RF-20: Abrir sesión de caja
    @PostMapping("/sesiones")
    public SesionCajaResponse abrirSesion(@RequestBody AbrirSesionRequest request) {
        var sesion = abrirSesionCaja.ejecutar(request.cajeroId(), request.fondoInicial());
        return new SesionCajaResponse(sesion.id(), sesion.cajeroId(), sesion.fondoInicial(), sesion.estado().name());
    }

    // RF-21: Registrar pago
    @PostMapping("/pagos")
    public PagoResponse registrarPago(@RequestBody RegistrarPagoRequest request) {
        var pago = registrarPago.ejecutar(request.cargoId(), request.sesionCajaId(), request.importe(), request.medio(), request.referencia());
        return new PagoResponse(pago.id(), pago.cargoId(), pago.sesionCajaId(), pago.importe(), pago.medio().name(), pago.tipo().name());
    }

    // RF-22: Consultar saldo
    @GetMapping("/cargos/{cargoId}/saldo")
    public SaldoResponse consultarSaldo(@PathVariable("cargoId") UUID cargoId) {
        var saldo = consultarSaldo.consultarPorCargo(cargoId);
        return new SaldoResponse(
                saldo.cargo().id(),
                saldo.cargo().importe(),
                saldo.totalPagado(),
                saldo.totalReversiones(),
                saldo.saldoPendiente()
        );
    }

    // RF-23: Solicitar reversión
    @PostMapping("/reversiones")
    public SolicitudReversionResponse solicitarReversion(@RequestBody SolicitarReversionRequest request) {
        var solicitud = solicitarReversion.ejecutar(request.operacionId(), request.tipo(), request.solicitanteId(), request.motivo());
        return new SolicitudReversionResponse(solicitud.id(), solicitud.operacionId(), solicitud.estado().name());
    }

    // RF-23: Aprobar o rechazar reversión
    @PostMapping("/reversiones/{solicitudId}/resolver")
    public SolicitudReversionResponse resolverReversion(
            @PathVariable("solicitudId") UUID solicitudId,
            @RequestBody ResolverReversionRequest request) {
        var solicitud = aprobarReversion.ejecutar(solicitudId, request.aprobadorId(), request.aprobar());
        return new SolicitudReversionResponse(solicitud.id(), solicitud.operacionId(), solicitud.estado().name());
    }

    // RF-24: Cerrar caja
    @PostMapping("/sesiones/{sesionId}/cerrar")
    public CierreCajaResponse cerrarCaja(
            @PathVariable("sesionId") UUID sesionId,
            @RequestBody CerrarCajaRequest request) {
        var cierre = cerrarCaja.ejecutar(sesionId, request.efectivoDeclarado(), request.motivoDiferencia());
        return new CierreCajaResponse(
                cierre.sesion().id(),
                cierre.efectivoEsperado(),
                cierre.sesion().efectivoDeclarado(),
                cierre.sesion().diferencia()
        );
    }

    // RF-27: Reporte económico
    @GetMapping("/reportes/economico")
    public ReporteEconomicoResponse reporteEconomico(
            @RequestParam("fechaInicio") LocalDate fechaInicio,
            @RequestParam("fechaFin") LocalDate fechaFin) {
        var reporte = consultarReporteEconomico.consultarPorPeriodo(fechaInicio, fechaFin);
        return new ReporteEconomicoResponse(
                reporte.fechaInicio(),
                reporte.fechaFin(),
                reporte.totalCobros(),
                reporte.totalDevoluciones(),
                reporte.ingresosNetos()
        );
    }

    // --- Manejadores de errores ---
    @ExceptionHandler(CargoNoEncontrado.class)
    public ProblemDetail cargoNoEncontrado(CargoNoEncontrado error) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, error.getMessage());
    }

    @ExceptionHandler(SesionCajaNoEncontrada.class)
    public ProblemDetail sesionNoEncontrada(SesionCajaNoEncontrada error) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, error.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail argumentoInvalido(IllegalArgumentException error) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, error.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail estadoInvalido(IllegalStateException error) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, error.getMessage());
    }

    // --- DTOs de request/response ---
    public record RegistrarCargoRequest(UUID citaId, UUID pacienteId, BigDecimal importe) {}
    public record AbrirSesionRequest(UUID cajeroId, BigDecimal fondoInicial) {}
    public record RegistrarPagoRequest(UUID cargoId, UUID sesionCajaId, BigDecimal importe, MedioPago medio, String referencia) {}
    public record SolicitarReversionRequest(UUID operacionId, SolicitudReversion.TipoOperacion tipo, UUID solicitanteId, String motivo) {}
    public record ResolverReversionRequest(UUID aprobadorId, boolean aprobar) {}
    public record CerrarCajaRequest(BigDecimal efectivoDeclarado, String motivoDiferencia) {}

    public record CargoResponse(UUID id, UUID citaId, UUID pacienteId, BigDecimal importe, String estado) {}
    public record SesionCajaResponse(UUID id, UUID cajeroId, BigDecimal fondoInicial, String estado) {}
    public record PagoResponse(UUID id, UUID cargoId, UUID sesionCajaId, BigDecimal importe, String medio, String tipo) {}
    public record SaldoResponse(UUID cargoId, BigDecimal importe, BigDecimal totalPagado, BigDecimal totalReversiones, BigDecimal saldoPendiente) {}
    public record SolicitudReversionResponse(UUID id, UUID operacionId, String estado) {}
    public record CierreCajaResponse(UUID sesionId, BigDecimal efectivoEsperado, BigDecimal efectivoDeclarado, BigDecimal diferencia) {}
    public record ReporteEconomicoResponse(LocalDate fechaInicio, LocalDate fechaFin, BigDecimal totalCobros, BigDecimal totalDevoluciones, BigDecimal ingresosNetos) {}
}
