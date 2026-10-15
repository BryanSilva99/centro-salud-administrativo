package pe.centrosalud.modules.appointments.application.usecases;

public final class VersionCitaObsoleta extends RuntimeException {
    public VersionCitaObsoleta() { super("La cita cambió mientras se editaba; consulta la agenda nuevamente"); }
}
