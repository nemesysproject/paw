use axum::{
    extract::State,
    http::StatusCode,
    response::{IntoResponse, Response},
    Json,
};
use serde_json::{json, Value};
use sqlx::Row;

use crate::infrastructure::auth::JwtMiddleware;
use crate::infrastructure::services::gemini::GeminiService;
use crate::models::ai::{AiChatRequest, AiChatResponse, AiNavigationAction};
use crate::repositories::ad_repo::AdRepository;
use crate::repositories::dashboard_repo::DashboardRepository;
use crate::AppState;

/// Copiloto IA del panel administrativo web (consultas en lenguaje natural + navegación asistida).
#[utoipa::path(
    post,
    path = "/api/v1/ai/chat",
    request_body = AiChatRequest,
    responses(
        (status = 200, description = "Respuesta del copiloto IA", body = AiChatResponse),
        (status = 400, description = "Mensaje vacío"),
        (status = 401, description = "No autorizado")
    ),
    tag = "AI"
)]
pub async fn chat_assistant(
    _auth: JwtMiddleware,
    State(state): State<AppState>,
    Json(payload): Json<AiChatRequest>,
) -> Result<Response, Response> {
    let message = payload.message.trim();
    if message.is_empty() {
        return Err((
            StatusCode::BAD_REQUEST,
            Json(json!({"error": "El mensaje no puede estar vacío"})),
        )
            .into_response());
    }

    let context = gather_live_context(&state).await;
    let gemini = GeminiService::from_env();

    if gemini.is_configured() {
        match gemini
            .chat_with_context(&context, &payload.history, message)
            .await
        {
            Ok((reply, actions)) => {
                let resp = AiChatResponse {
                    reply,
                    actions,
                    provider: "gemini".to_string(),
                    model: gemini.model_name().to_string(),
                };
                return Ok((StatusCode::OK, Json(resp)).into_response());
            }
            Err(e) => {
                tracing::warn!("Gemini falló ({}), usando respuesta local de respaldo", e);
            }
        }
    }

    // Respaldo determinístico local (permite probar el flujo PoC incluso sin GEMINI_API_KEY)
    let (reply, actions) = build_local_fallback(message, &context);
    let resp = AiChatResponse {
        reply,
        actions,
        provider: "local_fallback".to_string(),
        model: "paw-local-summary".to_string(),
    };
    Ok((StatusCode::OK, Json(resp)).into_response())
}

async fn gather_live_context(state: &AppState) -> Value {
    let stats = DashboardRepository::get_stats(&state.pool).await.ok();

    let recent_pets: Vec<Value> = sqlx::query(
        r#"
        SELECT p.id, p.name, p.status, p.gender, p.description, p."lastGeohash" AS geohash,
               s.name AS species_name
        FROM "Pet" p
        LEFT JOIN "Species" s ON s.id = p."speciesId"
        ORDER BY p."createdAt" DESC
        LIMIT 25
        "#,
    )
    .fetch_all(&state.pool)
    .await
    .unwrap_or_default()
    .into_iter()
    .map(|row| {
        json!({
            "id": row.try_get::<String, _>("id").unwrap_or_default(),
            "name": row.try_get::<Option<String>, _>("name").ok().flatten().unwrap_or_else(|| "Sin nombre".to_string()),
            "status": row.try_get::<String, _>("status").unwrap_or_default(),
            "gender": row.try_get::<String, _>("gender").unwrap_or_default(),
            "species": row.try_get::<Option<String>, _>("species_name").ok().flatten().unwrap_or_default(),
            "description": row.try_get::<Option<String>, _>("description").ok().flatten().unwrap_or_default(),
            "geohash": row.try_get::<Option<String>, _>("geohash").ok().flatten()
        })
    })
    .collect();

    let shelters: Vec<Value> = sqlx::query(
        r#"SELECT id, name, address, phone, email FROM "Shelter" ORDER BY "createdAt" DESC LIMIT 20"#,
    )
    .fetch_all(&state.pool)
    .await
    .unwrap_or_default()
    .into_iter()
    .map(|row| {
        json!({
            "id": row.try_get::<String, _>("id").unwrap_or_default(),
            "name": row.try_get::<String, _>("name").unwrap_or_default(),
            "address": row.try_get::<String, _>("address").unwrap_or_default(),
            "phone": row.try_get::<Option<String>, _>("phone").ok().flatten(),
            "email": row.try_get::<Option<String>, _>("email").ok().flatten()
        })
    })
    .collect();

    let veterinaries: Vec<Value> = sqlx::query(
        r#"
        SELECT id, name, address, phone, whatsapp, "isVerified" AS is_verified, "isFeatured" AS is_featured
        FROM "Veterinary"
        ORDER BY "isFeatured" DESC, "createdAt" DESC
        LIMIT 20
        "#,
    )
    .fetch_all(&state.pool)
    .await
    .unwrap_or_default()
    .into_iter()
    .map(|row| {
        json!({
            "id": row.try_get::<String, _>("id").unwrap_or_default(),
            "name": row.try_get::<String, _>("name").unwrap_or_default(),
            "address": row.try_get::<String, _>("address").unwrap_or_default(),
            "phone": row.try_get::<Option<String>, _>("phone").ok().flatten(),
            "whatsapp": row.try_get::<Option<String>, _>("whatsapp").ok().flatten(),
            "is_verified": row.try_get::<bool, _>("is_verified").unwrap_or(false),
            "is_featured": row.try_get::<bool, _>("is_featured").unwrap_or(false)
        })
    })
    .collect();

    let campaigns: Vec<Value> = AdRepository::list_campaigns(&state.pool)
        .await
        .unwrap_or_default()
        .into_iter()
        .take(20)
        .map(|c| {
            let ctr = if c.impressions_count > 0 {
                ((c.clicks_count as f64 / c.impressions_count as f64) * 10000.0).round() / 100.0
            } else {
                0.0
            };
            json!({
                "id": c.id,
                "title": c.title,
                "category": c.category,
                "placement": c.placement,
                "is_active": c.is_active,
                "impressions": c.impressions_count,
                "clicks": c.clicks_count,
                "ctr_percentage": ctr
            })
        })
        .collect();

    json!({
        "dashboard_stats": stats,
        "recent_pets": recent_pets,
        "shelters": shelters,
        "veterinaries": veterinaries,
        "ad_campaigns": campaigns
    })
}

