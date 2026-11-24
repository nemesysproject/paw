use axum::{
    extract::{Path, State},
    http::StatusCode,
    response::{IntoResponse, Response},
    Json,
};
use chrono::Utc;
use geohash::{encode, Coord};
use serde_json::json;
use uuid::Uuid;

use crate::infrastructure::auth::JwtMiddleware;
use crate::models::entities::{
    AdoptionRequest, ContentReport, Conversation, ConversationParticipant, DeviceToken, Message,
    PetSighting, UserBlock,
};
use crate::models::social::{
    AdoptionRequestResponse, BlockUserRequest, ConversationSummaryResponse, CreateAdoptionRequest,
    CreateContentReportRequest, CreateConversationRequest, CreateSightingRequest, MessageResponse,
    PetSightingResponse, RegisterDeviceTokenRequest, SendMessageRequest,
    UpdateAdoptionStatusRequest,
};
use crate::repositories::social_repo::SocialRepository;
use crate::AppState;

// =============================================================================
// 1. Avistamientos Colaborativos (/api/v1/sightings)
// =============================================================================

/// Lista los avistamientos reportados para una mascota.
#[utoipa::path(
    get,
    path = "/api/v1/sightings/pet/{pet_id}",
    params(("pet_id" = String, Path, description = "ID de la mascota")),
    responses(
        (status = 200, description = "Lista de avistamientos", body = [PetSightingResponse]),
        (status = 500, description = "Error interno")
    ),
    tag = "Sightings"
)]
pub async fn list_sightings_by_pet(
    State(state): State<AppState>,
    Path(pet_id): Path<String>,
) -> Result<Response, Response> {
    let sightings = SocialRepository::list_sightings_by_pet(&state.pool, &pet_id)
        .await
        .map_err(|e| internal_error("Error listando avistamientos", e))?;
    Ok((StatusCode::OK, Json(sightings)).into_response())
}

/// Reporta un nuevo avistamiento ("¡Vi a tu mascota aquí!") con ubicación GPS.
#[utoipa::path(
    post,
    path = "/api/v1/sightings",
    request_body = CreateSightingRequest,
    responses(
        (status = 201, description = "Avistamiento registrado", body = PetSighting),
        (status = 401, description = "No autorizado")
    ),
    tag = "Sightings"
)]
pub async fn create_sighting(
    JwtMiddleware(claims): JwtMiddleware,
    State(state): State<AppState>,
    Json(payload): Json<CreateSightingRequest>,
) -> Result<Response, Response> {
    let now = Utc::now().naive_utc();
    let geohash = encode(
        Coord {
            x: payload.longitude,
            y: payload.latitude,
        },
        9,
    )
    .ok();

    let sighting = PetSighting {
        id: Uuid::new_v4().to_string(),
        pet_id: payload.pet_id,
        reporter_id: claims.sub,
        notes: payload.notes,
        latitude: payload.latitude,
        longitude: payload.longitude,
        geohash,
        image_url: payload.image_url,
        image_public_id: payload.image_public_id,
        sighted_at: now,
        is_confirmed_by_owner: false,
        created_at: now,
    };

    SocialRepository::create_sighting(&state.pool, &sighting)
        .await
        .map_err(|e| internal_error("Error creando avistamiento", e))?;

    Ok((StatusCode::CREATED, Json(sighting)).into_response())
}

/// Confirma un avistamiento por parte del dueño de la mascota.
#[utoipa::path(
    patch,
    path = "/api/v1/sightings/{id}/confirm",
    params(("id" = String, Path, description = "ID del avistamiento")),
    responses(
        (status = 200, description = "Avistamiento confirmado"),
        (status = 404, description = "No encontrado")
    ),
    tag = "Sightings"
)]
pub async fn confirm_sighting(
    _auth: JwtMiddleware,
    State(state): State<AppState>,
    Path(id): Path<String>,
) -> Result<Response, Response> {
    let rows = SocialRepository::confirm_sighting(&state.pool, &id)
        .await
        .map_err(|e| internal_error("Error confirmando avistamiento", e))?;

    if rows == 0 {
        Ok(StatusCode::NOT_FOUND.into_response())
    } else {
        Ok((StatusCode::OK, Json(json!({"confirmed": true}))).into_response())
    }
}

