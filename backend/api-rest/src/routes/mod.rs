use axum::{Router, routing::get};
use crate::AppState;
use crate::handlers::dashboard;
use crate::handlers::health;

pub mod pet;
pub mod catalog;
pub mod auth;
pub mod ads;
pub mod ai;
pub mod social;

pub fn api_routes() -> Router<AppState> {
    Router::new()
        .route("/health", get(health::health_check))
        .route("/dashboard", get(dashboard::get_dashboard_data))
        .nest("/pets", pet::pet_routes())
        .nest("/catalogs", catalog::catalog_routes())
        .nest("/auth", auth::auth_routes())
        .nest("/ads", ads::ad_routes())
        .nest("/ai", ai::ai_routes())
        .nest("/sightings", social::sighting_routes())
        .nest("/conversations", social::conversation_routes())
        .nest("/adoptions", social::adoption_routes())
        .nest("/social", social::moderation_routes())
}
