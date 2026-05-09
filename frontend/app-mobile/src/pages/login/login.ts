import { login } from '../../shared/services/auth.service';
import { ROUTES } from '../../shared/services/api.config';
import { showToast, setButtonLoading } from '../../shared/ui/ui.utils';
import { 
  hasBiometricSetup, 
  loginWithBiometric, 
  isBiometricAvailable, 
  enableBiometricAccess 
} from '../../shared/services/biometric.service';
import { isTauri } from '../../shared/services/platform.service';

document.addEventListener('DOMContentLoaded', async () => {
  const loginForm = document.getElementById('login-form') as HTMLFormElement;
  const bioContainer = document.getElementById('biometric-login-container');
  const btnBiometric = document.getElementById('btn-biometric') as HTMLButtonElement;

  // 1. Lógica Biométrica (Solo en Tauri)
  if (isTauri()) {
    const isAvailable = await isBiometricAvailable();
    const isConfigured = await hasBiometricSetup();

    if (isAvailable && isConfigured) {
      if (bioContainer) bioContainer.classList.remove('hidden');

      // Intentar login biométrico automático al cargar
      setTimeout(async () => {
        try {
          const success = await loginWithBiometric();
          if (success) {
            showToast('¡Bienvenido de nuevo!', 'success');
            setTimeout(() => window.location.href = ROUTES.HOME, 600);
          }
        } catch (e) {
          console.warn('Auto-biometric login failed or canceled', e);
        }
      }, 500);
    }

    if (btnBiometric) {
      btnBiometric.addEventListener('click', async () => {
        const restoreBtn = setButtonLoading(btnBiometric);
        try {
          const success = await loginWithBiometric();
          if (success) {
            showToast('¡Acceso biométrico exitoso!', 'success');
            setTimeout(() => window.location.href = ROUTES.HOME, 600);
          } else {
            showToast('Autenticación fallida o cancelada', 'info');
            restoreBtn();
          }
        } catch (error: any) {
          showToast(error.message || 'Error en biometría', 'error');
          restoreBtn();
        }
      });
    }
  }

  // 2. Lógica de Login Manual (Disponible en Web y Tauri)
  if (loginForm) {
    loginForm.addEventListener('submit', async (event) => {
      event.preventDefault();

      const email = (document.getElementById('email') as HTMLInputElement).value.trim();
      const password = (document.getElementById('password') as HTMLInputElement).value;
      const submitBtn = loginForm.querySelector('button[type="submit"]') as HTMLButtonElement;

      if (!email || !password) {
        showToast('Completa todos los campos', 'error');
        return;
      }

      const restoreBtn = setButtonLoading(submitBtn);

      try {
        await login(email, password);
        
        // 3. Ofrecer biometría solo si estamos en Tauri y está disponible
        if (isTauri()) {
          const isAvailable = await isBiometricAvailable();
          const isConfigured = await hasBiometricSetup();
          
          if (isAvailable && !isConfigured) {
            const wantBio = confirm('¿Deseas habilitar el acceso por huella o rostro para entrar más rápido la próxima vez?');
            if (wantBio) {
              try {
                await enableBiometricAccess(email, password);
                showToast('Acceso biométrico activado correctamente', 'success');
              } catch (bioError: any) {
                console.error('Error activando biometría:', bioError);
                showToast('No se pudo activar la biometría', 'info');
              }
            }
          }
        }

        showToast('¡Inicio de sesión exitoso!', 'success');
        setTimeout(() => window.location.href = ROUTES.HOME, 600);
      } catch (error: unknown) {
        const message = error instanceof Error ? error.message : 'Error al iniciar sesión';
        showToast(message, 'error');
        restoreBtn();
      }
    });
  }
});
