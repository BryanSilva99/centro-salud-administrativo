package pe.centrosalud.adapters.outbound.postgres.billing;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositorio Spring Data JPA para cargos. */
public interface CargoJpaRepository extends JpaRepository<CargoJpaEntity, UUID> {
  java.util.List<CargoJpaEntity> findByCitaId(UUID citaId);

  java.util.List<CargoJpaEntity> findByPacienteId(UUID pacienteId);
}
