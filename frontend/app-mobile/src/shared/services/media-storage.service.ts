import { BaseDirectory, writeFile, readFile, remove, exists, mkdir } from '@tauri-apps/plugin-fs';
import { appDataDir } from '@tauri-apps/api/path';
import { isTauri } from './platform.service';

/**
 * Servicio para gestionar archivos multimedia (fotos/videos) localmente.
 * Almacena archivos en el directorio de datos de la aplicación.
 * 
 * Soporta content:// URIs de Android usando readFile de Tauri plugin-fs
 * que maneja ContentResolver internamente.
 */

const MEDIA_DIR = 'media_cache';

export async function initMediaDir() {
  if (!isTauri()) return;
  const dirExists = await exists(MEDIA_DIR, { baseDir: BaseDirectory.AppData });
  if (!dirExists) {
    await mkdir(MEDIA_DIR, { baseDir: BaseDirectory.AppData, recursive: true });
  }
}

/**
 * Copia un archivo desde cualquier ruta (incluyendo content:// URIs) 
 * a la carpeta local de la aplicación.
 * 
 * Usa readFile/writeFile de Tauri plugin-fs que soporta content:// en Android.
 */
export async function copyMediaLocally(sourcePath: string, fileName: string): Promise<string> {
  if (!isTauri()) return '';
  
  // If the file is already inside our local app cache, bypass copying
  if (sourcePath.includes(MEDIA_DIR) && !sourcePath.startsWith('content://')) {
    console.log(`ℹ️ El archivo ya es local en la caché: ${sourcePath}`);
    return sourcePath;
  }

  await initMediaDir();
  
  const destName = `${Date.now()}_${fileName}`;
  const relativePath = `${MEDIA_DIR}/${destName}`;
  
  console.log(`📂 Copiando archivo: ${fileName}...`);
  
  // readFile de Tauri plugin-fs soporta content:// URIs en Android
  const content = await readFile(sourcePath);
  await writeFile(relativePath, content, { baseDir: BaseDirectory.AppData });
  
  const appDir = await appDataDir();
  const separator = (appDir.endsWith('/') || appDir.endsWith('\\')) ? '' : '/';
  const fullPath = `${appDir}${separator}${relativePath}`;
  
  console.log(`✅ Archivo copiado: ${fullPath}`);
  return fullPath;
}

/** Elimina un archivo local */
export async function deleteLocalMedia(filePath: string): Promise<void> {
  if (!isTauri()) return;
  
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
