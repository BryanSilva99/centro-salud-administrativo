package pe.centrosalud.adapters.outbound.mock.appointments;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import pe.centrosalud.modules.appointments.application.ports.CatalogoCitas;

/** Catálogo sintético y determinista para el primer recorrido local. */
public final class CatalogoCitasMock implements CatalogoCitas {
    public static final UUID PACIENTE_1 = UUID.fromString("10000000-0000-4000-8000-000000000001");
    public static final UUID PACIENTE_2 = UUID.fromString("10000000-0000-4000-8000-000000000002");
    public static final UUID PACIENTE_3 = UUID.fromString("10000000-0000-4000-8000-000000000003");
    public static final UUID PROFESIONAL_1 = UUID.fromString("20000000-0000-4000-8000-000000000001");
    public static final UUID PROFESIONAL_2 = UUID.fromString("20000000-0000-4000-8000-000000000002");
    public static final UUID SERVICIO_1 = UUID.fromString("30000000-0000-4000-8000-000000000001");
    public static final UUID SERVICIO_2 = UUID.fromString("30000000-0000-4000-8000-000000000002");
    public static final UUID CONSULTORIO_1 = UUID.fromString("40000000-0000-4000-8000-000000000001");
    public static final UUID CONSULTORIO_2 = UUID.fromString("40000000-0000-4000-8000-000000000002");

    private final Contexto contexto = new Contexto(
            List.of(new Paciente(PACIENTE_1, "Paciente Demo 01", true),
                    new Paciente(PACIENTE_2, "Paciente Demo 02", true),
                    new Paciente(PACIENTE_3, "Paciente Demo 03", true)),
            List.of(new Profesional(PROFESIONAL_1, "Profesional Demo 01", "Medicina general", true),
                    new Profesional(PROFESIONAL_2, "Profesional Demo 02", "Nutrición", true)),
            List.of(new Servicio(SERVICIO_1, "Consulta médica general", true, 30,
                            List.of(new Modalidad("PARTICULAR", "Particular", new BigDecimal("50.00")),
                                    new Modalidad("SIN_CARGO", "Sin cargo", new BigDecimal("0.00")))),
                    new Servicio(SERVICIO_2, "Orientación nutricional", true, 45,
                            List.of(new Modalidad("PARTICULAR", "Particular", new BigDecimal("60.00"))))),
            List.of(new Consultorio(CONSULTORIO_1, "Consultorio Demo 01", true),
                    new Consultorio(CONSULTORIO_2, "Consultorio Demo 02", true)));

    @Override public Optional<Paciente> paciente(UUID id) { return contexto.pacientes().stream().filter(x -> x.id().equals(id)).findFirst(); }
    @Override public Optional<Profesional> profesional(UUID id) { return contexto.profesionales().stream().filter(x -> x.id().equals(id)).findFirst(); }
    @Override public Optional<Servicio> servicio(UUID id) { return contexto.servicios().stream().filter(x -> x.id().equals(id)).findFirst(); }
    @Override public Optional<Consultorio> consultorio(UUID id) { return contexto.consultorios().stream().filter(x -> x.id().equals(id)).findFirst(); }
    @Override public Contexto contexto() { return contexto; }
}
