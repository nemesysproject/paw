import { getDb } from './database.service';
import { isOnline, onConnectionChange } from './connectivity.service';
import { createPet } from './pets.service';
import { deleteLocalMedia } from './media-storage.service';

/**
 * Motor de Sincronización.
 * Gestiona la cola de operaciones pendientes y las ejecuta cuando hay red.
 */

export interface SyncAction {
  id?: number;
  action: 'CREATE_PET' | 'UPDATE_PET' | 'UPLOAD_MEDIA';
  payload: any;
  status: 'pending' | 'syncing' | 'failed';
  retry_count: number;
}

// Iniciar monitoreo de conexión para disparar sincronización
onConnectionChange(async (status) => {
  if (status === 'online') {
    console.log('🔄 Iniciando procesador de sincronización...');
    await processSyncQueue();
  }
});

/** Agrega una acción a la cola de sincronización */
export async function enqueueSyncAction(action: SyncAction['action'], payload: any) {
  const db = await getDb();
  if (!db) return;

  await db.execute(
    'INSERT INTO sync_queue (action, payload) VALUES (?, ?)',
    [action, JSON.stringify(payload)]
  );

  // Intentar procesar inmediatamente si hay red
  if (isOnline()) {
    console.log('Nueva acción en cola, procesando...');
    processSyncQueue();
  }
}

/** Procesa todos los elementos pendientes en la cola */
export async function processSyncQueue() {
  if (!isOnline()) return;

  const db = await getDb();
  if (!db) return;

  // Obtener elementos pendientes
  const pending = await db.select<any[]>(
    "SELECT * FROM sync_queue WHERE status != 'syncing' AND retry_count < 5 ORDER BY created_at ASC"
  );


  
  if (pending.length === 0) return;

  console.log(`📦 Procesando ${pending.length} acciones en la cola...`);

  // Intentar auto-login biométrico si la sesión expiró (importante para sync de fondo)
  // Nota: Esto podría fallar si requiere interacción, pero en Tauri el authenticate 
  // puede configurarse para persistir o simplemente fallará y el usuario lo hará manual.
  // Por ahora asumimos que la sesión puede estar activa o el usuario será notificado.

  for (const item of pending) {
    try {
      await updateActionStatus(item.id, 'syncing');
      
      const payload = JSON.parse(item.payload);
      
      switch (item.action) {
        case 'CREATE_PET':
          await handleCreatePetSync(payload);
          break;
        // Otros casos como UPDATE_PET o UPLOAD_MEDIA se añadirán aquí
      }

      // Si tiene éxito, eliminar de la cola
      await db.execute('DELETE FROM sync_queue WHERE id = ?', [item.id]);
      console.log(`✅ Acción ${item.id} (${item.action}) sincronizada con éxito`);

    } catch (error: any) {
      console.error(`❌ Error sincronizando acción ${item.id}:`, error);
      await updateActionStatus(item.id, 'failed', error.message);
    }
  }
}

async function updateActionStatus(id: number, status: string, error?: string) {
  const db = await getDb();
  if (!db) return;
  
  if (status === 'failed') {
    await db.execute(
      'UPDATE sync_queue SET status = ?, retry_count = retry_count + 1, error_message = ? WHERE id = ?',
      [status, error || '', id]
    );
  } else {
    await db.execute('UPDATE sync_queue SET status = ? WHERE id = ?', [status, id]);
  }
}

/** Lógica específica para sincronizar una nueva mascota */
async function handleCreatePetSync(payload: any) {
  // 1. Si hay multimedia local, subirla primero (TODO)
  // 2. Llamar al servicio REST real
  await createPet(payload);
  
  // 3. Si hay archivos locales asociados en el payload, borrarlos tras éxito
  if (payload.localMediaPaths && Array.isArray(payload.localMediaPaths)) {
    for (const path of payload.localMediaPaths) {
      await deleteLocalMedia(path);
    }
  }
}
