import { requireAuth, logout } from '../../shared/services/api.service';
import { listPets } from '../../shared/services/pets.service';
import { ROUTES } from '../../shared/services/api.config';
import { syncAllCatalogs } from '../../shared/services/catalogs.service';
import type { PetDetailResponse } from '../../shared/models';

// Placeholder image para mascotas sin media
const PLACEHOLDER_IMG = 'https://images.unsplash.com/photo-1548199973-03cce0bbc87b?auto=format&fit=crop&q=80&w=800';

function createPetCard(pet: PetDetailResponse): string {
  const statusClass = pet.status.toLowerCase();
  const petName = pet.name || 'Sin nombre';
  const imageUrl = pet.media && pet.media.length > 0 ? pet.media[0].media_url : PLACEHOLDER_IMG;
  const imageCount = pet.media.length || 1;
  const description = pet.description || 'Sin descripción disponible.';

  // Calcular tiempo relativo desde created_at
  const createdDate = new Date(pet.created_at);
  const now = new Date();
  const diffMs = now.getTime() - createdDate.getTime();
  const diffHours = Math.floor(diffMs / (1000 * 60 * 60));
  const diffDays = Math.floor(diffHours / 24);
  let timeAgo: string;
  if (diffDays > 0) {
    timeAgo = `Hace ${diffDays} día${diffDays > 1 ? 's' : ''}`;
  } else if (diffHours > 0) {
    timeAgo = `Hace ${diffHours} hora${diffHours > 1 ? 's' : ''}`;
  } else {
    timeAgo = 'Hace unos minutos';
  }

  // Ubicación
  const hasLocation = pet.last_latitude !== null && pet.last_longitude !== null;
  const locationText = hasLocation
    ? `${pet.last_latitude!.toFixed(4)}, ${pet.last_longitude!.toFixed(4)}`
    : 'Sin ubicación';

  // Pagination dots
  const dots = Array.from({ length: Math.min(imageCount, 5) }, (_, i) =>
    `<span class="dot ${i === 0 ? 'active' : ''}"></span>`
  ).join('');

  return `
    <article class="pet-card" data-pet-id="${pet.id}">
      <div class="pet-image-container">
        <img src="${imageUrl}" alt="${petName}" loading="lazy">
        <span class="image-counter">1/${imageCount}</span>
        <div class="pagination-dots">
          ${dots}
        </div>
      </div>
      <div class="pet-info">
        <div class="pet-header">
          <h2 class="pet-name">${petName}</h2>
          <span class="badge ${statusClass}">${pet.status}</span>
        </div>
        <div class="pet-breed">${pet.gender}</div>
        <p class="pet-description">${description}</p>
        <div class="pet-meta">
          <div class="meta-item">
            <i class="ph ph-map-pin"></i>
            <span>${locationText}</span>
          </div>
          <div class="meta-item">
            <i class="ph ph-clock"></i>
            <span>${timeAgo}</span>
          </div>
        </div>
      </div>
    </article>
  `;
}

function createSkeletonCard(): string {
  return `
    <article class="pet-card skeleton-card">
      <div class="pet-image-container skeleton-shimmer" style="height:220px"></div>
      <div class="pet-info">
        <div class="skeleton-line skeleton-shimmer" style="width:60%;height:1.25rem;margin-bottom:0.5rem"></div>
        <div class="skeleton-line skeleton-shimmer" style="width:40%;height:0.85rem;margin-bottom:0.8rem"></div>
        <div class="skeleton-line skeleton-shimmer" style="width:100%;height:0.9rem;margin-bottom:0.4rem"></div>
        <div class="skeleton-line skeleton-shimmer" style="width:80%;height:0.9rem"></div>
      </div>
    </article>
  `;
}

function renderEmpty(): string {
  return `
    <div class="empty-state">
      <i class="ph ph-paw-print empty-state__icon"></i>
      <h3 class="empty-state__title">No se encontraron mascotas</h3>
      <p class="empty-state__text">Intenta ajustar los filtros de búsqueda o vuelve a intentarlo más tarde.</p>
    </div>
  `;
}

async function renderPets() {
  const petListEl = document.getElementById('pet-list');
  const resultsCountEl = document.getElementById('results-count');

  if (!petListEl || !resultsCountEl) return;

  // Mostrar skeletons mientras carga
  petListEl.innerHTML = Array.from({ length: 3 }, () => createSkeletonCard()).join('');
  resultsCountEl.textContent = 'Cargando mascotas...';

  try {
    const pets = await listPets();

    if (pets.length === 0) {
      petListEl.innerHTML = renderEmpty();
      resultsCountEl.textContent = '0 mascotas encontradas';
    } else {
      petListEl.innerHTML = pets.map(createPetCard).join('');
      resultsCountEl.textContent = `${pets.length} mascota${pets.length !== 1 ? 's' : ''} encontrada${pets.length !== 1 ? 's' : ''}`;
    }
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : 'Error al cargar mascotas';
    petListEl.innerHTML = `
      <div class="empty-state empty-state--error">
        <i class="ph ph-warning-circle empty-state__icon"></i>
        <h3 class="empty-state__title">Error de conexión</h3>
        <p class="empty-state__text">${message}</p>
        <button class="btn-primary" id="btn-retry" style="max-width:200px;margin:1rem auto 0">Reintentar</button>
      </div>
    `;
    resultsCountEl.textContent = 'Error al cargar';

    document.getElementById('btn-retry')?.addEventListener('click', () => renderPets());
  }
}

function setupFiltersToggle() {
  const btnToggle = document.getElementById('btn-toggle-filters');
  const filtersPanel = document.getElementById('filters-panel');

  if (btnToggle && filtersPanel) {
    btnToggle.addEventListener('click', () => {
      filtersPanel.classList.toggle('hidden');
    });
  }
}

function setupSlider() {
  const slider = document.getElementById('distance-slider') as HTMLInputElement;
  const distanceValue = document.getElementById('distance-value');

  if (slider && distanceValue) {
    slider.addEventListener('input', (e) => {
      const target = e.target as HTMLInputElement;
      distanceValue.textContent = target.value;
    });
  }
}

function setupLogoutButton() {
  const logoutBtn = document.getElementById('btn-logout');
  if (logoutBtn) {
    logoutBtn.addEventListener('click', (e) => {
      e.preventDefault();
      logout();
    });
  }
}

// Exportar ROUTES para uso en el HTML
export { ROUTES };

window.addEventListener('DOMContentLoaded', () => {
  // Guard de autenticación
  requireAuth();

  // Sincronizar catálogos en segundo plano
  syncAllCatalogs();

  renderPets();
  setupFiltersToggle();
  setupSlider();
  setupLogoutButton();
});
