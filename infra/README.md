# Infraestructura

`local/` contiene dos alternativas para PostgreSQL de desarrollo:

- `npm ci` y `npm run db` dentro de `infra/local`: herramienta local verificada en Linux x64, PostgreSQL 18.4. Ctrl+C detiene el proceso sin borrar datos.
- `compose.yml`: alternativa con Docker; no ejecutada en este equipo.

Usar una sola alternativa en el puerto 5432. Los datos de la herramienta local se conservan en `.data/postgres`, fuera de Git. Credenciales en `backend/.env` y `.data/postgres-admin.json`, también excluidas. Instrucciones completas en [Inicio del desarrollo](../docs/INICIO_DESARROLLO.md).

La configuración `compose.yaml` de la raíz ejecuta frontend, backend y PostgreSQL para pruebas del equipo. Ver [Docker](../docs/DOCKER.md) para configurar, construir, crear el primer administrador y conservar los datos. `infra/docker` contiene la inicialización del usuario de base y la herramienta de aprovisionamiento explícito.

`infra/local/compose.yml` conserva su función original: únicamente PostgreSQL para quienes ejecuten Java/React en el host. No mezclar sus volúmenes con el entorno completo.

El despliegue público y SigNoz siguen pendientes.
