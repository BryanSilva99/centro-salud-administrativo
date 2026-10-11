package pe.centrosalud.unit;

import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import pe.centrosalud.modules.administration.application.ports.Pacientes;
import pe.centrosalud.modules.administration.application.usecases.ActualizarPaciente;
import pe.centrosalud.modules.administration.application.usecases.ErrorPacientes;
import pe.centrosalud.modules.administration.domain.Paciente;
import pe.centrosalud.modules.administration.domain.TipoDocumento;

class ActualizarPacienteTest {
  // 2026-10-11 02:00 UTC equivale a 2026-10-10 21:00 en Lima.
  private static final Clock RELOJ =
      Clock.fixed(Instant.parse("2026-10-11T02:00:00Z"), ZoneOffset.UTC);
  private static final UUID AUTOR = UUID.randomUUID();
  private final Paciente existente =
      new Paciente(
          UUID.randomUUID(),
          "P00000001",
          TipoDocumento.DNI,
          "12345678",
          "Ana",
          "Quispe",
          LocalDate.of(1990, 1, 1),
          null,
          null,
          3);

  private static class PacientesFalsas implements Pacientes {
    Paciente actualizado;

    @Override
    public Paciente registrar(Paciente nuevo, UUID autor) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Optional<Paciente> buscarPorId(UUID id) {
      return Optional.empty();
    }

    @Override
    public Pagina<Paciente> buscar(String texto, int pagina) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Paciente actualizar(Paciente anterior, Paciente nuevo, UUID autor) {
      actualizado = nuevo;
      return nuevo;
    }
  }

  private class PacientesConExistente extends PacientesFalsas {
    @Override
    public Optional<Paciente> buscarPorId(UUID id) {
      return id.equals(existente.id()) ? Optional.of(existente) : Optional.empty();
    }
  }

  @Test
  void actualizaConservandoElCodigoYNormalizandoElDocumento() {
    var puerto = new PacientesConExistente();
    var resultado =
        new ActualizarPaciente(puerto, RELOJ)
            .actualizar(
                AUTOR,
                existente.id(),
                TipoDocumento.CE,
                "ab1234567",
                "Ana María",
                "Quispe Rojas",
                LocalDate.of(1990, 1, 1),
                null,
                null,
                3);
    assertEquals("P00000001", puerto.actualizado.codigo());
    assertEquals("AB1234567", puerto.actualizado.numeroDocumento());
    assertEquals("Ana María", resultado.nombres());
  }

  @Test
  void rechazaVersionObsoletaYPacienteInexistente() {
    var puerto = new PacientesConExistente();
    var actualizarPaciente = new ActualizarPaciente(puerto, RELOJ);
    var conflicto =
        assertThrows(
            ErrorPacientes.class,
            () ->
                actualizarPaciente.actualizar(
                    AUTOR,
                    existente.id(),
                    TipoDocumento.DNI,
                    "12345678",
                    "Ana",
                    "Quispe",
                    LocalDate.of(1990, 1, 1),
                    null,
                    null,
                    2));
    assertEquals(ErrorPacientes.Codigo.CONFLICTO, conflicto.codigo());
    var noExiste =
        assertThrows(
            ErrorPacientes.class,
            () ->
                actualizarPaciente.actualizar(
                    AUTOR,
                    UUID.randomUUID(),
                    TipoDocumento.DNI,
                    "12345678",
                    "Ana",
                    "Quispe",
                    LocalDate.of(1990, 1, 1),
                    null,
                    null,
                    0));
    assertEquals(ErrorPacientes.Codigo.NO_EXISTE, noExiste.codigo());
    assertNull(puerto.actualizado);
  }

  @Test
  void rechazaFechaDeNacimientoFutura() {
    var puerto = new PacientesConExistente();
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ActualizarPaciente(puerto, RELOJ)
                .actualizar(
                    AUTOR,
                    existente.id(),
                    TipoDocumento.DNI,
                    "12345678",
                    "Ana",
                    "Quispe",
                    LocalDate.of(2026, 10, 11),
                    null,
                    null,
                    3));
    assertNull(puerto.actualizado, "Una fecha futura no llega a persistencia");
  }

  @Test
  void guardadoIdenticoNoEscribe() {
    var puerto = new PacientesConExistente();
    var resultado =
        new ActualizarPaciente(puerto, RELOJ)
            .actualizar(
                AUTOR,
                existente.id(),
                TipoDocumento.DNI,
                " 12345678 ",
                " Ana ",
                " Quispe ",
                LocalDate.of(1990, 1, 1),
                null,
                null,
                3);
    assertEquals(existente, resultado);
    assertNull(puerto.actualizado);
  }

  @Test
  void propagaElDuplicadoInformadoPorPersistencia() {
    var puerto =
        new PacientesConExistente() {
          @Override
          public Paciente actualizar(Paciente anterior, Paciente nuevo, UUID autor) {
            throw new ErrorPacientes(ErrorPacientes.Codigo.DOCUMENTO_DUPLICADO);
          }
        };
    assertThrows(
        ErrorPacientes.class,
        () ->
            new ActualizarPaciente(puerto, RELOJ)
                .actualizar(
                    AUTOR,
                    existente.id(),
                    TipoDocumento.DNI,
                    "87654321",
                    "Ana",
                    "Quispe",
                    LocalDate.of(1990, 1, 1),
                    null,
                    null,
                    3));
  }
}
