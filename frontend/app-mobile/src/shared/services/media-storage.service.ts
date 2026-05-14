import { BaseDirectory, writeFile, remove, exists, mkdir, copyFile } from '@tauri-apps/plugin-fs';
import { appDataDir } from '@tauri-apps/api/path';
import { invoke } from '@tauri-apps/api/core';
import { isTauri } from './platform.service';

/**
 * Servicio para gestionar archivos multimedia (fotos/videos) localmente.
 * Almacena archivos en el directorio de datos de la aplicación.
 */

const MEDIA_DIR = 'media_cache';

export async function initMediaDir() {
  if (!isTauri()) return;
  const dirExists = await exists(MEDIA_DIR, { baseDir: BaseDirectory.AppData });
  if (!dirExists) {
    await mkdir(MEDIA_DIR, { baseDir: BaseDirectory.AppData, recursive: true });
  }
}

/** Guarda un archivo binario localmente y retorna la ruta */
export async function saveMediaLocally(fileName: string, data: Uint8Array): Promise<string> {
  if (!isTauri()) return '';
  await initMediaDir();
  
  const path = `${MEDIA_DIR}/${Date.now()}_${fileName}`;
  await writeFile(path, data, { baseDir: BaseDirectory.AppData });
  
  const fullPath = `${await appDataDir()}/${path}`;
  return fullPath;
}

/** Copia un archivo desde una ruta absoluta a la carpeta local de la aplicación */
export async function copyMediaLocally(sourcePath: string, fileName: string): Promise<string> {
  if (!isTauri()) return '';
  await initMediaDir();
  
  const path = `${MEDIA_DIR}/${Date.now()}_${fileName}`;
  const appDir = await appDataDir();
  const fullPath = `${appDir}/${path}`;
  
  if (sourcePath.startsWith('content://')) {
    // En Android, usamos un comando de Rust personalizado para leer URIs de contenido
    await invoke('copy_android_content_to_local', { sourceUri: sourcePath, destPath: fullPath });
  } else {
    // En Desktop o rutas normales
    await copyFile(sourcePath, fullPath);
  }
  
  return fullPath;
}

/** Elimina un archivo local */
export async function deleteLocalMedia(filePath: string): Promise<void> {
  if (!isTauri()) return;
  
  // Extraer la ruta relativa al AppData si es necesario
  const appData = await appDataDir();
  const relativePath = filePath.replace(appData, '').replace(/^\/+/, '').replace(/^\\+/, '');
  
  try {
    if (await exists(relativePath, { baseDir: BaseDirectory.AppData })) {
      await remove(relativePath, { baseDir: BaseDirectory.AppData });
      console.log(`🗑️ Archivo eliminado: ${relativePath}`);
    }
  } catch (error) {
    console.error('Error eliminando archivo local:', error);
  }
}
