package pe.centrosalud.modules.appointments.application.usecases;

import pe.centrosalud.modules.appointments.application.ports.CatalogoCitas;
import pe.centrosalud.modules.appointments.domain.Cita;

/** Proyección de una cita enriquecida con nombres del catálogo vigente. */
public record CitaDetalle(Cita cita, CatalogoCitas.Paciente paciente,
                          CatalogoCitas.Profesional profesional,
                          CatalogoCitas.Servicio servicio,
                          CatalogoCitas.Consultorio consultorio) {}
