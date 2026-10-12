package pe.centrosalud.modules.billing.application.usecases;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import pe.centrosalud.modules.billing.application.ports.PagoRepository;
import pe.centrosalud.modules.billing.domain.Pago;

/**
 * Caso de uso: consultar reporte económico. RF-27, RN-16. Ingresos netos por periodo y medio de
 * pago.
 */
public final class ConsultarReporteEconomico {
  private final PagoRepository pagos;

  public ConsultarReporteEconomico(PagoRepository pagos) {
    this.pagos = Objects.requireNonNull(pagos);
  }

  public ReporteEconomico consultarPorPeriodo(LocalDate fechaInicio, LocalDate fechaFin) {
    Objects.requireNonNull(fechaInicio, "La fecha de inicio es obligatoria");
    Objects.requireNonNull(fechaFin, "La fecha de fin es obligatoria");

    var zona = ZoneId.of("America/Lima");
    var inicio = fechaInicio.atStartOfDay(zona).toInstant();
    var fin = fechaFin.plusDays(1).atStartOfDay(zona).toInstant();

    var movimientos = pagos.buscarPorPeriodo(inicio, fin);

    var cobros = movimientos.stream().filter(Pago::esCobro).toList();
    var devoluciones = movimientos.stream().filter(Pago::esDevolucion).toList();

    var totalCobros = cobros.stream().map(Pago::importe).reduce(BigDecimal.ZERO, BigDecimal::add);
    var totalDevoluciones =
        devoluciones.stream().map(Pago::importe).reduce(BigDecimal.ZERO, BigDecimal::add);
    var ingresosNetos = totalCobros.subtract(totalDevoluciones);

    return new ReporteEconomico(
        fechaInicio, fechaFin, cobros, devoluciones, totalCobros, totalDevoluciones, ingresosNetos);
  }

  /** Resumen del reporte económico. */
  public record ReporteEconomico(
      LocalDate fechaInicio,
      LocalDate fechaFin,
      List<Pago> cobros,
      List<Pago> devoluciones,
      BigDecimal totalCobros,
      BigDecimal totalDevoluciones,
      BigDecimal ingresosNetos) {}
}
