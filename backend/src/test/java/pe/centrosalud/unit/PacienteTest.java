package pe.centrosalud.unit;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import pe.centrosalud.modules.administration.domain.Paciente;
import pe.centrosalud.modules.administration.domain.TipoDocumento;

class PacienteTest {
  private static final LocalDate NACIMIENTO = LocalDate.of(1990, 5, 20);

  private static Paciente paciente(
      TipoDocumento tipo, String numero, String nombres, String telefono, String correo) {
    return new Paciente(
        UUID.randomUUID(),
        null,
        tipo,
        numero,
        nombres,
        "Quispe Rojas",
        NACIMIENTO,
        telefono,
        correo);
  }

  @Test
  void dniAceptaSoloOchoDigitos() {
    assertEquals(
        "12345678", paciente(TipoDocumento.DNI, " 12345678 ", "Ana", null, null).numeroDocumento());
    for (String invalido : new String[] {"1234567", "123456789", "1234567A", "", " "}) {
      assertThrows(
          IllegalArgumentException.class,
          () -> paciente(TipoDocumento.DNI, invalido, "Ana", null, null),
          invalido);
    }
  }

  @Test
  void carneDeExtranjeriaAceptaNueveAlfanumericosEnMayusculas() {
    assertEquals(
        "AB1234567", paciente(TipoDocumento.CE, "ab1234567", "Ana", null, null).numeroDocumento());
    for (String invalido : new String[] {"AB123456", "AB12345678", "AB-234567", "ÁB1234567"}) {
      assertThrows(
          IllegalArgumentException.class,
          () -> paciente(TipoDocumento.CE, invalido, "Ana", null, null),
          invalido);
    }
  }

  @Test
  void nombresYApellidosSonObligatoriosYAcotados() {
    assertEquals(
        "María José",
        paciente(TipoDocumento.DNI, "12345678", "  María José ", null, null).nombres());
    for (String invalido : new String[] {null, "", "   ", "x".repeat(101), "Ana\nMaría"}) {
      assertThrows(
          IllegalArgumentException.class,
          () -> paciente(TipoDocumento.DNI, "12345678", invalido, null, null));
    }
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new Paciente(
                UUID.randomUUID(),
                null,
                TipoDocumento.DNI,
                "12345678",
                "Ana",
                " ",
                NACIMIENTO,
                null,
                null));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new Paciente(
                UUID.randomUUID(),
                null,
                null,
                "12345678",
                "Ana",
                "Quispe",
                NACIMIENTO,
                null,
                null));
  }

  @Test
  void contactoEsOpcionalPeroSeValidaSiSeInforma() {
    var sinContacto = paciente(TipoDocumento.DNI, "12345678", "Ana", "  ", "");
    assertNull(sinContacto.telefono());
    assertNull(sinContacto.correo());
    var conContacto =
        paciente(TipoDocumento.DNI, "12345678", "Ana", " +51 987 654 321 ", " ana@correo.pe ");
    assertEquals("+51 987 654 321", conContacto.telefono());
    assertEquals("ana@correo.pe", conContacto.correo());
    assertThrows(
        IllegalArgumentException.class,
        () -> paciente(TipoDocumento.DNI, "12345678", "Ana", "12345", null));
    assertThrows(
        IllegalArgumentException.class,
        () -> paciente(TipoDocumento.DNI, "12345678", "Ana", "98765432a", null));
    assertThrows(
        IllegalArgumentException.class,
        () -> paciente(TipoDocumento.DNI, "12345678", "Ana", null, "ana@correo"));
    assertThrows(
        IllegalArgumentException.class,
        () -> paciente(TipoDocumento.DNI, "12345678", "Ana", null, "ana correo@x.pe"));
  }

  @Test
  void nacimientoNoPuedeSerFuturoNiAnteriorA1900() {
    LocalDate hoy = LocalDate.of(2026, 10, 10);
    assertDoesNotThrow(() -> Paciente.validarNacimiento(hoy, hoy));
    assertDoesNotThrow(() -> Paciente.validarNacimiento(LocalDate.of(1900, 1, 1), hoy));
    assertThrows(
        IllegalArgumentException.class, () -> Paciente.validarNacimiento(hoy.plusDays(1), hoy));
    assertThrows(
        IllegalArgumentException.class,
        () -> Paciente.validarNacimiento(LocalDate.of(1899, 12, 31), hoy));
    assertThrows(IllegalArgumentException.class, () -> Paciente.validarNacimiento(null, hoy));
  }
}
