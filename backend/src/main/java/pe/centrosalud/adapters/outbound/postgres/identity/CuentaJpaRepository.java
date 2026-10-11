package pe.centrosalud.adapters.outbound.postgres.identity;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.Repository;

public interface CuentaJpaRepository extends Repository<CuentaJpaEntity, UUID> {
  Optional<CuentaJpaEntity> findById(UUID id);

  Optional<CuentaJpaEntity> findByIdentificador(String identificador);
}
