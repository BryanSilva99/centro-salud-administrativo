package pe.centrosalud.modules.billing.application.usecases;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;
import pe.centrosalud.modules.billing.application.ports.CargoRepository;
import pe.centrosalud.modules.billing.domain.Cargo;
import pe.centrosalud.modules.billing.domain.EstadoCargo;

/**
 * Caso de uso: registrar cargo por servicio.
 * RF-19, RN-13. Un cargo vigente por cita; sin cargo no genera deuda.
 */
public final class RegistrarCargo {
    private final CargoRepository cargos;

    public RegistrarCargo(CargoRepository cargos) {
        this.cargos = Objects.requireNonNull(cargos);
    }

    public Cargo ejecutar(UUID citaId, UUID pacienteId, BigDecimal importe) {
        Objects.requireNonNull(citaId, "La cita es obligatoria");
        Objects.requireNonNull(pacienteId, "El paciente es obligatorio");
        Objects.requireNonNull(importe, "El importe es obligatorio");

        if (importe.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El importe debe ser positivo");
        }

        var existente = cargos.buscarPorCita(citaId).stream()
                .filter(Cargo::estaVigente)
                .findFirst();
        if (existente.isPresent()) {
            throw new IllegalStateException("La cita ya tiene un cargo vigente");
        }

        var cargo = new Cargo(UUID.randomUUID(), citaId, pacienteId, importe, EstadoCargo.PENDIENTE);
        return cargos.guardar(cargo);
    }
}