// =============================================================================
// 2. Conversaciones y Mensajería (/api/v1/conversations)
// =============================================================================

/// Lista las conversaciones activas del usuario autenticado.
#[utoipa::path(
    get,
    path = "/api/v1/conversations",
    responses(
        (status = 200, description = "Lista de conversaciones", body = [ConversationSummaryResponse]),
        (status = 401, description = "No autorizado")
    ),
    tag = "Conversations"
)]
pub async fn list_conversations(
    JwtMiddleware(claims): JwtMiddleware,
    State(state): State<AppState>,
) -> Result<Response, Response> {
    let list = SocialRepository::list_conversations_for_user(&state.pool, &claims.sub)
        .await
        .map_err(|e| internal_error("Error listando conversaciones", e))?;
    Ok((StatusCode::OK, Json(list)).into_response())
}

/// Inicia una conversación contextual (avistamiento, adopción, refugio o veterinaria).
#[utoipa::path(
    post,
    path = "/api/v1/conversations",
    request_body = CreateConversationRequest,
    responses(
        (status = 201, description = "Conversación creada", body = Conversation),
        (status = 400, description = "Tipo de conversación inválido"),
        (status = 401, description = "No autorizado")
    ),
    tag = "Conversations"
)]
pub async fn create_conversation(
    JwtMiddleware(claims): JwtMiddleware,
    State(state): State<AppState>,
    Json(payload): Json<CreateConversationRequest>,
) -> Result<Response, Response> {
    let conv_type = payload.r#type.trim().to_uppercase();
    if !matches!(
        conv_type.as_str(),
        "PET_SIGHTING" | "ADOPTION_INQUIRY" | "SHELTER_SUPPORT" | "VET_CONSULT"
    ) {
        return Err((
            StatusCode::BAD_REQUEST,
            Json(json!({"error": "type debe ser PET_SIGHTING, ADOPTION_INQUIRY, SHELTER_SUPPORT o VET_CONSULT"})),
        )
            .into_response());
    }

    let now = Utc::now().naive_utc();
    let conv_id = Uuid::new_v4().to_string();

    // Resolver destinatario si no vino explícito pero hay pet_id
    let mut target_user_id = payload.target_user_id.clone();
    if target_user_id.is_none() {
        if let Some(ref pet_id) = payload.pet_id {
            target_user_id = SocialRepository::find_pet_owner(&state.pool, pet_id)
                .await
                .ok()
                .flatten();
        }
    }

    let initial_msg = payload
        .initial_message
        .as_deref()
        .map(str::trim)
        .filter(|s| !s.is_empty())
        .map(|text| Message {
            id: Uuid::new_v4().to_string(),
            conversation_id: conv_id.clone(),
            sender_id: claims.sub.clone(),
            r#type: "TEXT".to_string(),
            content: Some(text.to_string()),
            media_url: None,
            media_public_id: None,
            latitude: None,
            longitude: None,
            geohash: None,
            location_label: None,
            created_at: now,
            deleted_at: None,
        });

    let conversation = Conversation {
        id: conv_id.clone(),
        r#type: conv_type,
        status: "OPEN".to_string(),
        pet_id: payload.pet_id,
        shelter_id: payload.shelter_id,
        veterinary_id: payload.veterinary_id,
        last_message_at: initial_msg.as_ref().map(|_| now),
        created_at: now,
        updated_at: now,
    };

    let mut participants = vec![ConversationParticipant {
        id: Uuid::new_v4().to_string(),
        conversation_id: conv_id.clone(),
        user_id: claims.sub.clone(),
        last_read_at: Some(now),
        is_muted: false,
        joined_at: now,
    }];

    if let Some(target_id) = target_user_id {
        if target_id != claims.sub {
            participants.push(ConversationParticipant {
                id: Uuid::new_v4().to_string(),
                conversation_id: conv_id.clone(),
                user_id: target_id,
                last_read_at: None,
                is_muted: false,
                joined_at: now,
            });
        }
    }

    SocialRepository::create_conversation_with_participants(
        &state.pool,
        &conversation,
        &participants,
        initial_msg.as_ref(),
    )
    .await
    .map_err(|e| internal_error("Error creando conversación", e))?;

    Ok((StatusCode::CREATED, Json(conversation)).into_response())
}

