use chrono::NaiveDateTime;
use serde::{Deserialize, Serialize};
use sqlx::FromRow;
use utoipa::{IntoParams, ToSchema};

use crate::models::entities::{AdCategoryEntity, AdEventTypeEntity, AdPlacementEntity};

// --- Query Params ---

#[derive(Debug, Deserialize, IntoParams)]
pub struct AdFeedQuery {
    /// Ubicación en la app: HOME_FEED, BANNER_TOP, POST_REPORT, DIRECTORY_FEATURED
    pub placement: Option<String>,
    /// Categoría: PET_FOOD, VETERINARY, GOVERNMENT, PET_SHOP, SHELTER_EVENT
    pub category: Option<String>,
    /// Latitud del usuario para segmentación geográfica
    pub lat: Option<f64>,
    /// Longitud del usuario para segmentación geográfica
    pub lon: Option<f64>,
    /// Radio de búsqueda en kilómetros (por defecto 10 km)
    pub radius_km: Option<f64>,
    /// Máximo de anuncios a retornar (por defecto 10, máximo 50)
    pub limit: Option<i64>,
}

// --- Advertisers DTOs ---

#[derive(Debug, Deserialize, ToSchema)]
pub struct CreateAdvertiserRequest {
    pub name: String,
    pub category: String,
    pub contact_name: Option<String>,
    pub email: Option<String>,
    pub phone: Option<String>,
    pub whatsapp: Option<String>,
    pub website: Option<String>,
    pub logo_url: Option<String>,
    pub logo_public_id: Option<String>,
    #[serde(default)]
    pub is_government: bool,
    pub official_source_url: Option<String>,
    pub veterinary_id: Option<String>,
}

#[derive(Debug, Deserialize, ToSchema)]
pub struct UpdateAdvertiserRequest {
    pub name: String,
    pub category: String,
    pub contact_name: Option<String>,
    pub email: Option<String>,
    pub phone: Option<String>,
    pub whatsapp: Option<String>,
    pub website: Option<String>,
    pub logo_url: Option<String>,
    pub logo_public_id: Option<String>,
    #[serde(default)]
    pub is_government: bool,
    pub official_source_url: Option<String>,
    pub veterinary_id: Option<String>,
    #[serde(default = "default_true")]
    pub is_active: bool,
}

// --- Campaigns DTOs ---

#[derive(Debug, Deserialize, ToSchema)]
pub struct CreateAdCampaignRequest {
    pub advertiser_id: String,
    pub title: String,
    pub description: Option<String>,
    pub cta_label: Option<String>,
    pub target_url: Option<String>,
    pub image_url: String,
    pub image_public_id: String,
    pub category: String,
    pub placement: String,
    pub badge_label: Option<String>,
    pub latitude: Option<f64>,
    pub longitude: Option<f64>,
    pub radius_km: Option<f64>,
    #[serde(default)]
    pub priority: i32,
    pub starts_at: NaiveDateTime,
    pub ends_at: NaiveDateTime,
    #[serde(default = "default_true")]
    pub is_active: bool,
}

#[derive(Debug, Deserialize, ToSchema)]
pub struct UpdateAdCampaignRequest {
    pub title: String,
    pub description: Option<String>,
    pub cta_label: Option<String>,
    pub target_url: Option<String>,
    pub image_url: String,
    pub image_public_id: String,
    pub category: String,
    pub placement: String,
    pub badge_label: Option<String>,
    pub latitude: Option<f64>,
    pub longitude: Option<f64>,
    pub radius_km: Option<f64>,
    #[serde(default)]
    pub priority: i32,
    pub starts_at: NaiveDateTime,
    pub ends_at: NaiveDateTime,
    #[serde(default = "default_true")]
    pub is_active: bool,
}

// --- Metric Events DTOs ---

#[derive(Debug, Deserialize, ToSchema)]
pub struct RecordAdEventRequest {
    /// IMPRESSION o CLICK
    pub event_type: String,
    pub user_id: Option<String>,
    pub latitude: Option<f64>,
    pub longitude: Option<f64>,
}

// --- Responses ---

/// Tarjeta de anuncio enriquecida con datos del anunciante, lista para renderizar en la app móvil.
#[derive(Debug, Clone, Serialize, Deserialize, FromRow, ToSchema)]
pub struct AdFeedItemResponse {
    pub id: String,
    pub advertiser_id: String,
    pub advertiser_name: String,
    pub advertiser_logo_url: Option<String>,
    pub is_government: bool,
    pub official_source_url: Option<String>,
    pub title: String,
    pub description: Option<String>,
    pub cta_label: String,
    pub target_url: Option<String>,
    pub image_url: String,
    pub category: String,
    pub placement: String,
    pub badge_label: String,
    pub latitude: Option<f64>,
    pub longitude: Option<f64>,
    pub radius_km: Option<f64>,
    pub priority: i32,
    pub starts_at: NaiveDateTime,
    pub ends_at: NaiveDateTime,
}

#[derive(Debug, Clone, Serialize, Deserialize, ToSchema)]
pub struct AdCampaignSummaryResponse {
    pub id: String,
    pub advertiser_id: String,
    pub advertiser_name: String,
    pub is_government: bool,
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
    pub ctr_percentage: f64,
    pub created_at: NaiveDateTime,
    pub updated_at: NaiveDateTime,
}

#[derive(Debug, Serialize, ToSchema)]
pub struct AdCatalogsResponse {
    pub categories: Vec<AdCategoryEntity>,
    pub placements: Vec<AdPlacementEntity>,
    pub event_types: Vec<AdEventTypeEntity>,
}

#[derive(Debug, Serialize, ToSchema)]
pub struct UploadAdImageResponse {
    pub image_url: String,
    pub image_public_id: String,
}

fn default_true() -> bool {
    true
}
