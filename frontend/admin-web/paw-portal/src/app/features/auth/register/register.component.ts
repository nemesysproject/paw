import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '@core/services/auth.service';
import { LucideAngularModule } from 'lucide-angular';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, LucideAngularModule, RouterLink],
  template: `
    <div class="login-container">
      <div class="login-card">
        <div class="login-header">
          <div class="logo">
            <lucide-icon name="dog" class="logo-icon"></lucide-icon>
          </div>
          <h1>Crear Cuenta</h1>
          <p>Únete al portal administrativo de Save Puppy</p>
        </div>

        <form [formGroup]="registerForm" (ngSubmit)="onSubmit()">
          <div class="mb-3">
            <label class="form-label">Nombre Completo</label>
            <div class="input-group">
              <span class="input-group-text"><lucide-icon name="users" size="18"></lucide-icon></span>
              <input type="text" formControlName="name" class="form-control" placeholder="Ej. Juan Pérez">
            </div>
          </div>

          <div class="mb-3">
            <label class="form-label">Correo Electrónico</label>
            <div class="input-group">
              <span class="input-group-text">@</span>
              <input type="email" formControlName="email" class="form-control" placeholder="admin@example.com">
            </div>
          </div>

          <div class="mb-3">
            <label class="form-label">Contraseña</label>
            <div class="input-group">
              <span class="input-group-text"><lucide-icon name="settings" size="18"></lucide-icon></span>
              <input type="password" formControlName="password" class="form-control" placeholder="Mínimo 6 caracteres">
            </div>
          </div>

          <div class="error-message" *ngIf="errorMessage()">
            {{ errorMessage() }}
          </div>

          <div class="success-message" *ngIf="isSuccess()">
            ¡Registro exitoso! Redirigiendo al login...
          </div>

          <button type="submit" class="btn btn-primary w-100" [disabled]="registerForm.invalid || isLoading() || isSuccess()">
            <span *ngIf="!isLoading()">Registrarse</span>
            <span *ngIf="isLoading()">Procesando...</span>
          </button>
        </form>

        <div class="login-footer">
          <p>¿Ya tienes cuenta? <a routerLink="/auth/login" class="text-primary font-weight-bold">Inicia sesión</a></p>
          <p class="mt-4">&copy; 2026 PAW Project</p>
        </div>
      </div>
    </div>
  `,
  styles: [`
    /* Reutilizamos los estilos del login que están en styles.css o podemos duplicarlos aquí para independencia */
    .login-container {
      height: 100vh;
      display: flex;
      align-items: center;
      justify-content: center;
      background: linear-gradient(135deg, #6366f1 0%, #4f46e5 100%);
    }

    .login-card {
      background: #fff;
      padding: 3rem;
      border-radius: 20px;
      box-shadow: 0 20px 25px -5px rgb(0 0 0 / 0.1), 0 8px 10px -6px rgb(0 0 0 / 0.1);
      width: 100%;
      max-width: 450px;
    }

    .login-header {
      text-align: center;
      margin-bottom: 2.5rem;
    }

    .logo-icon {
      color: var(--paw-primary);
      width: 48px;
      height: 48px;
      margin-bottom: 1rem;
    }

    .login-header h1 {
      font-size: 1.75rem;
      font-weight: 800;
      color: #1e293b;
      margin-bottom: 0.5rem;
    }

    .login-header p {
      color: #64748b;
    }

    .form-label {
      font-weight: 600;
      color: #475569;
    }

    .input-group-text {
      background: #f8fafc;
      border-right: none;
      color: #94a3b8;
    }

    .form-control {
      border-left: none;
      padding: 0.75rem;
      background-color: #f8fafc;
    }

    .form-control:focus {
      background-color: #fff;
      box-shadow: none;
      border-color: #e2e8f0;
    }

    .error-message {
      color: var(--paw-secondary);
      background: rgba(244, 63, 94, 0.1);
      padding: 0.75rem;
      border-radius: 8px;
      margin-bottom: 1.5rem;
      font-size: 0.875rem;
      text-align: center;
    }

    .success-message {
      color: #059669;
      background: #ecfdf5;
      padding: 0.75rem;
      border-radius: 8px;
      margin-bottom: 1.5rem;
      font-size: 0.875rem;
      text-align: center;
    }

    .login-footer {
      text-align: center;
      margin-top: 3rem;
      color: #94a3b8;
      font-size: 0.875rem;
    }

    a { text-decoration: none; }
  `]
})
export class RegisterComponent {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);
  private router = inject(Router);

  isLoading = signal(false);
  isSuccess = signal(false);
  errorMessage = signal<string | null>(null);

  registerForm = this.fb.group({
    name: ['', [Validators.required, Validators.minLength(3)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]]
  });

  onSubmit() {
    if (this.registerForm.valid) {
      this.isLoading.set(true);
      this.errorMessage.set(null);

      this.auth.register(this.registerForm.value).subscribe({
        next: () => {
          this.isSuccess.set(true);
          this.isLoading.set(false);
          setTimeout(() => {
            this.router.navigate(['/auth/login']);
          }, 2000);
        },
        error: (err: HttpErrorResponse) => {
          this.errorMessage.set(err.error?.error || 'Error al registrar usuario');
          this.isLoading.set(false);
        }
      });
    }
  }
}
