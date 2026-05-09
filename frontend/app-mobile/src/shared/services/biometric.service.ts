import { checkStatus, authenticate } from '@tauri-apps/plugin-biometric';
import { encryptData, decryptData } from './encryption.service';
import { saveCredentials, getLastCredentials } from './database.service';
import { login } from './auth.service';
import { isTauri } from './platform.service';
import { isOnline } from './connectivity.service';

/**
 * Servicio de Biometría.
 * Gestiona el acceso rápido y la persistencia de credenciales cifradas.
 */

export async function isBiometricAvailable(): Promise<boolean> {
  if (!isTauri()) return false;
  try {
    const status = await checkStatus();
    return status.isAvailable;
  } catch {
    return false;
  }
}

/**
 * Habilita la biometría para el usuario actual.
 */
export async function enableBiometricAccess(email: string, password: string): Promise<void> {
  if (!(await isBiometricAvailable())) {
    throw new Error('Biometría no disponible en este dispositivo');
  }

  await authenticate('Confirma tu identidad para habilitar el acceso rápido');

  const encryptedPassword = await encryptData(password);
  await saveCredentials(email, encryptedPassword, true);
}

/**
 * Intenta realizar un login automático usando biometría.
 * Soporta modo offline: si no hay red, valida biometría y permite acceso.
 */
export async function loginWithBiometric(): Promise<boolean> {
  const credentials = await getLastCredentials();
  if (!credentials) return false;

  if (!(await isBiometricAvailable())) return false;

  try {
    // 1. Solicitar huella/rostro (Siempre, incluso offline)
    await authenticate('Inicia sesión con biometría');

    // 2. Si hay red, intentar login real contra el API
    if (isOnline()) {
      try {
        const password = await decryptData(credentials.encrypted_password);
        await login(credentials.email, password);
        return true;
      } catch (apiError) {
        console.warn('Error de API en login biométrico, intentando modo offline:', apiError);
        // Si el error es de red, permitimos pasar. Si es de credenciales (401), no.
        if (apiError instanceof Error && apiError.message.includes('401')) {
           throw apiError; 
        }
      }
    }

    // 3. Si no hay red o el API falló por red, conceder acceso offline
    console.log('🔓 Acceso concedido en modo OFFLINE');
    return true;
  } catch (error) {
    console.error('Error en login biométrico:', error);
    return false;
  }
}

/** Verifica si el usuario actual tiene biometría configurada */
export async function hasBiometricSetup(): Promise<boolean> {
  const credentials = await getLastCredentials();
  return !!credentials;
}
