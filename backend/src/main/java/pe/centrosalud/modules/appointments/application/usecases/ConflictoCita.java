package pe.centrosalud.modules.appointments.application.usecases;

public final class ConflictoCita extends RuntimeException {
    public ConflictoCita() { super("El horario se cruza con otra cita del paciente, profesional o consultorio"); }
}
