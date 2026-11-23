import { Component, inject, OnInit, OnDestroy, signal, ElementRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DashboardService, DashboardResponse } from '@core/services/dashboard.service';
import { LucideAngularModule } from 'lucide-angular';
import * as L from 'leaflet';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  template: `
    <div class="dashboard-container">
      <!-- Stats Row -->
      <div class="row mb-4" *ngIf="dashboardData() as data">
        <div class="col-xl-3 col-md-6 mb-4">
          <div class="stat-card lost border-left-danger h-100 py-2 shadow-sm">
            <div class="card-body">
              <div class="row no-gutters align-items-center">
                <div class="col mr-2">
                  <div class="text-xs font-weight-bold text-danger text-uppercase mb-1">Perdidos</div>
                  <div class="h5 mb-0 font-weight-bold text-gray-800">{{ data.stats.total_lost || 0 }}</div>
                </div>
                <div class="col-auto">
                  <lucide-icon name="search" size="32" class="text-gray-300"></lucide-icon>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="col-xl-3 col-md-6 mb-4">
          <div class="stat-card found border-left-success h-100 py-2 shadow-sm">
            <div class="card-body">
              <div class="row no-gutters align-items-center">
                <div class="col mr-2">
                  <div class="text-xs font-weight-bold text-success text-uppercase mb-1">A Salvo</div>
                  <div class="h5 mb-0 font-weight-bold text-gray-800">{{ data.stats.total_safe || 0 }}</div>
                </div>
                <div class="col-auto">
                  <lucide-icon name="check-circle" size="32" class="text-gray-300"></lucide-icon>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="col-xl-3 col-md-6 mb-4">
          <div class="stat-card adopted border-left-info h-100 py-2 shadow-sm">
            <div class="card-body">
              <div class="row no-gutters align-items-center">
                <div class="col mr-2">
                  <div class="text-xs font-weight-bold text-info text-uppercase mb-1">En Adopción</div>
                  <div class="h5 mb-0 font-weight-bold text-gray-800">{{ data.stats.total_adoption || 0 }}</div>
                </div>
                <div class="col-auto">
                  <lucide-icon name="heart" size="32" class="text-gray-300"></lucide-icon>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="col-xl-3 col-md-6 mb-4">
          <div class="stat-card total border-left-warning h-100 py-2 shadow-sm">
            <div class="card-body">
              <div class="row no-gutters align-items-center">
                <div class="col mr-2">
                  <div class="text-xs font-weight-bold text-warning text-uppercase mb-1">Total Sistema</div>
                  <div class="h5 mb-0 font-weight-bold text-gray-800">{{ data.stats.total_pets || 0 }}</div>
                </div>
                <div class="col-auto">
                  <lucide-icon name="dog" size="32" class="text-gray-300"></lucide-icon>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Map Row -->
      <div class="row">
        <div class="col-12">
          <div class="card shadow-sm mb-4">
            <div class="card-header py-3 d-flex flex-row align-items-center justify-content-between bg-white">
              <h6 class="m-0 font-weight-bold text-primary">Mapa de Avistamientos (OpenStreetMap)</h6>
              <div class="small text-muted">Ubicación de mascotas registradas</div>
            </div>
            <div class="card-body p-0">
              <div #mapContainer id="map" style="height: 500px; width: 100%; border-radius: 0 0 8px 8px; z-index: 1;"></div>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .stat-card {
      background: white;
      border-radius: 8px;
      transition: transform 0.2s;
    }
    .stat-card:hover {
      transform: translateY(-5px);
    }
    .border-left-primary { border-left: 0.25rem solid #4e73df !important; }
    .border-left-success { border-left: 0.25rem solid #1cc88a !important; }
    .border-left-info { border-left: 0.25rem solid #36b9cc !important; }
    .border-left-warning { border-left: 0.25rem solid #f6c23e !important; }
    .border-left-danger { border-left: 0.25rem solid #e74a3b !important; }
    .text-xs { font-size: .7rem; }
    .text-gray-300 { color: #dddfeb !important; }
    .text-gray-800 { color: #5a5c69 !important; }
  `]
})
export class DashboardComponent implements OnInit, OnDestroy {
  private dashboardService = inject(DashboardService);
  @ViewChild('mapContainer') mapContainer!: ElementRef<HTMLDivElement>;

