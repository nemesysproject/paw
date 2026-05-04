use reqwest::{multipart, Client};
use serde::Deserialize;
use sha1::{Digest, Sha1};
use std::env;
use std::time::{SystemTime, UNIX_EPOCH};

#[derive(Debug, Deserialize)]
pub struct CloudinaryResponse {
    pub secure_url: String,
    pub public_id: String,
    pub format: String,
    // Podrían agregarse más campos como width, height, bytes, etc.
}

pub struct CloudinaryService {
    client: Client,
    cloud_name: String,
    api_key: String,
    api_secret: String,
}

impl CloudinaryService {
    // pub fn new() -> Self {
    //     // En un entorno de producción, es preferible inyectar esto desde un config
    //     Self {
    //         client: Client::new(),
    //         cloud_name: env::var("CLOUDINARY_CLOUD_NAME").unwrap_or_default(),
    //         api_key: env::var("CLOUDINARY_API_KEY").unwrap_or_default(),
    //         api_secret: env::var("CLOUDINARY_API_SECRET").unwrap_or_default(),
    //     }
    // }

    pub fn new() -> Self {
        // Usa expect para asegurar que las variables existan al iniciar
        Self {
            client: Client::new(),
            cloud_name: env::var("CLOUDINARY_CLOUD_NAME").expect("CLOUDINARY_CLOUD_NAME no definida"),
            api_key: env::var("CLOUDINARY_API_KEY").expect("CLOUDINARY_API_KEY no definida"),
            api_secret: env::var("CLOUDINARY_API_SECRET").expect("CLOUDINARY_API_SECRET no definida"),
        }
    }


    /// Sube una imagen o video a Cloudinary utilizando una petición firmada.
    pub async fn upload_media(
        &self,
        file_bytes: Vec<u8>,
        file_name: &str,
        folder: &str,
    ) -> Result<CloudinaryResponse, Box<dyn std::error::Error + Send + Sync>> {
        let timestamp = SystemTime::now()
            .duration_since(UNIX_EPOCH)?
            .as_secs()
            .to_string();

        // 1. Preparar parámetros para firmar (alfabéticamente ordenados)
        let signature_payload = format!("folder={}&timestamp={}{}", folder, timestamp, self.api_secret);
        
        // 2. Hashear con SHA-1
        let mut hasher = Sha1::new();
        hasher.update(signature_payload.as_bytes());
        let signature = hex::encode(hasher.finalize());

        // 3. Crear el formulario multipart
        let file_part = multipart::Part::bytes(file_bytes)
            .file_name(file_name.to_string());

        let form = multipart::Form::new()
            .part("file", file_part)
            .text("api_key", self.api_key.clone())
            .text("timestamp", timestamp)
            .text("signature", signature)
            .text("folder", folder.to_string());

        //let url = format!("https://api.cloudinary.com/v1_1/{}/auto/upload", self.cloud_name);
        let url = format!("https://api.cloudinary.com/v1_1/{}/image/upload", self.cloud_name);
        println!("Enviando a: {}", url); 

        // 4. Enviar Petición
        let response = self.client
            .post(&url)
            .multipart(form)
            .send()
            .await?;

        if response.status().is_success() {
            let cl_resp: CloudinaryResponse = response.json().await?;
            Ok(cl_resp)
        } else {
            let err_text = response.text().await?;
            Err(format!("Cloudinary Error: {}", err_text).into())
        }
    }

    /// Elimina un recurso de Cloudinary de forma física.
    pub async fn delete_media(&self, public_id: &str) -> Result<(), Box<dyn std::error::Error + Send + Sync>> {
        let timestamp = SystemTime::now()
            .duration_since(UNIX_EPOCH)?
            .as_secs()
            .to_string();

        // 1. Firmar (alfabéticamente: public_id, timestamp)
        let signature_payload = format!("public_id={}&timestamp={}{}", public_id, timestamp, self.api_secret);
        
        let mut hasher = Sha1::new();
        hasher.update(signature_payload.as_bytes());
        let signature = hex::encode(hasher.finalize());

        // 2. Parámetros del POST
        let params = [
            ("public_id", public_id),
            ("api_key", &self.api_key),
            ("timestamp", &timestamp),
            ("signature", &signature),
        ];

        let url = format!("https://api.cloudinary.com/v1_1/{}/auto/destroy", self.cloud_name);

        let response = self.client
            .post(&url)
            .form(&params)
            .send()
            .await?;

        if response.status().is_success() {
            Ok(())
        } else {
            let err_text = response.text().await?;
            Err(format!("Cloudinary Delete Error: {}", err_text).into())
        }
    }
}
