use axum::{
    extract::{Multipart, Path, Query, State},
    http::StatusCode,
    response::{IntoResponse, Response},
    Json,
};
use chrono::Utc;
use geohash::{encode, neighbors, Coord};
use serde_json::json;
use std::collections::HashMap;
use uuid::Uuid;

use crate::infrastructure::auth::JwtMiddleware;
use crate::models::ads::{
    AdCampaignSummaryResponse, AdCatalogsResponse, AdFeedItemResponse, AdFeedQuery,
    CreateAdCampaignRequest, CreateAdvertiserRequest, RecordAdEventRequest,
    UpdateAdCampaignRequest, UpdateAdvertiserRequest, UploadAdImageResponse,
};
use crate::models::entities::{AdCampaign, AdMetricEvent, Advertiser};
use crate::repositories::ad_repo::AdRepository;
use crate::AppState;

// =============================================================================
// Endpoints Públicos (App Móvil: Huellitas Safe)
// =============================================================================

/// Obtiene anuncios y campañas activas filtradas por ubicación (Geohash), categoría y placement.
#[utoipa::path(
    get,
    path = "/api/v1/ads",
    params(AdFeedQuery),
    responses(
        (status = 200, description = "Lista de anuncios vigentes", body = [AdFeedItemResponse]),
        (status = 500, description = "Error interno")
    ),
    tag = "Ads"
)]
pub async fn get_active_ads(
    State(state): State<AppState>,
    Query(query): Query<AdFeedQuery>,
) -> Result<Response, Response> {
    let limit = query.limit.unwrap_or(10).clamp(1, 50);

    let geo_patterns = match (query.lat, query.lon) {
        (Some(lat), Some(lon)) => {
            let radius_km = query.radius_km.unwrap_or(10.0).max(0.5);
            Some(build_geohash_prefixes(lat, lon, radius_km))
        }
        _ => None,
    };

    let ads = AdRepository::fetch_active_feed(
        &state.pool,
        query.placement.as_deref(),
        query.category.as_deref(),
        geo_patterns.as_deref(),
        limit,
    )
    .await
    .map_err(|e| {
        tracing::error!("Error obteniendo feed de anuncios: {:?}", e);
        (
            StatusCode::INTERNAL_SERVER_ERROR,
            Json(json!({"error": "Error al consultar anuncios"})),
        )
            .into_response()
    })?;

    Ok((StatusCode::OK, Json(ads)).into_response())
}

/// Registra un evento de métrica (IMPRESSION o CLICK) para una campaña.
#[utoipa::path(
    post,
    path = "/api/v1/ads/{id}/events",
    params(("id" = String, Path, description = "ID de la campaña")),
    request_body = RecordAdEventRequest,
    responses(
        (status = 201, description = "Evento registrado"),
        (status = 400, description = "Tipo de evento inválido"),
        (status = 404, description = "Campaña no encontrada")
    ),
    tag = "Ads"
)]
pub async fn record_ad_event(
    State(state): State<AppState>,
    Path(campaign_id): Path<String>,
    Json(payload): Json<RecordAdEventRequest>,
) -> Result<Response, Response> {
    let event_type = payload.event_type.trim().to_uppercase();
    if event_type != "IMPRESSION" && event_type != "CLICK" {
        return Err((
            StatusCode::BAD_REQUEST,
            Json(json!({"error": "event_type debe ser IMPRESSION o CLICK"})),
        )
            .into_response());
    }

    let geohash = match (payload.latitude, payload.longitude) {
        (Some(lat), Some(lon)) => encode(Coord { x: lon, y: lat }, 6).ok(),
        _ => None,
    };

    let event = AdMetricEvent {
        id: Uuid::new_v4().to_string(),
        campaign_id,
        event_type,
        user_id: payload.user_id,
        geohash,
        created_at: Utc::now().naive_utc(),
    };

    let recorded = AdRepository::record_metric_event(&state.pool, &event)
        .await
        .map_err(|e| {
            tracing::error!("Error registrando métrica de anuncio: {:?}", e);
            (
                StatusCode::INTERNAL_SERVER_ERROR,
                Json(json!({"error": "Error registrando métrica"})),
            )
                .into_response()
        })?;

    if !recorded {
        return Err((
            StatusCode::NOT_FOUND,
            Json(json!({"error": "Campaña no encontrada"})),
        )
            .into_response());
    }

    Ok((StatusCode::CREATED, Json(json!({"status": "recorded"}))).into_response())
}

