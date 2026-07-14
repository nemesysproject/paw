use serde::{Deserialize, Serialize};
use utoipa::ToSchema;
use sqlx::FromRow;

#[derive(Debug, Serialize, Deserialize, ToSchema, FromRow)]
pub struct DashboardStats {
    pub total_lost: i64,
    pub total_adoption: i64,
    pub total_street: i64,
    pub total_at_risk: i64,
    pub total_safe: i64,
    pub total_pets: i64,
}

#[derive(Debug, Serialize, Deserialize, ToSchema, FromRow)]
pub struct PetLocation {
    pub id: String,
    pub name: Option<String>,
    pub status: String,
    pub last_latitude: Option<f64>,
    pub last_longitude: Option<f64>,
    pub image_url: Option<String>,
}

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct DashboardResponse {
    pub stats: DashboardStats,
    pub locations: Vec<PetLocation>,
}
