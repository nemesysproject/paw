use serde::{Serialize, Deserialize};
use crate::models::entities::{Pet, Media};

#[derive(Debug, Serialize, Deserialize)]
#[serde(tag = "type", content = "data", rename_all = "SCREAMING_SNAKE_CASE")]
pub enum PetEvent {
    Created { pet: Pet, media: Vec<Media> },
    Updated { pet: Pet },
    Deleted { pet_id: String },
}
