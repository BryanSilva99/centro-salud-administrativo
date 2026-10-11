package pe.centrosalud.adapters.outbound.postgres.billing;

import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import pe.centrosalud.modules.billing.application.ports.SesionCajaRepository;
import pe.centrosalud.modules.billing.domain.EstadoSesionCaja;
import pe.centrosalud.modules.billing.domain.SesionCaja;

/** Implementación PostgreSQL del puerto de sesiones de caja. */
@Repository
public class SesionCajaRepositoryPostgres implements SesionCajaRepository {
    private final SesionCajaJpaRepository jpa;

    public SesionCajaRepositoryPostgres(SesionCajaJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public SesionCaja guardar(SesionCaja sesion) {
        return jpa.save(SesionCajaJpaEntity.fromDomain(sesion)).toDomain();
    }

    @Override
    public Optional<SesionCaja> buscarPorId(UUID id) {
        return jpa.findById(id).map(SesionCajaJpaEntity::toDomain);
    }

    @Override
    public Optional<SesionCaja> buscarSesionAbiertaPorCajero(UUID cajeroId) {
        return jpa.findByCajeroIdAndEstado(cajeroId, EstadoSesionCaja.ABIERTA)
                .map(SesionCajaJpaEntity::toDomain);
    }
}
