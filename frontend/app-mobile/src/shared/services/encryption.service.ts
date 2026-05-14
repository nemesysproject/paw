/**
 * Servicio de cifrado.
 * Utiliza localStorage para proteger la llave maestra y Web Crypto API para cifrar datos.
 */

const MASTER_KEY_ID = 'master_encryption_key';

/**
 * Genera o recupera una Master Key desde localStorage.
 */
async function getOrGenerateMasterKey(): Promise<Uint8Array> {
  const storedKey = window.localStorage.getItem(MASTER_KEY_ID);
  
  if (storedKey) {
    const binaryString = atob(storedKey);
    const keyBytes = new Uint8Array(binaryString.length);
    for (let i = 0; i < binaryString.length; i++) {
      keyBytes[i] = binaryString.charCodeAt(i);
    }
    return keyBytes;
  } else {
    // Generar nueva llave aleatoria de 32 bytes (256 bits)
    const newKey = window.crypto.getRandomValues(new Uint8Array(32));
    const base64Key = btoa(String.fromCharCode(...Array.from(newKey)));
    window.localStorage.setItem(MASTER_KEY_ID, base64Key);
    return newKey;
  }
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
