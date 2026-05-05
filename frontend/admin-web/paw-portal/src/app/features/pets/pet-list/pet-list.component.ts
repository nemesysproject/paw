import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PetService, Pet } from '@core/services/pet.service';
import { LucideAngularModule } from 'lucide-angular';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-pet-list',
  standalone: true,
  imports: [CommonModule, LucideAngularModule, RouterLink],
  template: `
    <div class="container-fluid">
      <div class="d-flex justify-content-between align-items-center mb-4">
        <div>
          <h2 class="h3 mb-0 text-gray-800">Mascotas Registradas</h2>
          <p class="text-muted">Gestiona el catálogo de mascotas perdidas y encontradas</p>
        </div>
        <button routerLink="/pets/create" class="btn btn-primary d-flex align-items-center gap-2">
          <lucide-icon name="dog" size="18"></lucide-icon>
          Nueva Mascota
        </button>
      </div>

      <div class="row" *ngIf="!isLoading(); else loadingTpl">
        <div class="col-xl-3 col-lg-4 col-md-6 mb-4" *ngFor="let pet of pets()">
          <div class="card pet-card h-100 border-0 shadow-sm">
            <div class="position-relative">
              <img [src]="getFirstImage(pet)" class="card-img-top pet-img" [alt]="pet.name || 'Mascota'">
              <span class="badge position-absolute top-0 end-0 m-3" [ngClass]="getStatusBadgeClass(pet.status)">
                {{ pet.status }}
              </span>
            </div>
            <div class="card-body">
              <h5 class="card-title font-weight-bold mb-1">{{ pet.name || 'Sin nombre' }}</h5>
              <p class="text-muted small mb-2">
                <lucide-icon name="settings" size="12" class="me-1"></lucide-icon>
                {{ pet.gender === 'MALE' ? 'Macho' : 'Hembra' }}
              </p>
              <p class="card-text text-truncate-2 small text-muted mb-3">
                {{ pet.description || 'Sin descripción disponible.' }}
              </p>
              <div class="d-flex justify-content-between align-items-center mt-auto">
                <span class="text-xs text-muted">
                  <lucide-icon name="layout-dashboard" size="12" class="me-1"></lucide-icon>
                  {{ pet.created_at | date:'shortDate' }}
                </span>
                <div class="d-flex gap-1">
                  <a [routerLink]="['/pets', pet.id, 'edit']" class="btn btn-sm btn-outline-secondary rounded-pill p-1 px-2">
                    <lucide-icon name="settings" size="14"></lucide-icon>
                  </a>
                  <a [routerLink]="['/pets', pet.id]" class="btn btn-sm btn-outline-primary rounded-pill px-3">
                    Ver detalle
                  </a>
                </div>
              </div>
            </div>
          </div>
        </div>
        
        <div class="col-12 text-center py-5" *ngIf="pets().length === 0">
          <lucide-icon name="search" size="48" class="text-muted mb-3"></lucide-icon>
          <p class="text-muted">No se encontraron mascotas registradas.</p>
        </div>
      </div>

      <ng-template #loadingTpl>
        <div class="text-center py-5">
          <div class="spinner-border text-primary" role="status">
            <span class="visually-hidden">Cargando...</span>
          </div>
          <p class="mt-2 text-muted">Cargando mascotas...</p>
        </div>
      </ng-template>
    </div>
  `,
  styles: [`
    .pet-card {
      transition: transform 0.2s, box-shadow 0.2s;
      border-radius: 15px;
      overflow: hidden;
    }
    .pet-card:hover {
      transform: translateY(-5px);
      box-shadow: 0 10px 20px rgba(0,0,0,0.1) !important;
    }
    .pet-img {
      height: 200px;
      object-fit: cover;
    }
    .text-truncate-2 {
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }
    .badge-lost { background-color: #f43f5e; }
    .badge-found { background-color: #10b981; }
    .badge-adopted { background-color: #6366f1; }
    .text-xs { font-size: 0.75rem; }
  `]
})
export class PetListComponent implements OnInit {
  private petService = inject(PetService);
  
  pets = signal<Pet[]>([]);
  isLoading = signal(true);

  ngOnInit() {
    this.loadPets();
  }

  loadPets() {
    this.isLoading.set(true);
    this.petService.getPets().subscribe({
      next: (data) => {
        this.pets.set(data);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
      }
    });
  }

  getFirstImage(pet: Pet): string {
    return pet.media && pet.media.length > 0 
      ? pet.media[0].url 
      : 'assets/images/placeholder-pet.png';
  }

  getStatusBadgeClass(status: string): string {
    switch(status) {
      case 'LOST': return 'badge-lost';
      case 'FOUND': return 'badge-found';
      case 'ADOPTED': return 'badge-adopted';
      default: return 'bg-secondary';
    }
  }
}