/// Lista los mensajes de una conversación y la marca como leída.
#[utoipa::path(
    get,
    path = "/api/v1/conversations/{id}/messages",
    params(("id" = String, Path, description = "ID de la conversación")),
    responses(
        (status = 200, description = "Mensajes de la conversación", body = [MessageResponse]),
        (status = 403, description = "No eres participante de esta conversación")
    ),
    tag = "Conversations"
)]
pub async fn list_messages(
    JwtMiddleware(claims): JwtMiddleware,
    State(state): State<AppState>,
    Path(conversation_id): Path<String>,
) -> Result<Response, Response> {
    let allowed = SocialRepository::is_participant(&state.pool, &conversation_id, &claims.sub)
        .await
        .map_err(|e| internal_error("Error verificando participante", e))?;

    if !allowed && claims.role != "ADMIN" {
        return Err((
            StatusCode::FORBIDDEN,
            Json(json!({"error": "No tienes acceso a esta conversación"})),
        )
            .into_response());
    }

    let messages = SocialRepository::list_messages(&state.pool, &conversation_id)
        .await
        .map_err(|e| internal_error("Error listando mensajes", e))?;

    let _ = SocialRepository::mark_conversation_read(
        &state.pool,
        &conversation_id,
        &claims.sub,
        Utc::now().naive_utc(),
    )
    .await;

    Ok((StatusCode::OK, Json(messages)).into_response())
}

/// Envía un mensaje (texto, imagen o ubicación GPS) en una conversación.
#[utoipa::path(
    post,
    path = "/api/v1/conversations/{id}/messages",
    params(("id" = String, Path, description = "ID de la conversación")),
    request_body = SendMessageRequest,
    responses(
        (status = 201, description = "Mensaje enviado", body = Message),
        (status = 400, description = "Mensaje vacío o inválido"),
        (status = 403, description = "No eres participante")
    ),
    tag = "Conversations"
)]
pub async fn send_message(
    JwtMiddleware(claims): JwtMiddleware,
    State(state): State<AppState>,
    Path(conversation_id): Path<String>,
    Json(payload): Json<SendMessageRequest>,
) -> Result<Response, Response> {
    let allowed = SocialRepository::is_participant(&state.pool, &conversation_id, &claims.sub)
        .await
        .map_err(|e| internal_error("Error verificando participante", e))?;

    if !allowed && claims.role != "ADMIN" {
        return Err((
            StatusCode::FORBIDDEN,
            Json(json!({"error": "No tienes permiso para escribir en esta conversación"})),
        )
            .into_response());
    }

    let msg_type = payload
        .r#type
        .as_deref()
        .unwrap_or("TEXT")
        .trim()
        .to_uppercase();

    if msg_type == "TEXT"
        && payload
            .content
            .as_deref()
            .map(|c| c.trim().is_empty())
            .unwrap_or(true)
    {
        return Err((
            StatusCode::BAD_REQUEST,
            Json(json!({"error": "El contenido del mensaje es obligatorio"})),
        )
            .into_response());
    }

    if msg_type == "LOCATION" && (payload.latitude.is_none() || payload.longitude.is_none()) {
        return Err((
            StatusCode::BAD_REQUEST,
            Json(json!({"error": "Un mensaje de tipo LOCATION requiere latitude y longitude"})),
        )
            .into_response());
    }

    let geohash = match (payload.latitude, payload.longitude) {
        (Some(lat), Some(lon)) => encode(Coord { x: lon, y: lat }, 9).ok(),
        _ => None,
    };

    let message = Message {
        id: Uuid::new_v4().to_string(),
        conversation_id,
        sender_id: claims.sub,
        r#type: msg_type,
        content: payload.content,
        media_url: payload.media_url,
        media_public_id: payload.media_public_id,
        latitude: payload.latitude,
        longitude: payload.longitude,
        geohash,
        location_label: payload.location_label,
        created_at: Utc::now().naive_utc(),
        deleted_at: None,
    };

    SocialRepository::send_message(&state.pool, &message)
        .await
        .map_err(|e| internal_error("Error enviando mensaje", e))?;

    Ok((StatusCode::CREATED, Json(message)).into_response())
}

