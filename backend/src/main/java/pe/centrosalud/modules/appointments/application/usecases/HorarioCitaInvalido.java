package pe.centrosalud.modules.appointments.application.usecases;

public final class HorarioCitaInvalido extends RuntimeException {
    public HorarioCitaInvalido(String mensaje) { super(mensaje); }
}
