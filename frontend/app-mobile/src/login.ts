document.addEventListener('DOMContentLoaded', () => {
  const loginForm = document.getElementById('login-form') as HTMLFormElement;

  if (loginForm) {
    loginForm.addEventListener('submit', async (event) => { // Hacemos la función asíncrona
      event.preventDefault(); // Evita el envío del formulario por defecto

      const email = (document.getElementById('email') as HTMLInputElement).value;
      const password = (document.getElementById('password') as HTMLInputElement).value;

      try {
        const response = await fetch('/auth/login', { // Asumiendo el endpoint /auth/login
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify({ email, password }),
        });

        if (!response.ok) {
          const errorData = await response.json();
          throw new Error(errorData.message || 'Error al iniciar sesión');
        }

        const data = await response.json();
        localStorage.setItem('authToken', data.token); // Almacenar el token JWT
        alert('Inicio de sesión exitoso!');
        window.location.href = '/dashboard.html'; // Redirigir al dashboard
      } catch (error: any) {
        console.error('Error de inicio de sesión:', error.message);
        alert(`Error: ${error.message}`);
      }
    });
  }
});