/// Obtiene los catálogos de publicidad (categorías, ubicaciones y tipos de evento).
#[utoipa::path(
    get,
    path = "/api/v1/ads/catalogs",
    responses(
        (status = 200, description = "Catálogos de publicidad", body = AdCatalogsResponse),
        (status = 500, description = "Error interno")
    ),
    tag = "Ads"
)]
pub async fn get_ad_catalogs(State(state): State<AppState>) -> Result<Response, Response> {
    let categories = AdRepository::get_categories(&state.pool).await.map_err(|e| {
        tracing::error!("Error obteniendo AdCategory: {:?}", e);
        (
            StatusCode::INTERNAL_SERVER_ERROR,
            Json(json!({"error": "Error interno"})),
        )
            .into_response()
    })?;

    let placements = AdRepository::get_placements(&state.pool).await.map_err(|e| {
        tracing::error!("Error obteniendo AdPlacement: {:?}", e);
        (
            StatusCode::INTERNAL_SERVER_ERROR,
            Json(json!({"error": "Error interno"})),
        )
            .into_response()
    })?;

    let event_types = AdRepository::get_event_types(&state.pool).await.map_err(|e| {
        tracing::error!("Error obteniendo AdEventType: {:?}", e);
        (
            StatusCode::INTERNAL_SERVER_ERROR,
            Json(json!({"error": "Error interno"})),
        )
            .into_response()
    })?;

    Ok((
        StatusCode::OK,
        Json(AdCatalogsResponse {
            categories,
            placements,
            event_types,
        }),
    )
        .into_response())
}

// =============================================================================
// Gestión de Anunciantes (Protegido con JWT — Portal Admin paw-portal)
// =============================================================================

#[utoipa::path(
    get,
    path = "/api/v1/ads/advertisers",
    responses(
        (status = 200, description = "Lista de anunciantes", body = [Advertiser]),
        (status = 401, description = "No autorizado")
    ),
    tag = "Ads"
)]
pub async fn list_advertisers(
    _auth: JwtMiddleware,
    State(state): State<AppState>,
) -> Result<Response, Response> {
    let advertisers = AdRepository::list_advertisers(&state.pool).await.map_err(|e| {
        tracing::error!("Error listando anunciantes: {:?}", e);
        (
            StatusCode::INTERNAL_SERVER_ERROR,
            Json(json!({"error": "Error listando anunciantes"})),
        )
            .into_response()
    })?;

    Ok((StatusCode::OK, Json(advertisers)).into_response())
}

