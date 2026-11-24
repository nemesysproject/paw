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
    #[serde(skip_serializing)]
    pub password: Option<String>, // Hash Argon2; None para usuarios creados únicamente con login social
    pub email_verified: bool,
    pub name: String,
    pub phone: Option<String>,
    pub avatar_url: Option<String>,
    pub role: String,
    pub created_at: NaiveDateTime,
    pub updated_at: NaiveDateTime,
}

#[derive(Debug, Clone, Serialize, Deserialize, FromRow, ToSchema)]
pub struct UserIdentity {
    pub id: String,
    pub user_id: String,
    pub provider: String,
    pub provider_user_id: String,
    pub email: Option<String>,
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
    pub owner_id: Option<String>,
    pub created_at: NaiveDateTime,
    pub updated_at: NaiveDateTime,
}

#[derive(Debug, Clone, Serialize, Deserialize, FromRow, ToSchema)]
pub struct Veterinary {
    pub id: String,
    pub name: String,
    pub address: String,
    pub phone: Option<String>,
    pub whatsapp: Option<String>,
    pub email: Option<String>,
    pub website: Option<String>,
    pub is_verified: bool,
    pub is_featured: bool,
    pub latitude: Option<f64>,
    pub longitude: Option<f64>,
    pub geohash: Option<String>,
    pub owner_id: Option<String>,
    pub created_at: NaiveDateTime,
    pub updated_at: NaiveDateTime,
}

#[derive(Debug, Clone, Serialize, Deserialize, sqlx::FromRow, utoipa::ToSchema)]
pub struct MediaTypeEntity {
    pub id: String,
    pub name: String,
}

#[derive(Debug, Clone, Serialize, Deserialize, FromRow, ToSchema)]
pub struct AdCategoryEntity {
    pub id: String,
    pub name: String,
}

#[derive(Debug, Clone, Serialize, Deserialize, FromRow, ToSchema)]
pub struct AdPlacementEntity {
    pub id: String,
    pub name: String,
}

#[derive(Debug, Clone, Serialize, Deserialize, FromRow, ToSchema)]
pub struct AdEventTypeEntity {
    pub id: String,
    pub name: String,
}

#[derive(Debug, Clone, Serialize, Deserialize, FromRow, ToSchema)]
pub struct Advertiser {
    pub id: String,
    pub name: String,
    pub category: String,
    pub contact_name: Option<String>,
    pub email: Option<String>,
    pub phone: Option<String>,
    pub whatsapp: Option<String>,
    pub website: Option<String>,
    pub logo_url: Option<String>,
    pub logo_public_id: Option<String>,
    pub is_government: bool,
    pub official_source_url: Option<String>,
    pub veterinary_id: Option<String>,
    pub is_active: bool,
    pub created_at: NaiveDateTime,
    pub updated_at: NaiveDateTime,
}

#[derive(Debug, Clone, Serialize, Deserialize, FromRow, ToSchema)]
pub struct AdCampaign {
    pub id: String,
    pub advertiser_id: String,
    pub title: String,
    pub description: Option<String>,
    pub cta_label: String,
    pub target_url: Option<String>,
    pub image_url: String,
    pub image_public_id: String,
    pub category: String,
    pub placement: String,
    pub badge_label: String,
    pub latitude: Option<f64>,
    pub longitude: Option<f64>,
    pub geohash: Option<String>,
    pub radius_km: Option<f64>,
    pub priority: i32,
    pub starts_at: NaiveDateTime,
    pub ends_at: NaiveDateTime,
    pub is_active: bool,
    pub impressions_count: i64,
    pub clicks_count: i64,
    pub created_at: NaiveDateTime,
    pub updated_at: NaiveDateTime,
}

#[derive(Debug, Clone, Serialize, Deserialize, FromRow, ToSchema)]
pub struct AdMetricEvent {
    pub id: String,
    pub campaign_id: String,
    pub event_type: String,
    pub user_id: Option<String>,
    pub geohash: Option<String>,
    pub created_at: NaiveDateTime,
}

// --- Entidades de Interactividad Social (Avistamientos, Chat, Adopciones, Push, UGC) ---

#[derive(Debug, Clone, Serialize, Deserialize, FromRow, ToSchema)]
pub struct DeviceToken {
    pub id: String,
    pub user_id: String,
    pub fcm_token: String,
    pub platform: String,
    pub created_at: NaiveDateTime,
    pub updated_at: NaiveDateTime,
}

#[derive(Debug, Clone, Serialize, Deserialize, FromRow, ToSchema)]
pub struct PetSighting {
    pub id: String,
    pub pet_id: String,
    pub reporter_id: String,
    pub notes: Option<String>,
    pub latitude: f64,
    pub longitude: f64,
    pub geohash: Option<String>,
    pub image_url: Option<String>,
    pub image_public_id: Option<String>,
    pub sighted_at: NaiveDateTime,
    pub is_confirmed_by_owner: bool,
    pub created_at: NaiveDateTime,
}

#[derive(Debug, Clone, Serialize, Deserialize, FromRow, ToSchema)]
pub struct Conversation {
    pub id: String,
    pub r#type: String,
    pub status: String,
    pub pet_id: Option<String>,
    pub shelter_id: Option<String>,
    pub veterinary_id: Option<String>,
    pub last_message_at: Option<NaiveDateTime>,
    pub created_at: NaiveDateTime,
    pub updated_at: NaiveDateTime,
}

#[derive(Debug, Clone, Serialize, Deserialize, FromRow, ToSchema)]
pub struct ConversationParticipant {
    pub id: String,
    pub conversation_id: String,
    pub user_id: String,
    pub last_read_at: Option<NaiveDateTime>,
    pub is_muted: bool,
    pub joined_at: NaiveDateTime,
}

#[derive(Debug, Clone, Serialize, Deserialize, FromRow, ToSchema)]
pub struct Message {
    pub id: String,
    pub conversation_id: String,
    pub sender_id: String,
    pub r#type: String,
    pub content: Option<String>,
    pub media_url: Option<String>,
    pub media_public_id: Option<String>,
    pub latitude: Option<f64>,
    pub longitude: Option<f64>,
    pub geohash: Option<String>,
    pub location_label: Option<String>,
    pub created_at: NaiveDateTime,
    pub deleted_at: Option<NaiveDateTime>,
}

#[derive(Debug, Clone, Serialize, Deserialize, FromRow, ToSchema)]
pub struct AdoptionRequest {
    pub id: String,
    pub pet_id: String,
    pub applicant_id: String,
    pub owner_id: String,
    pub shelter_id: Option<String>,
    pub conversation_id: Option<String>,
    pub status: String,
    pub notes: Option<String>,
    pub created_at: NaiveDateTime,
    pub updated_at: NaiveDateTime,
}

#[derive(Debug, Clone, Serialize, Deserialize, FromRow, ToSchema)]
pub struct UserBlock {
    pub id: String,
    pub blocker_id: String,
    pub blocked_user_id: String,
    pub reason: Option<String>,
    pub created_at: NaiveDateTime,
}

#[derive(Debug, Clone, Serialize, Deserialize, FromRow, ToSchema)]
pub struct ContentReport {
    pub id: String,
    pub reporter_id: String,
    pub target_type: String,
    pub target_id: String,
    pub reason: String,
    pub status: String,
    pub created_at: NaiveDateTime,
    pub updated_at: NaiveDateTime,
}
