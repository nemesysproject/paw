use mongodb::bson::{doc, to_document};
use crate::infrastructure::messaging::rabbitmq::RabbitMQService;
use crate::infrastructure::persistence::mongodb::MongoDBService;
use crate::models::events::PetEvent;
use futures_lite::StreamExt;
use tracing::{info, error};
use std::sync::Arc;
use lapin::options::BasicAckOptions;

pub struct PetSyncWorker {
    rabbit: Arc<RabbitMQService>,
    mongo: Arc<MongoDBService>,
}

impl PetSyncWorker {
    pub fn new(rabbit: Arc<RabbitMQService>, mongo: Arc<MongoDBService>) -> Self {
        Self { rabbit, mongo }
    }

    pub async fn run(&self) {
        info!("Iniciando PetSyncWorker...");
        
        let mut consumer = self.rabbit.get_consumer("pet_sync_queue", "pet.*").await.expect("Error al crear consumidor");

        while let Some(delivery) = consumer.next().await {
            match delivery {
                Ok(delivery) => {
                    let event: PetEvent = serde_json::from_slice(&delivery.data).expect("Error al deserializar evento");
                    
                    match self.handle_event(event).await {
                        Ok(_) => {
                            delivery.ack(BasicAckOptions::default()).await.expect("Error al enviar ACK");
                        }
                        Err(e) => {
                            error!("Error procesando evento: {}", e);
                        }
                    }
                }
                Err(e) => error!("Error en el consumidor de RabbitMQ: {}", e),
            }
        }
    }

    async fn handle_event(&self, event: PetEvent) -> Result<(), String> {
        let collection = self.mongo.pets_collection();
        
        match event {
            PetEvent::Created { pet, media } => {
                let mut doc = to_document(&pet).map_err(|e| e.to_string())?;
                let media_docs: Vec<_> = media.iter().map(|m| to_document(m).unwrap()).collect();
                doc.insert("media", media_docs);
                
                collection.insert_one(doc).await.map_err(|e: mongodb::error::Error| e.to_string())?;
                info!("Mascota {} sincronizada en MongoDB (Creación)", pet.id);
            }
            PetEvent::Updated { pet } => {
                let doc = to_document(&pet).map_err(|e| e.to_string())?;
                collection.replace_one(doc! { "id": &pet.id }, doc).await.map_err(|e: mongodb::error::Error| e.to_string())?;
                info!("Mascota {} sincronizada en MongoDB (Actualización)", pet.id);
            }
            PetEvent::Deleted { pet_id } => {
                collection.delete_one(doc! { "id": pet_id.clone() }).await.map_err(|e: mongodb::error::Error| e.to_string())?;
                info!("Mascota {} eliminada de MongoDB", pet_id);
            }
        }
        Ok(())
    }
}