#[utoipa::path(
    post,
    path = "/api/v1/ads/advertisers",
    request_body = CreateAdvertiserRequest,
    responses(
        (status = 201, description = "Anunciante creado", body = Advertiser),
        (status = 400, description = "Datos inválidos"),
        (status = 401, description = "No autorizado")
    ),
    tag = "Ads"
)]
pub async fn create_advertiser(
    _auth: JwtMiddleware,
    State(state): State<AppState>,
    Json(payload): Json<CreateAdvertiserRequest>,
) -> Result<Response, Response> {
    if payload.name.trim().is_empty() {
        return Err((
            StatusCode::BAD_REQUEST,
            Json(json!({"error": "El nombre del anunciante es obligatorio"})),
        )
            .into_response());
    }

    if payload.is_government
        && payload
            .official_source_url
            .as_deref()
            .map(|u| u.trim().is_empty())
            .unwrap_or(true)
    {
        return Err((
            StatusCode::BAD_REQUEST,
            Json(json!({
                "error": "Las campañas gubernamentales requieren official_source_url para cumplimiento en Google Play"
            })),
        )
            .into_response());
    }

    let now = Utc::now().naive_utc();
    let advertiser = Advertiser {
        id: Uuid::new_v4().to_string(),
        name: payload.name.trim().to_string(),
        category: payload.category,
        contact_name: payload.contact_name,
        email: payload.email,
        phone: payload.phone,
        whatsapp: payload.whatsapp,
        website: payload.website,
        logo_url: payload.logo_url,
        logo_public_id: payload.logo_public_id,
        is_government: payload.is_government,
        official_source_url: payload.official_source_url,
        veterinary_id: payload.veterinary_id,
        is_active: true,
        created_at: now,
        updated_at: now,
    };

    AdRepository::create_advertiser(&state.pool, &advertiser)
        .await
        .map_err(|e| {
            tracing::error!("Error creando anunciante: {:?}", e);
            (
                StatusCode::INTERNAL_SERVER_ERROR,
                Json(json!({"error": e.to_string()})),
            )
                .into_response()
        })?;

    Ok((StatusCode::CREATED, Json(advertiser)).into_response())
}

#[utoipa::path(
    put,
    path = "/api/v1/ads/advertisers/{id}",
    params(("id" = String, Path, description = "ID del anunciante")),
    request_body = UpdateAdvertiserRequest,
    responses(
        (status = 200, description = "Anunciante actualizado", body = Advertiser),
        (status = 404, description = "No encontrado")
    ),
    tag = "Ads"
)]
pub async fn update_advertiser(
    _auth: JwtMiddleware,
    State(state): State<AppState>,
    Path(id): Path<String>,
    Json(payload): Json<UpdateAdvertiserRequest>,
) -> Result<Response, Response> {
    let existing = AdRepository::get_advertiser_by_id(&state.pool, &id)
        .await
        .map_err(|e| {
            tracing::error!("Error buscando anunciante: {:?}", e);
            (
                StatusCode::INTERNAL_SERVER_ERROR,
                Json(json!({"error": "Error interno"})),
            )
                .into_response()
        })?;

    let existing = match existing {
        Some(a) => a,
        None => {
            return Err((
                StatusCode::NOT_FOUND,
                Json(json!({"error": "Anunciante no encontrado"})),
            )
                .into_response())
        }
    };

    let updated = Advertiser {
        id: id.clone(),
        name: payload.name.trim().to_string(),
        category: payload.category,
        contact_name: payload.contact_name,
        email: payload.email,
        phone: payload.phone,
        whatsapp: payload.whatsapp,
        website: payload.website,
        logo_url: payload.logo_url,
        logo_public_id: payload.logo_public_id,
        is_government: payload.is_government,
        official_source_url: payload.official_source_url,
        veterinary_id: payload.veterinary_id,
        is_active: payload.is_active,
        created_at: existing.created_at,
        updated_at: Utc::now().naive_utc(),
    };

    AdRepository::update_advertiser(&state.pool, &id, &updated)
        .await
        .map_err(|e| {
            tracing::error!("Error actualizando anunciante: {:?}", e);
            (
                StatusCode::INTERNAL_SERVER_ERROR,
                Json(json!({"error": e.to_string()})),
            )
                .into_response()
        })?;

    Ok((StatusCode::OK, Json(updated)).into_response())
}

#[utoipa::path(
    delete,
    path = "/api/v1/ads/advertisers/{id}",
    params(("id" = String, Path, description = "ID del anunciante")),
    responses(
        (status = 200, description = "Anunciante eliminado"),
        (status = 404, description = "No encontrado")
    ),
    tag = "Ads"
)]
pub async fn delete_advertiser(
    _auth: JwtMiddleware,
    State(state): State<AppState>,
    Path(id): Path<String>,
) -> Result<Response, Response> {
    let rows = AdRepository::delete_advertiser(&state.pool, &id)
        .await
        .map_err(|e| {
            tracing::error!("Error eliminando anunciante: {:?}", e);
            (
                StatusCode::INTERNAL_SERVER_ERROR,
                Json(json!({"error": e.to_string()})),
            )
                .into_response()
        })?;

    if rows == 0 {
        Ok(StatusCode::NOT_FOUND.into_response())
    } else {
        Ok(StatusCode::OK.into_response())
    }
}

