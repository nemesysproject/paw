use axum::{
    routing::get,
    Router,
};
use crate::handlers::catalog::{commands, queries};
use crate::AppState;

pub fn catalog_routes() -> Router<AppState> {
    Router::new()
        // Species
        .route("/species", get(queries::get_all_species).post(commands::create_species))
        .route("/species/:id", get(queries::get_species_by_id).put(commands::update_species).delete(commands::delete_species))
        // Breeds
        .route("/species/:id/breeds", get(queries::get_breeds_by_species))
        .route("/breeds", get(queries::get_all_breeds).post(commands::create_breed))
        .route("/breeds/:id", get(queries::get_breed_by_id).put(commands::update_breed).delete(commands::delete_breed))
}
