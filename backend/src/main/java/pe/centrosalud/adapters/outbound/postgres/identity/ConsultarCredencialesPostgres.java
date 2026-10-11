package pe.centrosalud.adapters.outbound.postgres.identity;

import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import pe.centrosalud.modules.identity.application.ports.ConsultarCredenciales;
import pe.centrosalud.modules.identity.application.ports.CredencialesCuenta;

@Repository
@Transactional(readOnly = true)
public class ConsultarCredencialesPostgres implements ConsultarCredenciales {
  private final CuentaJpaRepository repository;

  public ConsultarCredencialesPostgres(CuentaJpaRepository repository) {
    this.repository = repository;
  }

  @Override
  public Optional<CredencialesCuenta> buscarPorIdentificador(String identificador) {
    return repository.findByIdentificador(identificador).map(CuentaJpaEntity::toCredenciales);
  }
}
