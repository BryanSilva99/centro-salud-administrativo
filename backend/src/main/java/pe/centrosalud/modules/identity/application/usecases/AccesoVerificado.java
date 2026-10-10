package pe.centrosalud.modules.identity.application.usecases;

import pe.centrosalud.modules.identity.domain.Cuenta;

public record AccesoVerificado(Cuenta cuenta, long versionAcceso) {}
