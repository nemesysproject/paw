document.addEventListener('DOMContentLoaded', () => {
  const registerForm = document.getElementById('register-form') as HTMLFormElement;

  if (registerForm) {
    registerForm.addEventListener('submit', (event) => {
      event.preventDefault(); // Evita el envío del formulario por defecto

      const name = (document.getElementById('name') as HTMLInputElement).value;
      const email = (document.getElementById('email') as HTMLInputElement).value;
      const password = (document.getElementById('password') as HTMLInputElement).value;
      const confirmPassword = (document.getElementById('confirm-password') as HTMLInputElement).value;

      console.log('Intento de registro:', { name, email, password, confirmPassword });
      // Aquí se integraría la llamada a la API de Rust a través de Tauri
      alert('Registro simulado. Revisa la consola para los datos.');
      // En una aplicación real, redirigirías o manejarías la respuesta del backend
      // window.location.href = '/login.html'; // Ejemplo de redirección
    });
  }
});