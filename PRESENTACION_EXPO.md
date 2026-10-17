# Presentación Expo — Centro de Salud Administrativo
**Tiempo estimado:** 20 minutos  
**Formato:** Guion para Canva (cada "Diapositiva" es una slide)

---

## Diapositiva 1 — Portada

**Título:** Sistema Administrativo para Centro de Salud  
**Subtítulo:** Gestión integral de pacientes, citas, caja y recursos  
**Equipo:** Bryan, Oscar, Percy, J.J.  
**Fecha:** Octubre 2026  

---

## Diapositiva 2 — Descripción del Problema

**Título:** El problema

Los centros de salud manejan información dispersa:
- Papeles para citas y registros
- No hay control de pagos ni cierre de caja
- Los pacientes esperan mucho por desorganización
- No hay trazabilidad de quién hizo cada operación

**Conclusión:** Necesitamos un sistema que centralice toda la operación administrativa.

---

## Diapositiva 3 — Descripción del Proyecto

**Título:** Nuestro solución

Un sistema web interno que integra:
- Gestión de pacientes y personal
- Programación de citas y atención
- Caja con cargos, pagos y cierre
- Reportes y auditoría

**Beneficios:**
- Información centralizada
- Acceso por roles (recepción, caja, administrador, profesional)
- Trazabilidad de operaciones

---

## Diapositiva 4 — Arquitectura del Sistema

**Título:** Cómo está construido

**Stack:**
- Frontend: React
- Backend: Java Spring Boot
- Base de datos: PostgreSQL
- Observabilidad: SigNoz

**Patrón:** Monolito modular con arquitectura hexagonal
- Dominio independiente de frameworks
- Puertos y adaptadores
- Módulos: identidad, administración, citas, caja, reportes

---

## Diapositiva 5 — Contexto del Sistema (ADD)

**Título:** Contexto del sistema

**Actores:**
- Recepción/Admisión (REC): pacientes, citas, llegadas
- Caja (CAJ): cargos, pagos, cierres
- Administrador (ADM): configuración, usuarios, reportes
- Profesional de salud (PRO): agenda, atención

**El paciente NO es usuario** del sistema.

---

## Diapositiva 6 — Módulos del Sistema

**Título:** Módulos implementados

| Módulo | Responsable | Funcionalidad |
|--------|-------------|---------------|
| Identidad y acceso | Bryan | Login, roles, permisos, auditoría |
| Administración | Oscar | Pacientes, profesionales, servicios, consultorios, horarios |
| Citas y atención | Percy | Reservas, reprogramaciones, llegadas, atención |
| Caja | J.J. | Cargos, pagos, anulaciones, cierre, reportes |

---

## Diapositiva 7 — Principales Requerimientos del Negocio

**Título:** Lo que el negocio necesita (P8)

1. **Control de acceso** — Cada rol solo ve lo que necesita
2. **Citas sin conflictos** — Un profesional no puede tener dos citas a la vez
3. **Caja con conciliación** — Al cerrar, el efectivo debe cuadrar
4. **Sin duplicados** — Un paciente no se registra dos veces
5. **Trazabilidad** — Todo queda registrado con quién, cuándo y qué

---

## Diapositiva 8 — Requerimientos Funcionales Clave

**Título:** RF implementados

- **RF-19 a RF-24:** Caja completa (cargos, pagos, saldos, anulaciones, cierre)
- **RF-27:** Reporte económico
- **RF-04 a RF-10:** Pacientes, profesionales, servicios, consultorios, disponibilidad
- **RF-12 a RF-18:** Citas, reprogramaciones, llegadas, atención

---

## Diapositiva 9 — Reglas de Negocio Críticas

**Título:** Reglas que el sistema aplica

- **RN-07:** No se pueden reservar citas que se crucen (mismo paciente, profesional o consultorio)
- **RN-13:** Los pagos no pueden exceder el saldo pendiente
- **RN-14:** El solicitante de anulación no puede aprobarla
- **RN-15:** Una sola caja abierta por cajero; el cierre es inmutable

---

## Diapositiva 10 — Tendencias Seleccionadas

**Título:** 6 Tendencias tecnológicas

| # | Tendencia | Categoría |
|---|-----------|-----------|
| 1 | Zero Trust Architecture | Techniques |
| 2 | Mutation Testing | Techniques |
| 3 | OpenAI Codex | Tools |
| 4 | Axe-core | Tools |
| 5 | SigNoz | Platforms |
| 6 | React JS | Languages and Frameworks |

**Distribución:** 2 + 2 + 1 + 1

---

## Diapositiva 11 — Zero Trust Architecture

**Título:** Zero Trust Architecture

**Qué es:** Verificar cada operación, sin confiar en la red ni en el navegador.

**Cómo lo implementamos:**
- Autenticación por sesión con cookies seguras (HttpOnly, SameSite, Secure)
- Autorización por rol Y permiso en cada endpoint
- CSRF tokens en operaciones de escritura
- Límite de intentos de login (5 en 15 minutos)
- El servidor valida todo, no confía en el cliente

