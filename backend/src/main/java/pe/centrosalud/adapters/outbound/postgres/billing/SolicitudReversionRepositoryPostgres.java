package pe.centrosalud.adapters.outbound.postgres.billing;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import pe.centrosalud.modules.billing.application.ports.SolicitudReversionRepository;
import pe.centrosalud.modules.billing.domain.SolicitudReversion;

/** Implementación PostgreSQL del puerto de solicitudes de reversión. */
@Repository
public class SolicitudReversionRepositoryPostgres implements SolicitudReversionRepository {
    private final SolicitudReversionJpaRepository jpa;

    public SolicitudReversionRepositoryPostgres(SolicitudReversionJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public SolicitudReversion guardar(SolicitudReversion solicitud) {
        return jpa.save(SolicitudReversionJpaEntity.fromDomain(solicitud)).toDomain();
    }

    @Override
    public Optional<SolicitudReversion> buscarPorId(UUID id) {
        return jpa.findById(id).map(SolicitudReversionJpaEntity::toDomain);
    }

    @Override
    public List<SolicitudReversion> buscarPorSolicitante(UUID solicitanteId) {
        return jpa.findBySolicitanteId(solicitanteId).stream().map(SolicitudReversionJpaEntity::toDomain).toList();
    }
}
