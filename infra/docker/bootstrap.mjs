// Herramienta explícita de instalación local; no forma parte de la API de negocio.
import { pbkdf2Sync, randomBytes, randomUUID, timingSafeEqual } from 'node:crypto';
import pg from 'pg';

const identifier = (process.env.ADMIN_IDENTIFIER ?? 'admin.local').trim();
const password = process.env.ADMIN_PASSWORD;
if (!/^[\p{L}\p{N}._@-]{1,254}$/u.test(identifier)) {
  throw new Error('ADMIN_IDENTIFIER no es válido.');
}
if (!password || !password.trim() || [...password].length < 12 || [...password].length > 128) {
  throw new Error('Configura ADMIN_PASSWORD con entre 12 y 128 caracteres en .env.');
}
const prefix = '{pbkdf2-sha256-v1}';
const salt = randomBytes(16);
const hash = prefix + Buffer.concat([salt, pbkdf2Sync(password, salt, 600_000, 32, 'sha256')]).toString('hex');
const client = new pg.Client({
  host: process.env.DB_HOST ?? 'postgres',
  port: Number(process.env.DB_PORT ?? 5432),
  database: process.env.DB_NAME ?? 'centro_salud',
  user: process.env.DB_USERNAME ?? 'centro_salud',
  password: process.env.DB_PASSWORD,
  connectionTimeoutMillis: 10_000,
});
try {
  await client.connect();
  await client.query('BEGIN');
  // Coordina este aprovisionamiento con las operaciones de identidad existentes.
  await client.query('SELECT id FROM control_administracion WHERE id=1 FOR UPDATE');
  const existing = await client.query('SELECT id, activa, contrasena_hash FROM cuentas WHERE identificador=$1', [identifier]);
  if (existing.rowCount) {
    const account = existing.rows[0];
    const roles = await client.query("SELECT 1 FROM cuenta_roles WHERE cuenta_id=$1 AND rol='ADM'", [account.id]);
    const stored = account.contrasena_hash?.startsWith(prefix) ? Buffer.from(account.contrasena_hash.slice(prefix.length), 'hex') : Buffer.alloc(0);
    if (!account.activa || !roles.rowCount || stored.length !== 48 ||
        !timingSafeEqual(stored.subarray(16), pbkdf2Sync(password, stored.subarray(0, 16), 600_000, 32, 'sha256'))) {
      throw new Error('La cuenta existente no coincide con la configuración. No se modifica ni se restablece su contraseña.');
    }
    console.log('La cuenta ADM configurada ya existe y se verificó.');
  } else {
    const admins = await client.query("SELECT 1 FROM cuentas c JOIN cuenta_roles r ON r.cuenta_id=c.id WHERE c.activa AND r.rol='ADM' LIMIT 1");
    if (admins.rowCount) throw new Error('Ya existe un administrador activo. Crea las otras cuentas desde Usuarios y roles.');
    const id = randomUUID();
    await client.query('INSERT INTO cuentas(id,identificador,activa,contrasena_hash) VALUES ($1,$2,true,$3)', [id, identifier, hash]);
    await client.query("INSERT INTO cuenta_roles(cuenta_id,rol) VALUES ($1,'ADM')", [id]);
    await client.query("INSERT INTO auditoria_cuentas(id,actor_id,cuenta_id,tipo,identificador_nuevo,activa_nueva,roles,fecha) VALUES ($1,$2,$2,'CREACION',$3,true,ARRAY['ADM']::varchar[],now())", [randomUUID(), id, identifier]);
    console.log('Cuenta ADM inicial creada. Usa el identificador y contraseña configurados en .env.');
  }
  await client.query('COMMIT');
} catch (error) {
  await client.query('ROLLBACK').catch(() => {});
  console.error(error.message);
  process.exitCode = 1;
} finally {
  await client.end();
}
