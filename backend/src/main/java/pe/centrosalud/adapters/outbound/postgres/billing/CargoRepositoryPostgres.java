package pe.centrosalud.adapters.outbound.postgres.billing;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import pe.centrosalud.modules.billing.application.ports.CargoRepository;
import pe.centrosalud.modules.billing.domain.Cargo;

/** Implementación PostgreSQL del puerto de cargos. */
@Repository
public class CargoRepositoryPostgres implements CargoRepository {
    private final CargoJpaRepository jpa;

    public CargoRepositoryPostgres(CargoJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Cargo guardar(Cargo cargo) {
        return jpa.save(CargoJpaEntity.fromDomain(cargo)).toDomain();
    }

    @Override
    public Optional<Cargo> buscarPorId(UUID id) {
        return jpa.findById(id).map(CargoJpaEntity::toDomain);
    }

    @Override
    public List<Cargo> buscarPorCita(UUID citaId) {
        return jpa.findAll().stream()
                .filter(e -> e.getCitaId().equals(citaId))
                .map(CargoJpaEntity::toDomain)
                .toList();
    }

    @Override
    public List<Cargo> buscarPorPaciente(UUID pacienteId) {
        return jpa.findAll().stream()
                .filter(e -> e.getPacienteId().equals(pacienteId))
                .map(CargoJpaEntity::toDomain)
                .toList();
    }
}
