use axum::Router;
use crate::AppState;

pub mod pet;
pub mod catalog;

pub fn api_routes() -> Router<AppState> {
    Router::new()
        .nest("/pets", pet::pet_routes())
        .nest("/catalogs", catalog::catalog_routes())
}
