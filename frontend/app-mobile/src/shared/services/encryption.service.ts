import { Stronghold, Client } from '@tauri-apps/plugin-stronghold';
import { appDataDir } from '@tauri-apps/api/path';
import { isTauri } from './platform.service';

/**
 * Servicio de cifrado.
 * Utiliza Stronghold para proteger la llave maestra y Web Crypto API para cifrar datos.
 */

const VAULT_PATH = 'client_vault.hold';
const MASTER_KEY_ID = 'master_encryption_key';

let stronghold: Stronghold | null = null;
let client: Client | null = null;

async function initStronghold() {
  if (!isTauri()) return { stronghold: null, client: null };
  if (!stronghold) {
    const path = `${await appDataDir()}/${VAULT_PATH}`;
    stronghold = await Stronghold.load(path, 'password_de_la_boveda_temporal'); // En producción esto debería ser más dinámico
    client = await stronghold.loadClient('main_client');
  }
  return { stronghold, client };
}

/**
 * Genera o recupera una Master Key desde Stronghold.
 */
async function getOrGenerateMasterKey(): Promise<Uint8Array> {
  const { client } = await initStronghold();
  if (!client) return new Uint8Array(32); // Fallback for non-tauri
  
  const store = client.getStore();
  
  let key = await store.get(MASTER_KEY_ID);
  
  if (!key) {
    // Generar nueva llave aleatoria de 32 bytes (256 bits)
    const newKey = window.crypto.getRandomValues(new Uint8Array(32));
    await store.insert(MASTER_KEY_ID, Array.from(newKey));
    key = newKey;
  }
  
  return key;
}

/**
 * Cifra un texto usando AES-GCM.
 */
export async function encryptData(text: string): Promise<string> {
  const masterKeyBytes = await getOrGenerateMasterKey();
  const iv = window.crypto.getRandomValues(new Uint8Array(12));
  const encodedText = new TextEncoder().encode(text);
  
  const cryptoKey = await window.crypto.subtle.importKey(
    'raw',
    masterKeyBytes,
    { name: 'AES-GCM' },
    false,
    ['encrypt']
  );
  
  const encryptedContent = await window.crypto.subtle.encrypt(
    { name: 'AES-GCM', iv },
    cryptoKey,
    encodedText
  );
  
  // Combinar IV + Contenido cifrado y convertir a Base64
  const combined = new Uint8Array(iv.length + encryptedContent.byteLength);
  combined.set(iv);
  combined.set(new Uint8Array(encryptedContent), iv.length);
  
  return btoa(String.fromCharCode(...Array.from(combined)));
}

/**
 * Descifra un texto usando AES-GCM.
 */
export async function decryptData(encryptedBase64: string): Promise<string> {
  const masterKeyBytes = await getOrGenerateMasterKey();
  const binaryString = atob(encryptedBase64);
  const combined = new Uint8Array(binaryString.length);
  for (let i = 0; i < binaryString.length; i++) {
    combined[i] = binaryString.charCodeAt(i);
  }
  
  const iv = combined.slice(0, 12);
  const data = combined.slice(12);
  
  const cryptoKey = await window.crypto.subtle.importKey(
    'raw',
    masterKeyBytes,
    { name: 'AES-GCM' },
    false,
    ['decrypt']
  );
  
  const decryptedContent = await window.crypto.subtle.decrypt(
    { name: 'AES-GCM', iv },
    cryptoKey,
    data
  );
  
  return new TextDecoder().decode(decryptedContent);
}