// =============================================================================
// 3. Solicitudes de Adopción (/api/v1/adoptions)
// =============================================================================

/// Lista las solicitudes de adopción enviadas o recibidas por el usuario autenticado.
#[utoipa::path(
    get,
    path = "/api/v1/adoptions",
    responses(
        (status = 200, description = "Lista de solicitudes de adopción", body = [AdoptionRequestResponse]),
        (status = 401, description = "No autorizado")
    ),
    tag = "Adoptions"
)]
pub async fn list_adoptions(
    JwtMiddleware(claims): JwtMiddleware,
    State(state): State<AppState>,
) -> Result<Response, Response> {
    let items = SocialRepository::list_adoptions_for_user(&state.pool, &claims.sub)
        .await
        .map_err(|e| internal_error("Error listando adopciones", e))?;
    Ok((StatusCode::OK, Json(items)).into_response())
}

/// Crea una solicitud formal de adopción y abre automáticamente su conversación vinculada.
#[utoipa::path(
    post,
    path = "/api/v1/adoptions",
    request_body = CreateAdoptionRequest,
    responses(
        (status = 201, description = "Solicitud de adopción creada", body = AdoptionRequest),
        (status = 404, description = "Mascota no encontrada")
    ),
    tag = "Adoptions"
)]
pub async fn create_adoption(
    JwtMiddleware(claims): JwtMiddleware,
    State(state): State<AppState>,
    Json(payload): Json<CreateAdoptionRequest>,
) -> Result<Response, Response> {
    let owner_id = match SocialRepository::find_pet_owner(&state.pool, &payload.pet_id).await {
        Ok(Some(id)) => id,
        Ok(None) => {
            return Err((
                StatusCode::NOT_FOUND,
                Json(json!({"error": "Mascota no encontrada"})),
            )
                .into_response())
        }
        Err(e) => return Err(internal_error("Error buscando dueño de mascota", e)),
    };

    let now = Utc::now().naive_utc();
    let conv_id = Uuid::new_v4().to_string();

    let conversation = Conversation {
        id: conv_id.clone(),
        r#type: "ADOPTION_INQUIRY".to_string(),
        status: "OPEN".to_string(),
        pet_id: Some(payload.pet_id.clone()),
        shelter_id: payload.shelter_id.clone(),
        veterinary_id: None,
        last_message_at: payload.notes.as_ref().map(|_| now),
        created_at: now,
        updated_at: now,
    };

    let mut participants = vec![ConversationParticipant {
        id: Uuid::new_v4().to_string(),
        conversation_id: conv_id.clone(),
        user_id: claims.sub.clone(),
        last_read_at: Some(now),
        is_muted: false,
        joined_at: now,
    }];
    if owner_id != claims.sub {
        participants.push(ConversationParticipant {
            id: Uuid::new_v4().to_string(),
            conversation_id: conv_id.clone(),
            user_id: owner_id.clone(),
            last_read_at: None,
            is_muted: false,
            joined_at: now,
        });
    }

    let initial_msg = payload
        .notes
        .as_deref()
        .map(str::trim)
        .filter(|s| !s.is_empty())
        .map(|text| Message {
            id: Uuid::new_v4().to_string(),
            conversation_id: conv_id.clone(),
            sender_id: claims.sub.clone(),
            r#type: "TEXT".to_string(),
            content: Some(text.to_string()),
            media_url: None,
            media_public_id: None,
            latitude: None,
            longitude: None,
            geohash: None,
            location_label: None,
            created_at: now,
            deleted_at: None,
        });

    SocialRepository::create_conversation_with_participants(
        &state.pool,
        &conversation,
        &participants,
        initial_msg.as_ref(),
    )
    .await
    .map_err(|e| internal_error("Error creando conversación de adopción", e))?;

    let adoption = AdoptionRequest {
        id: Uuid::new_v4().to_string(),
        pet_id: payload.pet_id,
        applicant_id: claims.sub,
        owner_id,
        shelter_id: payload.shelter_id,
        conversation_id: Some(conv_id),
        status: "PENDING".to_string(),
        notes: payload.notes,
        created_at: now,
        updated_at: now,
    };

    SocialRepository::create_adoption_request(&state.pool, &adoption)
        .await
        .map_err(|e| internal_error("Error creando solicitud de adopción", e))?;

    Ok((StatusCode::CREATED, Json(adoption)).into_response())
}

