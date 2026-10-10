# Implementación del Módulo de Caja — J.J.

**Fecha:** 10 de octubre de 2026  
**Rama:** `feature/billing-jj`  
**Responsable:** J.J.

---

## 1. Alcance implementado

| RF | Descripción | Estado |
|----|-------------|--------|
| RF-19 | Registrar cargo por servicio | Implementado |
| RF-20 | Abrir sesión de caja | Implementado |
| RF-21 | Registrar pagos (parciales/totales) | Implementado |
| RF-22 | Consultar saldos y movimientos | Implementado |
| RF-23 | Solicitar y resolver anulaciones económicas | Implementado |
| RF-24 | Cerrar caja y conciliar | Implementado |
| RF-27 | Reporte económico | Implementado |

---

## 2. Arquitectura implementada

Se aplicó arquitectura hexagonal (puertos y adaptadores) siguiendo el patrón establecido en el módulo de identidad:

```
┌─────────────────────────────────────────────────────────────┐
│                    Adaptadores HTTP                          │
│              BillingController (Spring Boot)                 │
└─────────────────────────┬───────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────┐
│                    Casos de Uso                              │
│  RegistrarCargo, AbrirSesionCaja, RegistrarPago,            │
│  ConsultarSaldo, SolicitarReversion, AprobarReversion,       │
│  CerrarCaja, ConsultarReporteEconomico                       │
└─────────────────────────┬───────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────┐
│                    Puertos (Interfaces)                      │
│  CargoRepository, PagoRepository, SesionCajaRepository,      │
│  SolicitudReversionRepository                                │
└─────────────────────────┬───────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────┐
│                    Dominio (Java puro)                       │
│  Cargo, Pago, SesionCaja, SolicitudReversion                 │
│  Enums: EstadoCargo, EstadoSesionCaja, etc.                 │
└─────────────────────────────────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────┐
│              Adaptadores PostgreSQL                         │
│  Entidades JPA + Repositorios Spring Data                   │
└─────────────────────────────────────────────────────────────┘
```

---

## 3. Entidades del dominio

### Cargo
- **Campos:** `id`, `citaId`, `pacienteId`, `importe`, `estado`
- **Estados:** `PENDIENTE`, `PARCIAL`, `PAGADO`, `ANULADO`
- **Reglas:** Importe no negativo; un cargo vigente por cita

### Pago
- **Campos:** `id`, `cargoId`, `sesionCajaId`, `importe`, `medio`, `referencia`, `tipo`, `fechaRegistro`
- **Tipos:** `COBRO`, `DEVOLUCION`
- **Reglas:** Importe positivo; vinculado a sesión abierta

### SesionCaja
- **Campos:** `id`, `cajeroId`, `fondoInicial`, `estado`, `fechaApertura`, `fechaCierre`, `efectivoDeclarado`, `diferencia`, `motivoDiferencia`
- **Estados:** `ABIERTA`, `CERRADA`
- **Reglas:** Fondo inicial no negativo; una sola sesión abierta por cajero

### SolicitudReversion
- **Campos:** `id`, `operacionId`, `operacionTipo`, `solicitanteId`, `motivo`, `estado`, `aprobadorId`, `fechaSolicitud`, `fechaResolucion`
- **Estados:** `PENDIENTE`, `APROBADA`, `RECHAZADA`
- **Reglas:** El solicitante no puede aprobar su propia solicitud

---

## 4. Casos de uso

| Caso de uso | Descripción | Reglas aplicadas |
|-------------|-------------|------------------|
| `RegistrarCargo` | Crea un cargo vinculado a cita y paciente | Un cargo vigente por cita; importe positivo |
| `AbrirSesionCaja` | Abre una nueva sesión de caja | Una sola sesión abierta por cajero; fondo no negativo |
| `RegistrarPago` | Registra un pago parcial o total | No exceder saldo; sesión debe estar abierta |
| `ConsultarSaldo` | Consulta saldo y movimientos de un cargo | Calcula saldo = importe - pagos + reversiones |
| `SolicitarReversion` | Solicita anulación de cargo o pago | Motivo obligatorio |
| `AprobarReversion` | Aprueba o rechaza una solicitud | Solicitante ≠ aprobador; solo una resolución |
| `CerrarCaja` | Cierra sesión y concilia efectivo | Calcula diferencia; motivo si hay diferencia |
| `ConsultarReporteEconomico` | Genera reporte por período | Ingresos netos = cobros - devoluciones |

---

## 5. Endpoints de la API

| Método | Ruta | Descripción | Rol |
|--------|------|-------------|-----|
| POST | `/api/billing/cargos` | Registrar cargo | CAJ |
| POST | `/api/billing/sesiones` | Abrir sesión de caja | CAJ |
| POST | `/api/billing/pagos` | Registrar pago | CAJ |
| GET | `/api/billing/cargos/{cargoId}/saldo` | Consultar saldo | CAJ, ADM |
| POST | `/api/billing/reversiones` | Solicitar reversión | CAJ |
| POST | `/api/billing/reversiones/{solicitudId}/resolver` | Resolver reversión | ADM |
| POST | `/api/billing/sesiones/{sesionId}/cerrar` | Cerrar caja | CAJ |
| GET | `/api/billing/reportes/economico` | Reporte económico | CAJ, ADM |

---

## 6. Migración de base de datos

**Archivo:** `V2__billing_tables.sql`

**Tablas creadas:**
- `cargos` — Cargos por servicio
- `sesiones_caja` — Sesiones de caja
- `pagos` — Pagos y devoluciones
- `solicitudes_reversion` — Solicitudes de anulación

**Índices:**
- `idx_cargos_cita`, `idx_cargos_paciente`
- `idx_pagos_cargo`, `idx_pagos_sesion`, `idx_pagos_fecha`
- `idx_sesiones_cajero_estado`
- `idx_solicitudes_solicitante`

---

## 7. Frontend

**Archivos:**
- `frontend/src/features/billing/api/billing.ts` — Cliente HTTP para la API de billing
- `frontend/src/features/billing/components/BillingView.tsx` — Componente principal con formularios

**Pantallas implementadas:**
- Registrar Cargo
- Abrir Sesión de Caja
- Registrar Pago
- Consultar Saldo
- Solicitar Reversión
- Cerrar Caja
- Reporte Económico

---

## 8. Seguridad

Las rutas de billing están protegidas en `SecurityConfiguration.java`:

```java
.requestMatchers("/api/billing/**").hasAnyAuthority("ROLE_CAJ", "ROLE_ADM")
```

Solo usuarios con rol `CAJ` (Caja) o `ADM` (Administrador) pueden acceder a los endpoints del módulo.

---

## 9. Pendientes y observaciones

1. **Concurrencia:** Se requiere implementar bloqueos optimistas/pesimistas para pagos concurrentes (RNF-03)
2. **Idempotencia:** Las operaciones de pago y cierre deben incluir clave de idempotencia
3. **Auditoría:** Las operaciones deben registrarse en el módulo de auditoría (RF-28)
4. **Validaciones adicionales:** Verificar que la cita existe y pertenece al paciente antes de generar cargo
5. **Manejo de errores:** Mejorar respuestas de error con códigos específicos
6. **Pruebas:** Se requieren pruebas unitarias e integración para todos los casos de uso

---

## 10. Commits

- `333894b` — feat(billing): implementar modulo de caja - cargos, pagos, anulaciones, cierre y reporte
