use axum::{routing::post, Router};
use crate::AppState;
use crate::handlers::auth::{register, login, social_login, refresh};

pub fn auth_routes() -> Router<AppState> {
    Router::new()
        .route("/register", post(register))
        .route("/login", post(login))
        .route("/social", post(social_login))
        .route("/refresh", post(refresh))
}
