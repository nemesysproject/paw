import { API_BASE_URL, STORAGE_KEYS, ROUTES } from './api.config';
import { notifyApiResponse } from './notification.service';
import type { TokenResponse } from '../models';

/**
 * Servicio HTTP centralizado con manejo automático de JWT.
 * - Inyecta el header Authorization: Bearer <token>
 * - Intenta auto-refresh si recibe un 401
 */

// ──────────────────────────────────────────────
// Token Management
// ──────────────────────────────────────────────

export function getAccessToken(): string | null {
  return localStorage.getItem(STORAGE_KEYS.ACCESS_TOKEN);
}

export function getRefreshToken(): string | null {
  return localStorage.getItem(STORAGE_KEYS.REFRESH_TOKEN);
}

export function saveTokens(tokens: TokenResponse): void {
  localStorage.setItem(STORAGE_KEYS.ACCESS_TOKEN, tokens.access_token);
  localStorage.setItem(STORAGE_KEYS.REFRESH_TOKEN, tokens.refresh_token);
}

export function clearTokens(): void {
  localStorage.removeItem(STORAGE_KEYS.ACCESS_TOKEN);
  localStorage.removeItem(STORAGE_KEYS.REFRESH_TOKEN);
}

export function isAuthenticated(): boolean {
  return !!getAccessToken();
}

export function logout(): void {
  clearTokens();
  window.location.href = ROUTES.LOGIN;
}

// ──────────────────────────────────────────────
// Auth Guard
// ──────────────────────────────────────────────

/** Redirige a login si no hay token. Llamar al inicio de páginas protegidas. */
export function requireAuth(): void {
  if (!isAuthenticated()) {
    window.location.href = ROUTES.LOGIN;
  }
}

// ──────────────────────────────────────────────
// HTTP Helpers
// ──────────────────────────────────────────────

/** Flag para evitar loops infinitos de refresh */
let isRefreshing = false;

/** Helper para verificar si un cuerpo de petición es FormData de forma robusta en cualquier entorno/compilación. */
function isFormData(body: any): boolean {
  return !!(
    body &&
    (body instanceof FormData ||
      Object.prototype.toString.call(body) === '[object FormData]' ||
      typeof body.append === 'function')
  );
}

/**
 * Fetch wrapper que inyecta el JWT y reintenta con refresh si recibe 401.
 */
export async function fetchWithAuth(
  endpoint: string,
  options: RequestInit = {}
): Promise<Response> {
  const url = `${API_BASE_URL}${endpoint}`;

  const headers = new Headers(options.headers || {});
  const token = getAccessToken();
  if (token) {
    headers.set('Authorization', `Bearer ${token}`);
  }
  if (!headers.has('Content-Type') && !isFormData(options.body)) {
    headers.set('Content-Type', 'application/json');
  }

  const response = await fetch(url, { ...options, headers });
  
  // Notificar el código de respuesta genérico
  notifyApiResponse(response.status);

  // Si recibimos 401, intentar refresh una sola vez
  if (response.status === 401 && !isRefreshing) {
    const refreshed = await tryRefreshToken();
    if (refreshed) {
      // Re-intentar la petición original con el nuevo token
      const retryHeaders = new Headers(options.headers || {});
      retryHeaders.set('Authorization', `Bearer ${getAccessToken()}`);
      if (!retryHeaders.has('Content-Type') && !isFormData(options.body)) {
        retryHeaders.set('Content-Type', 'application/json');
      }
      const retryResponse = await fetch(url, { ...options, headers: retryHeaders });
      notifyApiResponse(retryResponse.status);
      return retryResponse;
    } else {
      // Refresh falló → forzar logout
      logout();
    }
  }

  return response;
}

/**
 * Fetch sin autenticación para endpoints públicos (login, register).
 */
export async function fetchPublic(
  endpoint: string,
  options: RequestInit = {}
): Promise<Response> {
  const url = `${API_BASE_URL}${endpoint}`;

  const headers = new Headers(options.headers || {});
  if (!headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }

  const response = await fetch(url, { ...options, headers });
  notifyApiResponse(response.status);
  return response;
}

// ──────────────────────────────────────────────
// Token Refresh
// ──────────────────────────────────────────────

async function tryRefreshToken(): Promise<boolean> {
  const refreshToken = getRefreshToken();
  if (!refreshToken) return false;

  isRefreshing = true;
  try {
    const response = await fetch(`${API_BASE_URL}/auth/refresh`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refresh_token: refreshToken }),
    });

    if (!response.ok) return false;

    const tokens: TokenResponse = await response.json();
    saveTokens(tokens);
    return true;
  } catch {
    return false;
  } finally {
    isRefreshing = false;
  }
}
