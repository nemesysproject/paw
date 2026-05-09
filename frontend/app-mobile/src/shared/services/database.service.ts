import Database from '@tauri-apps/plugin-sql';
import { isTauri } from './platform.service';

/**
 * Servicio de base de datos SQLite.
 * Maneja la persistencia local de credenciales y datos de la app.
 */

let db: Database | null = null;

export async function getDb(): Promise<Database | null> {
  if (!isTauri()) return null;
  if (!db) {
    // Inicializa la base de datos local
    db = await Database.load('sqlite:paw_mobile.db');
    await initTables(db);
  }
  return db;
}

async function initTables(db: Database) {
  // 1. Tabla para credenciales cifradas
  await db.execute(`
    CREATE TABLE IF NOT EXISTS auth_credentials (
      id INTEGER PRIMARY KEY,
      email TEXT UNIQUE NOT NULL,
      encrypted_password TEXT NOT NULL,
      is_biometric_enabled INTEGER DEFAULT 0,
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP
    )
  `);

  // 2. Tabla para Mascotas locales (Cache + Offline capture)
  await db.execute(`
    CREATE TABLE IF NOT EXISTS pets_local (
      local_id TEXT PRIMARY KEY,
      remote_id TEXT, 
      name TEXT,
      gender TEXT,
      status TEXT,
      description TEXT,
      species_id TEXT,
      breed_id TEXT,
      last_latitude REAL,
      last_longitude REAL,
      is_synced INTEGER DEFAULT 0,
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP
    )
  `);

  // 3. Tabla para Multimedia local (Rutas a archivos en disco)
  await db.execute(`
    CREATE TABLE IF NOT EXISTS media_local (
      local_id TEXT PRIMARY KEY,
      pet_local_id TEXT,
      local_path TEXT,
      remote_url TEXT,
      type TEXT,
      is_synced INTEGER DEFAULT 0,
      FOREIGN KEY(pet_local_id) REFERENCES pets_local(local_id)
    )
  `);

  // 4. Cola de Sincronización
  await db.execute(`
    CREATE TABLE IF NOT EXISTS sync_queue (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      action TEXT NOT NULL,
      payload TEXT NOT NULL,
      status TEXT DEFAULT 'pending',
      retry_count INTEGER DEFAULT 0,
      error_message TEXT,
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP
    )
  `);

  // 5. Cache de Catálogos (Especies y Razas)
  await db.execute(`
    CREATE TABLE IF NOT EXISTS catalogs_cache (
      type TEXT, 
      id TEXT PRIMARY KEY,
      name TEXT NOT NULL,
      parent_id TEXT, 
      updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
    )
  `);
}

/** Guarda o actualiza credenciales cifradas */
export async function saveCredentials(
  email: string,
  encryptedPassword: string,
  isBiometric: boolean
): Promise<void> {
  const database = await getDb();
  if (!database) return;
  await database.execute(
    `INSERT INTO auth_credentials (email, encrypted_password, is_biometric_enabled) 
     VALUES (?, ?, ?)
     ON CONFLICT(email) DO UPDATE SET 
     encrypted_password = excluded.encrypted_password,
     is_biometric_enabled = excluded.is_biometric_enabled`,
    [email, encryptedPassword, isBiometric ? 1 : 0]
  );
}

/** Recupera las credenciales por email */
export async function getCredentialsByEmail(email: string) {
  const database = await getDb();
  if (!database) return null;
  const results = await database.select<any[]>(
    'SELECT * FROM auth_credentials WHERE email = ? LIMIT 1',
    [email]
  );
  return results.length > 0 ? results[0] : null;
}

/** Obtiene las últimas credenciales usadas (para login rápido) */
export async function getLastCredentials() {
  const database = await getDb();
  if (!database) return null;
  const results = await database.select<any[]>(
    'SELECT * FROM auth_credentials WHERE is_biometric_enabled = 1 ORDER BY created_at DESC LIMIT 1'
  );
  return results.length > 0 ? results[0] : null;
}
