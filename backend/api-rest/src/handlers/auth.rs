use axum::{
    extract::State,
    http::StatusCode,
    response::{IntoResponse, Response},
    Json,
};
use crate::AppState;
use crate::models::auth::{LoginRequest, RegisterRequest, SocialLoginRequest, TokenResponse, RefreshRequest};
use crate::models::entities::{User, UserIdentity};
use crate::repositories::user_repo::UserRepository;
use crate::infrastructure::auth::{generate_tokens, verify_token};
use crate::infrastructure::google_auth::{verify_google_id_token, GoogleAuthError};
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

    let normalized_email = req.email.trim().to_lowercase();

    // Verificar si el usuario ya existe
    if let Ok(Some(_)) = UserRepository::find_by_email(&state.pool, &normalized_email).await {
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
        email: normalized_email,
        password: Some(hashed_password),
        email_verified: false,
        name: req.name,
        phone: None,
        avatar_url: None,
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

/// Inicia sesión con email/contraseña y devuelve tokens JWT.
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

    let user = UserRepository::find_by_email(&state.pool, req.email.trim()).await.map_err(|e| {
        tracing::error!("Error buscando usuario por email: {:?}", e);
        (StatusCode::INTERNAL_SERVER_ERROR, Json(json!({"error": "Error interno del servidor"}))).into_response()
    })?.ok_or_else(|| {
        (StatusCode::UNAUTHORIZED, Json(json!({"error": "Invalid credentials"}))).into_response()
    })?;

    let password_hash_str = user.password.as_deref().ok_or_else(|| {
        (
            StatusCode::UNAUTHORIZED,
            Json(json!({"error": "Esta cuenta utiliza inicio de sesión con Google. Continúa con Google para acceder."})),
        )
            .into_response()
    })?;

    let parsed_hash = PasswordHash::new(password_hash_str).map_err(|e| {
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
        email: Some(user.email),
        name: Some(user.name),
        avatar_url: user.avatar_url,
    })).into_response())
}

