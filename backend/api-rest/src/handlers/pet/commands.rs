use axum::{
    extract::{Multipart, State, Path},
    http::StatusCode,
    response::{IntoResponse, Response},
    Json,
};
use serde_json::json;
use crate::AppState;
use crate::models::pet::commands::{CreatePetCommand, UpdatePetCommand};
use crate::models::entities::{Pet, Media};
use crate::models::events::PetEvent;
use crate::repositories::pet_repo::PetRepository;
use crate::repositories::media_repo::MediaRepository;
use uuid::Uuid;
use geohash::{encode, Coord};
use chrono::Utc;
use crate::infrastructure::auth::JwtMiddleware;


/// Registra una nueva mascota con fotos/videos.
#[utoipa::path(
    post,
    path = "/api/v1/pets",
    request_body(content = CreatePetCommand, content_type = "multipart/form-data"),
    responses(
        (status = 201, description = "Mascota creada exitosamente", body = String),
        (status = 400, description = "Datos inválidos o archivos peligrosos"),
        (status = 500, description = "Error interno del servidor")
    ),
    tag = "Pets"
)]
pub async fn create_pet(
    _auth: JwtMiddleware,
    State(state): State<AppState>,
    mut multipart: Multipart,
) -> Result<Response, Response> {
    let mut command_map = serde_json::Map::new();
    let mut files: Vec<(Vec<u8>, String)> = Vec::new();

    while let Ok(Some(field)) = multipart.next_field().await {
        let name = field.name().unwrap_or_default().to_string();
        if name == "file" {
            let file_name = field.file_name().unwrap_or("image.jpg").to_string();
            let data = field.bytes().await.unwrap_or_default().to_vec();
            files.push((data, file_name));
        } else {
            let text = field.text().await.unwrap_or_default();
            if !text.is_empty() && text != "null" {
                // Solo intentamos parsear como número si el campo es de tipo numérico conocido.
                // Esto evita que IDs como "1" sean convertidos a integer y fallen al mapear a String.
                let val = if name == "last_latitude" || name == "last_longitude" {
                    text.parse::<f64>().map(|n| json!(n)).unwrap_or_else(|_| json!(text))
                } else if text.starts_with('{') || text.starts_with('[') {
                    serde_json::from_str::<serde_json::Value>(&text).unwrap_or_else(|_| json!(text))
                } else {
                    json!(text)
                };
                
                command_map.insert(name, val);
            }
        }
    }

    let command: CreatePetCommand = match serde_json::from_value(serde_json::Value::Object(command_map.clone())) {
        Ok(c) => c,
        Err(e) => {
            tracing::error!("Error parseando datos de mascota: {}. Datos recibidos: {:?}", e, command_map);
            return Err((StatusCode::BAD_REQUEST, Json(json!({"error": format!("Datos de mascota inválidos: {}", e)}))).into_response());
        }
    };

    if files.is_empty() {
        tracing::warn!("Rechazando creación de mascota: No se enviaron archivos.");
        return Err((StatusCode::BAD_REQUEST, Json(json!({"error": "Se requiere al menos una imagen"}))).into_response());
    }

    for (data, _) in &files {
        if infer::get(data).map_or(true, |k| k.matcher_type() != infer::MatcherType::Image && k.matcher_type() != infer::MatcherType::Video) {
            tracing::warn!("Rechazando creación de mascota: Tipo de archivo no permitido.");
            return Err((StatusCode::BAD_REQUEST, Json(json!({"error": "Archivo inválido"}))).into_response());
        }
    }

    let geohash = if let (Some(lat), Some(lon)) = (command.last_latitude, command.last_longitude) {
        encode(Coord { x: lon, y: lat }, 10).ok()
    } else {
        None
    };

    let mut media_results = Vec::new();
    for (data, file_name) in files {
        match state.cloudinary_service.upload_media(data, &file_name, "pets").await {
            Ok(resp) => media_results.push(resp),
            Err(e) => return Err((StatusCode::INTERNAL_SERVER_ERROR, Json(json!({"error": e.to_string()}))).into_response()),
        }
    }

    let mut tx = state.pool.begin().await.map_err(|e| {
        eprintln!("Error en BD: {:?}", e);
        (StatusCode::INTERNAL_SERVER_ERROR, Json(json!({"error": e.to_string()}))).into_response()
    })?;
    let pet_id = Uuid::new_v4().to_string();
    let now = Utc::now().naive_utc();

    let pet = Pet {
        id: pet_id.clone(),
        name: command.name,
        gender: command.gender,
        status: command.status,
        description: command.description,
        species_id: command.species_id,
        breed_id: command.breed_id,
        reporter_id: command.reporter_id,
        shelter_id: command.shelter_id,
        created_at: now,
        updated_at: now,
        last_latitude: command.last_latitude,
        last_longitude: command.last_longitude,
        last_geohash: geohash,
    };

    PetRepository::create(&mut *tx, &pet).await.map_err(|e| {
        eprintln!("Error en BD: {:?}", e);
        (StatusCode::INTERNAL_SERVER_ERROR, Json(json!({"error": e.to_string()}))).into_response()
    })?;

    let mut media_list = Vec::new();
    for res in media_results {
        //let media_type = if res.secure_url.contains(".mp4") { MediaType::SightingVideo } else { MediaType::SightingImage };
        let media_type = if res.secure_url.contains(".mp4") { 
            "SightingVideo" 
        } else { 
            "SightingImage" 
        };
        let media = Media {
            id: Uuid::new_v4().to_string(),
            url: res.secure_url,
            public_id: res.public_id,
            r#type: media_type.to_string(), 
            pet_id: pet_id.clone(),
            latitude: None,
            longitude: None,
            geohash: None,
            created_at: now,
        };
        MediaRepository::create(&mut *tx, &media).await.map_err(|e| {
            eprintln!("Error en BD: {:?}", e);
            (StatusCode::INTERNAL_SERVER_ERROR, Json(json!({"error": e.to_string()}))).into_response()
        })?;
        media_list.push(media);
    }

    tx.commit().await.map_err(|e| {
        eprintln!("Error en BD: {:?}", e);
        (StatusCode::INTERNAL_SERVER_ERROR, Json(json!({"error": e.to_string()}))).into_response()
    })?;

    // Notificar a RabbitMQ
    let _ = state.rabbit_service.publish_event("pet.created", &PetEvent::Created { pet, media: media_list }).await;

    Ok((StatusCode::CREATED, Json(json!({"id": pet_id}))).into_response())
}

