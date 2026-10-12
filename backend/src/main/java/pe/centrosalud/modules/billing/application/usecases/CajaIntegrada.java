package pe.centrosalud.modules.billing.application.usecases;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import pe.centrosalud.modules.billing.application.ports.*;
import pe.centrosalud.modules.billing.domain.*;
import pe.centrosalud.modules.billing.domain.SolicitudReversion.TipoOperacion;

/** Frontera de caja: permisos actuales, referencias reales y operación transaccional completa. */
public final class CajaIntegrada {
  private final CargoRepository cargos;
  private final PagoRepository pagos;
  private final SesionCajaRepository sesiones;
  private final SolicitudReversionRepository solicitudes;
  private final RegistroCaja registro;
  private final AccesoCaja acceso;
  private final ReservasCaja reservas;

  public CajaIntegrada(
      CargoRepository cargos,
      PagoRepository pagos,
      SesionCajaRepository sesiones,
      SolicitudReversionRepository solicitudes,
      RegistroCaja registro,
      AccesoCaja acceso,
      ReservasCaja reservas) {
    this.cargos = cargos;
    this.pagos = pagos;
    this.sesiones = sesiones;
    this.solicitudes = solicitudes;
    this.registro = registro;
    this.acceso = acceso;
    this.reservas = reservas;
  }

  private void cajero(UUID actor) {
    if (!acceso.perfil(actor).cajero()) throw new AccesoCajaDenegado();
  }

  private void administrador(UUID actor) {
    if (!acceso.perfil(actor).administrador()) throw new AccesoCajaDenegado();
  }

  private SesionCaja propia(UUID actor, UUID id) {
    var s = sesiones.buscarPorId(id).orElseThrow(SesionCajaNoEncontrada::new);
    if (!s.cajeroId().equals(actor)) throw new AccesoCajaDenegado();
    return s;
  }

  private void texto(String valor, int max, String campo) {
    if (valor == null || valor.isBlank() || valor.length() > max)
      throw new IllegalArgumentException(campo + " es obligatorio (máximo " + max + " caracteres)");
  }

  public Contexto contexto(UUID actor, LocalDate fecha) {
    return registro.ejecutar(
        () -> {
          var perfil = acceso.perfil(actor);
          var lista =
              reservas.listar(fecha).stream()
                  .map(
                      r ->
                          new CuentaCita(
                              r,
                              cargos.buscarPorCita(r.id()).stream()
                                  .map(
                                      c ->
                                          new ConsultarSaldo(cargos, pagos)
                                              .consultarPorCargo(c.id()))
                                  .toList()))
                  .toList();
          return new Contexto(
              perfil,
              lista,
              sesiones.listar().stream()
                  .filter(s -> perfil.administrador() || s.cajeroId().equals(actor))
                  .toList(),
              solicitudes.listar().stream()
                  .filter(s -> perfil.administrador() || s.solicitanteId().equals(actor))
                  .toList());
        });
  }

  public Cargo cargo(UUID actor, UUID citaId) {
    return registro.ejecutar(
        () -> {
          cajero(actor);
          var r = reservas.consultar(citaId);
          var existente =
              cargos.buscarPorCita(citaId).stream().filter(Cargo::estaVigente).findFirst();
          if (existente.isPresent()) return existente.get();
          if (r.estado().equals("CANCELADA"))
            throw new IllegalStateException("No se puede generar cargo para una cita cancelada");
          if (r.importe().signum() == 0)
            throw new IllegalStateException(
                "La modalidad es sin cargo: no genera deuda ni requiere pago");
          return new RegistrarCargo(cargos).ejecutar(citaId, r.pacienteId(), r.importe());
        });
  }

  public SesionCaja abrir(UUID actor, BigDecimal fondo, UUID clave) {
    return registro.ejecutar(
        () -> {
          cajero(actor);
          Dinero.validar(fondo);
          var contenido = "abrir|" + fondo.toPlainString();
          var previo = reintento(clave, actor, contenido);
          if (previo.isPresent())
            return sesiones.buscarPorId(previo.get()).orElseThrow(SesionCajaNoEncontrada::new);
          var s = new AbrirSesionCaja(sesiones).ejecutar(actor, fondo);
          registro.registrar(clave, actor, contenido, s.id());
          return s;
        });
  }

  private Optional<UUID> reintento(UUID clave, UUID actor, String contenido) {
    if (clave == null) throw new IllegalArgumentException("La clave de operación es obligatoria");
    var r = registro.reintento(clave);
    if (r.isPresent() && (!r.get().actor().equals(actor) || !r.get().contenido().equals(contenido)))
      throw new IllegalStateException("La clave de operación ya fue usada con otros datos");
    return r.map(RegistroCaja.Reintento::resultado);
  }

