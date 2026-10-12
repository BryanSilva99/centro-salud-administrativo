package pe.centrosalud.adapters.outbound.postgres.billing;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositorio Spring Data JPA para pagos. */
public interface PagoJpaRepository extends JpaRepository<PagoJpaEntity, UUID> {
  List<PagoJpaEntity> findByCargoId(UUID cargoId);

  List<PagoJpaEntity> findBySesionCajaId(UUID sesionCajaId);

  List<PagoJpaEntity> findByFechaRegistroGreaterThanEqualAndFechaRegistroLessThan(
      Instant inicio, Instant fin);
}
