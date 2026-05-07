window.addEventListener('DOMContentLoaded', () => {
  const loginForm = document.getElementById('login-form');

  if (loginForm) {
    loginForm.addEventListener('submit', (e) => {
      e.preventDefault();
      // Simulamos la autenticación exitosa
      console.log('Iniciando sesión...');
      
      // Redirigir a la pantalla principal
      window.location.href = '/index.html';
    });
  }
});
