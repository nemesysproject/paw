import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';

export interface CatalogItem {
  id: string;
  name: string;
}

@Injectable({
  providedIn: 'root'
})
export class CatalogService {
  private http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/catalogs`;

  getSpecies(): Observable<CatalogItem[]> {
    return this.http.get<CatalogItem[]>(`${this.apiUrl}/species`);
  }

  getBreedsBySpecies(speciesId: string): Observable<CatalogItem[]> {
    return this.http.get<CatalogItem[]>(`${this.apiUrl}/species/${speciesId}/breeds`);
  }

  getGenders(): Observable<CatalogItem[]> {
    return this.http.get<CatalogItem[]>(`${this.apiUrl}/genders`);
  }

  getStatuses(): Observable<CatalogItem[]> {
    return this.http.get<CatalogItem[]>(`${this.apiUrl}/statuses`);
  }
}
