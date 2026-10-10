package pe.centrosalud.adapters.outbound.postgres.identity;

import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import pe.centrosalud.modules.identity.application.ports.ConsultarCuentas;
import pe.centrosalud.modules.identity.domain.Cuenta;

@Repository
@Transactional(readOnly = true)
public class ConsultarCuentasPostgres implements ConsultarCuentas {
    private final CuentaJpaRepository repository;

    public ConsultarCuentasPostgres(CuentaJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Cuenta> buscarPorId(UUID id) {
        return repository.findById(id).map(CuentaJpaEntity::toDomain);
    }
}
