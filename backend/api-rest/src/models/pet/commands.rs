use serde::{Deserialize, Serialize};
// use crate::models::enums::{PetGender, PetStatus};
use utoipa::ToSchema;

#[derive(Debug, Clone, Serialize, Deserialize, ToSchema)]
pub struct CreatePetCommand {
    pub name: Option<String>,
    pub gender: String,
    pub status: String,
    pub description: Option<String>,
    pub species_id: String,
    pub breed_id: Option<String>,
    pub reporter_id: String,
    pub shelter_id: Option<String>,
    pub last_latitude: Option<f64>,
    pub last_longitude: Option<f64>,
}

#[derive(Debug, Clone, Serialize, Deserialize, ToSchema)]
pub struct UpdatePetCommand {
    pub name: Option<String>,
    pub gender: Option<String>,
    pub status: Option<String>,
    pub description: Option<String>,
    pub species_id: Option<String>,
    pub breed_id: Option<String>,
    pub shelter_id: Option<String>,
    pub last_latitude: Option<f64>,
    pub last_longitude: Option<f64>,
}
