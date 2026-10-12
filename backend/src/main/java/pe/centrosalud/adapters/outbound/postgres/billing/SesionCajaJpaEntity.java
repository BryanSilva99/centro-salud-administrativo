package pe.centrosalud.adapters.outbound.postgres.billing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import pe.centrosalud.modules.billing.domain.EstadoSesionCaja;
import pe.centrosalud.modules.billing.domain.SesionCaja;

/** Entidad JPA para la tabla de sesiones de caja. */
@Entity
@Table(name = "sesiones_caja")
public class SesionCajaJpaEntity {
  @Id private UUID id;

  @Column(name = "cajero_id", nullable = false)
  private UUID cajeroId;

  @Column(name = "fondo_inicial", nullable = false, precision = 12, scale = 2)
  private BigDecimal fondoInicial;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private EstadoSesionCaja estado;

  @Column(name = "fecha_apertura", nullable = false)
  private Instant fechaApertura;

  @Column(name = "fecha_cierre")
  private Instant fechaCierre;

  @Column(name = "efectivo_declarado", precision = 12, scale = 2)
  private BigDecimal efectivoDeclarado;

  @Column(name = "diferencia", precision = 12, scale = 2)
  private BigDecimal diferencia;

  @Column(name = "motivo_diferencia", length = 500)
  private String motivoDiferencia;

  protected SesionCajaJpaEntity() {}

  public SesionCajaJpaEntity(
      UUID id,
      UUID cajeroId,
      BigDecimal fondoInicial,
      EstadoSesionCaja estado,
      Instant fechaApertura,
      Instant fechaCierre,
      BigDecimal efectivoDeclarado,
      BigDecimal diferencia,
      String motivoDiferencia) {
    this.id = id;
    this.cajeroId = cajeroId;
    this.fondoInicial = fondoInicial;
    this.estado = estado;
    this.fechaApertura = fechaApertura;
    this.fechaCierre = fechaCierre;
    this.efectivoDeclarado = efectivoDeclarado;
    this.diferencia = diferencia;
    this.motivoDiferencia = motivoDiferencia;
  }

  public SesionCaja toDomain() {
    return new SesionCaja(
        id,
        cajeroId,
        fondoInicial,
        estado,
        fechaApertura,
        fechaCierre,
        efectivoDeclarado,
        diferencia,
        motivoDiferencia);
  }

  public static SesionCajaJpaEntity fromDomain(SesionCaja sesion) {
    return new SesionCajaJpaEntity(
        sesion.id(),
        sesion.cajeroId(),
        sesion.fondoInicial(),
        sesion.estado(),
        sesion.fechaApertura(),
        sesion.fechaCierre(),
        sesion.efectivoDeclarado(),
        sesion.diferencia(),
        sesion.motivoDiferencia());
  }

  public UUID getId() {
    return id;
  }

  public UUID getCajeroId() {
    return cajeroId;
  }

  public BigDecimal getFondoInicial() {
    return fondoInicial;
  }

  public EstadoSesionCaja getEstado() {
    return estado;
  }

  public Instant getFechaApertura() {
    return fechaApertura;
  }

  public Instant getFechaCierre() {
    return fechaCierre;
  }

  public BigDecimal getEfectivoDeclarado() {
    return efectivoDeclarado;
  }

  public BigDecimal getDiferencia() {
    return diferencia;
  }

  public String getMotivoDiferencia() {
    return motivoDiferencia;
  }
}
