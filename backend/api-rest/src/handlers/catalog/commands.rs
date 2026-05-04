use axum::{
    extract::{State, Path},
    http::StatusCode,
    response::{IntoResponse, Response},
    Json,
};
use crate::AppState;
use crate::models::catalog::commands::{CreateSpeciesCommand, UpdateSpeciesCommand, CreateBreedCommand, UpdateBreedCommand};
use crate::models::entities::{Species, Breed};
use crate::repositories::catalog_repo::CatalogRepository;
use uuid::Uuid;

// --- Species ---

#[utoipa::path(
    post,
    path = "/api/v1/catalogs/species",
    request_body = CreateSpeciesCommand,
    responses(
        (status = 201, description = "Especie creada", body = Species),
        (status = 500, description = "Error interno")
    ),
    tag = "Catalogs"
)]
pub async fn create_species(
    State(state): State<AppState>,
    Json(command): Json<CreateSpeciesCommand>,
) -> Result<Response, Response> {
    let species = Species {
        id: Uuid::new_v4().to_string(),
        name: command.name,
    };

    CatalogRepository::create_species(&state.pool, &species).await.map_err(|e| {
        eprintln!("Error en BD: {:?}", e);
        (StatusCode::INTERNAL_SERVER_ERROR, Json(serde_json::json!({"error": e.to_string()}))).into_response()
    })?;
    Ok((StatusCode::CREATED, Json(species)).into_response())
}

#[utoipa::path(
    put,
    path = "/api/v1/catalogs/species/{id}",
    params(("id" = String, Path, description = "ID de la especie")),
    request_body = UpdateSpeciesCommand,
    responses(
        (status = 200, description = "Especie actualizada"),
        (status = 404, description = "No encontrada")
    ),
    tag = "Catalogs"
)]
pub async fn update_species(
    State(state): State<AppState>,
    Path(id): Path<String>,
    Json(command): Json<UpdateSpeciesCommand>,
) -> Result<Response, Response> {
    let species = Species {
        id: id.clone(),
        name: command.name,
    };

    let rows = CatalogRepository::update_species(&state.pool, &id, &species).await.map_err(|e| {
        eprintln!("Error en BD: {:?}", e);
        (StatusCode::INTERNAL_SERVER_ERROR, Json(serde_json::json!({"error": e.to_string()}))).into_response()
    })?;
    if rows == 0 { Ok(StatusCode::NOT_FOUND.into_response()) } else { Ok(StatusCode::OK.into_response()) }
}

#[utoipa::path(
    delete,
    path = "/api/v1/catalogs/species/{id}",
    params(("id" = String, Path, description = "ID de la especie")),
    responses(
        (status = 200, description = "Especie eliminada"),
        (status = 404, description = "No encontrada")
    ),
    tag = "Catalogs"
)]
pub async fn delete_species(
    State(state): State<AppState>,
    Path(id): Path<String>,
) -> Result<Response, Response> {
    let rows = CatalogRepository::delete_species(&state.pool, &id).await.map_err(|e| {
        eprintln!("Error en BD: {:?}", e);
        (StatusCode::INTERNAL_SERVER_ERROR, Json(serde_json::json!({"error": e.to_string()}))).into_response()
    })?;
    if rows == 0 { Ok(StatusCode::NOT_FOUND.into_response()) } else { Ok(StatusCode::OK.into_response()) }
}

// --- Breeds ---

#[utoipa::path(
    post,
    path = "/api/v1/catalogs/breeds",
    request_body = CreateBreedCommand,
    responses(
        (status = 201, description = "Raza creada", body = Breed),
        (status = 500, description = "Error interno")
    ),
    tag = "Catalogs"
)]
pub async fn create_breed(
    State(state): State<AppState>,
    Json(command): Json<CreateBreedCommand>,
) -> Result<Response, Response> {
    let breed = Breed {
        id: Uuid::new_v4().to_string(),
        name: command.name,
        species_id: command.species_id,
    };

    CatalogRepository::create_breed(&state.pool, &breed).await.map_err(|e| {
        eprintln!("Error en BD: {:?}", e);
        (StatusCode::INTERNAL_SERVER_ERROR, Json(serde_json::json!({"error": e.to_string()}))).into_response()
    })?;
    Ok((StatusCode::CREATED, Json(breed)).into_response())
}

#[utoipa::path(
    put,
    path = "/api/v1/catalogs/breeds/{id}",
    params(("id" = String, Path, description = "ID de la raza")),
    request_body = UpdateBreedCommand,
    responses(
        (status = 200, description = "Raza actualizada"),
        (status = 404, description = "No encontrada")
    ),
    tag = "Catalogs"
)]
pub async fn update_breed(
    State(state): State<AppState>,
    Path(id): Path<String>,
    Json(command): Json<UpdateBreedCommand>,
) -> Result<Response, Response> {
    let breed = Breed {
        id: id.clone(),
        name: command.name,
        species_id: command.species_id,
    };

    let rows = CatalogRepository::update_breed(&state.pool, &id, &breed).await.map_err(|e| {
        eprintln!("Error en BD: {:?}", e);
        (StatusCode::INTERNAL_SERVER_ERROR, Json(serde_json::json!({"error": e.to_string()}))).into_response()
    })?;
    if rows == 0 { Ok(StatusCode::NOT_FOUND.into_response()) } else { Ok(StatusCode::OK.into_response()) }
}

#[utoipa::path(
    delete,
    path = "/api/v1/catalogs/breeds/{id}",
    params(("id" = String, Path, description = "ID de la raza")),
    responses(
        (status = 200, description = "Raza eliminada"),
        (status = 404, description = "No encontrada")
    ),
    tag = "Catalogs"
)]
pub async fn delete_breed(
    State(state): State<AppState>,
    Path(id): Path<String>,
) -> Result<Response, Response> {
    let rows = CatalogRepository::delete_breed(&state.pool, &id).await.map_err(|e| {
        eprintln!("Error en BD: {:?}", e);
        (StatusCode::INTERNAL_SERVER_ERROR, Json(serde_json::json!({"error": e.to_string()}))).into_response()
    })?;
    if rows == 0 { Ok(StatusCode::NOT_FOUND.into_response()) } else { Ok(StatusCode::OK.into_response()) }
}
