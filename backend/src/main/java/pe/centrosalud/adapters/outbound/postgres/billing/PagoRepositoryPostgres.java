package pe.centrosalud.adapters.outbound.postgres.billing;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import pe.centrosalud.modules.billing.application.ports.PagoRepository;
import pe.centrosalud.modules.billing.domain.Pago;

/** Implementación PostgreSQL del puerto de pagos. */
@Repository
public class PagoRepositoryPostgres implements PagoRepository {
    private final PagoJpaRepository jpa;

    public PagoRepositoryPostgres(PagoJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Pago guardar(Pago pago) {
        return jpa.save(PagoJpaEntity.fromDomain(pago)).toDomain();
    }

    @Override
    public List<Pago> buscarPorCargo(UUID cargoId) {
        return jpa.findByCargoId(cargoId).stream().map(PagoJpaEntity::toDomain).toList();
    }

    @Override
    public List<Pago> buscarPorSesion(UUID sesionCajaId) {
        return jpa.findBySesionCajaId(sesionCajaId).stream().map(PagoJpaEntity::toDomain).toList();
    }

    @Override
    public List<Pago> buscarPorPeriodo(Instant inicio, Instant fin) {
        return jpa.findByFechaRegistroBetween(inicio, fin).stream().map(PagoJpaEntity::toDomain).toList();
    }
}
