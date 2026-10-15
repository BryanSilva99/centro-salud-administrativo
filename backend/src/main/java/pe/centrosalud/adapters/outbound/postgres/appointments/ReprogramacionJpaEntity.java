package pe.centrosalud.adapters.outbound.postgres.appointments;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import pe.centrosalud.modules.appointments.domain.Reprogramacion;

@Entity
@Table(name = "reprogramaciones_cita")
public class ReprogramacionJpaEntity {
    @Id private UUID id;
    @Column(name = "cita_id", nullable = false) private UUID citaId;
    @Column(name = "actor_id", nullable = false) private UUID actorId;
    @Column(name = "horario_anterior_inicio", nullable = false) private Instant horarioAnteriorInicio;
    @Column(name = "horario_anterior_fin", nullable = false) private Instant horarioAnteriorFin;
    @Column(name = "nuevo_horario_inicio", nullable = false) private Instant nuevoHorarioInicio;
    @Column(name = "nuevo_horario_fin", nullable = false) private Instant nuevoHorarioFin;
    @Column(name = "motivo", nullable = false, columnDefinition = "text") private String motivo;
    @Column(name = "registrada_en", nullable = false) private Instant registradaEn;

    protected ReprogramacionJpaEntity() {}

    static ReprogramacionJpaEntity nueva(Reprogramacion registro) {
        var entidad = new ReprogramacionJpaEntity();
        entidad.id = UUID.randomUUID(); entidad.citaId = registro.citaId(); entidad.actorId = registro.actorId();
        entidad.horarioAnteriorInicio = registro.horarioAnteriorInicio(); entidad.horarioAnteriorFin = registro.horarioAnteriorFin();
        entidad.nuevoHorarioInicio = registro.nuevoHorarioInicio(); entidad.nuevoHorarioFin = registro.nuevoHorarioFin();
        entidad.motivo = registro.motivo(); entidad.registradaEn = registro.registradaEn();
        return entidad;
    }
}
