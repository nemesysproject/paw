/**
 * Modelos de datos unificados con el Backend API-REST.
 */

// --- Autenticación ---

export interface LoginRequest {
  email: string;
  password_hash: string;
}

export interface RegisterRequest extends LoginRequest {
  full_name: string;
}

export interface TokenResponse {
  access_token: string;
  refresh_token: string;
  expires_in: number;
}

// --- Catálogos ---

export interface Species {
  id: string;
  name: string;
  created_at?: string;
}

export interface Breed {
  id: string;
  species_id: string;
  name: string;
  created_at?: string;
}

// --- Mascotas ---

export type PetGender = 'MALE' | 'FEMALE' | 'UNKNOWN';
export type PetStatus = 'LOST' | 'FOUND' | 'ADOPTED';
export type MediaType = 'IMAGE' | 'VIDEO';

export interface PetCreateRequest {
  name: string;
  gender: PetGender;
  status: PetStatus;
  description: string;
  species_id: string;
  breed_id?: string | null;
  last_latitude: number;
  last_longitude: number;
}

export interface PetMediaResponse {
  id: string;
  pet_id: string;
  media_url: string;
  media_type: MediaType;
  created_at: string;
}

export interface PetDetailResponse {
  id: string;
  name: string;
  gender: PetGender;
  status: PetStatus;
  description: string;
  species_id: string;
  species_name: string;
  breed_id?: string;
  breed_name?: string;
  last_latitude: number;
  last_longitude: number;
  created_at: string;
  media: PetMediaResponse[];
}
