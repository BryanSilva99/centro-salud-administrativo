package pe.centrosalud.modules.appointments.application.ports;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import pe.centrosalud.modules.appointments.domain.Cita;
import pe.centrosalud.modules.appointments.domain.HorarioCita;
import pe.centrosalud.modules.appointments.domain.Reprogramacion;

public interface Citas {
    Optional<Cita> buscarPorId(UUID id);
    List<Cita> agenda(LocalDate fecha, UUID profesionalId, UUID servicioId, UUID consultorioId);
    default List<Cita> atencion(LocalDate fecha, UUID profesionalId) {
        return agenda(fecha, profesionalId, null, null).stream()
                .filter(cita -> cita.estado() == pe.centrosalud.modules.appointments.domain.EstadoCita.EN_ESPERA
                        || cita.estado() == pe.centrosalud.modules.appointments.domain.EstadoCita.EN_ATENCION)
                .toList();
    }
    /** Conteo diario; los adaptadores persistentes pueden resolverlo con una agregacion SQL. */
    default Map<pe.centrosalud.modules.appointments.domain.EstadoCita, Long> conteo(LocalDate fecha,
                                                                                     UUID profesionalId,
                                                                                     UUID servicioId,
                                                                                     UUID consultorioId) {
        var resultado = new EnumMap<pe.centrosalud.modules.appointments.domain.EstadoCita, Long>(
                pe.centrosalud.modules.appointments.domain.EstadoCita.class);
        for (var estado : pe.centrosalud.modules.appointments.domain.EstadoCita.values()) resultado.put(estado, 0L);
        agenda(fecha, profesionalId, servicioId, consultorioId)
                .forEach(cita -> resultado.compute(cita.estado(), (estado, cantidad) -> cantidad + 1));
        return Map.copyOf(resultado);
    }
    boolean existeConflicto(UUID pacienteId, UUID profesionalId, UUID consultorioId,
                            HorarioCita horario, UUID excluirId);
    Cita guardar(Cita cita);
    /** Actualiza una cita existente con control optimista de versión. */
    default Cita guardarTransicion(Cita cita) { return guardar(cita); }
    Cita guardarReprogramacion(Cita cita, Reprogramacion historial);
}