/// Actualiza el estado de una solicitud de adopción (PENDING, IN_REVIEW, APPROVED, REJECTED, CANCELLED).
#[utoipa::path(
    patch,
    path = "/api/v1/adoptions/{id}/status",
    params(("id" = String, Path, description = "ID de la solicitud de adopción")),
    request_body = UpdateAdoptionStatusRequest,
    responses(
        (status = 200, description = "Estado actualizado"),
        (status = 400, description = "Estado inválido"),
        (status = 404, description = "Solicitud no encontrada")
    ),
    tag = "Adoptions"
)]
pub async fn update_adoption_status(
    _auth: JwtMiddleware,
    State(state): State<AppState>,
    Path(id): Path<String>,
    Json(payload): Json<UpdateAdoptionStatusRequest>,
) -> Result<Response, Response> {
    let status = payload.status.trim().to_uppercase();
    if !matches!(
        status.as_str(),
        "PENDING" | "IN_REVIEW" | "APPROVED" | "REJECTED" | "CANCELLED"
    ) {
        return Err((
            StatusCode::BAD_REQUEST,
            Json(json!({"error": "Estado inválido"})),
        )
            .into_response());
    }

    let rows = SocialRepository::update_adoption_status(
        &state.pool,
        &id,
        &status,
        Utc::now().naive_utc(),
    )
    .await
    .map_err(|e| internal_error("Error actualizando estado de adopción", e))?;

    if rows == 0 {
        Ok(StatusCode::NOT_FOUND.into_response())
    } else {
        Ok((StatusCode::OK, Json(json!({"status": status}))).into_response())
    }
}

// =============================================================================
// 4. Push Tokens (FCM) y Moderación UGC (/api/v1/social)
// =============================================================================

/// Registra o actualiza el token FCM del dispositivo para notificaciones push.
#[utoipa::path(
    post,
    path = "/api/v1/social/device-tokens",
    request_body = RegisterDeviceTokenRequest,
    responses(
        (status = 200, description = "Token registrado", body = DeviceToken),
        (status = 400, description = "Token vacío")
    ),
    tag = "Moderation & Push"
)]
pub async fn register_device_token(
    JwtMiddleware(claims): JwtMiddleware,
    State(state): State<AppState>,
    Json(payload): Json<RegisterDeviceTokenRequest>,
) -> Result<Response, Response> {
    if payload.fcm_token.trim().is_empty() {
        return Err((
            StatusCode::BAD_REQUEST,
            Json(json!({"error": "fcm_token es obligatorio"})),
        )
            .into_response());
    }

    let now = Utc::now().naive_utc();
    let token = DeviceToken {
        id: Uuid::new_v4().to_string(),
        user_id: claims.sub,
        fcm_token: payload.fcm_token.trim().to_string(),
        platform: payload
            .platform
            .unwrap_or_else(|| "ANDROID".to_string())
            .to_uppercase(),
        created_at: now,
        updated_at: now,
    };

    SocialRepository::upsert_device_token(&state.pool, &token)
        .await
        .map_err(|e| internal_error("Error guardando device token", e))?;

    Ok((StatusCode::OK, Json(token)).into_response())
}

