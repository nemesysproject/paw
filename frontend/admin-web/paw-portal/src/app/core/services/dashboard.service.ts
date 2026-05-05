import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '@env/environment';
import { Observable } from 'rxjs';

export interface DashboardStats {
  total_lost: number;
  total_found: number;
  total_adopted: number;
  total_pets: number;
}

export interface PetLocation {
  id: string;
  name: string | null;
  status: string;
  last_latitude: number;
  last_longitude: number;
  image_url: string | null;
}

export interface DashboardResponse {
  stats: DashboardStats;
  locations: PetLocation[];
}

@Injectable({
  providedIn: 'root'
})
export class DashboardService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/dashboard`;

  getDashboardData(): Observable<DashboardResponse> {
    return this.http.get<DashboardResponse>(this.apiUrl);
  }
}
