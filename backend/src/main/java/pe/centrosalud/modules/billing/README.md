# Caja y movimientos económicos

Integrada mediante `CajaIntegrada`: autorización actual, citas/pacientes reales, transacciones y reintentos. El controlador solo traduce solicitudes de la sesión autenticada; los casos de uso conservan el dominio sin Spring.

Los adaptadores de `outbound/modules/billing` consultan contratos de identidad/administración/citas. `RegistroCajaPostgres` coordina la transacción y el bloqueo común de caja; los repositorios persisten exclusivamente tablas de caja. V18 añade el esquema integrado.

Ver `docs/INTEGRACION_CAJA.md` para decisiones, comprobaciones y límites. No afirmar cumplimiento completo de RF-19–24/27 sin considerar esos pendientes.