// =============================================================================
// Gestión de Campañas y Creatividades (Protegido con JWT — Portal Admin)
// =============================================================================

#[utoipa::path(
    get,
    path = "/api/v1/ads/campaigns",
    responses(
        (status = 200, description = "Lista de campañas con métricas CTR", body = [AdCampaignSummaryResponse]),
        (status = 401, description = "No autorizado")
    ),
    tag = "Ads"
)]
pub async fn list_campaigns(
    _auth: JwtMiddleware,
    State(state): State<AppState>,
) -> Result<Response, Response> {
    let campaigns = AdRepository::list_campaigns(&state.pool).await.map_err(|e| {
        tracing::error!("Error listando campañas: {:?}", e);
        (
            StatusCode::INTERNAL_SERVER_ERROR,
            Json(json!({"error": "Error listando campañas"})),
        )
            .into_response()
    })?;

    let advertisers = AdRepository::list_advertisers(&state.pool)
        .await
        .map_err(|e| {
            tracing::error!("Error listando anunciantes para resumen: {:?}", e);
            (
                StatusCode::INTERNAL_SERVER_ERROR,
                Json(json!({"error": "Error interno"})),
            )
                .into_response()
        })?;

    let adv_map: HashMap<String, Advertiser> =
        advertisers.into_iter().map(|a| (a.id.clone(), a)).collect();

    let summaries: Vec<AdCampaignSummaryResponse> = campaigns
        .into_iter()
        .map(|c| {
            let adv = adv_map.get(&c.advertiser_id);
            to_campaign_summary(c, adv)
        })
        .collect();

    Ok((StatusCode::OK, Json(summaries)).into_response())
}

#[utoipa::path(
    get,
    path = "/api/v1/ads/campaigns/{id}",
    params(("id" = String, Path, description = "ID de la campaña")),
    responses(
        (status = 200, description = "Detalle de campaña con CTR", body = AdCampaignSummaryResponse),
        (status = 404, description = "No encontrada")
    ),
    tag = "Ads"
)]
pub async fn get_campaign_by_id(
    _auth: JwtMiddleware,
    State(state): State<AppState>,
    Path(id): Path<String>,
) -> Result<Response, Response> {
    let campaign = AdRepository::get_campaign_by_id(&state.pool, &id)
        .await
        .map_err(|e| {
            tracing::error!("Error buscando campaña: {:?}", e);
            (
                StatusCode::INTERNAL_SERVER_ERROR,
                Json(json!({"error": "Error interno"})),
            )
                .into_response()
        })?;

    let campaign = match campaign {
        Some(c) => c,
        None => return Ok(StatusCode::NOT_FOUND.into_response()),
    };

    let advertiser = AdRepository::get_advertiser_by_id(&state.pool, &campaign.advertiser_id)
        .await
        .ok()
        .flatten();

    Ok((
        StatusCode::OK,
        Json(to_campaign_summary(campaign, advertiser.as_ref())),
    )
        .into_response())
}

