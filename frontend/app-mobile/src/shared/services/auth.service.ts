import { fetchPublic, saveTokens, clearTokens, isAuthenticated as checkAuth } from './api.service';
import { ROUTES } from './api.config';
import type { TokenResponse } from '../models';

/**
 * Servicio de autenticación.
 * Encapsula las llamadas a /auth/* del backend.
 */

export async function login(email: string, password: string): Promise<TokenResponse> {
  const response = await fetchPublic('/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email, password }),
  });

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({ error: 'Error de conexión' }));
    throw new Error(errorData.error || `Error ${response.status}`);
  }

  const tokens: TokenResponse = await response.json();
  saveTokens(tokens);
  return tokens;
}

export async function register(name: string, email: string, password: string): Promise<void> {
  const response = await fetchPublic('/auth/register', {
    method: 'POST',
    body: JSON.stringify({ email, password, name }),
  });

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({ error: 'Error de conexión' }));
    throw new Error(errorData.error || `Error ${response.status}`);
  }
}

export function logout(): void {
  clearTokens();
  window.location.href = ROUTES.LOGIN;
}

export function isAuthenticated(): boolean {
  return checkAuth();
}
