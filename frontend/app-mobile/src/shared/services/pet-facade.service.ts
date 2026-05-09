import { saveMediaLocally } from './media-storage.service';
import { getDb } from './database.service';
import { enqueueSyncAction } from './sync.service';
import { PetCreateRequest } from '../models';

/**
 * Fachada para la gestión de Mascotas.
 * Implementa el patrón Offline-First para la creación de registros.
 */

export interface MediaFile {
  file: File;
  type: 'image' | 'video';
  localPath?: string;
}

export interface PetCreateData extends PetCreateRequest {
  media: MediaFile[];
}

/**
 * Crea una mascota siguiendo la estrategia Offline-First.
 */
export async function createPetOffline(data: PetCreateData): Promise<string> {
  const local_id = crypto.randomUUID();
  const db = await getDb();
  if (!db) throw new Error('Base de datos no disponible');

  // 1. Guardar archivos multimedia localmente
  const mediaRecords: { local_path: string, type: string }[] = [];
  
  for (const media of data.media) {
    const arrayBuffer = await media.file.arrayBuffer();
    const uint8Array = new Uint8Array(arrayBuffer);
    
    const localPath = await saveMediaLocally(media.file.name, uint8Array);
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

  // 3. Guardar en tabla local media_local
  for (const m of mediaRecords) {
    await db.execute(
      'INSERT INTO media_local (local_id, pet_local_id, local_path, type) VALUES (?, ?, ?, ?)',
      [crypto.randomUUID(), local_id, m.local_path, m.type]
    );
  }

  // 4. Encolar acción de sincronización
  // Incluimos las rutas locales para que el Sync Engine sepa qué borrar después
  const syncPayload = {
    ...data,
    local_id,
    localMediaPaths: mediaRecords.map(m => m.local_path)
  };
  
  // Nota: Quitamos los objetos File del payload de sync porque no se pueden serializar a JSON
  delete (syncPayload as any).media; 

  await enqueueSyncAction('CREATE_PET', syncPayload);

  return local_id;
}
