/**
 * Configuración centralizada de la API.
 * En desarrollo, las rutas relativas pasan por el proxy de Vite.
 * En producción/Tauri, se puede apuntar a la URL real del backend.
 */
export const API_BASE_URL = 'http://192.168.1.12/api/v1';

/** Claves de localStorage para tokens JWT */
export const STORAGE_KEYS = {
  ACCESS_TOKEN: 'paw_access_token',
  REFRESH_TOKEN: 'paw_refresh_token',
} as const;

/** Rutas de navegación de la app */
export const ROUTES = {
  HOME: '/index.html',
  LOGIN: '/src/pages/login/login.html',
  REGISTER: '/src/pages/register/register.html',
  DASHBOARD: '/src/pages/dashboard/dashboard.html',
} as const;