/// Actualiza los datos de una mascota y permite añadir nuevas fotos.
#[utoipa::path(
    put,
    path = "/api/v1/pets/{id}",
    params(("id" = String, Path, description = "ID de la mascota")),
    request_body(content = UpdatePetCommand, content_type = "multipart/form-data"),
    responses(
        (status = 200, description = "Mascota actualizada"),
        (status = 404, description = "No encontrada")
    ),
    tag = "Pets"
)]
pub async fn update_pet(
    _auth: JwtMiddleware,
    State(state): State<AppState>,
    Path(id): Path<String>,
    mut multipart: Multipart,
) -> Result<Response, Response> {
    let mut command_map = serde_json::Map::new();
    let mut files: Vec<(Vec<u8>, String)> = Vec::new();

    while let Ok(Some(field)) = multipart.next_field().await {
        let name = field.name().unwrap_or_default().to_string();
        if name == "file" {
            let file_name = field.file_name().unwrap_or("update.jpg").to_string();
            let data = field.bytes().await.unwrap_or_default().to_vec();
            files.push((data, file_name));
        } else {
            let text = field.text().await.unwrap_or_default();
            if !text.is_empty() && text != "null" {
                let val = if name == "last_latitude" || name == "last_longitude" {
                    text.parse::<f64>().map(|n| json!(n)).unwrap_or_else(|_| json!(text))
                } else if text.starts_with('{') || text.starts_with('[') {
                    serde_json::from_str::<serde_json::Value>(&text).unwrap_or_else(|_| json!(text))
                } else {
                    json!(text)
                };
                
                command_map.insert(name, val);
            }
        }
    }

    let command: UpdatePetCommand = match serde_json::from_value(serde_json::Value::Object(command_map)) {
        Ok(c) => c,
        Err(e) => return Err((StatusCode::BAD_REQUEST, Json(json!({"error": format!("Datos de mascota inválidos: {}", e)}))).into_response()),
    };

    let geohash = if let (Some(lat), Some(lon)) = (command.last_latitude, command.last_longitude) {
        encode(Coord { x: lon, y: lat }, 10).ok()
    } else {
        None
    };

    let mut media_results = Vec::new();
    for (data, file_name) in files {
        if let Ok(resp) = state.cloudinary_service.upload_media(data, &file_name, "pets").await {
            media_results.push(resp);
        }
    }

    let mut tx = state.pool.begin().await.map_err(|e| {
        eprintln!("Error en BD: {:?}", e);
        (StatusCode::INTERNAL_SERVER_ERROR, Json(json!({"error": e.to_string()}))).into_response()
    })?;
    let now = Utc::now().naive_utc();

    let pet_update = Pet {
        id: id.clone(),
        name: command.name,
        gender: command.gender.unwrap_or_default(),
        status: command.status.unwrap_or_default(),
        description: command.description,
        species_id: command.species_id.unwrap_or_default(),
        breed_id: command.breed_id,
        reporter_id: "".to_string(),
        shelter_id: command.shelter_id,
        created_at: now,
        updated_at: now,
        last_latitude: command.last_latitude,
        last_longitude: command.last_longitude,
        last_geohash: geohash,
    };

    let rows = PetRepository::update(&mut *tx, &id, &pet_update).await.map_err(|e| {
        eprintln!("Error en BD: {:?}", e);
        (StatusCode::INTERNAL_SERVER_ERROR, Json(json!({"error": e.to_string()}))).into_response()
    })?;

    if rows == 0 {
        return Err((StatusCode::NOT_FOUND, "Mascota no encontrada").into_response());
    }

    for res in media_results {
        // let media_type = if res.secure_url.contains(".mp4") { MediaType::SightingVideo } else { MediaType::SightingImage };
        let media_type = if res.secure_url.contains(".mp4") { 
            "SightingVideo" 
        } else { 
            "SightingImage" 
        };
        let media = Media {
            id: Uuid::new_v4().to_string(),
            url: res.secure_url,
            public_id: res.public_id,
            r#type: media_type.to_string(), 
            pet_id: id.clone(),
            latitude: None,
            longitude: None,
            geohash: None,
            created_at: now,
        };
        MediaRepository::create(&mut *tx, &media).await.map_err(|e| {
            eprintln!("Error en BD: {:?}", e);
            (StatusCode::INTERNAL_SERVER_ERROR, Json(json!({"error": e.to_string()}))).into_response()
        })?;
    }

    tx.commit().await.map_err(|e| {
        eprintln!("Error en BD: {:?}", e);
        (StatusCode::INTERNAL_SERVER_ERROR, Json(json!({"error": e.to_string()}))).into_response()
    })?;

    // Notificar actualización (obteniendo el objeto completo actualizado)
    if let Ok(Some(full_pet)) = PetRepository::find_by_id(&state.pool, &id).await {
        let _ = state.rabbit_service.publish_event("pet.updated", &PetEvent::Updated { pet: full_pet }).await;
    }

    Ok(StatusCode::OK.into_response())
}

