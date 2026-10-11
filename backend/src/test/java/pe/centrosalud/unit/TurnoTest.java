package pe.centrosalud.unit;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import pe.centrosalud.modules.administration.domain.TipoTurno;
import pe.centrosalud.modules.administration.domain.Turno;

class TurnoTest {
  private static final UUID RECURSO = UUID.randomUUID();
  private static final LocalDate FECHA = LocalDate.of(2026, 10, 15);

  private static Turno turno(LocalTime inicio, LocalTime fin) {
    return new Turno(UUID.randomUUID(), RECURSO, FECHA, inicio, fin, TipoTurno.DISPONIBLE, true);
  }

  @Test
  void finDebeSerPosteriorAlInicio() {
    assertDoesNotThrow(() -> turno(LocalTime.of(8, 0), LocalTime.of(12, 0)));
    assertThrows(
        IllegalArgumentException.class, () -> turno(LocalTime.of(12, 0), LocalTime.of(12, 0)));
    assertThrows(
        IllegalArgumentException.class, () -> turno(LocalTime.of(12, 0), LocalTime.of(8, 0)));
    assertThrows(IllegalArgumentException.class, () -> turno(LocalTime.of(8, 0), null));
    assertThrows(IllegalArgumentException.class, () -> turno(null, LocalTime.of(12, 0)));
  }

  @Test
  void intervalosAdyacentesNoSeSuperponen() {
    var manana = turno(LocalTime.of(8, 0), LocalTime.of(12, 0));
    var tarde = turno(LocalTime.of(12, 0), LocalTime.of(17, 0));
    assertFalse(manana.seSuperponeCon(tarde));
    assertFalse(tarde.seSuperponeCon(manana));
  }

  @Test
  void intervalosQueSeCruzanSeSuperponen() {
    var manana = turno(LocalTime.of(8, 0), LocalTime.of(12, 0));
    var descanso = turno(LocalTime.of(11, 45), LocalTime.of(13, 0));
    assertTrue(manana.seSuperponeCon(descanso));
    assertTrue(descanso.seSuperponeCon(manana));
  }

  @Test
  void unIntervaloContenidoEnOtroSeSuperpone() {
    var jornada = turno(LocalTime.of(8, 0), LocalTime.of(17, 0));
    var descanso = turno(LocalTime.of(12, 0), LocalTime.of(13, 0));
    assertTrue(jornada.seSuperponeCon(descanso));
  }
}
