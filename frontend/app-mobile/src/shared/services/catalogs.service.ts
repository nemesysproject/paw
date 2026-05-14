import { API_BASE_URL } from './api.config';
import { getDb } from './database.service';
import { isOnline } from './connectivity.service';
import { Species, Breed } from '../models';

/**
 * Servicio de Catálogos.
 * Maneja la sincronización inicial y consultas locales (Local-First).
 */

/**
 * Sincroniza todos los catálogos del servidor a SQLite.
 * Se llama al arrancar la app. No requiere autenticación.
 */
export async function syncAllCatalogs(): Promise<void> {
  if (!isOnline()) {
    console.warn('📡 Offline: No se puede sincronizar catálogos ahora.');
    return;
  }

  try {
    const db = await getDb();
    if (!db) return;

    console.log('📦 Iniciando sincronización de catálogos...');

    // Intentar obtener especies con reintento
    let speciesRes = await fetch(`${API_BASE_URL}/catalogs/species`);

    if (!speciesRes.ok) {
      console.error(`❌ Error persistente en API Especies: ${speciesRes.status}`);
      return;
    }

    const species: any[] = await speciesRes.json();
    console.log(`🧬 Catálogo: Recibidas ${species.length} especies del servidor.`);
    
    for (const s of species) {
      await db.execute(
        'INSERT INTO catalogs_cache (type, id, name) VALUES (?, ?, ?) ON CONFLICT(id) DO UPDATE SET name = excluded.name',
        ['species', s.id, s.name]
      );

      // Cargar razas para esta especie
      const breedsUrl = `${API_BASE_URL}/catalogs/breeds?species_id=${s.id}`;
      let breedsRes = await fetch(breedsUrl);

      if (breedsRes.ok) {
        const breeds: any[] = await breedsRes.json();
        console.log(`   └─ ${s.name}: ${breeds.length} razas sincronizadas.`);
        for (const b of breeds) {
          const sId = b.speciesId || b.species_id || s.id;
          await db.execute(
            'INSERT INTO catalogs_cache (type, id, name, parent_id) VALUES (?, ?, ?, ?) ON CONFLICT(id) DO UPDATE SET name = excluded.name',
            ['breed', b.id, b.name, sId]
          );
        }
      }
    }
    console.log('✅ Sincronización de catálogos finalizada correctamente.');
  } catch (error) {
    console.error('Fallo crítico en la comunicación con el API de catálogos:', error);
  }
}

/** Obtiene las especies disponibles (Local-First) */
export async function getSpecies(): Promise<Species[]> {
  return getLocalCatalog('species');
}

/** Obtiene las razas de una especie (Local-First) */
export async function getBreeds(speciesId: string): Promise<Breed[]> {
  return getLocalCatalog('breed', speciesId);
}

/** Consulta genérica a la cache local de SQLite */
async function getLocalCatalog(type: 'species' | 'breed', parentId?: string): Promise<any[]> {
  const db = await getDb();
  if (!db) return [];

  let query = 'SELECT id, name, parent_id FROM catalogs_cache WHERE type = ?';
  const params: any[] = [type];
  
  if (parentId) {
    query += ' AND parent_id = ?';
    params.push(parentId);
  }

  return await db.select<any[]>(query, params);
}