  public Pago pagar(
      UUID actor,
      UUID cargoId,
      UUID sesionId,
      BigDecimal importe,
      MedioPago medio,
      String referencia,
      UUID clave) {
    return registro.ejecutar(
        () -> {
          cajero(actor);
          propia(actor, sesionId);
          Dinero.validar(importe);
          if (referencia != null && referencia.length() > 100)
            throw new IllegalArgumentException("La referencia admite máximo 100 caracteres");
          var contenido =
              "pago|"
                  + cargoId
                  + "|"
                  + sesionId
                  + "|"
                  + importe.toPlainString()
                  + "|"
                  + medio
                  + "|"
                  + referencia;
          var previo = reintento(clave, actor, contenido);
          if (previo.isPresent()) return pagos.buscarPorId(previo.get()).orElseThrow();
          var p =
              new RegistrarPago(pagos, cargos, sesiones)
                  .ejecutar(cargoId, sesionId, importe, medio, referencia);
          registro.registrar(clave, actor, contenido, p.id());
          return p;
        });
  }

  public ConsultarSaldo.SaldoCargo saldo(UUID actor, UUID cargo) {
    return registro.ejecutar(
        () -> {
          acceso.perfil(actor);
          return new ConsultarSaldo(cargos, pagos).consultarPorCargo(cargo);
        });
  }

  public SolicitudReversion solicitar(
      UUID actor, UUID original, TipoOperacion tipo, String motivo) {
    return registro.ejecutar(
        () -> {
          cajero(actor);
          texto(motivo, 500, "El motivo");
          validarOriginal(original, tipo);
          var existente =
              solicitudes.listar().stream()
                  .filter(
                      s ->
                          s.estaPendiente()
                              && s.operacionId().equals(original)
                              && s.operacionTipo() == tipo)
                  .findFirst();
          if (existente.isPresent()) {
            if (!existente.get().solicitanteId().equals(actor))
              throw new IllegalStateException(
                  "Ya existe una solicitud pendiente para esta operación");
            return existente.get();
          }
          return new SolicitarReversion(solicitudes).ejecutar(original, tipo, actor, motivo);
        });
  }

  private Cargo validarOriginal(UUID original, TipoOperacion tipo) {
    if (tipo == null || original == null)
      throw new IllegalArgumentException("La operación y el tipo son obligatorios");
    if (registro.compensada(original, tipo))
      throw new IllegalStateException("La operación ya fue revertida");
    if (tipo == TipoOperacion.CARGO) {
      var c = cargos.buscarPorId(original).orElseThrow(CargoNoEncontrado::new);
      if (c.estaAnulado()) throw new IllegalStateException("El cargo ya está anulado");
      var saldo = new ConsultarSaldo(cargos, pagos).consultarPorCargo(original);
      if (saldo.totalPagado().compareTo(saldo.totalReversiones()) != 0)
        throw new IllegalStateException("Primero deben resolverse los pagos vigentes del cargo");
      return c;
    }
    var pago =
        pagos
            .buscarPorId(original)
            .orElseThrow(() -> new IllegalArgumentException("El pago no existe"));
    if (!pago.esCobro())
      throw new IllegalArgumentException("Solo puede revertirse el cobro original");
    return cargos.buscarPorId(pago.cargoId()).orElseThrow(CargoNoEncontrado::new);
  }

  public SolicitudReversion resolver(UUID actor, UUID id, boolean aprobar, String referencia) {
    return registro.ejecutar(
        () -> {
          administrador(actor);
          var s = solicitudes.buscarPorId(id).orElseThrow(SolicitudNoEncontrada::new);
          if (s.solicitanteId().equals(actor)) throw new AccesoCajaDenegado();
          if (!s.estaPendiente()) {
            if (s.estaAprobada() == aprobar) return s;
            throw new IllegalStateException("La solicitud ya fue resuelta");
          }
          if (aprobar) {
            var c = validarOriginal(s.operacionId(), s.operacionTipo());
            UUID movimiento = null;
            if (s.operacionTipo() == TipoOperacion.PAGO) {
              var original = pagos.buscarPorId(s.operacionId()).orElseThrow();
              var caja =
                  sesiones
                      .buscarSesionAbiertaPorCajero(s.solicitanteId())
                      .orElseThrow(
                          () ->
                              new IllegalStateException(
                                  "El solicitante necesita una caja abierta para la devolución"));
              if (original.medio() != MedioPago.EFECTIVO)
                texto(referencia, 100, "La referencia de devolución");
              if (referencia != null && referencia.length() > 100)
                throw new IllegalArgumentException("La referencia admite máximo 100 caracteres");
              var dev =
                  new Pago(
                      UUID.randomUUID(),
                      c.id(),
                      caja.id(),
                      original.importe(),
                      original.medio(),
                      referencia,
                      TipoMovimiento.DEVOLUCION,
                      Instant.now());
              pagos.guardar(dev);
              movimiento = dev.id();
              var saldo = new ConsultarSaldo(cargos, pagos).consultarPorCargo(c.id());
              cargos.guardar(
                  new Cargo(
                      c.id(),
                      c.citaId(),
                      c.pacienteId(),
                      c.importe(),
                      saldo.saldoPendiente().compareTo(c.importe()) == 0
                          ? EstadoCargo.PENDIENTE
                          : EstadoCargo.PARCIAL));
            } else
              cargos.guardar(
                  new Cargo(c.id(), c.citaId(), c.pacienteId(), c.importe(), EstadoCargo.ANULADO));
            registro.compensar(s.operacionId(), s.operacionTipo(), s.id(), movimiento);
          }
          return new AprobarReversion(solicitudes).ejecutar(id, actor, aprobar);
        });
  }

