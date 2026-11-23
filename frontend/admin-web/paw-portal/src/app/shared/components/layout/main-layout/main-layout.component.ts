import { Component, inject } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { CommonModule } from '@angular/common';
import { LucideAngularModule } from 'lucide-angular';
import { AuthService } from '@core/services/auth.service';
import { AiAssistantDrawerComponent } from '@shared/components/ai-assistant-drawer/ai-assistant-drawer.component';

@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive, LucideAngularModule, AiAssistantDrawerComponent],
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

          <a routerLink="/shelters" routerLinkActive="active" class="nav-item">
            <lucide-icon name="home" class="nav-icon"></lucide-icon>
            <span class="nav-label" *ngIf="!isSidebarCollapsed">Refugios</span>
          </a>

          <a routerLink="/campaigns" routerLinkActive="active" class="nav-item">
            <lucide-icon name="megaphone" class="nav-icon"></lucide-icon>
            <span class="nav-label" *ngIf="!isSidebarCollapsed">Campañas y Ads</span>
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

      <!-- Copiloto IA flotante disponible en todo el portal -->
      <app-ai-assistant-drawer></app-ai-assistant-drawer>
    </div>
  `,
  styles: [`
    .main-wrapper {
      display: flex;
      min-height: 100vh;
      background: transparent;
      padding: 14px;
      gap: 14px;
    }

    .sidebar {
      width: 248px;
      background: rgba(255, 255, 255, 0.14);
      backdrop-filter: blur(26px) saturate(170%);
      -webkit-backdrop-filter: blur(26px) saturate(170%);
      border: 1px solid rgba(255, 255, 255, 0.32);
      border-radius: 24px;
      color: rgba(255, 255, 255, 0.82);
      display: flex;
      flex-direction: column;
      transition: all 0.25s ease-in-out;
      box-shadow: 0 14px 36px rgba(10, 15, 36, 0.3), inset 0 1px 0 rgba(255, 255, 255, 0.45);
      z-index: 1000;
      overflow: hidden;
    }

    .sidebar.collapsed {
      width: 76px;
    }

    .sidebar-header {
      padding: 1.4rem 1.1rem;
      border-bottom: 1px solid rgba(255, 255, 255, 0.18);
    }

    .logo {
      display: flex;
      align-items: center;
      gap: 10px;
      color: #ffffff;
    }

    .logo-icon { color: #ffffff; width: 28px; filter: drop-shadow(0 2px 6px rgba(255,255,255,0.35)); }
    .logo-text { font-weight: 800; font-size: 1.05rem; letter-spacing: 1.2px; }

    .sidebar-user {
      padding: 1.2rem 1.1rem;
      display: flex;
      align-items: center;
      gap: 12px;
      border-bottom: 1px solid rgba(255, 255, 255, 0.14);
    }

    .user-avatar img {
      width: 44px;
      border-radius: 14px;
      border: 1.5px solid rgba(255, 255, 255, 0.45);
    }
    .user-info { display: flex; flex-direction: column; }
    .user-name { color: #ffffff; font-weight: 700; font-size: 0.9rem; }
    .user-role { font-size: 0.75rem; color: rgba(255, 255, 255, 0.68); }

    .sidebar-nav { flex-grow: 1; padding: 0.75rem 0.65rem; }
    .nav-section-title {
      font-size: 0.65rem;
      font-weight: 800;
      color: rgba(255, 255, 255, 0.55);
      padding: 1.1rem 0.9rem 0.4rem;
      text-transform: uppercase;
      letter-spacing: 1.5px;
    }

    .nav-item {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 0.75rem 0.95rem;
      margin-bottom: 4px;
      color: rgba(255, 255, 255, 0.84);
      text-decoration: none;
      transition: all 0.2s ease;
      border-radius: 14px;
      border: 1px solid transparent;
      background: none;
      width: 100%;
      text-align: left;
    }

    .nav-item:hover {
      background: rgba(255, 255, 255, 0.16);
      border-color: rgba(255, 255, 255, 0.24);
      color: #ffffff;
    }

    .nav-item.active {
      color: #ffffff;
      background: rgba(255, 255, 255, 0.24);
      border: 1px solid rgba(255, 255, 255, 0.45);
      box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.5), 0 6px 16px rgba(10, 15, 36, 0.2);
      font-weight: 600;
    }

    .nav-icon { width: 18px; height: 18px; }

    .sidebar-footer { padding: 0.85rem; border-top: 1px solid rgba(255, 255, 255, 0.16); }
    .logout-btn { color: #fecdd3; }
    .logout-btn:hover { background: rgba(244, 63, 94, 0.24); border-color: rgba(251, 113, 133, 0.45); color: #ffffff; }

    .content-container {
      flex-grow: 1;
      display: flex;
      flex-direction: column;
      overflow-x: hidden;
      gap: 14px;
    }

    .top-navbar {
      height: 68px;
      background: rgba(255, 255, 255, 0.16);
      backdrop-filter: blur(24px) saturate(165%);
      -webkit-backdrop-filter: blur(24px) saturate(165%);
      border: 1px solid rgba(255, 255, 255, 0.32);
      border-radius: 22px;
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 0 1.4rem;
      box-shadow: 0 10px 28px rgba(10, 15, 36, 0.22), inset 0 1px 0 rgba(255, 255, 255, 0.42);
      z-index: 900;
    }

    .btn-toggle {
      background: rgba(255, 255, 255, 0.16);
      border: 1px solid rgba(255, 255, 255, 0.3);
      border-radius: 12px;
      padding: 6px 8px;
      color: #ffffff;
      font-size: 1.1rem;
      display: flex;
      align-items: center;
    }

    .header-right { display: flex; align-items: center; gap: 0.85rem; }
    .icon-btn {
      background: rgba(255, 255, 255, 0.14);
      border: 1px solid rgba(255, 255, 255, 0.26);
      border-radius: 12px;
      padding: 6px 8px;
      color: #ffffff;
      position: relative;
      display: flex;
      align-items: center;
    }
    .icon-btn:hover { background: rgba(255, 255, 255, 0.26); color: #ffffff; }
    .badge {
      position: absolute; top: -6px; right: -6px;
      background: rgba(244, 63, 94, 0.9); color: #fff; font-size: 0.6rem;
      padding: 2px 6px; border-radius: 999px;
      border: 1px solid rgba(255, 255, 255, 0.5);
    }

    .divider { width: 1px; height: 24px; background: rgba(255, 255, 255, 0.28); margin: 0 0.25rem; }
    .user-display-name { font-size: 0.85rem; font-weight: 700; color: #ffffff; line-height: 1.2; }
    .user-display-role { font-size: 0.7rem; font-weight: 600; color: rgba(255, 255, 255, 0.72) !important; }
    .avatar-sm { width: 36px; height: 36px; border-radius: 50%; border: 2px solid rgba(255, 255, 255, 0.55); }

    .page-body { padding: 0.5rem 0.25rem; flex-grow: 1; }

    .main-footer {
      padding: 1rem 1.4rem;
      background: rgba(255, 255, 255, 0.12);
      backdrop-filter: blur(20px);
      -webkit-backdrop-filter: blur(20px);
      border: 1px solid rgba(255, 255, 255, 0.24);
      border-radius: 18px;
      color: rgba(255, 255, 255, 0.78);
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
