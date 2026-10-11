package pe.centrosalud.unit;

import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import pe.centrosalud.modules.administration.application.ports.Pacientes;
import pe.centrosalud.modules.administration.application.usecases.ErrorPacientes;
import pe.centrosalud.modules.administration.application.usecases.RegistrarPaciente;
import pe.centrosalud.modules.administration.domain.Paciente;
import pe.centrosalud.modules.administration.domain.TipoDocumento;

class RegistrarPacienteTest {
  // 2026-10-11 02:00 UTC equivale a 2026-10-10 21:00 en Lima.
  private static final Clock RELOJ =
      Clock.fixed(Instant.parse("2026-10-11T02:00:00Z"), ZoneOffset.UTC);
  private static final UUID AUTOR = UUID.randomUUID();

  private static class PacientesFalsos implements Pacientes {
    Paciente recibido;
    UUID autor;
    RuntimeException error;

    @Override
    public Paciente registrar(Paciente nuevo, UUID autor) {
      if (error != null) throw error;
      recibido = nuevo;
      this.autor = autor;
      return new Paciente(
          nuevo.id(),
          "P00000001",
          nuevo.tipoDocumento(),
          nuevo.numeroDocumento(),
          nuevo.nombres(),
          nuevo.apellidos(),
          nuevo.fechaNacimiento(),
          nuevo.telefono(),
          nuevo.correo(),
          0);
    }

    @Override
    public java.util.Optional<Paciente> buscarPorId(UUID id) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Pagina<Paciente> buscar(String texto, int pagina) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Paciente actualizar(Paciente anterior, Paciente nuevo, UUID autor) {
      throw new UnsupportedOperationException();
    }
  }

  @Test
  void registraDatosNormalizadosConSuAutor() {
    var puerto = new PacientesFalsos();
    var registrado =
        new RegistrarPaciente(puerto, RELOJ)
            .registrar(
                AUTOR,
                TipoDocumento.CE,
                "ab1234567",
                " Ana ",
                " Quispe ",
                LocalDate.of(1990, 1, 1),
                "",
                null);
    assertEquals(AUTOR, puerto.autor);
    assertNull(puerto.recibido.codigo());
    assertEquals("AB1234567", puerto.recibido.numeroDocumento());
    assertEquals("Ana", puerto.recibido.nombres());
    assertNull(puerto.recibido.telefono());
    assertEquals("P00000001", registrado.codigo());
  }

  @Test
  void usaLaFechaDelCentroYNoLaDeUtc() {
    var puerto = new PacientesFalsos();
    var registrar = new RegistrarPaciente(puerto, RELOJ);
    assertThrows(
        IllegalArgumentException.class,
        () ->
            registrar.registrar(
                AUTOR,
                TipoDocumento.DNI,
                "12345678",
                "Ana",
                "Quispe",
                LocalDate.of(2026, 10, 11),
                null,
                null));
    assertNull(puerto.recibido, "Una fecha futura no llega a persistencia");
    registrar.registrar(
        AUTOR,
        TipoDocumento.DNI,
        "12345678",
        "Ana",
        "Quispe",
        LocalDate.of(2026, 10, 10),
        null,
        null);
    assertNotNull(puerto.recibido);
  }

  @Test
  void propagaElDuplicadoInformadoPorPersistencia() {
    var puerto = new PacientesFalsos();
    puerto.error = new ErrorPacientes(ErrorPacientes.Codigo.DOCUMENTO_DUPLICADO);
    assertThrows(
        ErrorPacientes.class,
        () ->
            new RegistrarPaciente(puerto, RELOJ)
                .registrar(
                    AUTOR,
                    TipoDocumento.DNI,
                    "12345678",
                    "Ana",
                    "Quispe",
                    LocalDate.of(1990, 1, 1),
                    null,
                    null));
  }
}
