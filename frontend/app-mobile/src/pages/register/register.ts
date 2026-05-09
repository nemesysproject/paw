import { register } from '../../shared/services/auth.service';
import { ROUTES } from '../../shared/services/api.config';
import { showToast, setButtonLoading } from '../../shared/ui/ui.utils';

document.addEventListener('DOMContentLoaded', () => {
  const registerForm = document.getElementById('register-form') as HTMLFormElement;

  if (registerForm) {
    registerForm.addEventListener('submit', async (event) => {
      event.preventDefault();

      const name = (document.getElementById('name') as HTMLInputElement).value.trim();
      const email = (document.getElementById('email') as HTMLInputElement).value.trim();
      const password = (document.getElementById('password') as HTMLInputElement).value;
      const confirmPassword = (document.getElementById('confirm-password') as HTMLInputElement).value;
      const submitBtn = registerForm.querySelector('button[type="submit"]') as HTMLButtonElement;

      // Validaciones del lado cliente
      if (!name || !email || !password || !confirmPassword) {
        showToast('Completa todos los campos', 'error');
        return;
      }

      if (password.length < 6) {
        showToast('La contraseña debe tener al menos 6 caracteres', 'error');
        return;
      }

      if (password !== confirmPassword) {
        showToast('Las contraseñas no coinciden', 'error');
        return;
      }

      const restoreBtn = setButtonLoading(submitBtn);

      try {
        await register(name, email, password);
        showToast('¡Registro exitoso! Inicia sesión para continuar.', 'success');
        setTimeout(() => {
          window.location.href = ROUTES.LOGIN;
        }, 1200);
      } catch (error: unknown) {
        const message = error instanceof Error ? error.message : 'Error al registrarse';
        showToast(message, 'error');
        restoreBtn();
      }
    });
  }
});
