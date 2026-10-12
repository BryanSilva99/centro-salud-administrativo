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
import pe.centrosalud.modules.billing.domain.MedioPago;
import pe.centrosalud.modules.billing.domain.Pago;
import pe.centrosalud.modules.billing.domain.TipoMovimiento;

/** Entidad JPA para la tabla de pagos. */
@Entity
@Table(name = "pagos")
public class PagoJpaEntity {
  @Id private UUID id;

  @Column(name = "cargo_id", nullable = false)
  private UUID cargoId;

  @Column(name = "sesion_caja_id", nullable = false)
  private UUID sesionCajaId;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal importe;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private MedioPago medio;

  @Column(length = 100)
  private String referencia;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private TipoMovimiento tipo;

  @Column(name = "fecha_registro", nullable = false)
  private Instant fechaRegistro;

  protected PagoJpaEntity() {}

  public PagoJpaEntity(
      UUID id,
      UUID cargoId,
      UUID sesionCajaId,
      BigDecimal importe,
      MedioPago medio,
      String referencia,
      TipoMovimiento tipo,
      Instant fechaRegistro) {
    this.id = id;
    this.cargoId = cargoId;
    this.sesionCajaId = sesionCajaId;
    this.importe = importe;
    this.medio = medio;
    this.referencia = referencia;
    this.tipo = tipo;
    this.fechaRegistro = fechaRegistro;
  }

  public Pago toDomain() {
    return new Pago(id, cargoId, sesionCajaId, importe, medio, referencia, tipo, fechaRegistro);
  }

  public static PagoJpaEntity fromDomain(Pago pago) {
    return new PagoJpaEntity(
        pago.id(),
        pago.cargoId(),
        pago.sesionCajaId(),
        pago.importe(),
        pago.medio(),
        pago.referencia(),
        pago.tipo(),
        pago.fechaRegistro());
  }

  public UUID getId() {
    return id;
  }

  public UUID getCargoId() {
    return cargoId;
  }

  public UUID getSesionCajaId() {
    return sesionCajaId;
  }

  public BigDecimal getImporte() {
    return importe;
  }

  public MedioPago getMedio() {
    return medio;
  }

  public String getReferencia() {
    return referencia;
  }

  public TipoMovimiento getTipo() {
    return tipo;
  }

  public Instant getFechaRegistro() {
    return fechaRegistro;
  }
}
