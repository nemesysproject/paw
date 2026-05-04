use axum::{
    extract::{State, Path, Query},
    http::StatusCode,
    response::{IntoResponse, Response},
    Json,
};
use crate::AppState;
use crate::repositories::catalog_repo::CatalogRepository;
use crate::models::entities::{Species, Breed};
use crate::models::catalog::queries::BreedQuery;

#[utoipa::path(
    get,
    path = "/api/v1/catalogs/species",
    responses(
        (status = 200, description = "Lista de todas las especies", body = [Species])
    ),
    tag = "Catalogs"
)]
pub async fn get_all_species(
    State(state): State<AppState>,
) -> Result<Response, Response> {
    let species = CatalogRepository::get_all_species(&state.pool).await.map_err(|e| {
        eprintln!("Error en BD: {:?}", e);
        (StatusCode::INTERNAL_SERVER_ERROR, Json(serde_json::json!({"error": e.to_string()}))).into_response()
    })?;
    Ok(Json(species).into_response())
}

#[utoipa::path(
    get,
    path = "/api/v1/catalogs/breeds",
    params(BreedQuery),
    responses(
        (status = 200, description = "Lista de todas las razas (filtrable por especie)", body = [Breed])
    ),
    tag = "Catalogs"
)]
pub async fn get_all_breeds(
    State(state): State<AppState>,
    Query(query): Query<BreedQuery>,
) -> Result<Response, Response> {
    let breeds = CatalogRepository::get_all_breeds(&state.pool, query.species_id).await.map_err(|e| {
        eprintln!("Error en BD: {:?}", e);
        (StatusCode::INTERNAL_SERVER_ERROR, Json(serde_json::json!({"error": e.to_string()}))).into_response()
    })?;
    Ok(Json(breeds).into_response())
}

#[utoipa::path(
    get,
    path = "/api/v1/catalogs/species/{id}",
    params(("id" = String, Path, description = "ID de la especie")),
    responses(
        (status = 200, description = "Detalle de la especie", body = Species),
        (status = 404, description = "No encontrada")
    ),
    tag = "Catalogs"
)]
pub async fn get_species_by_id(
    State(state): State<AppState>,
    Path(id): Path<String>,
) -> impl IntoResponse {
    match CatalogRepository::get_species_by_id(&state.pool, &id).await.unwrap() {
        Some(s) => (StatusCode::OK, Json(s)).into_response(),
        None => StatusCode::NOT_FOUND.into_response(),
    }
}

#[utoipa::path(
    get,
    path = "/api/v1/catalogs/breeds/{id}",
    params(("id" = String, Path, description = "ID de la raza")),
    responses(
        (status = 200, description = "Detalle de la raza", body = Breed),
        (status = 404, description = "No encontrada")
    ),
    tag = "Catalogs"
)]
pub async fn get_breed_by_id(
    State(state): State<AppState>,
    Path(id): Path<String>,
) -> impl IntoResponse {
    match CatalogRepository::get_breed_by_id(&state.pool, &id).await.unwrap() {
        Some(b) => (StatusCode::OK, Json(b)).into_response(),
        None => StatusCode::NOT_FOUND.into_response(),
    }
}

#[utoipa::path(
    get,
    path = "/api/v1/catalogs/species/{id}/breeds",
    params(("id" = String, Path, description = "ID de la especie")),
    responses(
        (status = 200, description = "Lista de razas para una especie", body = [Breed]),
        (status = 404, description = "Especie no encontrada")
    ),
    tag = "Catalogs"
)]
pub async fn get_breeds_by_species(
    State(state): State<AppState>,
    Path(id): Path<String>,
) -> Result<Response, Response> {
    let breeds = CatalogRepository::get_breeds_by_species(&state.pool, &id).await.map_err(|e| {
        eprintln!("Error en BD: {:?}", e);
        (StatusCode::INTERNAL_SERVER_ERROR, Json(serde_json::json!({"error": e.to_string()}))).into_response()
    })?;
    Ok(Json(breeds).into_response())
}
