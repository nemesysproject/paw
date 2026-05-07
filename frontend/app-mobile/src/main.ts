interface Pet {
  id: number;
  name: string;
  status: 'Perdido' | 'Rescatado';
  type: string;
  breed: string;
  description: string;
  distance: string;
  time: string;
  imageUrl: string;
  imageCount: number;
}

const mockPets: Pet[] = [
  {
    id: 1,
    name: 'Max',
    status: 'Perdido',
    type: 'Perro',
    breed: 'Labrador',
    description: 'Perro labrador dorado muy amigable. Se perdió cerca del parque. Responde a su nombre y tiene una mancha blanca en el pecho.',
    distance: '0m',
    time: 'Hace 2 días',
    imageUrl: 'https://images.unsplash.com/photo-1552053831-71594a27632d?auto=format&fit=crop&q=80&w=800',
    imageCount: 3
  },
  {
    id: 2,
    name: 'Sin nombre',
    status: 'Rescatado',
    type: 'Gato',
    breed: 'Mestizo',
    description: 'Gato atigrado encontrado vagando cerca de la avenida principal. Muy asustadizo pero manso.',
    distance: '2.5km',
    time: 'Hace 5 horas',
    imageUrl: 'https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?auto=format&fit=crop&q=80&w=800',
    imageCount: 2
  }
];

function createPetCard(pet: Pet): string {
  const statusClass = pet.status.toLowerCase();
  
  return `
    <article class="pet-card">
      <div class="pet-image-container">
        <img src="${pet.imageUrl}" alt="${pet.name}">
        <span class="image-counter">1/${pet.imageCount}</span>
        <div class="pagination-dots">
          <span class="dot active"></span>
          ${pet.imageCount > 1 ? '<span class="dot"></span>' : ''}
          ${pet.imageCount > 2 ? '<span class="dot"></span>' : ''}
        </div>
      </div>
      <div class="pet-info">
        <div class="pet-header">
          <h2 class="pet-name">${pet.name}</h2>
          <span class="badge ${statusClass}">${pet.status}</span>
        </div>
        <div class="pet-breed">${pet.type} • ${pet.breed}</div>
        <p class="pet-description">${pet.description}</p>
        <div class="pet-meta">
          <div class="meta-item">
            <i class="ph ph-map-pin"></i>
            <span>${pet.distance}</span>
          </div>
          <div class="meta-item">
            <i class="ph ph-clock"></i>
            <span>${pet.time}</span>
          </div>
        </div>
      </div>
    </article>
  `;
}

function renderPets() {
  const petListEl = document.getElementById('pet-list');
  const resultsCountEl = document.getElementById('results-count');
  
  if (petListEl && resultsCountEl) {
    petListEl.innerHTML = mockPets.map(createPetCard).join('');
    resultsCountEl.textContent = `${mockPets.length} mascotas encontradas`;
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

window.addEventListener('DOMContentLoaded', () => {
  renderPets();
  setupFiltersToggle();
  setupSlider();
  
  // Ocultar filtros por defecto al cargar si se desea, 
  // aunque el diseño los muestra. Para coincidir con el pantallazo, lo dejamos visible.
});

