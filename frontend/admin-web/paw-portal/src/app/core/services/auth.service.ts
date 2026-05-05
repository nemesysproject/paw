import { Injectable, signal, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap, of } from 'rxjs';
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

  register(data: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/register`, data);
  }

  login(credentials: any): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/login`, credentials).pipe(
      tap(res => this.handleAuthentication(res))
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
    if (!refreshToken) return of();

    return this.http.post<AuthResponse>(`${this.apiUrl}/refresh`, { refresh_token: refreshToken }).pipe(
      tap(res => this.handleAuthentication(res))
    );
  }

  private handleAuthentication(res: AuthResponse): void {
    localStorage.setItem('access_token', res.access_token);
    localStorage.setItem('refresh_token', res.refresh_token);
    this.isAuthenticated.set(true);
    // Decode user info from JWT if needed or fetch from another endpoint
    // For now, we'll set a mock user or wait for a /me endpoint
    this.currentUser.set({ id: '1', email: '', name: 'Admin', role: 'USER' });
  }

  private loadToken(): void {
    const token = localStorage.getItem('access_token');
    if (token) {
      this.isAuthenticated.set(true);
      this.currentUser.set({ id: '1', email: '', name: 'Admin', role: 'USER' });
    }
  }

  getAccessToken(): string | null {
    return localStorage.getItem('access_token');
  }
}
