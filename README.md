# Sistema administrativo para un centro de salud

**Estado: esqueleto arquitectónico. No contiene funcionalidades implementadas.**

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
- API prevista: FastAPI, dentro de un único backend modular.
- Persistencia prevista: PostgreSQL mediante adaptadores.
- Observabilidad prevista: SigNoz; separada de la auditoría del negocio.
- Reglas y casos de uso independientes de las pantallas y de la base de datos.

Estas decisiones todavía no están implementadas. No hay dependencias instalables declaradas, comandos de arranque, servidores ni pruebas ejecutables en este repositorio.

## Documentación

1. [Contexto y problema](docs/01_CONTEXTO_Y_PROBLEMA.md).
2. [Requerimientos y reglas de negocio](docs/02_REQUERIMIENTOS_DEL_SISTEMA.md).
3. [Matriz de tendencias y requerimientos](docs/03_MATRIZ_TENDENCIAS_REQUERIMIENTOS.md).
4. [ADD — Primera iteración](docs/05_ADD_ITERACION_1.md).
5. [Requisitos de la presentación parcial](docs/PRESENTACION_PARCIAL.md).

Se excluyen los PDF del profesor, respaldos locales, credenciales y datos de demostración. El siguiente paso requiere una indicación expresa del usuario sobre qué parte implementar.
