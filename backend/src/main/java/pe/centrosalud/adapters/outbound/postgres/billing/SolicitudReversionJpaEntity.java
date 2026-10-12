package pe.centrosalud.adapters.outbound.postgres.billing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import pe.centrosalud.modules.billing.domain.EstadoSolicitudReversion;
import pe.centrosalud.modules.billing.domain.SolicitudReversion;

/** Entidad JPA para la tabla de solicitudes de reversión. */
@Entity
@Table(name = "solicitudes_reversion")
public class SolicitudReversionJpaEntity {
  @Id private UUID id;

  @Column(name = "operacion_id", nullable = false)
  private UUID operacionId;

  @Column(name = "operacion_tipo", nullable = false, length = 20)
  @Enumerated(EnumType.STRING)
  private SolicitudReversion.TipoOperacion operacionTipo;

  @Column(name = "solicitante_id", nullable = false)
  private UUID solicitanteId;

  @Column(nullable = false, length = 500)
  private String motivo;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private EstadoSolicitudReversion estado;

  @Column(name = "aprobador_id")
  private UUID aprobadorId;

  @Column(name = "fecha_solicitud", nullable = false)
  private Instant fechaSolicitud;

  @Column(name = "fecha_resolucion")
  private Instant fechaResolucion;

  protected SolicitudReversionJpaEntity() {}

  public SolicitudReversionJpaEntity(
      UUID id,
      UUID operacionId,
      SolicitudReversion.TipoOperacion operacionTipo,
      UUID solicitanteId,
      String motivo,
      EstadoSolicitudReversion estado,
      UUID aprobadorId,
      Instant fechaSolicitud,
      Instant fechaResolucion) {
    this.id = id;
    this.operacionId = operacionId;
    this.operacionTipo = operacionTipo;
    this.solicitanteId = solicitanteId;
    this.motivo = motivo;
    this.estado = estado;
    this.aprobadorId = aprobadorId;
    this.fechaSolicitud = fechaSolicitud;
    this.fechaResolucion = fechaResolucion;
  }

  public SolicitudReversion toDomain() {
    return new SolicitudReversion(
        id,
        operacionId,
        operacionTipo,
        solicitanteId,
        motivo,
        estado,
        aprobadorId,
        fechaSolicitud,
        fechaResolucion);
  }

  public static SolicitudReversionJpaEntity fromDomain(SolicitudReversion solicitud) {
    return new SolicitudReversionJpaEntity(
        solicitud.id(),
        solicitud.operacionId(),
        solicitud.operacionTipo(),
        solicitud.solicitanteId(),
        solicitud.motivo(),
        solicitud.estado(),
        solicitud.aprobadorId(),
        solicitud.fechaSolicitud(),
        solicitud.fechaResolucion());
  }

  public UUID getId() {
    return id;
  }

  public UUID getOperacionId() {
    return operacionId;
  }

  public SolicitudReversion.TipoOperacion getOperacionTipo() {
    return operacionTipo;
  }

  public UUID getSolicitanteId() {
    return solicitanteId;
  }

  public String getMotivo() {
    return motivo;
  }

  public EstadoSolicitudReversion getEstado() {
    return estado;
  }

  public UUID getAprobadorId() {
    return aprobadorId;
  }

  public Instant getFechaSolicitud() {
    return fechaSolicitud;
  }

  public Instant getFechaResolucion() {
    return fechaResolucion;
  }
}
