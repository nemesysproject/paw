import { Component, inject } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { CommonModule } from '@angular/common';
import { LucideAngularModule } from 'lucide-angular';
import { AuthService } from '@core/services/auth.service';

@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive, LucideAngularModule],
  template: `
    <div class="main-wrapper">
      <!-- Sidebar -->
      <aside class="sidebar" [class.collapsed]="isSidebarCollapsed">
        <div class="sidebar-header">
          <div class="logo">
            <lucide-icon name="dog" class="logo-icon"></lucide-icon>
            <span class="logo-text" *ngIf="!isSidebarCollapsed">PAW PORTAL</span>
          </div>
        </div>

        <div class="sidebar-user" *ngIf="!isSidebarCollapsed">
          <div class="user-avatar">
            <img src="https://ui-avatars.com/api/?name=Admin&background=6366f1&color=fff" alt="User">
          </div>
          <div class="user-info">
            <span class="user-name">{{ auth.currentUser()?.name }}</span>
            <span class="user-role">Administrador</span>
          </div>
        </div>

        <nav class="sidebar-nav">
          <div class="nav-section-title" *ngIf="!isSidebarCollapsed">PRINCIPAL</div>
          <a routerLink="/dashboard" routerLinkActive="active" class="nav-item">
            <lucide-icon name="layout-dashboard" class="nav-icon"></lucide-icon>
            <span class="nav-label" *ngIf="!isSidebarCollapsed">Dashboard</span>
          </a>

          <div class="nav-section-title" *ngIf="!isSidebarCollapsed">GESTIÓN</div>
          <a routerLink="/pets" routerLinkActive="active" class="nav-item">
            <lucide-icon name="dog" class="nav-icon"></lucide-icon>
            <span class="nav-label" *ngIf="!isSidebarCollapsed">Mascotas</span>
          </a>

          <a class="nav-section-title" routerLink="/shelters" routerLinkActive="active" class="nav-item">
            <lucide-icon name="home" class="nav-icon"></lucide-icon>
            <span class="nav-label" *ngIf="!isSidebarCollapsed">Refugios</span>
          </a>

          <a routerLink="/users" routerLinkActive="active" class="nav-item">
            <lucide-icon name="users" class="nav-icon"></lucide-icon>
            <span class="nav-label" *ngIf="!isSidebarCollapsed">Usuarios</span>
          </a>

          <div class="nav-section-title" *ngIf="!isSidebarCollapsed">SISTEMA</div>
          <a routerLink="/config" routerLinkActive="active" class="nav-item">
            <lucide-icon name="settings" class="nav-icon"></lucide-icon>
            <span class="nav-label" *ngIf="!isSidebarCollapsed">Configuración</span>
          </a>
        </nav>

        <div class="sidebar-footer">
          <button (click)="logout()" class="nav-item logout-btn">
            <lucide-icon name="log-out" class="nav-icon"></lucide-icon>
            <span class="nav-label" *ngIf="!isSidebarCollapsed">Cerrar Sesión</span>
          </button>
        </div>
      </aside>

      <!-- Main Content -->
      <main class="content-container">
        <!-- Top Navbar -->
        <header class="top-navbar">
          <button class="btn-toggle" (click)="isSidebarCollapsed = !isSidebarCollapsed">
            <lucide-icon name="layout-dashboard"></lucide-icon>
          </button>
          
          <div class="header-right">
            <button class="icon-btn" title="Buscar"><lucide-icon name="search"></lucide-icon></button>
            <button class="icon-btn" title="Notificaciones">
              <lucide-icon name="bell"></lucide-icon>
              <span class="badge">3</span>
            </button>
            <div class="divider"></div>
            <div class="user-nav-item d-flex align-items-center gap-2">
              <div class="text-end d-none d-md-block">
                <div class="user-display-name">{{ auth.currentUser()?.name || 'Usuario' }}</div>
                <div class="user-display-role text-muted small">Administrador</div>
              </div>
              <img [src]="'https://ui-avatars.com/api/?name=' + (auth.currentUser()?.name || 'U') + '&background=6366f1&color=fff'" 
                   class="avatar-sm shadow-sm" 
                   alt="User">
            </div>
          </div>
        </header>

        <!-- Dynamic Content -->
        <div class="page-body">
          <router-outlet></router-outlet>
        </div>

        <footer class="main-footer">
          <div class="container-fluid d-flex justify-content-between">
            <span>&copy; 2026 Save Puppy Project</span>
            <span class="text-muted">v1.0.0-beta</span>
          </div>
        </footer>
      </main>
    </div>
  `,
  styles: [`
    .main-wrapper {
      display: flex;
      min-height: 100vh;
      background-color: #f8f9fc;
    }

    .sidebar {
      width: 240px;
      background: #1e293b;
      color: #94a3b8;
      display: flex;
      flex-direction: column;
      transition: all 0.25s ease-in-out;
      box-shadow: 0 0.15rem 1.75rem 0 rgba(33, 40, 50, 0.15);
      z-index: 1000;
    }

    .sidebar.collapsed {
      width: 70px;
    }

    .sidebar-header {
      padding: 1.5rem 1rem;
      border-bottom: 1px solid rgba(255, 255, 255, 0.1);
    }

    .logo {
      display: flex;
      align-items: center;
      gap: 10px;
      color: #fff;
    }

    .logo-icon { color: #818cf8; width: 28px; }
    .logo-text { font-weight: 800; font-size: 1.1rem; letter-spacing: 1px; }

    .sidebar-user {
      padding: 1.5rem 1rem;
      display: flex;
      align-items: center;
      gap: 12px;
      border-bottom: 1px solid rgba(255, 255, 255, 0.05);
    }

    .user-avatar img { width: 45px; border-radius: 12px; }
    .user-info { display: flex; flex-direction: column; }
    .user-name { color: #fff; font-weight: 600; font-size: 0.9rem; }
    .user-role { font-size: 0.75rem; color: #64748b; }

    .sidebar-nav { flex-grow: 1; padding: 1rem 0; }
    .nav-section-title {
      font-size: 0.65rem;
      font-weight: 800;
      color: #475569;
      padding: 1.5rem 1.5rem 0.5rem;
      text-transform: uppercase;
      letter-spacing: 1.5px;
    }

    .nav-item {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 0.85rem 1.5rem;
      color: #94a3b8;
      text-decoration: none;
      transition: all 0.2s;
      border-left: 3px solid transparent;
      border: none; background: none; width: 100%; text-align: left;
    }

    .nav-item:hover { background: rgba(255, 255, 255, 0.05); color: #fff; }
    .nav-item.active {
      color: #fff;
      background: rgba(99, 102, 241, 0.1);
      border-left: 3px solid #6366f1;
    }

    .nav-icon { width: 18px; height: 18px; }

    .sidebar-footer { padding: 1rem; border-top: 1px solid rgba(255, 255, 255, 0.05); }
    .logout-btn { color: #f43f5e; }

    .content-container {
      flex-grow: 1;
      display: flex;
      flex-direction: column;
      overflow-x: hidden;
    }

    .top-navbar {
      height: 70px;
      background: #fff;
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 0 1.5rem;
      box-shadow: 0 0.15rem 1.75rem 0 rgba(33, 40, 50, 0.15);
      z-index: 900;
    }

    .btn-toggle {
      background: none; border: none; color: #4e73df; font-size: 1.2rem;
    }

    .header-right { display: flex; align-items: center; gap: 1rem; }
    .icon-btn { background: none; border: none; color: #b7c1d1; position: relative; }
    .icon-btn:hover { color: #4e73df; }
    .badge {
      position: absolute; top: -5px; right: -5px;
      background: #e74a3b; color: #fff; font-size: 0.6rem;
      padding: 2px 5px; border-radius: 10px;
    }

    .divider { width: 1px; height: 24px; background: #e3e6f0; margin: 0 0.5rem; }
    .user-display-name { font-size: 0.85rem; font-weight: 700; color: #4e73df; line-height: 1.2; }
    .user-display-role { font-size: 0.7rem; font-weight: 600; color: #b7c1d1; }
    .avatar-sm { width: 35px; height: 35px; border-radius: 50%; border: 2px solid #fff; }

    .page-body { padding: 1.5rem; flex-grow: 1; }

    .main-footer {
      padding: 1.5rem 0;
      background: #fff;
      border-top: 1px solid #e3e6f0;
      color: #858796;
      font-size: 0.8rem;
    }
  `]
})
export class MainLayoutComponent {
  auth = inject(AuthService);
  isSidebarCollapsed = false;

  logout() {
    this.auth.logout();
  }
}
