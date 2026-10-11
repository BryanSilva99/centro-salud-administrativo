package pe.centrosalud.unit;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import pe.centrosalud.modules.administration.domain.Modalidad;
import pe.centrosalud.modules.administration.domain.Servicio;

class ServicioTest {
  static Modalidad modalidad(String nombre, boolean conCargo, String tarifa, boolean activa) {
    return new Modalidad(UUID.randomUUID(), nombre, conCargo, new BigDecimal(tarifa), activa);
  }

  static Servicio servicio(int duracion, boolean activo, List<Modalidad> modalidades) {
    return new Servicio(UUID.randomUUID(), " Consulta general ", duracion, activo, 0, modalidades);
  }

  @Test
  void tarifaSeExpresaEnSolesConDosDecimales() {
    assertEquals(new BigDecimal("40.00"), modalidad("Particular", true, "40", true).tarifa());
    assertEquals(new BigDecimal("40.50"), modalidad("Particular", true, "40.500", true).tarifa());
    assertEquals(new BigDecimal("0.00"), modalidad("Campaña", false, "0", true).tarifa());
    assertEquals(
        new BigDecimal("99999999.99"), modalidad("Particular", true, "99999999.99", true).tarifa());
    for (String invalida : new String[] {"-0.01", "40.555", "100000000.00"}) {
      assertThrows(
          IllegalArgumentException.class,
          () -> modalidad("Particular", true, invalida, true),
          invalida);
    }
    assertThrows(
        IllegalArgumentException.class,
        () -> new Modalidad(UUID.randomUUID(), "Particular", true, null, true));
  }

  @Test
  void conCargoRequiereImporteYSinCargoImporteCero() {
    assertThrows(IllegalArgumentException.class, () -> modalidad("Particular", true, "0.00", true));
    assertThrows(IllegalArgumentException.class, () -> modalidad("Campaña", false, "10.00", true));
  }

  @Test
  void nombreDeModalidadEsObligatorioYAcotado() {
    assertEquals("Particular", modalidad("  Particular ", true, "40", true).nombre());
    for (String invalido : new String[] {"", "  ", "x".repeat(61), "Parti\tcular"}) {
      assertThrows(
          IllegalArgumentException.class, () -> modalidad(invalido, true, "40", true), invalido);
    }
  }

  @Test
  void duracionDebeEstarEntreUnoY480Minutos() {
    var particular = List.of(modalidad("Particular", true, "40", true));
    assertEquals(1, servicio(1, true, particular).duracionMinutos());
    assertEquals(480, servicio(480, true, particular).duracionMinutos());
    assertThrows(IllegalArgumentException.class, () -> servicio(0, true, particular));
    assertThrows(IllegalArgumentException.class, () -> servicio(481, true, particular));
    assertEquals("Consulta general", servicio(30, true, particular).nombre());
  }

  @Test
  void requiereModalidadesSinNombresRepetidos() {
    assertThrows(IllegalArgumentException.class, () -> servicio(30, false, List.of()));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            servicio(
                30,
                true,
                List.of(
                    modalidad("Particular", true, "40", true),
                    modalidad("PARTICULAR", true, "45", false))));
  }

  @Test
  void servicioActivoRequiereUnaModalidadActiva() {
    var inactiva = List.of(modalidad("Particular", true, "40", false));
    assertThrows(IllegalArgumentException.class, () -> servicio(30, true, inactiva));
    assertFalse(servicio(30, false, inactiva).activo());
  }
}
