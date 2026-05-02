use axum::{
    extract::{State, Path},
    http::StatusCode,
    response::IntoResponse,
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
) -> impl IntoResponse {
    let species = Species {
        id: Uuid::new_v4().to_string(),
        name: command.name,
    };

    CatalogRepository::create_species(&state.pool, &species).await.unwrap();
    (StatusCode::CREATED, Json(species))
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
) -> impl IntoResponse {
    let species = Species {
        id: id.clone(),
        name: command.name,
    };

    let rows = CatalogRepository::update_species(&state.pool, &id, &species).await.unwrap();
    if rows == 0 { StatusCode::NOT_FOUND } else { StatusCode::OK }
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
) -> impl IntoResponse {
    let rows = CatalogRepository::delete_species(&state.pool, &id).await.unwrap();
    if rows == 0 { StatusCode::NOT_FOUND } else { StatusCode::OK }
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
) -> impl IntoResponse {
    let breed = Breed {
        id: Uuid::new_v4().to_string(),
        name: command.name,
        species_id: command.species_id,
    };

    CatalogRepository::create_breed(&state.pool, &breed).await.unwrap();
    (StatusCode::CREATED, Json(breed))
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
) -> impl IntoResponse {
    let breed = Breed {
        id: id.clone(),
        name: command.name,
        species_id: command.species_id,
    };

    let rows = CatalogRepository::update_breed(&state.pool, &id, &breed).await.unwrap();
    if rows == 0 { StatusCode::NOT_FOUND } else { StatusCode::OK }
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
) -> impl IntoResponse {
    let rows = CatalogRepository::delete_breed(&state.pool, &id).await.unwrap();
    if rows == 0 { StatusCode::NOT_FOUND } else { StatusCode::OK }
}
