use chrono::NaiveDateTime;
use serde::{Deserialize, Serialize};
use sqlx::FromRow;
use utoipa::ToSchema;

// --- Sightings ---

#[derive(Debug, Deserialize, ToSchema)]
pub struct CreateSightingRequest {
    pub pet_id: String,
    pub notes: Option<String>,
    pub latitude: f64,
    pub longitude: f64,
    pub image_url: Option<String>,
    pub image_public_id: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize, FromRow, ToSchema)]
pub struct PetSightingResponse {
    pub id: String,
    pub pet_id: String,
    pub reporter_id: String,
    pub reporter_name: Option<String>,
    pub notes: Option<String>,
    pub latitude: f64,
    pub longitude: f64,
    pub geohash: Option<String>,
    pub image_url: Option<String>,
    pub sighted_at: NaiveDateTime,
    pub is_confirmed_by_owner: bool,
    pub created_at: NaiveDateTime,
}

// --- Conversations & Messages ---

#[derive(Debug, Deserialize, ToSchema)]
pub struct CreateConversationRequest {
    /// PET_SIGHTING, ADOPTION_INQUIRY, SHELTER_SUPPORT, VET_CONSULT
    pub r#type: String,
    pub pet_id: Option<String>,
    pub shelter_id: Option<String>,
    pub veterinary_id: Option<String>,
    pub target_user_id: Option<String>,
    pub initial_message: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize, FromRow, ToSchema)]
pub struct ConversationSummaryResponse {
    pub id: String,
    pub r#type: String,
    pub status: String,
    pub pet_id: Option<String>,
    pub pet_name: Option<String>,
    pub shelter_id: Option<String>,
    pub veterinary_id: Option<String>,
    pub counterpart_name: Option<String>,
    pub last_message_preview: Option<String>,
    pub last_message_at: Option<NaiveDateTime>,
    pub created_at: NaiveDateTime,
    pub updated_at: NaiveDateTime,
}

#[derive(Debug, Deserialize, ToSchema)]
pub struct SendMessageRequest {
    /// TEXT, IMAGE, LOCATION
    pub r#type: Option<String>,
    pub content: Option<String>,
    pub media_url: Option<String>,
    pub media_public_id: Option<String>,
    pub latitude: Option<f64>,
    pub longitude: Option<f64>,
    pub location_label: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize, FromRow, ToSchema)]
pub struct MessageResponse {
    pub id: String,
    pub conversation_id: String,
    pub sender_id: String,
    pub sender_name: Option<String>,
    pub r#type: String,
    pub content: Option<String>,
    pub media_url: Option<String>,
    pub latitude: Option<f64>,
    pub longitude: Option<f64>,
    pub geohash: Option<String>,
    pub location_label: Option<String>,
    pub created_at: NaiveDateTime,
}

// --- Adoptions ---

#[derive(Debug, Deserialize, ToSchema)]
pub struct CreateAdoptionRequest {
    pub pet_id: String,
    pub shelter_id: Option<String>,
    pub notes: Option<String>,
}

#[derive(Debug, Deserialize, ToSchema)]
pub struct UpdateAdoptionStatusRequest {
    /// PENDING, IN_REVIEW, APPROVED, REJECTED, CANCELLED
    pub status: String,
}

#[derive(Debug, Clone, Serialize, Deserialize, FromRow, ToSchema)]
pub struct AdoptionRequestResponse {
    pub id: String,
    pub pet_id: String,
    pub pet_name: Option<String>,
    pub applicant_id: String,
    pub applicant_name: Option<String>,
    pub owner_id: String,
    pub shelter_id: Option<String>,
    pub conversation_id: Option<String>,
    pub status: String,
    pub notes: Option<String>,
    pub created_at: NaiveDateTime,
    pub updated_at: NaiveDateTime,
}

// --- Push Tokens & UGC Moderation ---

#[derive(Debug, Deserialize, ToSchema)]
pub struct RegisterDeviceTokenRequest {
    pub fcm_token: String,
    pub platform: Option<String>,
}

#[derive(Debug, Deserialize, ToSchema)]
pub struct BlockUserRequest {
    pub blocked_user_id: String,
    pub reason: Option<String>,
}

#[derive(Debug, Deserialize, ToSchema)]
pub struct CreateContentReportRequest {
    /// USER, MESSAGE, SIGHTING, PET
    pub target_type: String,
    pub target_id: String,
    pub reason: String,
}
