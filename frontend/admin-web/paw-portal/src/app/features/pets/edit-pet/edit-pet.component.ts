import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, ActivatedRoute, RouterLink } from '@angular/router';
import { PetService } from '@core/services/pet.service';
import { CatalogService, CatalogItem } from '@core/services/catalog.service';
import { AuthService } from '@core/services/auth.service';
import { LucideAngularModule } from 'lucide-angular';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  selector: 'app-edit-pet',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, LucideAngularModule, RouterLink],
  template: `
    <div class="container-fluid py-4">
      <div class="row justify-content-center">
        <div class="col-lg-8">
          <div class="card shadow-sm border-0 rounded-lg">
            <div class="card-header bg-white py-3 border-0">
              <div class="d-flex align-items-center gap-3">
                <a routerLink="/pets" class="btn btn-light rounded-circle p-2">
                  <lucide-icon name="layout-dashboard" size="20"></lucide-icon>
                </a>
                <div>
                  <h3 class="mb-0 font-weight-bold">Editar Mascota</h3>
                  <p class="text-muted small mb-0">Actualiza la información de {{ petName() }}</p>
                </div>
              </div>
            </div>

            <div class="card-body p-4">
              <div *ngIf="isInitialLoading()" class="text-center py-5">
                <div class="spinner-border text-primary" role="status"></div>
                <p class="mt-2 text-muted">Cargando información...</p>
              </div>

              <form [formGroup]="petForm" (ngSubmit)="onSubmit()" *ngIf="!isInitialLoading()">
                <div class="row">
                  <!-- Datos Básicos -->
                  <div class="col-md-6 mb-3">
                    <label class="form-label font-weight-bold">Nombre (opcional)</label>
                    <input type="text" formControlName="name" class="form-control" placeholder="Ej. Toby">
                  </div>

                  <div class="col-md-6 mb-3">
                    <label class="form-label font-weight-bold">Estado *</label>
                    <select formControlName="status" class="form-select">
                      <option *ngFor="let s of statuses()" [value]="s.id">{{ s.name }}</option>
                    </select>
                  </div>

                  <!-- Clasificación -->
                  <div class="col-md-6 mb-3">
                    <label class="form-label font-weight-bold">Especie *</label>
                    <select formControlName="species_id" class="form-select" (change)="onSpeciesChange()">
                      <option *ngFor="let s of species()" [value]="s.id">{{ s.name }}</option>
                    </select>
                  </div>

                  <div class="col-md-6 mb-3">
                    <label class="form-label font-weight-bold">Raza (opcional)</label>
                    <select formControlName="breed_id" class="form-select">
                      <option value="">Desconocida / Mestizo</option>
                      <option *ngFor="let b of breeds()" [value]="b.id">{{ b.name }}</option>
                    </select>
                  </div>

                  <div class="col-md-6 mb-3">
                    <label class="form-label font-weight-bold">Género *</label>
                    <select formControlName="gender" class="form-select">
                      <option *ngFor="let g of genders()" [value]="g.id">{{ g.name }}</option>
                    </select>
                  </div>

                  <!-- Ubicación -->
                  <div class="col-12 mb-3">
                    <label class="form-label font-weight-bold">Descripción</label>
                    <textarea formControlName="description" class="form-control" rows="3"></textarea>
                  </div>

                  <div class="col-md-6 mb-3">
                    <label class="form-label font-weight-bold">Latitud</label>
                    <input type="number" formControlName="last_latitude" class="form-control" step="any">
                  </div>

                  <div class="col-md-6 mb-3">
                    <label class="form-label font-weight-bold">Longitud</label>
                    <input type="number" formControlName="last_longitude" class="form-control" step="any">
                  </div>

                  <!-- Multimedia Actual -->
                  <div class="col-12 mb-4" *ngIf="existingMedia().length > 0">
                    <label class="form-label font-weight-bold d-block">Fotos Actuales</label>
                    <div class="d-flex flex-wrap gap-2">
                      <div class="existing-media-item" *ngFor="let media of existingMedia()">
                        <img [src]="media.url" class="rounded shadow-sm">
                      </div>
                    </div>
                  </div>

                  <!-- Multimedia Nueva -->
                  <div class="col-12 mb-3">
                    <label class="form-label font-weight-bold d-block">Añadir nuevas Fotos o Videos (Opcional)</label>
                    <div class="upload-zone p-4 text-center border-dashed rounded-lg" (click)="fileInput.click()">
                      <input type="file" #fileInput (change)="onFileSelected($event)" multiple accept="image/*,video/*" class="d-none">
                      <lucide-icon name="dog" size="32" class="text-muted mb-2"></lucide-icon>
                      <p class="mb-0 text-muted small">Haz clic para añadir más archivos</p>
                    </div>

                    <!-- Previsualización -->
                    <div class="file-previews mt-3 d-flex flex-wrap gap-2" *ngIf="selectedFiles().length > 0">
                      <div class="preview-item position-relative" *ngFor="let file of selectedFiles(); let i = index">
                        <img [src]="previews()[i]" class="rounded shadow-sm" *ngIf="isImage(file)">
                        <button type="button" (click)="removeFile(i)" class="btn btn-danger btn-xs position-absolute top-0 end-0 rounded-circle">
                          &times;
                        </button>
                      </div>
                    </div>
                  </div>
                </div>

                <div class="error-message mt-3" *ngIf="errorMessage()">
                  {{ errorMessage() }}
                </div>

                <div class="d-flex justify-content-end gap-2 mt-4">
                  <button type="button" routerLink="/pets" class="btn btn-light px-4 rounded-pill">Cancelar</button>
                  <button type="submit" class="btn btn-primary px-5 rounded-pill" [disabled]="petForm.invalid || isLoading()">
                    <span *ngIf="!isLoading()">Guardar Cambios</span>
                    <span *ngIf="isLoading()">Guardando...</span>
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .form-label { color: #475569; }
    .form-control, .form-select {
      background-color: #f8fafc;
      border: 1px solid #e2e8f0;
      padding: 0.65rem 0.75rem;
    }
    .error-message {
      color: #f43f5e;
      background: rgba(244, 63, 94, 0.1);
      padding: 0.75rem;
      border-radius: 8px;
    }
    .upload-zone { border: 2px dashed #e2e8f0; cursor: pointer; }
    .preview-item img, .existing-media-item img {
      width: 100px;
      height: 100px;
      object-fit: cover;
    }
    .btn-xs { padding: 0.1rem 0.3rem; font-size: 0.75rem; }
  `]
})
export class EditPetComponent implements OnInit {
  private fb = inject(FormBuilder);
  private petService = inject(PetService);
  private catalogService = inject(CatalogService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  petId: string | null = null;
  petName = signal('');
  isInitialLoading = signal(true);
  isLoading = signal(false);
  errorMessage = signal<string | null>(null);
  
  selectedFiles = signal<File[]>([]);
  previews = signal<string[]>([]);
  existingMedia = signal<any[]>([]);

  species = signal<CatalogItem[]>([]);
  breeds = signal<CatalogItem[]>([]);
  genders = signal<CatalogItem[]>([]);
  statuses = signal<CatalogItem[]>([]);

  petForm = this.fb.group({
    name: [''],
    gender: ['', Validators.required],
    status: ['', Validators.required],
    description: [''],
    species_id: ['', Validators.required],
    breed_id: [''],
    last_latitude: [null],
    last_longitude: [null]
  });

  ngOnInit() {
    this.petId = this.route.snapshot.paramMap.get('id');
    this.loadCatalogs();
    if (this.petId) {
      this.loadPet();
    }
  }

  loadCatalogs() {
    this.catalogService.getSpecies().subscribe(data => this.species.set(data));
    this.catalogService.getGenders().subscribe(data => this.genders.set(data));
    this.catalogService.getStatuses().subscribe(data => this.statuses.set(data));
  }

  loadPet() {
    if (!this.petId) return;
    
    this.petService.getPetById(this.petId).subscribe({
      next: (pet) => {
        this.petName.set(pet.name || 'Mascota');
        this.existingMedia.set(pet.media || []);
        
        this.petForm.patchValue({
          name: pet.name,
          gender: pet.gender,
          status: pet.status,
          description: pet.description,
          species_id: pet.species_id,
          breed_id: pet.breed_id,
          last_latitude: pet.last_latitude as any,
          last_longitude: pet.last_longitude as any
        });

        if (pet.species_id) {
          this.catalogService.getBreedsBySpecies(pet.species_id).subscribe(data => {
            this.breeds.set(data);
          });
        }

        this.isInitialLoading.set(false);
      },
      error: () => {
        this.errorMessage.set('Error al cargar la mascota');
        this.isInitialLoading.set(false);
      }
    });
  }

  onFileSelected(event: any) {
    const files: FileList = event.target.files;
    if (files) {
      const newFiles = Array.from(files);
      const currentFiles = this.selectedFiles();
      const currentPreviews = this.previews();
      newFiles.forEach(file => {
        currentFiles.push(file);
        currentPreviews.push(URL.createObjectURL(file));
      });
      this.selectedFiles.set([...currentFiles]);
      this.previews.set([...currentPreviews]);
    }
  }

  removeFile(index: number) {
    const currentFiles = this.selectedFiles();
    const currentPreviews = this.previews();
    URL.revokeObjectURL(currentPreviews[index]);
    currentFiles.splice(index, 1);
    currentPreviews.splice(index, 1);
    this.selectedFiles.set([...currentFiles]);
    this.previews.set([...currentPreviews]);
  }

  isImage(file: File): boolean {
    return file.type.startsWith('image/');
  }

  onSpeciesChange() {
    const speciesId = this.petForm.get('species_id')?.value;
    if (speciesId) {
      this.catalogService.getBreedsBySpecies(speciesId).subscribe(data => {
        this.breeds.set(data);
        this.petForm.patchValue({ breed_id: '' });
      });
    }
  }

  onSubmit() {
    if (this.petForm.valid && this.petId) {
      this.isLoading.set(true);
      this.errorMessage.set(null);

      const formData = new FormData();
      const formRawValue = this.petForm.getRawValue();
      // Agregar campos del formulario
      Object.keys(formRawValue).forEach(key => {
        const value = (formRawValue as any)[key];
        if (value !== null && value !== undefined) {
          formData.append(key, value.toString());
        }
      });

      this.selectedFiles().forEach(file => {
        formData.append('file', file);
      });

      this.petService.updatePet(this.petId, formData).subscribe({
        next: () => {
          this.router.navigate(['/pets']);
        },
        error: (err: HttpErrorResponse) => {
          this.errorMessage.set(err.error?.error || 'Error al actualizar la mascota');
          this.isLoading.set(false);
        }
      });
    }
  }
}
