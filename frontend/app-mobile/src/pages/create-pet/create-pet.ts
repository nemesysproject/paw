import { createPetOffline, MediaFile } from '../../shared/services/pet-facade.service';
import { showToast, setButtonLoading } from '../../shared/ui/ui.utils';
import { getSpecies, getBreeds } from '../../shared/services/catalogs.service';
import { PetGender, PetStatus } from '../../shared/models';
import { open } from '@tauri-apps/plugin-dialog';
import { invoke, convertFileSrc } from '@tauri-apps/api/core';
import { stat } from '@tauri-apps/plugin-fs';

/**
 * Lógica de la página de creación de mascotas.
 */

let selectedMedia: MediaFile[] = [];
let userLocation: { lat: number; lon: number } | null = null;

const MAX_FILES = 10;
const MAX_VIDEO_SIZE = 10 * 1024 * 1024; // 10MB
const SUBMIT_TIMEOUT_MS = 15000; // 15 segundos

document.addEventListener('DOMContentLoaded', async () => {
  const form = document.getElementById('create-pet-form') as HTMLFormElement;
  const btnAddMedia = document.getElementById('btn-add-media');
  const previewGrid = document.getElementById('media-preview-grid');
  const btnSubmit = document.getElementById('btn-submit-pet') as HTMLButtonElement | null;
  const locationStatus = document.getElementById('location-status') as HTMLElement | null;

  const speciesSelect = document.getElementById('species') as HTMLSelectElement | null;
  const breedSelect = document.getElementById('breed') as HTMLSelectElement | null;

  // 0. Cargar Catálogo de Especies
  async function loadInitialCatalogs() {
    if (!speciesSelect) return;
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
  speciesSelect?.addEventListener('change', async () => {
    if (!breedSelect) return;

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

  // 2. Obtener ubicación automáticamente
  if (navigator.geolocation) {
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        userLocation = { lat: pos.coords.latitude, lon: pos.coords.longitude };
        if (locationStatus) {
          locationStatus.innerHTML = `<i class="ph ph-check-circle"></i> Ubicación capturada: ${userLocation.lat.toFixed(4)}`;
        }
        validateForm();
      },
      (err) => {
        console.error('Error GPS:', err);
        if (locationStatus) {
          locationStatus.textContent = 'Error al obtener ubicación. Por favor activa el GPS.';
          locationStatus.style.color = 'var(--error-color)';
        }
      },
      // Añadimos opciones para evitar bloqueos en Android
      { enableHighAccuracy: true, timeout: 5000, maximumAge: 0 }
    );
  }

  // 3. Manejar selección de archivos nativa
  if (btnAddMedia) {
    btnAddMedia.addEventListener('click', async (e) => {
      e.preventDefault();

      // En Android, usamos nuestras rutinas nativas para captura directa
      // Para galería seguimos usando el dialog
      const useCamera = confirm("¿Usar cámara para capturar? (Aceptar: Cámara, Cancelar: Galería)");

      if (useCamera) {
        try {
          const isVideo = confirm("¿Deseas grabar un video? (Aceptar: Video, Cancelar: Foto)");
          const command = isVideo ? 'plugin:media|takeVideo' : 'plugin:media|takePhoto';
          
          const result = await invoke<{path: string, type: string}>(command);
          
          if (selectedMedia.length >= MAX_FILES) {
            showToast(`Máximo ${MAX_FILES} archivos permitidos`, 'error');
            return;
          }

          const fileName = result.path.split(/[/\\]/).pop() || 'captura';
          
          selectedMedia.push({
            path: result.path,
            name: fileName,
            type: result.type as 'image' | 'video'
          });

          renderPreview();
          validateForm();
        } catch (err) {
          console.error('Error en captura nativa:', err);
          if (err !== 'Capture cancelled or failed') {
            showToast('Error al usar la cámara', 'error');
          }
        }
        return;
      }

      try {
        const selected = await open({
          multiple: true,
          directory: false,
          filters: [{
            name: 'Media',
            extensions: ['jpg', 'jpeg', 'png', 'mp4']
          }]
        });

        if (!selected) return;

        const paths = Array.isArray(selected) ? selected : [selected];

        if (selectedMedia.length + paths.length > MAX_FILES) {
          showToast(`Máximo ${MAX_FILES} archivos permitidos`, 'error');
          return;
        }

        for (const filePath of paths) {
          // Obtener nombre del archivo de la ruta
          const fileName = filePath.split(/[/\\]/).pop() || 'archivo';
          const isVideo = fileName.toLowerCase().endsWith('.mp4');

          // En Android, el selector devuelve URIs (content://). 
          // Las funciones de @tauri-apps/plugin-fs solo aceptan rutas absolutas sin protocolo.
          const isUri = filePath.includes('://');

          if (isVideo && !isUri) {
            try {
              const fileStat = await stat(filePath);
              if (fileStat.size > MAX_VIDEO_SIZE) {
                showToast(`El video ${fileName} excede los 10MB`, 'error');
                continue;
              }
            } catch (err) {
              console.warn('No se pudo verificar tamaño del video', err);
            }
          }

          selectedMedia.push({
            path: filePath,
            name: fileName,
            type: isVideo ? 'video' : 'image'
          });
        }

        renderPreview();
        validateForm();
      } catch (err) {
        console.error('Error abriendo selector de archivos:', err);
      }
    });
  }

  // 4. Validar formulario
  function validateForm() {
    if (!btnSubmit) return;
    const hasPhoto = selectedMedia.some(m => m.type === 'image');
    const hasLocation = userLocation !== null;

    btnSubmit.disabled = !hasPhoto || !hasLocation;
  }

  // 5. Renderizar miniaturas
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
        img.src = convertFileSrc(media.path);
        card.appendChild(img);
      } else {
        const video = document.createElement('video');
        video.src = convertFileSrc(media.path);
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

  // 6. Enviar formulario
  if (form) {
    form.addEventListener('submit', async (e) => {
      e.preventDefault();

      if (!userLocation) {
        showToast('Esperando ubicación...', 'info');
        return;
      }

      const formData = new FormData(form);
      const restoreBtn = setButtonLoading(btnSubmit as HTMLButtonElement);

      const timeoutPromise = new Promise((_, reject) =>
        setTimeout(() => reject(new Error('Tiempo de espera agotado')), SUBMIT_TIMEOUT_MS)
      );

      try {



        await Promise.race([
          createPetOffline({
            name: formData.get('name') as string,
            gender: formData.get('gender') as PetGender,
            status: formData.get('status') as PetStatus,
            description: formData.get('description') as string,
            species_id: formData.get('species_id') as string,
            breed_id: formData.get('breed_id') as string || null,
            last_latitude: userLocation.lat,
            last_longitude: userLocation.lon,
            media: selectedMedia
          }),
          timeoutPromise
        ]);

        showToast('¡Mascota registrada localmente! Se sincronizará al tener red.', 'success');

        setTimeout(() => {
          window.location.href = '/index.html';
        }, 1500);

      } catch (error: any) {
        console.error('Error creando mascota offline:', error);
        showToast(error || 'Error al guardar offline', 'error');
        restoreBtn();
      }
    });
  }
});
