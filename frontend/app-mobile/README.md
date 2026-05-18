# PAW Mobile App (Offline-First)

Aplicación móvil desarrollada con Tauri, HTML5 y TypeScript, diseñada para operar en condiciones de conectividad limitada.

## Arquitectura Offline-First

La aplicación utiliza un patrón de **Local-First Proxy** para garantizar que el usuario pueda capturar datos en campo sin interrupciones.

### Componentes de Sincronización

1.  **Connectivity Service**: Monitorea el estado de la red (`online`/`offline`) y dispara procesos de sincronización al recuperar conectividad.
2.  **Sync Engine (SQLite)**: 
    *   **Tabla `sync_queue`**: Gestiona las operaciones pendientes (POST, PUT, DELETE).
    *   **Tabla `catalogs_cache`**: Almacena especies y razas localmente para uso inmediato.
    *   **Tablas `pets_local` y `media_local`**: Persisten los datos y rutas de archivos antes de subir al servidor.
3.  **Multimedia Storage**: Las fotos y videos se guardan en el directorio de datos de la app (`tauri-plugin-fs`) y se eliminan automáticamente tras una sincronización exitosa.

---

## Flujo de Trabajo Técnico

### 1. Autenticación Biométrica Offline
*   Si el usuario ya se ha logueado previamente, puede acceder a la app mediante huella o rostro incluso sin internet.
*   La app valida la biometría contra SQLite y concede acceso al "Dashboard Offline".

### 2. Registro de Mascotas (Offline)
*   **Validación**: Mínimo 1 foto, máximo 10 archivos (Video máx 10MB).
*   **Ubicación**: Capturada automáticamente vía GPS en el momento del registro.
*   **Persistencia**: Los datos se guardan en SQLite y se encolan para sincronización.

### 3. Motor de Sincronización Automática
*   Al detectar red (WiFi/4G), la app recorre la cola de sincronización.
*   Sube multimedia primero, obtiene las URLs remotas y finalmente crea el registro en el API-REST de Rust.

---

## Desarrollo y Build

### Prerrequisitos
*   Rust y Cargo
*   Node.js y npm
*   Tauri CLI

### Ejecución en Desarrollo
```bash
npm run tauri dev
```

### Configuración del API
La URL base del backend se configura en `src/shared/services/api.config.ts`. Por defecto apunta al proxy de Vite `/api/v1`.


### Comandos útiles:

npm run tauri android build --release --target universal
