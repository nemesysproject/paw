import { copyMediaLocally } from './media-storage.service';
import { getDb } from './database.service';
import { enqueueSyncAction } from './sync.service';
import { PetCreateRequest } from '../models';

/**
 * Fachada para la gestión de Mascotas.
 * Implementa el patrón Offline-First para la creación de registros.
 * 
 * Flujo: Copiar archivos localmente → Guardar en SQLite → Encolar sync
 */

export interface MediaFile {
  path: string;
  name: string;
  type: 'image' | 'video';
  localPath?: string;
}

export interface PetCreateData extends PetCreateRequest {
  media: MediaFile[];
}

/**
 * Crea una mascota siguiendo la estrategia Offline-First.
 * 1. Copia los archivos multimedia al almacenamiento local de la app
 * 2. Guarda los datos en SQLite
 * 3. Encola la sincronización para cuando haya red
 */
export async function createPetOffline(data: PetCreateData): Promise<string> {
  const local_id = crypto.randomUUID();
  const db = await getDb();
  if (!db) throw new Error('Base de datos no disponible');

  console.log('📝 Iniciando guardado offline de mascota...');

  // 1. Copiar archivos multimedia al almacenamiento local de la app
  //    Esto persiste los archivos incluso si las content:// URIs expiran
  const mediaRecords: { local_path: string, type: string }[] = [];

  for (const media of data.media) {
    console.log(`📂 Procesando: ${media.name} (${media.type})...`);
    const localPath = await copyMediaLocally(media.path, media.name);
    mediaRecords.push({ local_path: localPath, type: media.type });
  }

  // 2. Guardar en tabla local pets_local
  await db.execute(
    `INSERT INTO pets_local (
      local_id, name, gender, status, description, 
      species_id, breed_id, last_latitude, last_longitude
    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)`,
    [
      local_id, data.name, data.gender, data.status, data.description,
      data.species_id, data.breed_id || null, data.last_latitude, data.last_longitude
    ]
  );

  // 3. Guardar rutas locales en tabla media_local
  for (const m of mediaRecords) {
    await db.execute(
      'INSERT INTO media_local (local_id, pet_local_id, local_path, type) VALUES (?, ?, ?, ?)',
      [crypto.randomUUID(), local_id, m.local_path, m.type]
    );
  }

  console.log('✅ Mascota guardada con', mediaRecords.length, 'archivos');

  // 4. Encolar acción de sincronización
  const syncPayload = {
    name: data.name,
    gender: data.gender,
    status: data.status,
    description: data.description,
    species_id: data.species_id,
    breed_id: data.breed_id,
    last_latitude: data.last_latitude,
    last_longitude: data.last_longitude,
    local_id,
    localMediaPaths: mediaRecords.map(m => m.local_path)
  };

  await enqueueSyncAction('CREATE_PET', syncPayload);
  console.log('📦 Sync encolado');

  return local_id;
}
