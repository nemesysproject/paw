use axum::{
    extract::{State, Path, Query},
    http::StatusCode,
    response::{IntoResponse, Response},
    Json,
};
use crate::AppState;
use crate::models::pet::queries::{SearchParams, PetDetailResponse};
use geohash::{encode, Coord, neighbors};
use mongodb::bson::{doc, from_document};
use futures_lite::stream::StreamExt;

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
    let collection = state.mongo_service.pets_collection();
    let filter = doc! { "id": &id };

    match collection.find_one(filter).await {
        Ok(Some(doc)) => {
            match from_document::<PetDetailResponse>(doc) {
                Ok(pet_detail) => Ok((StatusCode::OK, Json(pet_detail)).into_response()),
                Err(e) => {
                    tracing::error!("Error deserializando desde MongoDB: {:?}", e);
                    Err((StatusCode::INTERNAL_SERVER_ERROR, Json(serde_json::json!({"error": "Error interno del servidor"}))).into_response())
                }
            }
        }
        Ok(None) => Ok(StatusCode::NOT_FOUND.into_response()),
        Err(e) => {
            tracing::error!("Error consultando MongoDB: {:?}", e);
            Err((StatusCode::INTERNAL_SERVER_ERROR, Json(serde_json::json!({"error": "Error interno del servidor"}))).into_response())
        }
    }
}

/// Lista todas las mascotas registradas.
#[utoipa::path(
    get,
    path = "/api/v1/pets",
    responses(
        (status = 200, description = "Lista de mascotas", body = [PetDetailResponse]),
    ),
    tag = "Pets"
)]
pub async fn list_pets(
    State(state): State<AppState>,
) -> Result<Response, Response> {
    let collection = state.mongo_service.pets_collection();
    
    let mut cursor = match collection.find(doc! {}).await {
        Ok(c) => c,
        Err(e) => {
            tracing::error!("Error consultando MongoDB: {:?}", e);
            return Err((StatusCode::INTERNAL_SERVER_ERROR, Json(serde_json::json!({"error": "Error interno del servidor"}))).into_response());
        }
    };

    let mut response: Vec<PetDetailResponse> = Vec::new();
    while let Some(result) = cursor.next().await {
        match result {
            Ok(doc) => {
                if let Ok(pet_detail) = from_document::<PetDetailResponse>(doc) {
                    response.push(pet_detail);
                }
            }
            Err(e) => {
                tracing::error!("Error iterando cursor de MongoDB: {:?}", e);
            }
        }
    }

    Ok((StatusCode::OK, Json(response)).into_response())
}

/// Busca mascotas por ubicación (100m, 1km, 5km).
#[utoipa::path(
    get,
    path = "/api/v1/pets/search",
    params(SearchParams),
    responses(
        (status = 200, description = "Mascotas encontradas", body = [PetDetailResponse]),
    ),
    tag = "Pets"
)]
pub async fn search_pets_by_location(
    State(state): State<AppState>,
    Query(params): Query<SearchParams>,
) -> Result<Response, Response> {
    let radius = params.radius_meters.unwrap_or(1000);
    let search_patterns = get_geohash_prefixes(params.lat, params.lon, radius);
    
    // Construir filtro $or para MongoDB usando expresiones regulares para prefijos
    let or_conditions: Vec<mongodb::bson::Document> = search_patterns.into_iter().map(|prefix| {
        // Remover el '%' del final que se usaba para SQL LIKE
        let clean_prefix = prefix.trim_end_matches('%');
        doc! { "last_geohash": { "$regex": format!("^{}", clean_prefix) } }
    }).collect();

    let filter = doc! { "$or": or_conditions };
    let collection = state.mongo_service.pets_collection();

    let mut cursor = match collection.find(filter).await {
        Ok(c) => c,
        Err(e) => {
            tracing::error!("Error consultando MongoDB: {:?}", e);
            return Err((StatusCode::INTERNAL_SERVER_ERROR, Json(serde_json::json!({"error": "Error interno del servidor"}))).into_response());
        }
    };

    let mut response: Vec<PetDetailResponse> = Vec::new();
    while let Some(result) = cursor.next().await {
        match result {
            Ok(doc) => {
                if let Ok(pet_detail) = from_document::<PetDetailResponse>(doc) {
                    response.push(pet_detail);
                }
            }
            Err(e) => {
                tracing::error!("Error iterando cursor de MongoDB: {:?}", e);
            }
        }
    }

    Ok((StatusCode::OK, Json(response)).into_response())
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