/// Bloquea a un usuario (política UGC de Google Play).
#[utoipa::path(
    post,
    path = "/api/v1/social/blocks",
    request_body = BlockUserRequest,
    responses(
        (status = 201, description = "Usuario bloqueado", body = UserBlock),
        (status = 400, description = "No puedes bloquearte a ti mismo")
    ),
    tag = "Moderation & Push"
)]
pub async fn block_user(
    JwtMiddleware(claims): JwtMiddleware,
    State(state): State<AppState>,
    Json(payload): Json<BlockUserRequest>,
) -> Result<Response, Response> {
    if payload.blocked_user_id == claims.sub {
        return Err((
            StatusCode::BAD_REQUEST,
            Json(json!({"error": "No puedes bloquearte a ti mismo"})),
        )
            .into_response());
    }

    let block = UserBlock {
        id: Uuid::new_v4().to_string(),
        blocker_id: claims.sub,
        blocked_user_id: payload.blocked_user_id,
        reason: payload.reason,
        created_at: Utc::now().naive_utc(),
    };

    SocialRepository::create_user_block(&state.pool, &block)
        .await
        .map_err(|e| internal_error("Error bloqueando usuario", e))?;

    Ok((StatusCode::CREATED, Json(block)).into_response())
}

/// Reporta contenido o usuario inapropiado (política UGC de Google Play).
#[utoipa::path(
    post,
    path = "/api/v1/social/reports",
    request_body = CreateContentReportRequest,
    responses(
        (status = 201, description = "Reporte registrado", body = ContentReport),
        (status = 400, description = "Datos inválidos")
    ),
    tag = "Moderation & Push"
)]
pub async fn create_content_report(
    JwtMiddleware(claims): JwtMiddleware,
    State(state): State<AppState>,
    Json(payload): Json<CreateContentReportRequest>,
) -> Result<Response, Response> {
    let target_type = payload.target_type.trim().to_uppercase();
    if !matches!(target_type.as_str(), "USER" | "MESSAGE" | "SIGHTING" | "PET") {
        return Err((
            StatusCode::BAD_REQUEST,
            Json(json!({"error": "target_type debe ser USER, MESSAGE, SIGHTING o PET"})),
        )
            .into_response());
    }
    if payload.reason.trim().is_empty() {
        return Err((
            StatusCode::BAD_REQUEST,
            Json(json!({"error": "reason es obligatorio"})),
        )
            .into_response());
    }

    let now = Utc::now().naive_utc();
    let report = ContentReport {
        id: Uuid::new_v4().to_string(),
        reporter_id: claims.sub,
        target_type,
        target_id: payload.target_id,
        reason: payload.reason.trim().to_string(),
        status: "OPEN".to_string(),
        created_at: now,
        updated_at: now,
    };

    SocialRepository::create_content_report(&state.pool, &report)
        .await
        .map_err(|e| internal_error("Error registrando reporte", e))?;

    Ok((StatusCode::CREATED, Json(report)).into_response())
}

fn internal_error(ctx: &str, err: sqlx::Error) -> Response {
    tracing::error!("{}: {:?}", ctx, err);
    (
        StatusCode::INTERNAL_SERVER_ERROR,
        Json(json!({"error": ctx})),
    )
        .into_response()
}
