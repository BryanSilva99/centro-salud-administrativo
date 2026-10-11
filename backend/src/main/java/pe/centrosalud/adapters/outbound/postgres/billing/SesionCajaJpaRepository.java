package pe.centrosalud.adapters.outbound.postgres.billing;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.centrosalud.modules.billing.domain.EstadoSesionCaja;

/** Repositorio Spring Data JPA para sesiones de caja. */
public interface SesionCajaJpaRepository extends JpaRepository<SesionCajaJpaEntity, UUID> {
    Optional<SesionCajaJpaEntity> findByCajeroIdAndEstado(UUID cajeroId, EstadoSesionCaja estado);
}
