package pe.centrosalud.adapters.outbound.postgres.billing;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositorio Spring Data JPA para solicitudes de reversión. */
public interface SolicitudReversionJpaRepository extends JpaRepository<SolicitudReversionJpaEntity, UUID> {
    List<SolicitudReversionJpaEntity> findBySolicitanteId(UUID solicitanteId);
}
