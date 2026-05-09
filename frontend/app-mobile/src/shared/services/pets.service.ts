import { fetchWithAuth } from './api.service';
import type { PetDetailResponse } from '../models';

/**
 * Servicio de mascotas.
 * Encapsula las llamadas a /pets/* del backend.
 */

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
  const response = await fetchWithAuth('/pets', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(petData)
  });

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({ error: 'Error al crear mascota' }));
    throw new Error(errorData.error || `Error ${response.status}`);
  }

  return response.json();
}
