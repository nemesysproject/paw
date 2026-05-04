use axum::{
    extract::{State, Path, Query},
    http::StatusCode,
    response::{IntoResponse, Response},
    Json,
};
use crate::AppState;
use crate::models::entities::Pet;
use crate::models::pet::queries::{SearchParams, PetDetailResponse};
use crate::repositories::pet_repo::PetRepository;
use crate::repositories::media_repo::MediaRepository;
use geohash::{encode, Coord, neighbors};

/// Obtiene el detalle de una mascota por su ID.
#[utoipa::path(
    get,
    path = "/api/v1/pets/{id}",
    params(("id" = String, Path, description = "ID de la mascota")),
    responses(
        (status = 200, description = "Detalle de la mascota", body = PetDetailResponse),
        (status = 404, description = "No encontrada")
    ),
    tag = "Pets"
)]
pub async fn get_pet_by_id(
    State(state): State<AppState>,
    Path(id): Path<String>,
) -> Result<Response, Response> {
    let pet = PetRepository::find_by_id(&state.pool, &id).await.map_err(|e| {
        eprintln!("Error en BD: {:?}", e);
        (StatusCode::INTERNAL_SERVER_ERROR, Json(serde_json::json!({"error": e.to_string()}))).into_response()
    })?;
    match pet {
        Some(pet) => {
            let media = MediaRepository::find_by_pet_id(&state.pool, &id).await.map_err(|e| {
                eprintln!("Error en BD: {:?}", e);
                (StatusCode::INTERNAL_SERVER_ERROR, Json(serde_json::json!({"error": e.to_string()}))).into_response()
            })?;
            Ok((StatusCode::OK, Json(PetDetailResponse { pet, media })).into_response())
        }
        None => Ok(StatusCode::NOT_FOUND.into_response()),
    }
}

/// Lista todas las mascotas registradas.
#[utoipa::path(
    get,
    path = "/api/v1/pets",
    responses(
        (status = 200, description = "Lista de mascotas", body = [Pet]),
    ),
    tag = "Pets"
)]
pub async fn list_pets(
    State(state): State<AppState>,
) -> Result<Response, Response> {
    let pets = PetRepository::find_all(&state.pool).await.map_err(|e| {
        eprintln!("Error en BD: {:?}", e);
        (StatusCode::INTERNAL_SERVER_ERROR, Json(serde_json::json!({"error": e.to_string()}))).into_response()
    })?;
    Ok((StatusCode::OK, Json(pets)).into_response())
}

/// Busca mascotas por ubicación (100m, 1km, 5km).
#[utoipa::path(
    get,
    path = "/api/v1/pets/search",
    params(SearchParams),
    responses(
        (status = 200, description = "Mascotas encontradas", body = [Pet]),
    ),
    tag = "Pets"
)]
pub async fn search_pets_by_location(
    State(state): State<AppState>,
    Query(params): Query<SearchParams>,
) -> Result<Response, Response> {
    let radius = params.radius_meters.unwrap_or(1000);
    let search_patterns = get_geohash_prefixes(params.lat, params.lon, radius);
    
    let pets = PetRepository::find_by_geohash_prefixes(&state.pool, &search_patterns).await.map_err(|e| {
        eprintln!("Error en BD: {:?}", e);
        (StatusCode::INTERNAL_SERVER_ERROR, Json(serde_json::json!({"error": e.to_string()}))).into_response()
    })?;

    Ok((StatusCode::OK, Json(pets)).into_response())
}

fn get_geohash_prefixes(lat: f64, lon: f64, radius_meters: u32) -> Vec<String> {
    let precision = match radius_meters {
        0..=150 => 8,
        151..=1500 => 6,
        _ => 5,
    };

    let center_hash = encode(Coord { x: lon, y: lat }, precision).unwrap();
    let mut prefixes = vec![center_hash.clone()];
    let neighs = neighbors(&center_hash).unwrap();
    prefixes.extend(vec![neighs.n, neighs.ne, neighs.e, neighs.se, neighs.s, neighs.sw, neighs.w, neighs.nw]);

    prefixes.into_iter().map(|p| format!("{}%", p)).collect()
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_geohash_precision_logic() {
        // 100m -> Precision 8
        let prefixes_100m = get_geohash_prefixes(-12.046374, -77.042793, 100);
        assert_eq!(prefixes_100m[0].len(), 9); // 8 chars + %
        
        // 1km -> Precision 6
        let prefixes_1km = get_geohash_prefixes(-12.046374, -77.042793, 1000);
        assert_eq!(prefixes_1km[0].len(), 7); // 6 chars + %
        
        // 5km -> Precision 5
        let prefixes_5km = get_geohash_prefixes(-12.046374, -77.042793, 5000);
        assert_eq!(prefixes_5km[0].len(), 6); // 5 chars + %
    }

    #[test]
    fn test_geohash_neighbors_count() {
        let prefixes = get_geohash_prefixes(-12.046374, -77.042793, 1000);
        assert_eq!(prefixes.len(), 9); // Center + 8 neighbors
    }
}
