# Propuesta funcional vigente

Actualizada el 6 de octubre de 2026.

## Objetivo
Integrar la gestión administrativa y operativa interna de un centro de salud de una sede.

## Módulos

### Pacientes
Registro, actualización, búsqueda, control de duplicidad e historial administrativo.

### Personal y disponibilidad
Profesionales, servicios habilitados, turnos, ausencias y resolución de citas afectadas.

### Servicios y consultorios
Catálogo de prestaciones, duración, modalidades económicas, tarifas y consultorios compatibles; bloqueos de disponibilidad.

### Citas y admisión
Programación, reprogramación y cancelación; prevención de conflictos; llegada, espera, inicio, finalización y ausencia.

### Caja
Cargos cuando corresponda, pagos parciales o totales, saldos, reversiones autorizadas y cierre con conciliación.

### Reportes y supervisión
Dashboard operativo, ingresos, saldos y cierres. Reportes detallados y exportación CSV para la entrega final.

### Usuarios y auditoría
Cuentas y roles, acceso según responsabilidades y trazabilidad de operaciones relevantes.

## Roles
Recepción/admisión, caja, administrador del centro y profesional de salud. El paciente no es usuario directo.

## Alcance
Incluye caja y auditoría desde el núcleo. Reportes detallados y exportación CSV se priorizan para la entrega final. Insumos permanece como posible ampliación. Sin portal de pacientes, historia clínica, diagnóstico, farmacia, facturación electrónica, integración de seguros ni multisede.

## Seis tendencias

- **Zero trust architecture** (Técnicas): Identidad, mínimo privilegio y permisos por operación y registro.
- **Mutation testing** (Técnicas): Comprobar que las pruebas detectan errores en reservas, saldos, aprobaciones y cierre.
- **OpenAI Codex** (Herramientas): Desarrollo asistido con revisión humana y evidencia de implementación.
- **Axe-core** (Herramientas): Detectar problemas de accesibilidad en la interfaz; complementar con revisión manual.
- **SigNoz** (Plataformas): Métricas, logs y trazas para investigar errores y tiempos de respuesta.
- **React JS** (Lenguajes y frameworks): Interfaz interna para pacientes, agenda, admisión, caja y reportes.

## Base tecnológica
React + FastAPI + PostgreSQL + SigNoz. Arquitectura hexagonal y diseño nativo para nube.
Selección confirmada; aplicación pendiente. Véanse [contexto](01_CONTEXTO_Y_PROBLEMA.md), [requerimientos](02_REQUERIMIENTOS_DEL_SISTEMA.md) y [matriz](03_MATRIZ_TENDENCIAS_REQUERIMIENTOS.md).
