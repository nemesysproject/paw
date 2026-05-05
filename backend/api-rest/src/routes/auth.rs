use axum::{routing::post, Router};
use crate::AppState;
use crate::handlers::auth::{register, login, refresh};

pub fn auth_routes() -> Router<AppState> {
    Router::new()
        .route("/register", post(register))
        .route("/login", post(login))
        .route("/refresh", post(refresh))
}
