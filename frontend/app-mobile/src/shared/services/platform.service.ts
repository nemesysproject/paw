/**
 * Utilidades para detectar el entorno de ejecución.
 */

export function isTauri(): boolean {
  return typeof window !== 'undefined' && (window as any).__TAURI_INTERNALS__ !== undefined;
}
