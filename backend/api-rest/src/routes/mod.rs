use axum::{Router, routing::get};
use crate::AppState;
use crate::handlers::dashboard;

pub mod pet;
pub mod catalog;
pub mod auth;

pub fn api_routes() -> Router<AppState> {
    Router::new()
        .route("/dashboard", get(dashboard::get_dashboard_data))
        .nest("/pets", pet::pet_routes())
        .nest("/catalogs", catalog::catalog_routes())
        .nest("/auth", auth::auth_routes())
}
