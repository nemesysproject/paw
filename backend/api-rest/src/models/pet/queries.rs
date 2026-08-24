use serde::{Deserialize, Serialize};
use utoipa::{IntoParams, ToSchema};
use crate::models::entities::{Pet, Media};

#[derive(Debug, Deserialize, IntoParams)]
pub struct SearchParams {
    pub lat: f64,
    pub lon: f64,
    pub radius_meters: Option<u32>,
}

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct PetDetailResponse {
    #[serde(flatten)]
    pub pet: Pet,
    pub media: Vec<Media>,
}
