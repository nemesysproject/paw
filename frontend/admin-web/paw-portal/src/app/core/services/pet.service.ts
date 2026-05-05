import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';

export interface Media {
  id: string;
  url: string;
  type: string;
}

export interface Pet {
  id: string;
  name: string;
  gender: string;
  status: string;
  description: string;
  species_id: string;
  breed_id: string;
  created_at: string;
  last_latitude?: number;
  last_longitude?: number;
  media: Media[];
}

@Injectable({
  providedIn: 'root'
})
export class PetService {
  private http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/pets`;

  getPets(): Observable<Pet[]> {
    return this.http.get<Pet[]>(this.apiUrl);
  }

  getPetById(id: string): Observable<Pet> {
    return this.http.get<Pet>(`${this.apiUrl}/${id}`);
  }

  createPet(pet: FormData): Observable<any> {
    return this.http.post<any>(this.apiUrl, pet);
  }

  updatePet(id: string, pet: FormData): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/${id}`, pet);
  }
}
