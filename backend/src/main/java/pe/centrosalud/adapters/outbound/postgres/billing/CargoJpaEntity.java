package pe.centrosalud.adapters.outbound.postgres.billing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import pe.centrosalud.modules.billing.domain.Cargo;
import pe.centrosalud.modules.billing.domain.EstadoCargo;

/** Entidad JPA para la tabla de cargos. */
@Entity
@Table(name = "cargos")
public class CargoJpaEntity {
    @Id
    private UUID id;

    @Column(name = "cita_id", nullable = false)
    private UUID citaId;

    @Column(name = "paciente_id", nullable = false)
    private UUID pacienteId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal importe;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoCargo estado;

    protected CargoJpaEntity() {}

    public CargoJpaEntity(UUID id, UUID citaId, UUID pacienteId, BigDecimal importe, EstadoCargo estado) {
        this.id = id;
        this.citaId = citaId;
        this.pacienteId = pacienteId;
        this.importe = importe;
        this.estado = estado;
    }

    public Cargo toDomain() {
        return new Cargo(id, citaId, pacienteId, importe, estado);
    }

    public static CargoJpaEntity fromDomain(Cargo cargo) {
        return new CargoJpaEntity(cargo.id(), cargo.citaId(), cargo.pacienteId(), cargo.importe(), cargo.estado());
    }

    public UUID getId() { return id; }
    public UUID getCitaId() { return citaId; }
    public UUID getPacienteId() { return pacienteId; }
    public BigDecimal getImporte() { return importe; }
    public EstadoCargo getEstado() { return estado; }

    public void setEstado(EstadoCargo estado) { this.estado = estado; }
}
