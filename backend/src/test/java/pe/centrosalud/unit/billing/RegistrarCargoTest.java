package pe.centrosalud.unit.billing;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.centrosalud.modules.billing.application.ports.CargoRepository;
import pe.centrosalud.modules.billing.application.usecases.RegistrarCargo;
import pe.centrosalud.modules.billing.domain.EstadoCargo;

class RegistrarCargoTest {

  @Test
  @DisplayName("RF-19/CA-1: registrar cargo válido con importe positivo")
  void registrarCargoValido() {
    CargoRepository cargos = mock(CargoRepository.class);
    when(cargos.buscarPorCita(any())).thenReturn(List.of());
    when(cargos.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

    var casoUso = new RegistrarCargo(cargos);
    UUID citaId = UUID.randomUUID();
    UUID pacienteId = UUID.randomUUID();

    var cargo = casoUso.ejecutar(citaId, pacienteId, new BigDecimal("50.00"));

    assertNotNull(cargo);
    assertEquals(citaId, cargo.citaId());
    assertEquals(pacienteId, cargo.pacienteId());
    assertEquals(new BigDecimal("50.00"), cargo.importe());
    assertEquals(EstadoCargo.PENDIENTE, cargo.estado());
  }

  @Test
  @DisplayName("RF-19/CA-2: rechazar cargo cuando ya existe uno vigente para la cita")
  void rechazarCargoDuplicado() {
    CargoRepository cargos = mock(CargoRepository.class);
    UUID citaId = UUID.randomUUID();
    var cargoExistente =
        new pe.centrosalud.modules.billing.domain.Cargo(
            UUID.randomUUID(),
            citaId,
            UUID.randomUUID(),
            new BigDecimal("30.00"),
            EstadoCargo.PENDIENTE);
    when(cargos.buscarPorCita(citaId)).thenReturn(List.of(cargoExistente));

    var casoUso = new RegistrarCargo(cargos);

    assertEquals(
        cargoExistente,
        casoUso.ejecutar(citaId, cargoExistente.pacienteId(), new BigDecimal("30.00")));
    verify(cargos, never()).guardar(any());
  }

  @Test
  @DisplayName("RF-19: rechazar importe negativo o cero")
  void rechazarImporteNoPositivo() {
    CargoRepository cargos = mock(CargoRepository.class);
    var casoUso = new RegistrarCargo(cargos);

    assertThrows(
        IllegalArgumentException.class,
        () -> casoUso.ejecutar(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("-10")));
    assertThrows(
        IllegalArgumentException.class,
        () -> casoUso.ejecutar(UUID.randomUUID(), UUID.randomUUID(), BigDecimal.ZERO));
  }
}
