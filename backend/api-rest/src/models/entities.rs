use chrono::NaiveDateTime;
use serde::{Deserialize, Serialize};
use sqlx::FromRow;
use utoipa::ToSchema;

// Removed enums as they are now table-based entities

#[derive(Debug, Clone, Serialize, Deserialize, FromRow, ToSchema)]
pub struct UserRole {
    pub id: String,
    pub name: String,
}

#[derive(Debug, Clone, Serialize, Deserialize, FromRow, ToSchema)]
pub struct User {
    pub id: String,
    pub email: String,
    pub password: String, // En la BD guardaremos el hash, ojo al exponer esto en el DTO final
    pub name: String,
    pub role: String,
    pub created_at: NaiveDateTime,
    pub updated_at: NaiveDateTime,
}

#[derive(Debug, Clone, Serialize, Deserialize, FromRow, ToSchema)]
pub struct Species {
    pub id: String,
    pub name: String,
}

#[derive(Debug, Clone, Serialize, Deserialize, FromRow, ToSchema)]
pub struct Breed {
    pub id: String,
    pub name: String,
    pub species_id: String,
}

#[derive(Debug, Clone, Serialize, Deserialize, FromRow, ToSchema)]
pub struct PetGender {
    pub id: String,
    pub name: String,
}

#[derive(Debug, Clone, Serialize, Deserialize, FromRow, ToSchema)]
pub struct PetStatus {
    pub id: String,
    pub name: String,
}

#[derive(Debug, Clone, Serialize, Deserialize, FromRow, ToSchema)]
pub struct Pet {
    pub id: String,
    pub name: Option<String>,
    pub gender: String,
    pub status: String,
    pub description: Option<String>,
    
    // Relaciones
    pub species_id: String,
    pub breed_id: Option<String>,
    pub reporter_id: String,
    pub shelter_id: Option<String>,
    
    pub created_at: NaiveDateTime,
    pub updated_at: NaiveDateTime,
    
    // Caché desnormalizado de ubicación
    pub last_latitude: Option<f64>,
    pub last_longitude: Option<f64>,
    pub last_geohash: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize, FromRow, ToSchema)]
pub struct MediaType {
    pub id: String,
    pub name: String,
}

#[derive(Debug, Clone, Serialize, Deserialize, FromRow, ToSchema)]
pub struct Media {
    pub id: String,
    pub url: String,
    pub public_id: String,
    pub r#type: String, // 'type' es palabra reservada en Rust
    pub pet_id: String,
    pub latitude: Option<f64>,
    pub longitude: Option<f64>,
    pub geohash: Option<String>,
    pub created_at: NaiveDateTime,
}

#[derive(Debug, Clone, Serialize, Deserialize, FromRow, ToSchema)]
pub struct Shelter {
    pub id: String,
    pub name: String,
    pub address: String,
    pub phone: Option<String>,
    pub email: Option<String>,
    pub latitude: Option<f64>,
    pub longitude: Option<f64>,
    pub created_at: NaiveDateTime,
    pub updated_at: NaiveDateTime,
}

#[derive(Debug, Clone, Serialize, Deserialize, FromRow, ToSchema)]
pub struct Veterinary {
    pub id: String,
    pub name: String,
    pub address: String,
    pub phone: Option<String>,
    pub email: Option<String>,
    pub latitude: Option<f64>,
    pub longitude: Option<f64>,
    pub created_at: NaiveDateTime,
    pub updated_at: NaiveDateTime,
}

#[derive(Debug, Clone, Serialize, Deserialize, sqlx::FromRow, utoipa::ToSchema)]
pub struct MediaTypeEntity {
    pub id: String,
    pub name: String,
}
