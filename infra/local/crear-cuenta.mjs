// Aprovisionamiento explícito de desarrollo; no es un endpoint de la aplicación.
import EmbeddedPostgres from 'embedded-postgres';
import { existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { randomBytes, randomUUID, pbkdf2Sync, timingSafeEqual } from 'node:crypto';
import { parseEnv } from 'node:util';
import { fileURLToPath } from 'node:url';
import { resolve } from 'node:path';

const args = process.argv.slice(2);
const rol = args.length === 0 ? null : args.length === 2 && args[0] === '--rol' ? args[1] : undefined;
if (rol === undefined || (rol !== null && !['REC', 'CAJ', 'ADM', 'PRO'].includes(rol))) throw new Error('Uso: npm run cuenta -- --rol REC|CAJ|ADM|PRO');

const root = fileURLToPath(new URL('../../', import.meta.url));
const envText = readFileSync(resolve(root, 'backend/.env'), 'utf8');
const env = parseEnv(envText);
// Spring admite un '#' dentro de un valor .properties sin comillas, mientras
// que parseEnv lo trata como comentario. Leer esta clave desde la línea cruda
// evita truncar la contraseña que usará el backend.
const passwordMatch = /^DB_PASSWORD=(.*)$/m.exec(envText);
const rawPassword = passwordMatch?.[1]?.trimEnd();
const databasePassword = rawPassword && rawPassword.length >= 2 &&
  ((rawPassword.startsWith('"') && rawPassword.endsWith('"')) ||
   (rawPassword.startsWith("'") && rawPassword.endsWith("'")))
  ? rawPassword.slice(1, -1)
  : rawPassword;
const dbUrl = /^jdbc:postgresql:\/\/(localhost|127\.0\.0\.1):5432\/([A-Za-z_][A-Za-z0-9_]*)$/.exec(env.DB_URL ?? '');
if (!dbUrl || !/^[A-Za-z_][A-Za-z0-9_]*$/.test(env.DB_USERNAME ?? '') || !databasePassword) {
  throw new Error('Esta herramienta solo admite una base PostgreSQL local en localhost/127.0.0.1:5432.');
}
const databaseName = dbUrl[2];
const data = resolve(root, '.data');
const file = resolve(data, 'cuenta-local.json');
mkdirSync(data, { recursive: true, mode: 0o700 });
if (!existsSync(file)) writeFileSync(file, JSON.stringify({ id: randomUUID(), identificador: 'bryan.local', contrasena: randomBytes(18).toString('base64url') }, null, 2) + '\n', { mode: 0o600, flag: 'wx' });
const account = JSON.parse(readFileSync(file, 'utf8'));
if (account.identificador !== 'bryan.local' || typeof account.contrasena !== 'string' || account.contrasena.length < 12) throw new Error('Archivo local de credenciales inválido.');
const postgres = new EmbeddedPostgres({ user: env.DB_USERNAME, password: databasePassword, port: 5432 });
const client = postgres.getPgClient(databaseName, '127.0.0.1');
const prefix = '{pbkdf2-sha256-v1}';
try {
  await client.connect();
  const salt = randomBytes(16);
  const derived = pbkdf2Sync(account.contrasena, salt, 600_000, 32, 'sha256');
  const hash = prefix + Buffer.concat([salt, derived]).toString('hex');
  await client.query('INSERT INTO cuentas (id, identificador, activa, contrasena_hash) VALUES ($1, $2, true, $3) ON CONFLICT DO NOTHING', [account.id, account.identificador, hash]);
  const { rows } = await client.query('SELECT id, activa, contrasena_hash FROM cuentas WHERE identificador = $1', [account.identificador]);
  const current = rows[0];
  if (!current || current.id !== account.id || !current.activa || !current.contrasena_hash?.startsWith(prefix)) throw new Error('La cuenta existente no coincide con este archivo local. No se modificó.');
  const stored = Buffer.from(current.contrasena_hash.slice(prefix.length), 'hex');
  if (stored.length !== 48 || !timingSafeEqual(stored.subarray(16), pbkdf2Sync(account.contrasena, stored.subarray(0, 16), 600_000, 32, 'sha256'))) throw new Error('La contraseña existente fue modificada. No se restableció automáticamente.');
  if (rol) {
    await client.query('INSERT INTO cuenta_roles(cuenta_id, rol) VALUES ($1, $2) ON CONFLICT DO NOTHING', [account.id, rol]);
    console.log(`Rol local asignado: ${rol}.`);
  }
  console.log('Cuenta local disponible: bryan.local. Credenciales en .data/cuenta-local.json (fuera de Git).');
} finally { await client.end(); }
