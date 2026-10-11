package pe.centrosalud.adapters.outbound.postgres.appointments;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import pe.centrosalud.modules.appointments.domain.Cita;
import pe.centrosalud.modules.appointments.domain.EstadoCita;
import pe.centrosalud.modules.appointments.domain.HorarioCita;
import pe.centrosalud.modules.appointments.domain.TipoCierreCita;

@Entity
@Table(name = "citas")
public class CitaJpaEntity {
    @Id private UUID id;
    @Column(name = "paciente_id", nullable = false) private UUID pacienteId;
    @Column(name = "profesional_id", nullable = false) private UUID profesionalId;
    @Column(name = "servicio_id", nullable = false) private UUID servicioId;
    @Column(name = "consultorio_id", nullable = false) private UUID consultorioId;
    @Column(name = "inicio", nullable = false) private Instant inicio;
    @Column(name = "fin", nullable = false) private Instant fin;
    @Enumerated(EnumType.STRING) @Column(name = "estado", nullable = false, length = 20) private EstadoCita estado;
    @Column(name = "duracion_minutos", nullable = false) private int duracionMinutos;
    @Column(name = "modalidad", nullable = false, length = 40) private String modalidad;
    @Column(name = "importe", nullable = false, precision = 12, scale = 2) private BigDecimal importe;
    @Column(name = "creada_por", nullable = false) private UUID creadaPor;
    @Column(name = "creada_en", nullable = false) private Instant creadaEn;
    @Column(name = "llegada_en") private Instant llegadaEn;
    @Column(name = "llegada_por") private UUID llegadaPor;
    @Column(name = "atencion_iniciada_en") private Instant atencionIniciadaEn;
    @Column(name = "atencion_iniciada_por") private UUID atencionIniciadaPor;
    @Column(name = "atencion_finalizada_en") private Instant atencionFinalizadaEn;
    @Column(name = "atencion_finalizada_por") private UUID atencionFinalizadaPor;
    @Enumerated(EnumType.STRING) @Column(name = "cierre_tipo", length = 20) private TipoCierreCita tipoCierre;
    @Column(name = "cierre_motivo", columnDefinition = "text") private String motivoCierre;
    @Column(name = "cerrada_en") private Instant cerradaEn;
    @Column(name = "cerrada_por") private UUID cerradaPor;
    @Version @Column(name = "version", nullable = false) private Long version;

    protected CitaJpaEntity() {}

    static CitaJpaEntity nueva(Cita cita) {
        var entidad = new CitaJpaEntity();
        entidad.id = cita.id();
        entidad.aplicar(cita);
        return entidad;
    }

    void aplicar(Cita cita) {
        pacienteId = cita.pacienteId(); profesionalId = cita.profesionalId(); servicioId = cita.servicioId();
        consultorioId = cita.consultorioId(); inicio = cita.horario().inicio(); fin = cita.horario().fin();
        estado = cita.estado(); duracionMinutos = cita.duracionMinutos(); modalidad = cita.modalidad();
        importe = cita.importe(); creadaPor = cita.creadaPor(); creadaEn = cita.creadaEn();
        llegadaEn = cita.llegadaEn(); llegadaPor = cita.llegadaPor();
        atencionIniciadaEn = cita.atencionIniciadaEn(); atencionIniciadaPor = cita.atencionIniciadaPor();
        atencionFinalizadaEn = cita.atencionFinalizadaEn(); atencionFinalizadaPor = cita.atencionFinalizadaPor();
        tipoCierre = cita.tipoCierre(); motivoCierre = cita.motivoCierre();
        cerradaEn = cita.cerradaEn(); cerradaPor = cita.cerradaPor();
    }

    long version() { return version == null ? 0 : version; }
    Cita toDomain() {
        return Cita.restaurar(id, pacienteId, profesionalId, servicioId, consultorioId,
                new HorarioCita(inicio, fin), duracionMinutos, modalidad, importe, creadaPor, creadaEn,
                estado, version(), llegadaEn, llegadaPor, atencionIniciadaEn, atencionIniciadaPor,
                atencionFinalizadaEn, atencionFinalizadaPor, tipoCierre, motivoCierre, cerradaEn, cerradaPor);
    }
}
