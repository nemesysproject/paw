use axum::{
    routing::{get, patch, post},
    Router,
};

use crate::handlers::social;
use crate::AppState;

pub fn sighting_routes() -> Router<AppState> {
    Router::new()
        .route("/", post(social::create_sighting))
        .route("/pet/:pet_id", get(social::list_sightings_by_pet))
        .route("/:id/confirm", patch(social::confirm_sighting))
}

pub fn conversation_routes() -> Router<AppState> {
    Router::new()
        .route(
            "/",
            get(social::list_conversations).post(social::create_conversation),
        )
        .route(
            "/:id/messages",
            get(social::list_messages).post(social::send_message),
        )
}

pub fn adoption_routes() -> Router<AppState> {
    Router::new()
        .route(
            "/",
            get(social::list_adoptions).post(social::create_adoption),
        )
        .route("/:id/status", patch(social::update_adoption_status))
}

pub fn moderation_routes() -> Router<AppState> {
    Router::new()
        .route("/device-tokens", post(social::register_device_token))
        .route("/blocks", post(social::block_user))
        .route("/reports", post(social::create_content_report))
}