---

## Diapositiva 12 — Mutation Testing

**Título:** Mutation Testing

**Qué es:** Verificar que las pruebas detectan errores. Se introduce código con errores (mutantes) y se comprueba que las pruebas fallan.

**Cómo lo aplicamos:**
- Pruebas de reglas críticas: superposición de citas, saldos, anulaciones
- Si una mutación sobrevive, la prueba es insuficiente
- Herramienta: PIT (pendiente de ejecutar)

---

## Diapositiva 13 — OpenAI Codex y Axe-core

**Título:** Herramientas de desarrollo

**OpenAI Codex:**
- Asistió en la generación de código del módulo de caja
- Se usó con revisión humana
- Evidencia: commits y documentación en docs/

**Axe-core:**
- Detecta problemas de accesibilidad en React
- Se integra en el build del frontend
- Verifica etiquetas, foco visible, contraste

---

## Diapositiva 14 — SigNoz y React JS

**Título:** Plataforma y framework

**SigNoz:**
- Observabilidad: métricas, logs y trazas
- Correlación de operaciones de reserva y pago
- No bloquea el negocio si se cae

**React JS:**
- Interfaz interna para los 4 roles
- Componentes reutilizables
- Navegación por permisos

---

## Diapositiva 15 — Matriz de Tendencias vs Requerimientos

**Título:** Tendencias ↔ Requerimientos

| Tendencia | RF-01 a 03 | RF-04 a 10 | RF-12 a 18 | RF-19 a 24 | RF-27 | RNF-01 |
|-----------|------------|------------|------------|------------|-------|--------|
| Zero Trust | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| Mutation Testing | — | ✓ | ✓ | ✓ | ✓ | ✓ |
| OpenAI Codex | — | — | ✓ | ✓ | ✓ | — |
| Axe-core | — | — | — | — | — | — |
| SigNoz | — | — | ✓ | ✓ | ✓ | ✓ |
| React JS | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |

---

## Diapositiva 16 — Desarrollo del ADD: Contexto

**Título:** ADD — Contexto del sistema

**Sistema:** Administración integral de un centro de salud

**Contenedores:**
1. Aplicación web (React)
2. API Spring Boot (monolito modular)
3. PostgreSQL
4. SigNoz (observabilidad)

**Restricciones:**
- Sin historia clínica
- Sin portal de paciente
- Sin facturación electrónica
- Una sola sede

---

## Diapositiva 17 — Desarrollo del ADD: Arquitectura del Software

**Título:** ADD — Arquitectura del software

**Patrón:** Monolito modular con arquitectura hexagonal

```
HTTP (Controladores) → Casos de Uso → Dominio
                                    ↓
                              Puertos
                                    ↓
                         Adaptadores (PostgreSQL, SigNoz)
```

**Decisiones:**
- DA-01: Monolito modular (transacciones que abarcan módulos)
- DA-02: Arquitectura hexagonal (dominio independiente)
- DA-04: Una transacción por operación de negocio
- DA-05: Movimientos compensatorios, nunca borrado

---

## Diapositiva 18 — Desarrollo del ADD: Vistas del Sistema

**Título:** ADD — Vistas del sistema

**Vista Lógica:**
- Módulo de Identidad
- Módulo de Administración
- Módulo de Citas
- Módulo de Caja
- Módulo de Reportes

**Vista de Despliegue:**
- Servidor web → API → PostgreSQL
- API → SigNoz (telemetría asíncrona)
- Sin estado local en memoria

---

## Diapositiva 19 — Ejecución del Software

**Título:** Demostración en vivo

**Flujos a mostrar:**
1. Login con rol de caja
2. Abrir sesión de caja
3. Registrar cargo por servicio
4. Registrar pago parcial y total
5. Consultar saldo
6. Cerrar caja con conciliación
7. Ver reporte económico

**Comando para levantar:**
```bash
docker compose up --build
```

---

## Diapositiva 20 — Conclusiones del Uso de Tendencias

**Título:** Conclusiones

**Zero Trust:**
- Garantiza que cada operación está autorizada
- Protege datos sensibles del centro

**Mutation Testing:**
- Asegura que las pruebas realmente verifican las reglas críticas

**OpenAI Codex:**
- Acelera el desarrollo con revisión humana

**Axe-core:**
- Garantiza accesibilidad para todo el personal

**SigNoz:**
- Permite diagnosticar problemas sin afectar operaciones

**React JS:**
- Interfaz moderna y mantenible

---

## Diapositiva 21 — Cierre

**Título:** Gracias

**Preguntas y respuestas**

**Equipo:**
- Bryan: Identidad, usuarios, roles
- Oscar: Administración, recursos
- Percy: Citas, atención
- J.J.: Caja, pagos, cierre