#[utoipa::path(
    post,
    path = "/api/v1/ads/campaigns",
    request_body = CreateAdCampaignRequest,
    responses(
        (status = 201, description = "Campaña creada", body = AdCampaign),
        (status = 400, description = "Datos inválidos"),
        (status = 401, description = "No autorizado")
    ),
    tag = "Ads"
)]
pub async fn create_campaign(
    _auth: JwtMiddleware,
    State(state): State<AppState>,
    Json(payload): Json<CreateAdCampaignRequest>,
) -> Result<Response, Response> {
    if payload.title.trim().is_empty() {
        return Err((
            StatusCode::BAD_REQUEST,
            Json(json!({"error": "El título de la campaña es obligatorio"})),
        )
            .into_response());
    }

    if payload.ends_at <= payload.starts_at {
        return Err((
            StatusCode::BAD_REQUEST,
            Json(json!({"error": "ends_at debe ser posterior a starts_at"})),
        )
            .into_response());
    }

    let geohash = match (payload.latitude, payload.longitude) {
        (Some(lat), Some(lon)) => encode(Coord { x: lon, y: lat }, 9).ok(),
        _ => None,
    };

    let default_badge = if payload.category == "GOVERNMENT" {
        "Campaña Pública".to_string()
    } else {
        "Patrocinado".to_string()
    };

    let now = Utc::now().naive_utc();
    let campaign = AdCampaign {
        id: Uuid::new_v4().to_string(),
        advertiser_id: payload.advertiser_id,
        title: payload.title.trim().to_string(),
        description: payload.description,
        cta_label: payload.cta_label.unwrap_or_else(|| "Ver más".to_string()),
        target_url: payload.target_url,
        image_url: payload.image_url,
        image_public_id: payload.image_public_id,
        category: payload.category,
        placement: payload.placement,
        badge_label: payload.badge_label.unwrap_or(default_badge),
        latitude: payload.latitude,
        longitude: payload.longitude,
        geohash,
        radius_km: payload.radius_km,
        priority: payload.priority,
        starts_at: payload.starts_at,
        ends_at: payload.ends_at,
        is_active: payload.is_active,
        impressions_count: 0,
        clicks_count: 0,
        created_at: now,
        updated_at: now,
    };

    AdRepository::create_campaign(&state.pool, &campaign)
        .await
        .map_err(|e| {
            tracing::error!("Error creando campaña: {:?}", e);
            (
                StatusCode::INTERNAL_SERVER_ERROR,
                Json(json!({"error": e.to_string()})),
            )
                .into_response()
        })?;

    Ok((StatusCode::CREATED, Json(campaign)).into_response())
}

/// Sube una imagen/banner de campaña a Cloudinary (carpeta `paw/ads`).
#[utoipa::path(
    post,
    path = "/api/v1/ads/campaigns/upload-image",
    responses(
        (status = 201, description = "Imagen subida a Cloudinary", body = UploadAdImageResponse),
        (status = 400, description = "Archivo faltante o formato no permitido")
    ),
    tag = "Ads"
)]
pub async fn upload_ad_image(
    _auth: JwtMiddleware,
    State(state): State<AppState>,
    mut multipart: Multipart,
) -> Result<Response, Response> {
    while let Some(field) = multipart.next_field().await.map_err(|e| {
        (
            StatusCode::BAD_REQUEST,
            Json(json!({"error": format!("Error leyendo multipart: {}", e)})),
        )
            .into_response()
    })? {
        let file_name = field
            .file_name()
            .unwrap_or("ad_banner.jpg")
            .to_string();
        let data = field.bytes().await.map_err(|e| {
            (
                StatusCode::BAD_REQUEST,
                Json(json!({"error": format!("Error leyendo bytes: {}", e)})),
            )
                .into_response()
        })?;

        if data.is_empty() {
            continue;
        }

        // Validar que sea una imagen real mediante magic bytes
        let is_valid_image = infer::get(&data)
            .map(|k| k.matcher_type() == infer::MatcherType::Image)
            .unwrap_or(false);

        if !is_valid_image {
            return Err((
                StatusCode::BAD_REQUEST,
                Json(json!({"error": "El archivo debe ser una imagen válida (JPG, PNG, WebP)"})),
            )
                .into_response());
        }

        let uploaded = state
            .cloudinary_service
            .upload_media(data.to_vec(), &file_name, "paw/ads")
            .await
            .map_err(|e| {
                tracing::error!("Error subiendo creatividad a Cloudinary: {:?}", e);
                (
                    StatusCode::INTERNAL_SERVER_ERROR,
                    Json(json!({"error": "No se pudo subir la imagen a Cloudinary"})),
                )
                    .into_response()
            })?;

        return Ok((
            StatusCode::CREATED,
            Json(UploadAdImageResponse {
                image_url: uploaded.secure_url,
                image_public_id: uploaded.public_id,
            }),
        )
            .into_response());
    }

    Err((
        StatusCode::BAD_REQUEST,
        Json(json!({"error": "Debe adjuntar un archivo de imagen en el formulario multipart"})),
    )
        .into_response())
}

