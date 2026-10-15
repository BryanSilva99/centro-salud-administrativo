# Sistema administrativo para un centro de salud

**Estado: base técnica, identidad citas implementadas. Login, sesión, agenda diaria, programación, reprogramación, llegada, atención, cancelación, ausencia, retiro de espera y dashboard operativo están disponibles con catálogo demo explícito; vista semanal, caja y auditoría global siguen pendientes.**

Sistema interno para gestionar pacientes, recursos, citas, admisión, seguimiento administrativo de atención, caja, reportes y auditoría. Se contemplan dos grupos de usuarios y cuatro roles: recepción/admisión, caja, administrador y profesional de salud.

## Organización

```text
backend/       Monolito modular con arquitectura hexagonal
frontend/      Estructura prevista de la aplicación React
infra/         Ubicación futura del despliegue y observabilidad
docs/          Contexto, requerimientos, ADD y mapa del código
```

La guía para revisar el proyecto es [Organización del código](docs/ORGANIZACION_DEL_CODIGO.md). Explica qué irá en cada carpeta, las dependencias permitidas y su relación con el ADD.

## Decisiones de arquitectura

- Interfaz prevista: React.
- API prevista: Java con Spring Boot, dentro de un único backend modular.
- Persistencia prevista: PostgreSQL mediante adaptadores.
- Observabilidad prevista: SigNoz; separada de la auditoría del negocio.
- Reglas y casos de uso independientes de las pantallas y de la base de datos.

Las dependencias y puntos de arranque están preparados. Consulta [Inicio del desarrollo](docs/INICIO_DESARROLLO.md) para instalar, compilar y configurar el entorno. La autenticación HTTP por sesión está implementada; el formulario React ya está conectado al login. Los recorridos actuales de `appointments` requieren `APPOINTMENTS_MOCK_ENABLED=true` para usar sus referencias sintéticas. Roles e integración con los módulos externos siguen pendientes. Ver [recorrido de Cuenta](docs/CUENTA_PASO_1.md).

## Documentación

1. [Contexto y problema](docs/01_CONTEXTO_Y_PROBLEMA.md).
2. [Requerimientos y reglas de negocio](docs/02_REQUERIMIENTOS_DEL_SISTEMA.md).
3. [Matriz de tendencias y requerimientos](docs/03_MATRIZ_TENDENCIAS_REQUERIMIENTOS.md).
4. [ADD — Primera iteración](docs/05_ADD_ITERACION_1.md).
5. [Requisitos de la presentación parcial](docs/PRESENTACION_PARCIAL.md).

Se excluyen los PDF del profesor, respaldos locales, credenciales y datos demo persistentes. Las fixtures de appointments solo se activan explícitamente en desarrollo y no representan datos reales.

Paso actual de identidad: [Verificación de credenciales](docs/CREDENCIALES_PASO_2.md). El paso posterior de sesiones está disponible en [Login con sesión persistida](docs/LOGIN_PASO_3.md).

Interfaz y cuenta local de prueba: [Login React, paso 4](docs/LOGIN_PASO_4_REACT.md).
