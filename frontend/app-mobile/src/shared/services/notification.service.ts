import { showToast } from '../ui/ui.utils';

/**
 * Servicio centralizado para manejar notificaciones globales basadas en
 * las respuestas HTTP de la API REST.
 */
export function notifyApiResponse(status: number): void {
  switch (status) {
    case 200:
      showToast('200: Solicitud exitosa', 'success');
      break;
    case 201:
      showToast('201: Creado correctamente', 'success');
      break;
    case 204:
      showToast('204: Acción completada (sin contenido)', 'success');
      break;
    case 400:
      showToast('400: Petición inválida o datos incorrectos', 'error');
      break;
    case 401:
      showToast('401: No autorizado (verifica tus credenciales)', 'error');
      break;
    case 403:
      showToast('403: Acceso denegado a este recurso', 'error');
      break;
    case 404:
      showToast('404: El recurso o endpoint no existe', 'error');
      break;
    case 409:
      showToast('409: Conflicto (ej. el registro ya existe)', 'error');
      break;
    case 422:
      showToast('422: Datos no procesables (validación fallida)', 'error');
      break;
    case 500:
      showToast('500: Error interno del servidor backend', 'error');
      break;
    case 502:
      showToast('502: Bad Gateway (El servidor backend no responde)', 'error');
      break;
    case 503:
      showToast('503: Servicio no disponible momentáneamente', 'error');
      break;
    default:
      if (status >= 200 && status < 300) {
        showToast(`Respuesta exitosa: ${status}`, 'success');
      } else if (status >= 400 && status < 500) {
        showToast(`Error de cliente: ${status}`, 'error');
      } else if (status >= 500) {
        showToast(`Error de servidor: ${status}`, 'error');
      } else {
        showToast(`Respuesta API: ${status}`, 'info');
      }
      break;
  }
}
