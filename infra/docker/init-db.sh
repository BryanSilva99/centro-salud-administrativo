#!/bin/sh
set -eu
# Solo se ejecuta cuando PostgreSQL inicializa un volumen nuevo.
# Las variables de psql se citan como literales SQL; no se concatena la contraseña.
psql --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" \
    --set=ON_ERROR_STOP=1 --set=app_password="$DB_PASSWORD" <<'SQL'
CREATE ROLE centro_salud LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE PASSWORD :'app_password';
ALTER DATABASE centro_salud OWNER TO centro_salud;
ALTER SCHEMA public OWNER TO centro_salud;
SQL
