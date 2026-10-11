package pe.centrosalud.adapters.outbound.postgres.appointments;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import pe.centrosalud.modules.appointments.application.ports.Citas;
import pe.centrosalud.modules.appointments.application.usecases.ConflictoCita;
import pe.centrosalud.modules.appointments.application.usecases.VersionCitaObsoleta;
import pe.centrosalud.modules.appointments.domain.Cita;
import pe.centrosalud.modules.appointments.domain.EstadoCita;
import pe.centrosalud.modules.appointments.domain.HorarioCita;
import pe.centrosalud.modules.appointments.domain.Reprogramacion;
import pe.centrosalud.modules.appointments.domain.TipoCierreCita;

@Repository
public class CitasPostgres implements Citas {
  private static final ZoneId ZONA = ZoneId.of("America/Lima");
  private final CitaJpaRepository citas;
  private final ReprogramacionJpaRepository reprogramaciones;

  public CitasPostgres(CitaJpaRepository citas, ReprogramacionJpaRepository reprogramaciones) {
    this.citas = citas;
    this.reprogramaciones = reprogramaciones;
  }

  @Override
  public Optional<Cita> buscarPorId(UUID id) {
    return citas.findById(id).map(CitaJpaEntity::toDomain);
  }

  @Override
  public List<Cita> agenda(
      LocalDate fecha, UUID profesionalId, UUID servicioId, UUID consultorioId) {
    var desde = fecha.atStartOfDay(ZONA).toInstant();
    var hasta = fecha.plusDays(1).atStartOfDay(ZONA).toInstant();
    return citas.agenda(desde, hasta, profesionalId, servicioId, consultorioId).stream()
        .map(CitaJpaEntity::toDomain)
        .toList();
  }

  @Override
  public List<Cita> atencion(LocalDate fecha, UUID profesionalId) {
    var desde = fecha.atStartOfDay(ZONA).toInstant();
    var hasta = fecha.plusDays(1).atStartOfDay(ZONA).toInstant();
    return citas
        .atencion(
            desde, hasta, profesionalId, List.of(EstadoCita.EN_ESPERA, EstadoCita.EN_ATENCION))
        .stream()
        .map(CitaJpaEntity::toDomain)
        .toList();
  }

  @Override
  public Map<EstadoCita, Long> conteo(
      LocalDate fecha, UUID profesionalId, UUID servicioId, UUID consultorioId) {
    var desde = fecha.atStartOfDay(ZONA).toInstant();
    var hasta = fecha.plusDays(1).atStartOfDay(ZONA).toInstant();
    var resultado = new EnumMap<EstadoCita, Long>(EstadoCita.class);
    for (var estado : EstadoCita.values()) resultado.put(estado, 0L);
    citas
        .conteo(desde, hasta, profesionalId, servicioId, consultorioId)
        .forEach(fila -> resultado.put(fila.getEstado(), fila.getCantidad()));
    return Map.copyOf(resultado);
  }

  @Override
  public boolean existeConflicto(
      UUID pacienteId,
      UUID profesionalId,
      UUID consultorioId,
      HorarioCita horario,
      UUID excluirId) {
    return citas.existeConflicto(
        pacienteId,
        profesionalId,
        consultorioId,
        horario.inicio(),
        horario.fin(),
        excluirId,
        List.of(EstadoCita.CANCELADA, EstadoCita.AUSENTE));
  }

  @Override
  @Transactional
  public Cita guardar(Cita cita) {
    try {
      return citas.saveAndFlush(CitaJpaEntity.nueva(cita)).toDomain();
    } catch (DataIntegrityViolationException error) {
      throw new ConflictoCita();
    }
  }

  @Override
  @Transactional
  public Cita guardarTransicion(Cita cita) {
    var entidad =
        citas
            .findById(cita.id())
            .orElseThrow(
                () ->
                    new pe.centrosalud.modules.appointments.application.usecases
                        .CitaNoEncontrada());
    if (entidad.version() != cita.version() - 1) {
      var actual = entidad.toDomain();
      if (transicionYaAplicada(actual, cita)) return actual;
      throw new VersionCitaObsoleta();
    }
    entidad.aplicar(cita);
    try {
      return citas.saveAndFlush(entidad).toDomain();
    } catch (OptimisticLockingFailureException error) {
      throw new VersionCitaObsoleta();
    } catch (DataIntegrityViolationException error) {
      throw new ConflictoCita();
    }
  }

  private boolean transicionYaAplicada(Cita actual, Cita solicitada) {
    return switch (solicitada.estado()) {
      case EN_ESPERA -> actual.llegadaEn() != null;
      case EN_ATENCION -> actual.atencionIniciadaEn() != null;
      case ATENDIDA -> actual.atencionFinalizadaEn() != null;
      case CANCELADA ->
          actual.estado() == EstadoCita.CANCELADA
              && actual.tipoCierre() != null
              && actual.tipoCierre() == solicitada.tipoCierre();
      case AUSENTE ->
          actual.estado() == EstadoCita.AUSENTE && actual.tipoCierre() == TipoCierreCita.AUSENCIA;
      default -> false;
    };
  }

  @Override
  @Transactional
  public Cita guardarReprogramacion(Cita cita, Reprogramacion historial) {
    var entidad =
        citas
            .findById(cita.id())
            .orElseThrow(
                () ->
                    new pe.centrosalud.modules.appointments.application.usecases
                        .CitaNoEncontrada());
    if (entidad.version() != cita.version() - 1) throw new VersionCitaObsoleta();
    entidad.aplicar(cita);
    try {
      var guardada = citas.saveAndFlush(entidad);
      reprogramaciones.saveAndFlush(ReprogramacionJpaEntity.nueva(historial));
      return guardada.toDomain();
    } catch (OptimisticLockingFailureException error) {
      throw new VersionCitaObsoleta();
    } catch (DataIntegrityViolationException error) {
      throw new ConflictoCita();
    }
  }
}
