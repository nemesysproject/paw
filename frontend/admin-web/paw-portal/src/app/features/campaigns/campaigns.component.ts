import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';
import {
  AdCampaignSummary,
  AdCatalogItem,
  AdsService,
  Advertiser,
  CreateAdCampaignRequest,
  CreateAdvertiserRequest,
  UpdateAdCampaignRequest,
  UpdateAdvertiserRequest
} from '@core/services/ads.service';

@Component({
  selector: 'app-campaigns',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, LucideAngularModule],
  templateUrl: './campaigns.component.html',
  styleUrl: './campaigns.component.css'
})
export class CampaignsComponent implements OnInit {
  private adsService = inject(AdsService);
  private fb = inject(FormBuilder);

  activeTab = signal<'campaigns' | 'advertisers'>('campaigns');
  isLoading = signal(false);
  isSaving = signal(false);
  isUploadingImage = signal(false);
  errorMessage = signal<string | null>(null);
  successMessage = signal<string | null>(null);

  campaigns = signal<AdCampaignSummary[]>([]);
  advertisers = signal<Advertiser[]>([]);
  categories = signal<AdCatalogItem[]>([
    { id: 'PET_FOOD', name: 'Alimento para Mascotas' },
    { id: 'VETERINARY', name: 'Veterinarias y Clínicas' },
    { id: 'GOVERNMENT', name: 'Campañas Gubernamentales' },
    { id: 'PET_SHOP', name: 'Tiendas y Negocios Afines' },
    { id: 'SHELTER_EVENT', name: 'Eventos de Refugios y Adopción' }
  ]);
  placements = signal<AdCatalogItem[]>([
    { id: 'HOME_FEED', name: 'Feed Principal (Tarjeta Nativa)' },
    { id: 'BANNER_TOP', name: 'Banner / Carrusel Superior' },
    { id: 'POST_REPORT', name: 'Confirmación Post-Reporte' },
    { id: 'DIRECTORY_FEATURED', name: 'Directorio Destacado' }
  ]);

  showCampaignForm = signal(false);
  editingCampaignId = signal<string | null>(null);

  showAdvertiserForm = signal(false);
  editingAdvertiserId = signal<string | null>(null);

  // Métricas globales
  totalCampaigns = computed(() => this.campaigns().length);
  activeCampaignsCount = computed(() => this.campaigns().filter((c) => c.is_active).length);
  totalImpressions = computed(() =>
    this.campaigns().reduce((acc, c) => acc + (c.impressions_count || 0), 0)
  );
  totalClicks = computed(() =>
    this.campaigns().reduce((acc, c) => acc + (c.clicks_count || 0), 0)
  );
  averageCtr = computed(() => {
    const imp = this.totalImpressions();
    if (imp === 0) return 0;
    return Math.round((this.totalClicks() / imp) * 10000) / 100;
  });

  campaignForm = this.fb.nonNullable.group({
    advertiser_id: ['', Validators.required],
    title: ['', [Validators.required, Validators.maxLength(120)]],
    description: [''],
    cta_label: ['Ver más'],
    target_url: [''],
    image_url: ['', Validators.required],
    image_public_id: [''],
    category: ['PET_FOOD', Validators.required],
    placement: ['HOME_FEED', Validators.required],
    badge_label: ['Patrocinado'],
    latitude: [null as number | null],
    longitude: [null as number | null],
    radius_km: [10 as number | null],
    priority: [0],
    starts_at: ['', Validators.required],
    ends_at: ['', Validators.required],
    is_active: [true]
  });