/// Elimina una mascota y sus archivos en Cloudinary.
#[utoipa::path(
    delete,
    path = "/api/v1/pets/{id}",
    params(("id" = String, Path, description = "ID de la mascota")),
    responses(
        (status = 200, description = "Mascota eliminada"),
        (status = 404, description = "No encontrada")
    ),
    tag = "Pets"
)]
pub async fn delete_pet(
    _auth: JwtMiddleware,
    State(state): State<AppState>,
    Path(id): Path<String>,
) -> Result<Response, Response> {
    let public_ids = MediaRepository::get_public_ids_by_pet_id(&state.pool, &id).await.map_err(|e| {
        eprintln!("Error en BD: {:?}", e);
        (StatusCode::INTERNAL_SERVER_ERROR, Json(json!({"error": e.to_string()}))).into_response()
    })?;

    for pid in public_ids {
        let _ = state.cloudinary_service.delete_media(&pid).await;
    }

    let rows = PetRepository::delete(&state.pool, &id).await.map_err(|e| {
        eprintln!("Error en BD: {:?}", e);
        (StatusCode::INTERNAL_SERVER_ERROR, Json(json!({"error": e.to_string()}))).into_response()
    })?;

    if rows == 0 {
        Ok(StatusCode::NOT_FOUND.into_response())
    } else {
        // Notificar eliminación
        let _ = state.rabbit_service.publish_event("pet.deleted", &PetEvent::Deleted { pet_id: id }).await;
        Ok(StatusCode::OK.into_response())
    }
}
