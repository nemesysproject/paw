use async_trait::async_trait;
use crate::models::events::PetEvent;
use mongodb::Collection;

#[async_trait]
pub trait MediaStorage: Send + Sync {
    async fn upload_media(&self, data: Vec<u8>, file_name: &str, folder: &str) -> Result<crate::infrastructure::services::cloudinary::CloudinaryResponse, String>;
    async fn delete_media(&self, public_id: &str) -> Result<(), String>;
}

#[async_trait]
pub trait EventBus: Send + Sync {
    async fn publish_event(&self, routing_key: &str, event: &PetEvent) -> Result<(), String>;
}

pub trait ReadModelDb: Send + Sync {
    fn pets_collection(&self) -> Collection<mongodb::bson::Document>;
}
