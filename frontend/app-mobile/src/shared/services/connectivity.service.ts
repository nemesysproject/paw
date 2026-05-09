/**
 * Servicio de Conectividad.
 * Monitorea el estado de la red y emite eventos cuando cambia.
 */

export type ConnectionStatus = 'online' | 'offline';

let currentStatus: ConnectionStatus = navigator.onLine ? 'online' : 'offline';
const listeners: ((status: ConnectionStatus) => void)[] = [];

// Escuchar eventos nativos del navegador
window.addEventListener('online', () => updateStatus('online'));
window.addEventListener('offline', () => updateStatus('offline'));

function updateStatus(status: ConnectionStatus) {
  if (currentStatus !== status) {
    currentStatus = status;
    listeners.forEach(callback => callback(status));
    
    if (status === 'online') {
      console.log('🌐 Conexión recuperada. Iniciando sincronización...');
      // Aquí dispararemos el SyncEngine más adelante
    }
  }
}

export function getStatus(): ConnectionStatus {
  return currentStatus;
}

export function isOnline(): boolean {
  return currentStatus === 'online';
}

export function onConnectionChange(callback: (status: ConnectionStatus) => void) {
  listeners.push(callback);
  return () => {
    const index = listeners.indexOf(callback);
    if (index > -1) listeners.splice(index, 1);
  };
}
