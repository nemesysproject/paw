use serde::{Deserialize, Serialize};
use utoipa::ToSchema;

#[derive(Debug, Deserialize, Serialize, ToSchema)]
pub struct CreateSpeciesCommand {
    pub name: String,
}

#[derive(Debug, Deserialize, Serialize, ToSchema)]
pub struct UpdateSpeciesCommand {
    pub name: String,
}

#[derive(Debug, Deserialize, Serialize, ToSchema)]
pub struct CreateBreedCommand {
    pub name: String,
    pub species_id: String,
}

#[derive(Debug, Deserialize, Serialize, ToSchema)]
pub struct UpdateBreedCommand {
    pub name: String,
    pub species_id: String,
}
