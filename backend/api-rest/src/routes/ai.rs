use axum::{routing::post, Router};

use crate::handlers::ai;
use crate::AppState;

pub fn ai_routes() -> Router<AppState> {
    Router::new().route("/chat", post(ai::chat_assistant))
}