#[utoipa::path(
    put,
    path = "/api/v1/ads/campaigns/{id}",
    params(("id" = String, Path, description = "ID de la campaña")),
    request_body = UpdateAdCampaignRequest,
    responses(
        (status = 200, description = "Campaña actualizada", body = AdCampaign),
        (status = 400, description = "Datos inválidos"),
        (status = 404, description = "No encontrada")
    ),
    tag = "Ads"
)]
pub async fn update_campaign(
    _auth: JwtMiddleware,
    State(state): State<AppState>,
    Path(id): Path<String>,
    Json(payload): Json<UpdateAdCampaignRequest>,
) -> Result<Response, Response> {
    if payload.ends_at <= payload.starts_at {
        return Err((
            StatusCode::BAD_REQUEST,
            Json(json!({"error": "ends_at debe ser posterior a starts_at"})),
        )
            .into_response());
    }

    let existing = AdRepository::get_campaign_by_id(&state.pool, &id)
        .await
        .map_err(|e| {
            tracing::error!("Error buscando campaña: {:?}", e);
            (
                StatusCode::INTERNAL_SERVER_ERROR,
                Json(json!({"error": "Error interno"})),
            )
                .into_response()
        })?;

    let existing = match existing {
        Some(c) => c,
        None => return Ok(StatusCode::NOT_FOUND.into_response()),
    };

    let geohash = match (payload.latitude, payload.longitude) {
        (Some(lat), Some(lon)) => encode(Coord { x: lon, y: lat }, 9).ok(),
        _ => None,
    };

    let updated = AdCampaign {
        id: id.clone(),
        advertiser_id: existing.advertiser_id,
        title: payload.title.trim().to_string(),
        description: payload.description,
        cta_label: payload.cta_label.unwrap_or(existing.cta_label),
        target_url: payload.target_url,
        image_url: payload.image_url,
        image_public_id: payload.image_public_id,
        category: payload.category,
        placement: payload.placement,
        badge_label: payload.badge_label.unwrap_or(existing.badge_label),
        latitude: payload.latitude,
        longitude: payload.longitude,
        geohash,
        radius_km: payload.radius_km,
        priority: payload.priority,
        starts_at: payload.starts_at,
        ends_at: payload.ends_at,
        is_active: payload.is_active,
        impressions_count: existing.impressions_count,
        clicks_count: existing.clicks_count,
        created_at: existing.created_at,
        updated_at: Utc::now().naive_utc(),
    };

    AdRepository::update_campaign(&state.pool, &id, &updated)
        .await
        .map_err(|e| {
            tracing::error!("Error actualizando campaña: {:?}", e);
            (
                StatusCode::INTERNAL_SERVER_ERROR,
                Json(json!({"error": e.to_string()})),
            )
                .into_response()
        })?;

    Ok((StatusCode::OK, Json(updated)).into_response())
}

