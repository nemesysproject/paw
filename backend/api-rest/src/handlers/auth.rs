use axum::{
    extract::State,
    http::StatusCode,
    response::{IntoResponse, Response},
    Json,
};
use crate::AppState;
use crate::models::auth::{LoginRequest, RegisterRequest, TokenResponse, RefreshRequest};
use crate::models::entities::User;
use crate::repositories::user_repo::UserRepository;
use crate::infrastructure::auth::{generate_tokens, verify_token};
use argon2::{
    password_hash::{rand_core::OsRng, PasswordHash, PasswordHasher, PasswordVerifier, SaltString},
    Argon2,
};
use uuid::Uuid;
use chrono::Utc;
use serde_json::json;
use validator::Validate;

/// Registra un nuevo usuario.
#[utoipa::path(
    post,
    path = "/api/v1/auth/register",
    request_body = RegisterRequest,
    responses(
        (status = 201, description = "Usuario registrado"),
        (status = 400, description = "Email ya existe o datos inválidos")
    ),
    tag = "Auth"
)]
pub async fn register(
    State(state): State<AppState>,
    Json(req): Json<RegisterRequest>,
) -> Result<Response, Response> {
    if let Err(e) = req.validate() {
        return Err((StatusCode::BAD_REQUEST, Json(json!({"error": "Error de validación", "details": e}))).into_response());
    }

    // Verificar si el usuario ya existe
    if let Ok(Some(_)) = UserRepository::find_by_email(&state.pool, &req.email).await {
        return Err((StatusCode::BAD_REQUEST, Json(json!({"error": "Email already exists"}))).into_response());
    }

    let salt = SaltString::generate(&mut OsRng);
    let argon2 = Argon2::default();
    let hashed_password = argon2.hash_password(req.password.as_bytes(), &salt)
        .map_err(|e| {
            tracing::error!("Error hashing password: {:?}", e);
            (StatusCode::INTERNAL_SERVER_ERROR, Json(json!({"error": "Error interno del servidor"}))).into_response()
        })?
        .to_string();

    let now = Utc::now().naive_utc();
    let user = User {
        id: Uuid::new_v4().to_string(),
        email: req.email,
        password: hashed_password,
        name: req.name,
        role: "USER".to_string(),
        created_at: now,
        updated_at: now,
    };

    UserRepository::create(&state.pool, &user).await.map_err(|e| {
        tracing::error!("Error creando usuario en la BD: {:?}", e);
        (StatusCode::INTERNAL_SERVER_ERROR, Json(json!({"error": "Error interno del servidor"}))).into_response()
    })?;

    Ok(StatusCode::CREATED.into_response())
}

/// Inicia sesión y devuelve tokens.
#[utoipa::path(
    post,
    path = "/api/v1/auth/login",
    request_body = LoginRequest,
    responses(
        (status = 200, description = "Login exitoso", body = TokenResponse),
        (status = 401, description = "Credenciales inválidas")
    ),
    tag = "Auth"
)]
pub async fn login(
    State(state): State<AppState>,
    Json(req): Json<LoginRequest>,
) -> Result<Response, Response> {
    if let Err(e) = req.validate() {
        return Err((StatusCode::BAD_REQUEST, Json(json!({"error": "Error de validación", "details": e}))).into_response());
    }

    let user = UserRepository::find_by_email(&state.pool, &req.email).await.map_err(|e| {
        tracing::error!("Error buscando usuario por email: {:?}", e);
        (StatusCode::INTERNAL_SERVER_ERROR, Json(json!({"error": "Error interno del servidor"}))).into_response()
    })?.ok_or_else(|| {
        (StatusCode::UNAUTHORIZED, Json(json!({"error": "Invalid credentials"}))).into_response()
    })?;

    let parsed_hash = PasswordHash::new(&user.password).map_err(|e| {
        tracing::error!("Error parsing password hash: {:?}", e);
        (StatusCode::INTERNAL_SERVER_ERROR, Json(json!({"error": "Error interno del servidor"}))).into_response()
    })?;

    if Argon2::default().verify_password(req.password.as_bytes(), &parsed_hash).is_err() {
        return Err((StatusCode::UNAUTHORIZED, Json(json!({"error": "Invalid credentials"}))).into_response());
    }

    let (access, refresh) = generate_tokens(&user.id, &user.role).map_err(|e| {
        tracing::error!("Error generando tokens: {:?}", e);
        (StatusCode::INTERNAL_SERVER_ERROR, Json(json!({"error": "Error interno del servidor"}))).into_response()
    })?;

    Ok((StatusCode::OK, Json(TokenResponse {
        access_token: access,
        refresh_token: refresh,
        token_type: "Bearer".to_string(),
        expires_in: 3600,
    })).into_response())
}

/// Refresca el token de acceso.
#[utoipa::path(
    post,
    path = "/api/v1/auth/refresh",
    request_body = RefreshRequest,
    responses(
        (status = 200, description = "Token refrescado", body = TokenResponse),
        (status = 401, description = "Refresh token inválido o expirado")
    ),
    tag = "Auth"
)]
pub async fn refresh(
    State(state): State<AppState>,
    Json(req): Json<RefreshRequest>,
) -> Result<Response, Response> {
    let claims = verify_token(&req.refresh_token).map_err(|_| {
        (StatusCode::UNAUTHORIZED, Json(json!({"error": "Invalid or expired refresh token"}))).into_response()
    })?;

    // Opcional: Verificar que el usuario aún exista
    let user = UserRepository::find_by_id(&state.pool, &claims.sub).await.map_err(|e| {
        tracing::error!("Error buscando usuario por ID: {:?}", e);
        (StatusCode::INTERNAL_SERVER_ERROR, Json(json!({"error": "Error interno del servidor"}))).into_response()
    })?.ok_or_else(|| {
        (StatusCode::UNAUTHORIZED, Json(json!({"error": "User not found"}))).into_response()
    })?;

    let (access, refresh) = generate_tokens(&user.id, &user.role).map_err(|e| {
        tracing::error!("Error generando tokens: {:?}", e);
        (StatusCode::INTERNAL_SERVER_ERROR, Json(json!({"error": "Error interno del servidor"}))).into_response()
    })?;

    Ok((StatusCode::OK, Json(TokenResponse {
        access_token: access,
        refresh_token: refresh,
        token_type: "Bearer".to_string(),
        expires_in: 3600,
    })).into_response())
}
