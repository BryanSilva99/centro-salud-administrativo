// Herramienta de desarrollo local; no forma parte del backend Java.
import EmbeddedPostgres from 'embedded-postgres';
import { existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { randomBytes } from 'node:crypto';
import { fileURLToPath } from 'node:url';
import { resolve } from 'node:path';
import { parseEnv } from 'node:util';
import { createServer } from 'node:net';

// Detectar otra instancia antes de tocar archivos o iniciar un segundo proceso.
await new Promise((resolvePort, reject) => {
  const probe = createServer();
  probe.once('error', () => reject(new Error('El puerto 5432 no está disponible. Si PostgreSQL ya está iniciado, usa esa instancia.')));
  probe.listen(5432, '127.0.0.1', () => probe.close(resolvePort));
});

const root = fileURLToPath(new URL('../../', import.meta.url));
const data = resolve(root, '.data');
const envPath = resolve(root, 'backend/.env');
const adminPath = resolve(data, 'postgres-admin.json');
const databaseDir = resolve(data, 'postgres');
mkdirSync(data, { recursive: true, mode: 0o700 });

if (!existsSync(envPath)) {
  writeFileSync(envPath, [
    'DB_URL=jdbc:postgresql://127.0.0.1:5432/centro_salud',
    'DB_USERNAME=centro_salud',
    `DB_PASSWORD=${randomBytes(24).toString('hex')}`,
    'SERVER_PORT=8080',
    'SESSION_COOKIE_SECURE=false',
    'APPOINTMENTS_MOCK_ENABLED=true',
    '',
  ].join('\n'), { mode: 0o600, flag: 'wx' });
}
const env = parseEnv(readFileSync(envPath, 'utf8'));
if (!/^jdbc:postgresql:\/\/(localhost|127\.0\.0\.1):5432\/centro_salud$/.test(env.DB_URL ?? '') ||
    env.DB_USERNAME !== 'centro_salud' || !env.DB_PASSWORD) {
  throw new Error('La herramienta requiere la base local centro_salud:5432 y DB_PASSWORD en backend/.env. No modifica configuraciones externas.');
}
if (!existsSync(adminPath)) {
  if (existsSync(resolve(databaseDir, 'PG_VERSION'))) {
    throw new Error('Faltan las credenciales del administrador local. Recuperar .data/postgres-admin.json sin borrar la base.');
  }
  writeFileSync(adminPath, JSON.stringify({ password: randomBytes(24).toString('hex') }), { mode: 0o600, flag: 'wx' });
}
const admin = JSON.parse(readFileSync(adminPath, 'utf8'));
const postgres = new EmbeddedPostgres({
  databaseDir,
  user: 'centro_local_admin',
  password: admin.password,
  port: 5432,
  persistent: true,
  authMethod: 'scram-sha-256',
  postgresFlags: ['-h', '127.0.0.1', '-k', ''],
  onLog: () => {},
  onError: () => {},
});

try {
  if (!existsSync(resolve(databaseDir, 'PG_VERSION'))) await postgres.initialise();
  await postgres.start();
  const client = postgres.getPgClient('postgres', '127.0.0.1');
  await client.connect();
  try {
    const role = await client.query("SELECT 1 FROM pg_roles WHERE rolname = 'centro_salud'");
    if (!role.rowCount) {
      const ddl = await client.query("SELECT format('CREATE ROLE centro_salud LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE PASSWORD %L', $1::text) AS sql", [env.DB_PASSWORD]);
      await client.query(ddl.rows[0].sql);
    }
    const db = await client.query("SELECT 1 FROM pg_database WHERE datname = 'centro_salud'");
    if (!db.rowCount) await client.query('CREATE DATABASE centro_salud OWNER centro_salud');
  } finally {
    await client.end();
  }
  // Comprobar la misma cuenta que usará Spring Boot, sin imprimir secretos.
  const app = new EmbeddedPostgres({ user: env.DB_USERNAME, password: env.DB_PASSWORD, port: 5432 });
  const check = app.getPgClient('centro_salud', '127.0.0.1');
  try {
    await check.connect();
    const result = await check.query('SELECT current_database() AS db, current_user AS usuario');
    console.log(`PostgreSQL listo: 127.0.0.1:5432 / ${result.rows[0].db} / ${result.rows[0].usuario}`);
  } finally {
    await check.end();
  }
  console.log('Mantén esta terminal abierta. Ctrl+C detiene PostgreSQL y conserva los datos.');
} catch (error) {
  await postgres.stop();
  console.error(`No se pudo iniciar/verificar PostgreSQL (${error.code ?? 'error local'}). Revisa el puerto 5432, las dependencias y backend/.env; no borres .data.`);
  process.exitCode = 1;
}
