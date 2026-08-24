use serde::{Deserialize, Serialize};
// use crate::models::enums::{PetGender, PetStatus};
use utoipa::ToSchema;
use validator::Validate;

#[derive(Debug, Clone, Serialize, Deserialize, ToSchema, Validate)]
pub struct CreatePetCommand {
    #[validate(length(max = 100))]
    pub name: Option<String>,
    #[validate(length(min = 1, max = 50))]
    pub gender: String,
    #[validate(length(min = 1, max = 50))]
    pub status: String,
    #[validate(length(max = 1000))]
    pub description: Option<String>,
    #[validate(length(min = 1))]
    pub species_id: String,
    pub breed_id: Option<String>,
    #[validate(length(min = 1))]
    pub reporter_id: String,
    pub shelter_id: Option<String>,
    #[validate(range(min = -90.0, max = 90.0))]
    pub last_latitude: Option<f64>,
    #[validate(range(min = -180.0, max = 180.0))]
    pub last_longitude: Option<f64>,
}

#[derive(Debug, Clone, Serialize, Deserialize, ToSchema, Validate)]
pub struct UpdatePetCommand {
    #[validate(length(max = 100))]
    pub name: Option<String>,
    #[validate(length(min = 1, max = 50))]
    pub gender: Option<String>,
    #[validate(length(min = 1, max = 50))]
    pub status: Option<String>,
    #[validate(length(max = 1000))]
    pub description: Option<String>,
    #[validate(length(min = 1))]
    pub species_id: Option<String>,
    pub breed_id: Option<String>,
    pub shelter_id: Option<String>,
    #[validate(range(min = -90.0, max = 90.0))]
    pub last_latitude: Option<f64>,
    #[validate(range(min = -180.0, max = 180.0))]
    pub last_longitude: Option<f64>,
}
