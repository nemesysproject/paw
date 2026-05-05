import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { PetService } from '@core/services/pet.service';
import { CatalogService, CatalogItem } from '@core/services/catalog.service';
import { AuthService } from '@core/services/auth.service';
import { LucideAngularModule } from 'lucide-angular';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  selector: 'app-create-pet',
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
                  <h3 class="mb-0 font-weight-bold">Registrar Mascota</h3>
                  <p class="text-muted small mb-0">Completa los datos para el nuevo avistamiento o mascota</p>
                </div>
              </div>
            </div>

            <div class="card-body p-4">
              <form [formGroup]="petForm" (ngSubmit)="onSubmit()">
                <div class="row">
                  <!-- Datos Básicos -->
                  <div class="col-md-6 mb-3">
                    <label class="form-label font-weight-bold">Nombre (opcional)</label>
                    <input type="text" formControlName="name" class="form-control" placeholder="Ej. Toby">
                  </div>

                  <div class="col-md-6 mb-3">
                    <label class="form-label font-weight-bold">Estado *</label>
                    <select formControlName="status" class="form-select">
                      <option value="" disabled>Selecciona estado</option>
                      <option *ngFor="let s of statuses()" [value]="s.id">{{ s.name }}</option>
                    </select>
                  </div>

                  <!-- Clasificación -->
                  <div class="col-md-6 mb-3">
                    <label class="form-label font-weight-bold">Especie *</label>
                    <select formControlName="species_id" class="form-select" (change)="onSpeciesChange()">
                      <option value="" disabled>Selecciona especie</option>
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
                      <option value="" disabled>Selecciona género</option>
                      <option *ngFor="let g of genders()" [value]="g.id">{{ g.name }}</option>
                    </select>
                  </div>

                  <!-- Ubicación -->
                  <div class="col-12 mb-3">
                    <label class="form-label font-weight-bold">Descripción</label>
                    <textarea formControlName="description" class="form-control" rows="3" placeholder="Detalles físicos, comportamiento, etc."></textarea>
                  </div>

                  <div class="col-md-6 mb-3">
                    <label class="form-label font-weight-bold">Latitud (Simulada)</label>
                    <input type="number" formControlName="last_latitude" class="form-control" step="any">
                  </div>

                  <div class="col-md-6 mb-3">
                    <label class="form-label font-weight-bold">Longitud (Simulada)</label>
                    <input type="number" formControlName="last_longitude" class="form-control" step="any">
                  </div>

                  <!-- Multimedia -->
                  <div class="col-12 mb-3">
                    <label class="form-label font-weight-bold d-block">Fotos o Videos *</label>
                    <div class="upload-zone p-4 text-center border-dashed rounded-lg" (click)="fileInput.click()">
                      <input type="file" #fileInput (change)="onFileSelected($event)" multiple accept="image/*,video/*" class="d-none">
                      <lucide-icon name="dog" size="32" class="text-muted mb-2"></lucide-icon>
                      <p class="mb-0 text-muted small">Haz clic para subir imágenes o videos</p>
                      <span class="text-xs text-muted">(Formatos permitidos: JPG, PNG, MP4)</span>
                    </div>

                    <!-- Previsualización -->
                    <div class="file-previews mt-3 d-flex flex-wrap gap-2" *ngIf="selectedFiles().length > 0">
                      <div class="preview-item position-relative" *ngFor="let file of selectedFiles(); let i = index">
                        <img [src]="previews()[i]" class="rounded shadow-sm" *ngIf="isImage(file)">
                        <div class="video-placeholder rounded shadow-sm d-flex align-items-center justify-content-center bg-light" *ngIf="!isImage(file)">
                          <lucide-icon name="settings" size="18"></lucide-icon>
                        </div>
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
                    <span *ngIf="!isLoading()">Guardar Mascota</span>
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
    .form-control:focus, .form-select:focus {
      background-color: #fff;
      box-shadow: 0 0 0 2px rgba(99, 102, 241, 0.1);
      border-color: #6366f1;
    }
    .error-message {
      color: #f43f5e;
      background: rgba(244, 63, 94, 0.1);
      padding: 0.75rem;
      border-radius: 8px;
      font-size: 0.875rem;
    }
    .upload-zone {
      border: 2px dashed #e2e8f0;
      cursor: pointer;
      transition: all 0.2s;
    }
    .upload-zone:hover {
      border-color: #6366f1;
      background-color: #f8fafc;
    }
    .preview-item img {
      width: 80px;
      height: 80px;
      object-fit: cover;
    }
    .video-placeholder {
      width: 80px;
      height: 80px;
      color: #6366f1;
    }
    .btn-xs {
      padding: 0.1rem 0.3rem;
      font-size: 0.75rem;
      line-height: 1;
    }
  `]
})
export class CreatePetComponent implements OnInit {
  private fb = inject(FormBuilder);
  private petService = inject(PetService);
  private catalogService = inject(CatalogService);
  private auth = inject(AuthService);
  private router = inject(Router);

  isLoading = signal(false);
  errorMessage = signal<string | null>(null);
  selectedFiles = signal<File[]>([]);
  previews = signal<string[]>([]);

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
    breed_id: [{ value: '', disabled: true }],
    reporter_id: ['', Validators.required],
    shelter_id: [null],
    last_latitude: [null],
    last_longitude: [null]
  });

  ngOnInit() {
    this.loadCatalogs();
    
    // Asignar el reporter_id del usuario actual de forma reactiva
    const user = this.auth.currentUser();
    if (user) {
      this.petForm.patchValue({ reporter_id: user.id });
    } else {
      // Si no hay usuario (caso anónimo o carga lenta), podrías poner 'anonimo' como en el ejemplo
      this.petForm.patchValue({ reporter_id: 'anonimo' });
    }
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
    
    // Liberar memoria del objeto URL
    URL.revokeObjectURL(currentPreviews[index]);
    
    currentFiles.splice(index, 1);
    currentPreviews.splice(index, 1);
    
    this.selectedFiles.set([...currentFiles]);
    this.previews.set([...currentPreviews]);
  }

  isImage(file: File): boolean {
    return file.type.startsWith('image/');
  }

  loadCatalogs() {
    this.catalogService.getSpecies().subscribe(data => this.species.set(data));
    this.catalogService.getGenders().subscribe(data => this.genders.set(data));
    this.catalogService.getStatuses().subscribe(data => this.statuses.set(data));
  }

  onSpeciesChange() {
    const speciesId = this.petForm.get('species_id')?.value;
    const breedControl = this.petForm.get('breed_id');
    
    if (speciesId) {
      breedControl?.enable();
      this.catalogService.getBreedsBySpecies(speciesId).subscribe(data => {
        this.breeds.set(data);
        this.petForm.patchValue({ breed_id: '' });
      });
    } else {
      breedControl?.disable();
      this.breeds.set([]);
    }
  }

  onSubmit() {
    if (this.petForm.valid) {
      if (this.selectedFiles().length === 0) {
        this.errorMessage.set('Debes subir al menos una imagen');
        return;
      }

      this.isLoading.set(true);
      this.errorMessage.set(null);

      // Crear FormData para multipart
      const formData = new FormData();
      const formRawValue = this.petForm.getRawValue();
      
      console.log('Enviando datos de mascota:', formRawValue);

      // Agregar campos del formulario
      Object.keys(formRawValue).forEach(key => {
        const value = (formRawValue as any)[key];
        // Solo omitimos si es estrictamente null o undefined para que el backend reciba Option::None
        // Pero enviamos strings vacíos si el usuario no seleccionó algo (ej. breed_id)
        if (value !== null && value !== undefined) {
          formData.append(key, value.toString());
        }
      });

      // Agregar archivos
      this.selectedFiles().forEach(file => {
        formData.append('file', file);
      });

      this.petService.createPet(formData).subscribe({
        next: () => {
          this.router.navigate(['/pets']);
        },
        error: (err: HttpErrorResponse) => {
          this.errorMessage.set(err.error?.error || 'Error al guardar la mascota');
          this.isLoading.set(false);
        }
      });
    }
  }
}
