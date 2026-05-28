import { fetchWithAuth, getAccessToken } from './api.service';
import type { PetDetailResponse } from '../models';
import { readFile } from '@tauri-apps/plugin-fs';

/**
 * Servicio de mascotas.
 * Encapsula las llamadas a /pets/* del backend.
 */

function getUserIdFromToken(): string | null {
  const token = getAccessToken();
  if (!token) return null;
  try {
    const base64Url = token.split('.')[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split('')
        .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    );
    return JSON.parse(jsonPayload).sub;
  } catch (err) {
    console.error('Error decoding JWT token:', err);
    return null;
  }
}

/** Lista todas las mascotas registradas. */
export async function listPets(): Promise<PetDetailResponse[]> {
  const response = await fetchWithAuth('/pets');

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({ error: 'Error de conexión' }));
    throw new Error(errorData.error || `Error ${response.status}`);
  }

  return response.json();
}

/** Obtiene el detalle de una mascota por ID. */
export async function getPetById(id: string): Promise<PetDetailResponse> {
  const response = await fetchWithAuth(`/pets/${id}`);

  if (!response.ok) {
    if (response.status === 404) {
      throw new Error('Mascota no encontrada');
    }
    const errorData = await response.json().catch(() => ({ error: 'Error de conexión' }));
    throw new Error(errorData.error || `Error ${response.status}`);
  }

  return response.json();
}

/** Busca mascotas por ubicación geográfica. */
export async function searchPets(
  lat: number,
  lon: number,
  radiusMeters?: number
): Promise<PetDetailResponse[]> {
  const params = new URLSearchParams({
    lat: lat.toString(),
    lon: lon.toString(),
  });
  if (radiusMeters !== undefined) {
    params.set('radius_meters', radiusMeters.toString());
  }

  const response = await fetchWithAuth(`/pets/search?${params.toString()}`);

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({ error: 'Error de conexión' }));
    throw new Error(errorData.error || `Error ${response.status}`);
  }

  return response.json();
}

/** Crea una nueva mascota en el servidor. */
export async function createPet(petData: any): Promise<PetDetailResponse> {
  const formData = new FormData();

  // 1. Adjuntar campos de texto/metadatos
  Object.keys(petData).forEach(key => {
    // No adjuntamos la lista de rutas locales como campo de texto
    if (key !== 'localMediaPaths' && petData[key] !== null && petData[key] !== undefined) {
      formData.append(key, petData[key].toString());
    }
  });

  // Adjuntar dinámicamente el ID del reportero desde el token JWT
  const reporterId = getUserIdFromToken();
  if (reporterId && !formData.has('reporter_id')) {
    formData.append('reporter_id', reporterId);
  } else if (!formData.has('reporter_id')) {
    throw new Error('No se pudo determinar el reporter_id (usuario no autenticado)');
  }

  // 2. Leer archivos del disco y adjuntarlos como Blobs
  if (petData.localMediaPaths && Array.isArray(petData.localMediaPaths)) {
    console.log(`📂 Cantidad de archivos a procesar: ${petData.localMediaPaths.length}`);
    for (const filePath of petData.localMediaPaths) {
      try {
        console.log(`📄 Leyendo archivo desde la ruta: "${filePath}"`);
        const content = await readFile(filePath);
        console.log(`💾 Archivo leído con éxito. Tamaño: ${content.length} bytes.`);
        
        if (content.length === 0) {
          throw new Error('El archivo está vacío (0 bytes)');
        }

        const fileName = filePath.split(/[/\\]/).pop() || 'upload.bin';

        // Detectar tipo MIME básico según la extensión
        const ext = fileName.split('.').pop()?.toLowerCase();
        let type = 'application/octet-stream';
        if (ext === 'jpg' || ext === 'jpeg') type = 'image/jpeg';
        else if (ext === 'png') type = 'image/png';
        else if (ext === 'mp4') type = 'video/mp4';

        console.log(`🏷️ Archivo detectado como: ${type} (${fileName})`);

        // Creamos un Blob con el tipo detectado para mejor compatibilidad con el backend
        const blob = new Blob([content], { type });
        formData.append('file', blob, fileName);
      } catch (err: any) {
        console.error(`❌ Error al leer archivo "${filePath}":`, err);
        throw new Error(`Error leyendo archivo multimedia local (${filePath}): ${err.message || err}`);
      }
    }
  }

  const response = await fetchWithAuth('/pets', {
    method: 'POST',
    body: formData
    // Nota: No establecemos 'Content-Type'. fetch establecerá automáticamente
    // 'multipart/form-data' con el boundary correcto al detectar un cuerpo FormData.
  });

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({ error: 'Error al crear mascota' }));
    throw new Error(errorData.error || `Error ${response.status}`);
  }

  return response.json();
}