  dashboardData = signal<DashboardResponse | null>(null);
  private map: L.Map | null = null;

  ngOnInit(): void {
    this.fetchData();
  }

  ngOnDestroy(): void {
    this.map?.remove();
    this.map = null;
  }

  fetchData(): void {
    this.dashboardService.getDashboardData().subscribe({
      next: (data) => {
        if (!data?.stats) {
          console.error('Respuesta inesperada de /api/v1/dashboard:', data);
          return;
        }
        this.dashboardData.set(data);
        this.initMap(data);
      },
      error: (err) => console.error('Error fetching dashboard data:', err)
    });
  }

  async initMap(data: DashboardResponse): Promise<void> {
    if (!this.mapContainer?.nativeElement) return;

    if (this.map) {
      this.map.remove();
      this.map = null;
    }

    const center = await this.getCurrentLocation();

    this.map = L.map(this.mapContainer.nativeElement, {
      center: [center.lat, center.lng],
      zoom: 13
    });

    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
    }).addTo(this.map);

    const bounds: L.LatLngExpression[] = [];

    (data.locations || []).forEach((loc) => {
      if (loc.last_latitude && loc.last_longitude && this.map) {
        const coords: [number, number] = [loc.last_latitude, loc.last_longitude];
        bounds.push(coords);

        const color = this.markerColor(loc.status);
        const marker = L.circleMarker(coords, {
          radius: 9,
          fillColor: color,
          color: '#ffffff',
          weight: 2,
          opacity: 1,
          fillOpacity: 0.9
        }).addTo(this.map);

        const safeName = this.escapeHtml(loc.name || 'Sin nombre');
        const safeStatus = this.escapeHtml(loc.status || 'UNKNOWN');
        const imgTag =
          loc.image_url && /^https?:\/\//i.test(loc.image_url)
            ? `<img src="${this.escapeHtml(loc.image_url)}" alt="${safeName}" style="width:100%;max-height:120px;object-fit:cover;border-radius:6px;margin-top:6px;">`
            : '';

        marker.bindPopup(`
          <div style="min-width:160px;max-width:210px;">
            <strong style="display:block;margin-bottom:4px;">${safeName}</strong>
            <span style="font-size:12px;">Estado: <b>${safeStatus}</b></span>
            ${imgTag}
          </div>
        `);
      }
    });

    if (bounds.length > 0) {
      this.map.fitBounds(L.latLngBounds(bounds), { padding: [40, 40], maxZoom: 15 });
    }
  }

  private markerColor(status: string): string {
    switch (status?.toUpperCase()) {
      case 'LOST':
      case 'AT_RISK':
        return '#e74a3b';
      case 'SAFE':
      case 'FOUND':
        return '#1cc88a';
      case 'ADOPTION':
        return '#36b9cc';
      default:
        return '#4e73df';
    }
  }

  private escapeHtml(text: string): string {
    return text
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  }

  private getCurrentLocation(): Promise<{ lat: number; lng: number }> {
    return new Promise((resolve) => {
      if (navigator.geolocation) {
        navigator.geolocation.getCurrentPosition(
          (position) => {
            resolve({
              lat: position.coords.latitude,
              lng: position.coords.longitude
            });
          },
          () => {
            resolve({ lat: 19.4326, lng: -99.1332 }); // CDMX fallback
          }
        );
      } else {
        resolve({ lat: 19.4326, lng: -99.1332 }); // CDMX fallback
      }
    });
  }
}
