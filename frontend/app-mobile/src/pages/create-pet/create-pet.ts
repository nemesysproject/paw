import { createPetOffline, MediaFile } from '../../shared/services/pet-facade.service';
import { showToast, setButtonLoading } from '../../shared/ui/ui.utils';
import { getSpecies, getBreeds } from '../../shared/services/catalogs.service';
import { PetGender, PetStatus } from '../../shared/models';

/**
 * Lógica de la página de creación de mascotas.
 */

let selectedMedia: MediaFile[] = [];
let userLocation: { lat: number; lon: number } | null = null;

const MAX_FILES = 10;
const MAX_VIDEO_SIZE = 10 * 1024 * 1024; // 10MB

document.addEventListener('DOMContentLoaded', async () => {
  const form = document.getElementById('create-pet-form') as HTMLFormElement;
  const mediaInput = document.getElementById('media-input') as HTMLInputElement;
  const previewGrid = document.getElementById('media-preview-grid');
  const btnSubmit = document.getElementById('btn-submit-pet') as HTMLButtonElement;
  const locationStatus = document.getElementById('location-status');
  
  const speciesSelect = document.getElementById('species') as HTMLSelectElement;
  const breedSelect = document.getElementById('breed') as HTMLSelectElement;

  // 0. Cargar Catálogo de Especies
  async function loadInitialCatalogs() {
    try {
      const species = await getSpecies();
      speciesSelect.innerHTML = '<option value="">Selecciona...</option>' + 
        species.map(s => `<option value="${s.id}">${s.name}</option>`).join('');
    } catch (err) {
      console.error('Error cargando especies:', err);
      showToast('Error al cargar catálogo', 'error');
    }
  }

  // 1. Manejar cambio de especie para cargar razas
  speciesSelect.addEventListener('change', async () => {
    const speciesId = speciesSelect.value;
    breedSelect.innerHTML = '<option value="">Cargando...</option>';
    breedSelect.disabled = true;

    if (speciesId) {
      try {
        const breeds = await getBreeds(speciesId);
        breedSelect.innerHTML = '<option value="">Selecciona...</option>' + 
          breeds.map(b => `<option value="${b.id}">${b.name}</option>`).join('');
        breedSelect.disabled = false;
      } catch (err) {
        console.error('Error cargando razas:', err);
        breedSelect.innerHTML = '<option value="">No disponible</option>';
      }
    } else {
      breedSelect.innerHTML = '<option value="">Selecciona especie primero</option>';
    }
  });

  loadInitialCatalogs();

  // 1. Obtener ubicación automáticamente
  if (navigator.geolocation) {
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        userLocation = { lat: pos.coords.latitude, lon: pos.coords.longitude };
        if (locationStatus) {
          locationStatus.textContent = `Ubicación capturada: ${userLocation.lat.toFixed(4)}, ${userLocation.lon.toFixed(4)}`;
        }
        validateForm();
      },
      (err) => {
        console.error('Error obteniendo ubicación:', err);
        if (locationStatus) locationStatus.textContent = 'Error al obtener ubicación. Por favor activa el GPS.';
      }
    );
  }

  // 2. Manejar selección de archivos
  if (mediaInput) {
    mediaInput.addEventListener('change', (e: any) => {
      const files = Array.from(e.target.files as FileList);
      
      if (selectedMedia.length + files.length > MAX_FILES) {
        showToast(`Máximo ${MAX_FILES} archivos permitidos`, 'error');
        return;
      }

      files.forEach(file => {
        const isVideo = file.type.startsWith('video/');
        
        if (isVideo && file.size > MAX_VIDEO_SIZE) {
          showToast(`El video ${file.name} excede los 10MB`, 'error');
          return;
        }

        selectedMedia.push({
          file,
          type: isVideo ? 'video' : 'image'
        });
      });

      renderPreview();
      validateForm();
      mediaInput.value = ''; // Reset input
    });
  }

  // 3. Validar formulario
  function validateForm() {
    const hasPhoto = selectedMedia.some(m => m.type === 'image');
    const hasLocation = userLocation !== null;
    
    // El botón se habilita solo si hay al menos 1 foto y ubicación
    btnSubmit.disabled = !hasPhoto || !hasLocation;
  }

  // 4. Renderizar miniaturas
  function renderPreview() {
    if (!previewGrid) return;
    
    // Limpiar excepto el botón de añadir
    const addCard = previewGrid.querySelector('.media-add-card');
    previewGrid.innerHTML = '';
    
    selectedMedia.forEach((media, index) => {
      const card = document.createElement('div');
      card.className = 'media-card';
      
      if (media.type === 'image') {
        const img = document.createElement('img');
        img.src = URL.createObjectURL(media.file);
        card.appendChild(img);
      } else {
        const video = document.createElement('video');
        video.src = URL.createObjectURL(media.file);
        card.appendChild(video);
      }

      const removeBtn = document.createElement('button');
      removeBtn.className = 'btn-remove-media';
      removeBtn.innerHTML = '<i class="ph ph-x"></i>';
      removeBtn.onclick = (e) => {
        e.preventDefault();
        selectedMedia.splice(index, 1);
        renderPreview();
        validateForm();
      };
      
      card.appendChild(removeBtn);
      previewGrid.appendChild(card);
    });

    if (addCard) previewGrid.appendChild(addCard);
  }

  // 5. Enviar formulario
  if (form) {
    form.addEventListener('submit', async (e) => {
      e.preventDefault();
      
      if (!userLocation) {
        showToast('Esperando ubicación...', 'info');
        return;
      }

      const formData = new FormData(form);
      const restoreBtn = setButtonLoading(btnSubmit);

      try {
        await createPetOffline({
          name: formData.get('name') as string,
          gender: formData.get('gender') as PetGender,
          status: formData.get('status') as PetStatus,
          description: formData.get('description') as string,
          species_id: formData.get('species_id') as string,
          breed_id: formData.get('breed_id') as string || null,
          last_latitude: userLocation.lat,
          last_longitude: userLocation.lon,
          media: selectedMedia
        });

        showToast('¡Mascota registrada localmente! Se sincronizará al tener red.', 'success');
        
        setTimeout(() => {
          window.location.href = '/index.html';
        }, 1500);

      } catch (error: any) {
        showToast(error.message || 'Error al guardar', 'error');
        restoreBtn();
      }
    });
  }
});
