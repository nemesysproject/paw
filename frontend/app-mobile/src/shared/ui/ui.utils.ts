/**
 * Sistema de notificaciones toast.
 * Muestra mensajes temporales estilizados en la parte superior de la pantalla.
 */

type ToastType = 'success' | 'error' | 'info';

let toastContainer: HTMLElement | null = null;

function ensureContainer(): HTMLElement {
  if (!toastContainer) {
    toastContainer = document.createElement('div');
    toastContainer.className = 'toast-container';
    toastContainer.id = 'toast-container';
    document.body.appendChild(toastContainer);
  }
  return toastContainer;
}

export function showToast(message: string, type: ToastType = 'info', duration = 4000): void {
  const container = ensureContainer();

  const toast = document.createElement('div');
  toast.className = `toast toast--${type}`;

  const iconMap: Record<ToastType, string> = {
    success: 'ph-check-circle',
    error: 'ph-warning-circle',
    info: 'ph-info',
  };

  toast.innerHTML = `
    <i class="ph ${iconMap[type]} toast__icon"></i>
    <span class="toast__message">${message}</span>
  `;

  container.appendChild(toast);

  // Trigger animation
  requestAnimationFrame(() => toast.classList.add('toast--visible'));

  setTimeout(() => {
    toast.classList.remove('toast--visible');
    toast.classList.add('toast--hiding');
    toast.addEventListener('transitionend', () => toast.remove());
  }, duration);
}

/**
 * Pone un botón en estado de loading (spinner + disabled).
 * Devuelve una función para restaurar el estado original.
 */
export function setButtonLoading(button: HTMLButtonElement): () => void {
  const originalText = button.innerHTML;
  const originalDisabled = button.disabled;

  button.disabled = true;
  button.classList.add('btn--loading');
  button.innerHTML = `<span class="spinner"></span> Cargando...`;

  return () => {
    button.innerHTML = originalText;
    button.disabled = originalDisabled;
    button.classList.remove('btn--loading');
  };
}
