package pe.centrosalud.adapters.outbound.postgres.appointments;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface ReprogramacionJpaRepository extends JpaRepository<ReprogramacionJpaEntity, UUID> {}
