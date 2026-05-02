use axum::{
    routing::get,
    Router,
};
use crate::handlers::pet::commands::{create_pet, update_pet, delete_pet};
use crate::handlers::pet::queries::{list_pets, get_pet_by_id, search_pets_by_location};
use crate::AppState;

pub fn pet_routes() -> Router<AppState> {
    Router::new()
        .route("/", get(list_pets).post(create_pet))
        .route("/search", get(search_pets_by_location))
        .route("/:id", get(get_pet_by_id).put(update_pet).delete(delete_pet))
}
