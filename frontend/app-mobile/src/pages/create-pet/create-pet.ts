import { createPetOffline, MediaFile } from '../../shared/services/pet-facade.service';
import { showToast, setButtonLoading } from '../../shared/ui/ui.utils';
import { getSpecies, getBreeds } from '../../shared/services/catalogs.service';
import { PetGender, PetStatus } from '../../shared/models';
import { open } from '@tauri-apps/plugin-dialog';
import { convertFileSrc } from '@tauri-apps/api/core';
import { stat, readFile } from '@tauri-apps/plugin-fs';
import { copyMediaLocally } from '../../shared/services/media-storage.service';

/**
 * Lógica de la página de creación de mascotas.
 */

let selectedMedia: MediaFile[] = [];
let userLocation: { lat: number; lon: number } | null = null;

const MAX_FILES = 10;
const MAX_VIDEO_SIZE = 10 * 1024 * 1024; // 10MB

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

  // 3. Manejar selección de archivos (galería)
  if (btnAddMedia) {
    btnAddMedia.addEventListener('click', async (e) => {
      e.preventDefault();

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

        // Mostrar el banner de carga de inmediato
        const loadingOverlay = document.getElementById('loading-overlay');
        loadingOverlay?.classList.remove('hidden');

        // Retardar el procesamiento para permitir que la interfaz dibuje el overlay
        setTimeout(async () => {
          try {
            for (const filePath of paths) {
              const fileName = filePath.split(/[/\\]/).pop() || 'archivo';
              const isVideo = fileName.toLowerCase().endsWith('.mp4');

              // Validar tamaño de video si es una ruta local (no content://)
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

              try {
                // Copiar el archivo localmente de inmediato para asegurar persistencia y evitar expiración de URIs
                const localPath = await copyMediaLocally(filePath, fileName);
                // console.log(`📸 Archivo copiado para vista previa a: ${localPath}`);

                // Generar vista previa Base64 para imágenes de forma instantánea y robusta
                let base64Preview = '';
                if (!isVideo) {
                  try {
                    const content = await readFile(localPath);
                    let binary = '';
                    const bytes = new Uint8Array(content);
                    const len = bytes.byteLength;
                    for (let i = 0; i < len; i++) {
                      binary += String.fromCharCode(bytes[i]);
                    }
                    const base64 = window.btoa(binary);
                    const ext = fileName.split('.').pop()?.toLowerCase();
                    const mimeType = ext === 'png' ? 'image/png' : 'image/jpeg';
                    base64Preview = `data:${mimeType};base64,${base64}`;
                  } catch (readErr) {
                    console.warn('No se pudo generar vista previa Base64:', readErr);
                  }
                }

                selectedMedia.push({
                  path: localPath,
                  name: fileName,
                  type: isVideo ? 'video' : 'image',
                  localPath: base64Preview || undefined // Almacena Base64 Data URL
                });
              } catch (copyErr) {
                console.error('Error al copiar el archivo para vista previa:', copyErr);
                showToast(`Error al procesar el archivo ${fileName}`, 'error');
              }
            }

            renderPreview();
            validateForm();
          } catch (err) {
            console.error('Error procesando archivos seleccionados:', err);
          } finally {
            // Ocultar el overlay de carga al finalizar
            loadingOverlay?.classList.add('hidden');
          }
        }, 50);
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
        img.src = media.localPath || convertFileSrc(media.path);
        card.appendChild(img);
      } else {
        const video = document.createElement('video');
        video.src = media.localPath || convertFileSrc(media.path);
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

      try {
        console.log('📝 Iniciando registro de mascota...');

        const localId = await createPetOffline({
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

        console.log('✅ Mascota registrada con ID local:', localId);
        showToast('¡Mascota registrada! Se sincronizará al tener red.', 'success');

        setTimeout(() => {
          window.location.href = '/index.html';
        }, 1500);

      } catch (error: any) {
        console.error('Error creando mascota offline:', error);
        showToast(error?.message || error || 'Error al guardar', 'error');
        restoreBtn();
      }
    });
  }

  // Ejecutar validación inicial al cargar la página
  validateForm();
});
