window.addEventListener('DOMContentLoaded', () => {
  const registerForm = document.getElementById('register-form');
  const passwordInput = document.getElementById('password') as HTMLInputElement;
  const confirmPasswordInput = document.getElementById('confirm-password') as HTMLInputElement;
  const passwordError = document.getElementById('password-error');

  if (registerForm && passwordInput && confirmPasswordInput && passwordError) {
    registerForm.addEventListener('submit', (e) => {
      e.preventDefault();
      
      if (passwordInput.value !== confirmPasswordInput.value) {
        passwordError.style.display = 'block';
        return;
      }
      
      passwordError.style.display = 'none';

      // Simulamos el registro exitoso
      console.log('Creando cuenta...');
      
      // Redirigir a la pantalla principal
      window.location.href = '/index.html';
    });
  }
});