#[utoipa::path(
    delete,
    path = "/api/v1/ads/campaigns/{id}",
    params(("id" = String, Path, description = "ID de la campaña")),
    responses(
        (status = 200, description = "Campaña eliminada"),
        (status = 404, description = "No encontrada")
    ),
    tag = "Ads"
)]
pub async fn delete_campaign(
    _auth: JwtMiddleware,
    State(state): State<AppState>,
    Path(id): Path<String>,
) -> Result<Response, Response> {
    let existing = AdRepository::get_campaign_by_id(&state.pool, &id)
        .await
        .ok()
        .flatten();

    let rows = AdRepository::delete_campaign(&state.pool, &id)
        .await
        .map_err(|e| {
            tracing::error!("Error eliminando campaña: {:?}", e);
            (
                StatusCode::INTERNAL_SERVER_ERROR,
                Json(json!({"error": e.to_string()})),
            )
                .into_response()
        })?;

    if rows == 0 {
        return Ok(StatusCode::NOT_FOUND.into_response());
    }

    if let Some(campaign) = existing {
        if !campaign.image_public_id.is_empty() {
            if let Err(e) = state
                .cloudinary_service
                .delete_media(&campaign.image_public_id)
                .await
            {
                tracing::warn!(
                    "No se pudo eliminar la imagen '{}' de Cloudinary: {:?}",
                    campaign.image_public_id,
                    e
                );
            }
        }
    }

    Ok(StatusCode::OK.into_response())
}

// =============================================================================
// Helpers
// =============================================================================

fn build_geohash_prefixes(lat: f64, lon: f64, radius_km: f64) -> Vec<String> {
    // Precisión aproximada de celdas Geohash:
    // 6 ≈ ±0.6 km, 5 ≈ ±2.4 km, 4 ≈ ±20 km, 3 ≈ ±78 km
    let precision = if radius_km <= 1.5 {
        6
    } else if radius_km <= 8.0 {
        5
    } else if radius_km <= 35.0 {
        4
    } else {
        3
    };

    let Ok(center) = encode(Coord { x: lon, y: lat }, precision) else {
        return Vec::new();
    };

    let mut prefixes = vec![center.clone()];
    if let Ok(neighs) = neighbors(&center) {
        prefixes.extend([
            neighs.n, neighs.ne, neighs.e, neighs.se, neighs.s, neighs.sw, neighs.w, neighs.nw,
        ]);
    }

    prefixes.into_iter().map(|p| format!("{}%", p)).collect()
}

fn to_campaign_summary(
    c: AdCampaign,
    advertiser: Option<&Advertiser>,
) -> AdCampaignSummaryResponse {
    let ctr_percentage = if c.impressions_count > 0 {
        ((c.clicks_count as f64 / c.impressions_count as f64) * 10000.0).round() / 100.0
    } else {
        0.0
    };

    AdCampaignSummaryResponse {
        id: c.id,
        advertiser_id: c.advertiser_id,
        advertiser_name: advertiser
            .map(|a| a.name.clone())
            .unwrap_or_else(|| "Desconocido".to_string()),
        is_government: advertiser.map(|a| a.is_government).unwrap_or(false),
        title: c.title,
        description: c.description,
        cta_label: c.cta_label,
        target_url: c.target_url,
        image_url: c.image_url,
        image_public_id: c.image_public_id,
        category: c.category,
        placement: c.placement,
        badge_label: c.badge_label,
        latitude: c.latitude,
        longitude: c.longitude,
        geohash: c.geohash,
        radius_km: c.radius_km,
        priority: c.priority,
        starts_at: c.starts_at,
        ends_at: c.ends_at,
        is_active: c.is_active,
        impressions_count: c.impressions_count,
        clicks_count: c.clicks_count,
        ctr_percentage,
        created_at: c.created_at,
        updated_at: c.updated_at,
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_geohash_prefixes_for_ads_radius() {
        let prefixes_local = build_geohash_prefixes(19.4326, -99.1332, 5.0);
        assert_eq!(prefixes_local.len(), 9);
        assert_eq!(prefixes_local[0].len(), 6); // 5 chars + '%'

        let prefixes_city = build_geohash_prefixes(19.4326, -99.1332, 20.0);
        assert_eq!(prefixes_city.len(), 9);
        assert_eq!(prefixes_city[0].len(), 5); // 4 chars + '%'
    }
}