/// Inicia sesión o registra usuario mediante proveedor social (Google Sign-In / Credential Manager).
#[utoipa::path(
    post,
    path = "/api/v1/auth/social",
    request_body = SocialLoginRequest,
    responses(
        (status = 200, description = "Autenticación social exitosa, retorna JWT propios", body = TokenResponse),
        (status = 400, description = "Proveedor no soportado o solicitud inválida"),
        (status = 401, description = "Token social inválido o expirado"),
        (status = 503, description = "Proveedor social no configurado en el servidor")
    ),
    tag = "Auth"
)]
pub async fn social_login(
    State(state): State<AppState>,
    Json(req): Json<SocialLoginRequest>,
) -> Result<Response, Response> {
    if let Err(e) = req.validate() {
        return Err((StatusCode::BAD_REQUEST, Json(json!({"error": "Error de validación", "details": e}))).into_response());
    }

    let provider = req.provider.trim().to_uppercase();
    if provider != "GOOGLE" {
        return Err((
            StatusCode::BAD_REQUEST,
            Json(json!({"error": format!("Proveedor '{}' no soportado aún. Usa 'GOOGLE'.", provider)})),
        )
            .into_response());
    }

    let profile = verify_google_id_token(&req.id_token, req.nonce.as_deref())
        .await
        .map_err(|err| match err {
            GoogleAuthError::NotConfigured => {
                tracing::error!("GOOGLE_OAUTH_CLIENT_ID no está configurado en el entorno");
                (
                    StatusCode::SERVICE_UNAVAILABLE,
                    Json(json!({"error": "Google Sign-In no está configurado en el servidor (falta GOOGLE_OAUTH_CLIENT_ID)"})),
                )
                    .into_response()
            }
            GoogleAuthError::InvalidToken(msg) => {
                tracing::warn!("Google ID Token rechazado: {}", msg);
                (StatusCode::UNAUTHORIZED, Json(json!({"error": msg}))).into_response()
            }
            GoogleAuthError::UpstreamError(msg) => {
                tracing::error!("Error verificando Google JWKS: {}", msg);
                (StatusCode::BAD_GATEWAY, Json(json!({"error": "No se pudo verificar el token con Google"}))).into_response()
            }
        })?;

    let now = Utc::now().naive_utc();

    // 1. Buscar identidad social ya vinculada
    let user = if let Some(identity) = UserRepository::find_identity(&state.pool, &provider, &profile.sub)
        .await
        .map_err(|e| {
            tracing::error!("Error buscando UserIdentity: {:?}", e);
            (StatusCode::INTERNAL_SERVER_ERROR, Json(json!({"error": "Error interno del servidor"}))).into_response()
        })?
    {
        UserRepository::find_by_id(&state.pool, &identity.user_id)
            .await
            .map_err(|e| {
                tracing::error!("Error buscando usuario vinculado: {:?}", e);
                (StatusCode::INTERNAL_SERVER_ERROR, Json(json!({"error": "Error interno del servidor"}))).into_response()
            })?
            .ok_or_else(|| {
                (StatusCode::UNAUTHORIZED, Json(json!({"error": "User not found"}))).into_response()
            })?
    } else if let Some(existing) = UserRepository::find_by_email(&state.pool, &profile.email)
        .await
        .map_err(|e| {
            tracing::error!("Error buscando usuario por email: {:?}", e);
            (StatusCode::INTERNAL_SERVER_ERROR, Json(json!({"error": "Error interno del servidor"}))).into_response()
        })?
    {
        // 2. Vincular cuenta existente (seguro porque Google ya garantizó email_verified == true)
        let new_identity = UserIdentity {
            id: Uuid::new_v4().to_string(),
            user_id: existing.id.clone(),
            provider: provider.clone(),
            provider_user_id: profile.sub.clone(),
            email: Some(profile.email.clone()),
            created_at: now,
            updated_at: now,
        };
        UserRepository::link_identity(&state.pool, &new_identity).await.map_err(|e| {
            tracing::error!("Error vinculando UserIdentity: {:?}", e);
            (StatusCode::INTERNAL_SERVER_ERROR, Json(json!({"error": "Error interno del servidor"}))).into_response()
        })?;
        let _ = UserRepository::mark_verified_and_update_avatar(
            &state.pool,
            &existing.id,
            profile.picture.as_deref(),
        )
        .await;
        existing
    } else {
        // 3. Crear usuario nuevo con rol USER (nunca elevar roles privilegiados desde login social)
        let new_user = User {
            id: Uuid::new_v4().to_string(),
            email: profile.email.clone(),
            password: None,
            email_verified: true,
            name: profile.name.clone(),
            phone: None,
            avatar_url: profile.picture.clone(),
            role: "USER".to_string(),
            created_at: now,
            updated_at: now,
        };
        UserRepository::create(&state.pool, &new_user).await.map_err(|e| {
            tracing::error!("Error creando usuario social: {:?}", e);
            (StatusCode::INTERNAL_SERVER_ERROR, Json(json!({"error": "Error interno del servidor"}))).into_response()
        })?;

        let new_identity = UserIdentity {
            id: Uuid::new_v4().to_string(),
            user_id: new_user.id.clone(),
            provider: provider.clone(),
            provider_user_id: profile.sub.clone(),
            email: Some(profile.email.clone()),
            created_at: now,
            updated_at: now,
        };
        UserRepository::link_identity(&state.pool, &new_identity).await.map_err(|e| {
            tracing::error!("Error guardando UserIdentity: {:?}", e);
            (StatusCode::INTERNAL_SERVER_ERROR, Json(json!({"error": "Error interno del servidor"}))).into_response()
        })?;
        new_user
    };

    let (access, refresh) = generate_tokens(&user.id, &user.role).map_err(|e| {
        tracing::error!("Error generando tokens: {:?}", e);
        (StatusCode::INTERNAL_SERVER_ERROR, Json(json!({"error": "Error interno del servidor"}))).into_response()
    })?;

    Ok((StatusCode::OK, Json(TokenResponse {
        access_token: access,
        refresh_token: refresh,
        token_type: "Bearer".to_string(),
        expires_in: 3600,
        email: Some(user.email),
        name: Some(user.name),
        avatar_url: user.avatar_url.or(profile.picture),
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

    if claims.token_type != "refresh" {
        return Err((StatusCode::UNAUTHORIZED, Json(json!({"error": "Invalid refresh token type"}))).into_response());
    }

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
        email: Some(user.email),
        name: Some(user.name),
        avatar_url: user.avatar_url,
    })).into_response())
}
