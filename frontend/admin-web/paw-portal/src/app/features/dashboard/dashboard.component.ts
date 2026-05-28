import { Component, inject, OnInit, signal, ElementRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DashboardService, DashboardResponse } from '@core/services/dashboard.service';
import { setOptions, importLibrary } from '@googlemaps/js-api-loader';
import { environment } from '@env/environment';
import { LucideAngularModule } from 'lucide-angular';

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
                  <div class="h5 mb-0 font-weight-bold text-gray-800">{{ data.stats?.total_lost || 0 }}</div>
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
                  <div class="text-xs font-weight-bold text-success text-uppercase mb-1">Encontrados</div>
                  <div class="h5 mb-0 font-weight-bold text-gray-800">{{ data.stats?.total_found || 0 }}</div>
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
                  <div class="text-xs font-weight-bold text-info text-uppercase mb-1">Adoptados</div>
                  <div class="h5 mb-0 font-weight-bold text-gray-800">{{ data.stats?.total_adopted || 0 }}</div>
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
                  <div class="h5 mb-0 font-weight-bold text-gray-800">{{ data.stats?.total_pets || 0 }}</div>
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
              <h6 class="m-0 font-weight-bold text-primary">Mapa de Avistamientos</h6>
              <div class="small text-muted">Ubicación de mascotas registradas</div>
            </div>
            <div class="card-body p-0">
              <div #mapContainer id="map" style="height: 500px; width: 100%; border-radius: 0 0 8px 8px;"></div>
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
export class DashboardComponent implements OnInit {
  private dashboardService = inject(DashboardService);
  @ViewChild('mapContainer') mapContainer!: ElementRef;

  dashboardData = signal<DashboardResponse | null>(null);
  map!: google.maps.Map;

  ngOnInit() {
    this.fetchData();
  }

  fetchData() {
    this.dashboardService.getDashboardData().subscribe({
      next: (data) => {
        this.dashboardData.set(data);
        this.initMap(data);
      },
      error: (err) => console.error('Error fetching dashboard data:', err)
    });
  }

  async initMap(data: DashboardResponse) {
    // setOptions({
    //   key: environment.googleMapKey,
    //   v: 'weekly',
    // });

    const { Map, InfoWindow } = await importLibrary('maps');
    const { Marker } = await importLibrary('marker');

    const center = await this.getCurrentLocation();

    this.map = new Map(this.mapContainer.nativeElement, {
      center: center,
      zoom: 13,
      mapId: 'PAW_DASHBOARD_MAP',
    });

    (data.locations || []).forEach(loc => {
      if (loc.last_latitude && loc.last_longitude) {
        const marker = new Marker({
          position: { lat: loc.last_latitude, lng: loc.last_longitude },
          map: this.map,
          title: loc.name || 'Mascota sin nombre',
          icon: {
            url: loc.status === 'LOST' ? 'http://maps.google.com/mapfiles/ms/icons/red-dot.png' :
              loc.status === 'FOUND' ? 'http://maps.google.com/mapfiles/ms/icons/green-dot.png' :
                'http://maps.google.com/mapfiles/ms/icons/blue-dot.png'
          }
        });

        const infoWindow = new InfoWindow({
          content: `
            <div style="padding: 10px; max-width: 200px;">
              <h6 style="margin-bottom: 5px;">${loc.name || 'Sin nombre'}</h6>
              <p style="font-size: 12px; margin-bottom: 5px;">Estado: <b>${loc.status}</b></p>
              ${loc.image_url ? `<img src="${loc.image_url}" style="width: 100%; border-radius: 4px;">` : ''}
            </div>
          `
        });

        marker.addListener('click', () => {
          infoWindow.open(this.map, marker);
        });
      }
    });
  }

  private getCurrentLocation(): Promise<google.maps.LatLngLiteral> {
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
