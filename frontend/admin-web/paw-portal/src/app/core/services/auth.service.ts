import { Injectable, signal, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap, throwError } from 'rxjs';
import { Router } from '@angular/router';
import { environment } from '@env/environment';

export interface User {
  id: string;
  email: string;
  name: string;
  role: string;
}

export interface AuthResponse {
  access_token: string;
  refresh_token: string;
  token_type: string;
  expires_in: number;
}

interface JwtPayload {
  sub?: string;
  role?: string;
  exp?: number;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);
  private readonly apiUrl = `${environment.apiUrl}/auth`;

  // Signals for state management
  currentUser = signal<User | null>(null);
  isAuthenticated = signal<boolean>(false);

  constructor() {
    this.loadToken();
  }

  register(data: unknown): Observable<unknown> {
    return this.http.post(`${this.apiUrl}/register`, data);
  }

  login(credentials: unknown): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/login`, credentials).pipe(
      tap((res) => this.handleAuthentication(res))
    );
  }

  logout(): void {
    localStorage.removeItem('access_token');
    localStorage.removeItem('refresh_token');
    this.currentUser.set(null);
    this.isAuthenticated.set(false);
    this.router.navigate(['/auth/login']);
  }

  refreshToken(): Observable<AuthResponse> {
    const refreshToken = localStorage.getItem('refresh_token');
    if (!refreshToken) {
      return throwError(() => new Error('No refresh token available'));
    }

    return this.http
      .post<AuthResponse>(`${this.apiUrl}/refresh`, { refresh_token: refreshToken })
      .pipe(tap((res) => this.handleAuthentication(res)));
  }

  private handleAuthentication(res: AuthResponse): void {
    const payload = this.decodeJwtPayload(res.access_token);
    if (!payload || this.isTokenExpired(payload)) {
      this.logout();
      return;
    }

    localStorage.setItem('access_token', res.access_token);
    localStorage.setItem('refresh_token', res.refresh_token);
    this.isAuthenticated.set(true);
    this.currentUser.set({
      id: payload.sub ?? '',
      email: '',
      name: payload.role === 'ADMIN' ? 'Administrador' : 'Usuario',
      role: payload.role ?? 'USER'
    });
  }

  private loadToken(): void {
    const token = localStorage.getItem('access_token');
    if (!token) return;

    const payload = this.decodeJwtPayload(token);
    if (!payload || this.isTokenExpired(payload)) {
      localStorage.removeItem('access_token');
      this.isAuthenticated.set(false);
      this.currentUser.set(null);
      return;
    }

    this.isAuthenticated.set(true);
    this.currentUser.set({
      id: payload.sub ?? '',
      email: '',
      name: payload.role === 'ADMIN' ? 'Administrador' : 'Usuario',
      role: payload.role ?? 'USER'
    });
  }

  getAccessToken(): string | null {
    const token = localStorage.getItem('access_token');
    if (!token) return null;
    const payload = this.decodeJwtPayload(token);
    if (!payload || this.isTokenExpired(payload)) {
      return null;
    }
    return token;
  }

  private decodeJwtPayload(token: string): JwtPayload | null {
    try {
      const parts = token.split('.');
      if (parts.length !== 3) return null;
      const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
      const padded = base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), '=');
      return JSON.parse(atob(padded)) as JwtPayload;
    } catch {
      return null;
    }
  }

  private isTokenExpired(payload: JwtPayload): boolean {
    if (!payload.exp) return false;
    return payload.exp * 1000 <= Date.now();
  }
}