  advertiserForm = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(120)]],
    category: ['PET_FOOD', Validators.required],
    contact_name: [''],
    email: ['', Validators.email],
    phone: [''],
    whatsapp: [''],
    website: [''],
    logo_url: [''],
    is_government: [false],
    official_source_url: [''],
    is_active: [true]
  });

  ngOnInit(): void {
    this.loadAll();
  }

  loadAll(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.adsService.getCatalogs().subscribe({
      next: (res) => {
        if (res.categories.length) this.categories.set(res.categories);
        if (res.placements.length) this.placements.set(res.placements);
      },
      error: () => {
        // Mantiene catálogos por defecto si el backend no responde
      }
    });

    this.adsService.listAdvertisers().subscribe({
      next: (data) => this.advertisers.set(data),
      error: (err) => this.handleError('No se pudieron cargar los anunciantes.', err)
    });

    this.adsService.listCampaigns().subscribe({
      next: (data) => {
        this.campaigns.set(data);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.isLoading.set(false);
        this.handleError('No se pudieron cargar las campañas.', err);
      }
    });
  }

  // --- Campaign Form Actions ---

  openCreateCampaign(): void {
    this.clearMessages();
    this.editingCampaignId.set(null);
    const now = new Date();
    const nextMonth = new Date(now.getTime() + 30 * 24 * 60 * 60 * 1000);

    this.campaignForm.reset({
      advertiser_id: this.advertisers()[0]?.id ?? '',
      title: '',
      description: '',
      cta_label: 'Ver más',
      target_url: '',
      image_url: '',
      image_public_id: '',
      category: 'PET_FOOD',
      placement: 'HOME_FEED',
      badge_label: 'Patrocinado',
      latitude: null,
      longitude: null,
      radius_km: 10,
      priority: 1,
      starts_at: this.toDatetimeLocal(now),
      ends_at: this.toDatetimeLocal(nextMonth),
      is_active: true
    });
    this.showCampaignForm.set(true);
  }

  openEditCampaign(c: AdCampaignSummary): void {
    this.clearMessages();
    this.editingCampaignId.set(c.id);
    this.campaignForm.reset({
      advertiser_id: c.advertiser_id,
      title: c.title,
      description: c.description ?? '',
      cta_label: c.cta_label,
      target_url: c.target_url ?? '',
      image_url: c.image_url,
      image_public_id: c.image_public_id,
      category: c.category,
      placement: c.placement,
      badge_label: c.badge_label,
      latitude: c.latitude ?? null,
      longitude: c.longitude ?? null,
      radius_km: c.radius_km ?? null,
      priority: c.priority,
      starts_at: c.starts_at.slice(0, 16),
      ends_at: c.ends_at.slice(0, 16),
      is_active: c.is_active
    });
    this.showCampaignForm.set(true);
  }

  cancelCampaignForm(): void {
    this.showCampaignForm.set(false);
    this.editingCampaignId.set(null);
  }

  onBannerFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;

    this.isUploadingImage.set(true);
    this.clearMessages();
    this.adsService.uploadAdImage(file).subscribe({
      next: (res) => {
        this.campaignForm.patchValue({
          image_url: res.image_url,
          image_public_id: res.image_public_id
        });
        this.isUploadingImage.set(false);
        this.successMessage.set('Imagen subida correctamente a Cloudinary.');
      },
      error: (err) => {
        this.isUploadingImage.set(false);
        this.handleError('Error al subir la imagen a Cloudinary.', err);
      }
    });
  }

  saveCampaign(): void {
    if (this.campaignForm.invalid) {
      this.campaignForm.markAllAsTouched();
      return;
    }

    const v = this.campaignForm.getRawValue();
    const startsAt = this.normalizeNaiveDateTime(v.starts_at);
    const endsAt = this.normalizeNaiveDateTime(v.ends_at);

    if (endsAt <= startsAt) {
      this.errorMessage.set('La fecha de fin debe ser posterior a la fecha de inicio.');
      return;
    }

    this.isSaving.set(true);
    this.clearMessages();

    const editId = this.editingCampaignId();
    if (editId) {
      const payload: UpdateAdCampaignRequest = {
        title: v.title,
        description: v.description || null,
        cta_label: v.cta_label || 'Ver más',
        target_url: v.target_url || null,
        image_url: v.image_url,
        image_public_id: v.image_public_id || '',
        category: v.category,
        placement: v.placement,
        badge_label: v.badge_label || 'Patrocinado',
        latitude: v.latitude !== null && v.latitude !== undefined ? Number(v.latitude) : null,
        longitude: v.longitude !== null && v.longitude !== undefined ? Number(v.longitude) : null,
        radius_km: v.radius_km !== null && v.radius_km !== undefined ? Number(v.radius_km) : null,
        priority: Number(v.priority) || 0,
        starts_at: startsAt,
        ends_at: endsAt,
        is_active: v.is_active
      };
      this.adsService.updateCampaign(editId, payload).subscribe({
        next: () => {
          this.isSaving.set(false);
          this.showCampaignForm.set(false);
          this.successMessage.set('Campaña actualizada correctamente.');
          this.loadAll();
        },
        error: (err) => {
          this.isSaving.set(false);
          this.handleError('No se pudo actualizar la campaña.', err);
        }
      });
    } else {
      const payload: CreateAdCampaignRequest = {
        advertiser_id: v.advertiser_id,
        title: v.title,
        description: v.description || null,
        cta_label: v.cta_label || 'Ver más',
        target_url: v.target_url || null,
        image_url: v.image_url,
        image_public_id: v.image_public_id || '',
        category: v.category,
        placement: v.placement,
        badge_label: v.badge_label || (v.category === 'GOVERNMENT' ? 'Campaña Pública' : 'Patrocinado'),
        latitude: v.latitude !== null && v.latitude !== undefined ? Number(v.latitude) : null,
        longitude: v.longitude !== null && v.longitude !== undefined ? Number(v.longitude) : null,
        radius_km: v.radius_km !== null && v.radius_km !== undefined ? Number(v.radius_km) : null,
        priority: Number(v.priority) || 0,
        starts_at: startsAt,
        ends_at: endsAt,
        is_active: v.is_active
      };
      this.adsService.createCampaign(payload).subscribe({
        next: () => {
          this.isSaving.set(false);
          this.showCampaignForm.set(false);
          this.successMessage.set('Campaña creada correctamente.');
          this.loadAll();
        },
        error: (err) => {
          this.isSaving.set(false);
          this.handleError('No se pudo crear la campaña.', err);
        }
      });
    }
  }

  deleteCampaign(c: AdCampaignSummary): void {
    if (!confirm(`¿Eliminar la campaña "${c.title}"?`)) return;
    this.clearMessages();
    this.adsService.deleteCampaign(c.id).subscribe({
      next: () => {
        this.successMessage.set('Campaña eliminada.');
        this.loadAll();
      },
      error: (err) => this.handleError('No se pudo eliminar la campaña.', err)
    });
  }

  // --- Advertiser Form Actions ---

  openCreateAdvertiser(): void {
    this.clearMessages();
    this.editingAdvertiserId.set(null);
    this.advertiserForm.reset({
      name: '',
      category: 'PET_FOOD',
      contact_name: '',
      email: '',
      phone: '',
      whatsapp: '',
      website: '',
      logo_url: '',
      is_government: false,
      official_source_url: '',
      is_active: true
    });
    this.showAdvertiserForm.set(true);
  }

  openEditAdvertiser(a: Advertiser): void {
    this.clearMessages();
    this.editingAdvertiserId.set(a.id);
    this.advertiserForm.reset({
      name: a.name,
      category: a.category,
      contact_name: a.contact_name ?? '',
      email: a.email ?? '',
      phone: a.phone ?? '',
      whatsapp: a.whatsapp ?? '',
      website: a.website ?? '',
      logo_url: a.logo_url ?? '',
      is_government: a.is_government,
      official_source_url: a.official_source_url ?? '',
      is_active: a.is_active
    });
    this.showAdvertiserForm.set(true);
  }

  cancelAdvertiserForm(): void {
    this.showAdvertiserForm.set(false);
    this.editingAdvertiserId.set(null);
  }

  saveAdvertiser(): void {
    if (this.advertiserForm.invalid) {
      this.advertiserForm.markAllAsTouched();
      return;
    }

    const v = this.advertiserForm.getRawValue();
    if (v.is_government && !v.official_source_url.trim()) {
      this.errorMessage.set(
        'Para anunciantes gubernamentales es obligatorio indicar la URL de fuente oficial (política de Google Play).'
      );
      return;
    }

    this.isSaving.set(true);
    this.clearMessages();

    const basePayload: CreateAdvertiserRequest = {
      name: v.name,
      category: v.category,
      contact_name: v.contact_name || null,
      email: v.email || null,
      phone: v.phone || null,
      whatsapp: v.whatsapp || null,
      website: v.website || null,
      logo_url: v.logo_url || null,
      is_government: v.is_government,
      official_source_url: v.official_source_url || null
    };

    const editId = this.editingAdvertiserId();
    if (editId) {
      const updatePayload: UpdateAdvertiserRequest = { ...basePayload, is_active: v.is_active };
      this.adsService.updateAdvertiser(editId, updatePayload).subscribe({
        next: () => {
          this.isSaving.set(false);
          this.showAdvertiserForm.set(false);
          this.successMessage.set('Anunciante actualizado.');
          this.loadAll();
        },
        error: (err) => {
          this.isSaving.set(false);
          this.handleError('No se pudo actualizar el anunciante.', err);
        }
      });
    } else {
      this.adsService.createAdvertiser(basePayload).subscribe({
        next: () => {
          this.isSaving.set(false);
          this.showAdvertiserForm.set(false);
          this.successMessage.set('Anunciante creado.');
          this.loadAll();
        },
        error: (err) => {
          this.isSaving.set(false);
          this.handleError('No se pudo crear el anunciante.', err);
        }
      });
    }
  }

  deleteAdvertiser(a: Advertiser): void {
    if (!confirm(`¿Eliminar el anunciante "${a.name}" y todas sus campañas?`)) return;
    this.clearMessages();
    this.adsService.deleteAdvertiser(a.id).subscribe({
      next: () => {
        this.successMessage.set('Anunciante eliminado.');
        this.loadAll();
      },
      error: (err) => this.handleError('No se pudo eliminar el anunciante.', err)
    });
  }

  categoryName(id: string): string {
    return this.categories().find((c) => c.id === id)?.name ?? id;
  }

  placementName(id: string): string {
    return this.placements().find((p) => p.id === id)?.name ?? id;
  }

  private toDatetimeLocal(date: Date): string {
    const pad = (n: number) => n.toString().padStart(2, '0');
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
  }

  private normalizeNaiveDateTime(value: string): string {
    // Convierte 'YYYY-MM-DDTHH:mm' a 'YYYY-MM-DDTHH:mm:00' esperado por chrono::NaiveDateTime
    return value.length === 16 ? `${value}:00` : value;
  }

  private clearMessages(): void {
    this.errorMessage.set(null);
    this.successMessage.set(null);
  }

  private handleError(fallback: string, err: unknown): void {
    const apiErr = (err as { error?: { error?: string } })?.error?.error;
    this.errorMessage.set(apiErr || fallback);
  }
}
