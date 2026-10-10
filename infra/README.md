# Infraestructura

`local/` contiene dos alternativas para PostgreSQL de desarrollo:

- `npm ci` y `npm run db` dentro de `infra/local`: herramienta local verificada en Linux x64, PostgreSQL 18.4. Ctrl+C detiene el proceso sin borrar datos.
- `compose.yml`: alternativa con Docker; no ejecutada en este equipo.

Usar una sola alternativa en el puerto 5432. Los datos de la herramienta local se conservan en `.data/postgres`, fuera de Git. Credenciales en `backend/.env` y `.data/postgres-admin.json`, también excluidas. Instrucciones completas en [Inicio del desarrollo](../docs/INICIO_DESARROLLO.md).

`deployment/` y `observability/` siguen reservadas para trabajo futuro. No hay despliegue de la aplicación ni de SigNoz implementado.
