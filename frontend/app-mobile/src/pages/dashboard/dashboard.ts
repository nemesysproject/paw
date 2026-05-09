import { requireAuth } from '../../shared/services/api.service';

/**
 * Dashboard - Mapa de mascotas.
 * Inicializa el mapa Leaflet y carga mascotas desde la API.
 */

declare const L: any;

window.addEventListener('DOMContentLoaded', () => {
  requireAuth();

  // Inicializar mapa Leaflet centrado en Lima, Perú
  const map = L.map('map').setView([-12.046374, -77.042793], 13);

  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '&copy; OpenStreetMap contributors',
    maxZoom: 19,
  }).addTo(map);
});