fn build_local_fallback(message: &str, context: &Value) -> (String, Vec<AiNavigationAction>) {
    let lower = message.to_lowercase();
    let total_pets = context.pointer("/dashboard_stats/total_pets").and_then(|v| v.as_i64()).unwrap_or(0);
    let total_lost = context.pointer("/dashboard_stats/total_lost").and_then(|v| v.as_i64()).unwrap_or(0);
    let total_at_risk = context.pointer("/dashboard_stats/total_at_risk").and_then(|v| v.as_i64()).unwrap_or(0);
    let total_adoption = context.pointer("/dashboard_stats/total_adoption").and_then(|v| v.as_i64()).unwrap_or(0);
    let shelters_count = context.get("shelters").and_then(|v| v.as_array()).map(|a| a.len()).unwrap_or(0);
    let vets_count = context.get("veterinaries").and_then(|v| v.as_array()).map(|a| a.len()).unwrap_or(0);
    let campaigns_count = context.get("ad_campaigns").and_then(|v| v.as_array()).map(|a| a.len()).unwrap_or(0);

    if lower.contains("campaña") || lower.contains("anuncio") || lower.contains("publi") || lower.contains("ctr") {
        (
            format!(
                "Actualmente tienes {} campaña(s) registrada(s) en el módulo de publicidad. Puedes crear campañas geolocalizadas, subir banners a Cloudinary y consultar impresiones/CTR directamente en Campañas y Ads.\n\n(Modo local activo: configura GEMINI_API_KEY en el backend para habilitar respuestas conversacionales con Gemini).",
                campaigns_count
            ),
            vec![AiNavigationAction {
                label: "Ir a Campañas y Ads".to_string(),
                route: "/campaigns".to_string(),
            }],
        )
    } else if lower.contains("refugio") || lower.contains("veterinar") {
        (
            format!(
                "Resumen actual de aliados: {} refugio(s) y {} veterinaria(s) registrada(s) en PostgreSQL.\n\n(Modo local activo: configura GEMINI_API_KEY en el backend para consultas avanzadas con Gemini).",
                shelters_count, vets_count
            ),
            vec![
                AiNavigationAction {
                    label: "Ver Refugios".to_string(),
                    route: "/shelters".to_string(),
                },
                AiNavigationAction {
                    label: "Gestionar Anunciantes".to_string(),
                    route: "/campaigns".to_string(),
                },
            ],
        )
    } else {
        (
            format!(
                "Resumen en vivo de PAW:\n• Total mascotas: {}\n• Perdidas: {} | En riesgo: {} | En adopción: {}\n• Refugios: {} | Veterinarias: {} | Campañas: {}\n\n(Modo local activo",
                total_pets, total_lost, total_at_risk, total_adoption, shelters_count, vets_count, campaigns_count
            ),
            vec![
                AiNavigationAction {
                    label: "Ver Dashboard".to_string(),
                    route: "/dashboard".to_string(),
                },
                AiNavigationAction {
                    label: "Ver Mascotas".to_string(),
                    route: "/pets".to_string(),
                },
                AiNavigationAction {
                    label: "Ir a Campañas".to_string(),
                    route: "/campaigns".to_string(),
                },
            ],
        )
    }
}
