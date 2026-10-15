package pe.centrosalud.modules.appointments.application.usecases;

import pe.centrosalud.modules.appointments.application.ports.CatalogoCitas;
import pe.centrosalud.modules.appointments.application.ports.PerfilCitas;

public record ContextoCitas(CatalogoCitas.Contexto catalogo, PerfilCitas perfil) {}
