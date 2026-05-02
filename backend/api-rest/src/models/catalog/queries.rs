use serde::{Deserialize, Serialize};
use utoipa::{IntoParams, ToSchema};

#[derive(Debug, Deserialize, Serialize, IntoParams, ToSchema)]
pub struct BreedQuery {
    pub species_id: Option<String>,
}