  public CerrarCaja.CierreCaja cerrar(UUID actor, UUID id, BigDecimal declarado, String motivo) {
    return registro.ejecutar(
        () -> {
          cajero(actor);
          var s = propia(actor, id);
          Dinero.validar(declarado);
          if (motivo != null && motivo.length() > 500)
            throw new IllegalArgumentException("El motivo admite máximo 500 caracteres");
          if (s.estaCerrada()) {
            if (s.efectivoDeclarado().compareTo(declarado) != 0
                || !Objects.equals(s.motivoDiferencia(), motivo))
              throw new IllegalStateException("La caja ya está cerrada con otros datos");
            return resumen(s);
          }
          return new CerrarCaja(sesiones, pagos).ejecutar(id, declarado, motivo);
        });
  }

  public CerrarCaja.CierreCaja cierre(UUID actor, UUID id) {
    return registro.ejecutar(
        () -> {
          var p = acceso.perfil(actor);
          var s = sesiones.buscarPorId(id).orElseThrow(SesionCajaNoEncontrada::new);
          if (!p.administrador() && !s.cajeroId().equals(actor)) throw new AccesoCajaDenegado();
          return resumen(s);
        });
  }

  private CerrarCaja.CierreCaja resumen(SesionCaja s) {
    var lista = pagos.buscarPorSesion(s.id());
    var cobros =
        total(lista.stream().filter(p -> p.esCobro() && p.medio() == MedioPago.EFECTIVO).toList());
    var devoluciones =
        total(
            lista.stream()
                .filter(p -> p.esDevolucion() && p.medio() == MedioPago.EFECTIVO)
                .toList());
    return new CerrarCaja.CierreCaja(
        s, lista, s.fondoInicial().add(cobros).subtract(devoluciones), cobros, devoluciones);
  }

  private BigDecimal total(List<Pago> lista) {
    return lista.stream().map(Pago::importe).reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  public ConsultarReporteEconomico.ReporteEconomico reporte(
      UUID actor, LocalDate desde, LocalDate hasta) {
    return registro.ejecutar(
        () -> {
          var perfil = acceso.perfil(actor);
          if (desde == null || hasta == null || hasta.isBefore(desde))
            throw new IllegalArgumentException("El periodo no es válido");
          var zona = ZoneId.of("America/Lima");
          var lista =
              pagos
                  .buscarPorPeriodo(
                      desde.atStartOfDay(zona).toInstant(),
                      hasta.plusDays(1).atStartOfDay(zona).toInstant())
                  .stream()
                  .filter(
                      p ->
                          perfil.administrador()
                              || sesiones
                                  .buscarPorId(p.sesionCajaId())
                                  .orElseThrow()
                                  .cajeroId()
                                  .equals(actor))
                  .toList();
          var cobros = lista.stream().filter(Pago::esCobro).toList();
          var dev = lista.stream().filter(Pago::esDevolucion).toList();
          return new ConsultarReporteEconomico.ReporteEconomico(
              desde,
              hasta,
              cobros,
              dev,
              total(cobros),
              total(dev),
              total(cobros).subtract(total(dev)));
        });
  }

  public record CuentaCita(ReservasCaja.Reserva cita, List<ConsultarSaldo.SaldoCargo> cargos) {}

  public record Contexto(
      AccesoCaja.Perfil perfil,
      List<CuentaCita> citas,
      List<SesionCaja> sesiones,
      List<SolicitudReversion> solicitudes) {}
}
