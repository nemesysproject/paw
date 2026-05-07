import { Component, ChangeDetectionStrategy, signal } from '@angular/core';

interface Shelter {
  id: number;
  name: string;
  contactEmail: string;
  phone: string;
  location: string;
  capacity: number;
}

@Component({
  selector: 'app-shelters',
  imports: [],
  templateUrl: './shelters.html',
  styleUrl: './shelters.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Shelters {
  // Usamos signals para un mejor rendimiento y reactividad
  shelters = signal<Shelter[]>([
    { id: 1, name: 'Huellitas Felices', contactEmail: 'info@huellitas.org', phone: '555-0123', location: 'Zona Norte', capacity: 45 },
    { id: 2, name: 'Refugio San Roque', contactEmail: 'ayuda@sanroque.com', phone: '555-0456', location: 'Barrio Sur', capacity: 20 }
  ]);

  applyFilter(event: Event): void {
    const input = event.target as HTMLInputElement;
    console.log('Buscando refugios con filtro:', input.value);
  }

  openAddShelterDialog(): void {
    console.log('Abriendo diálogo para añadir refugio...');
  }

  editShelter(shelter: Shelter): void {
    console.log('Editando refugio:', shelter);
  }

  deleteShelter(id: number): void {
    this.shelters.update(list => list.filter(s => s.id !== id));
  }
}
