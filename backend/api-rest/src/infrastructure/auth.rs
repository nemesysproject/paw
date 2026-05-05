use axum::{
    async_trait,
    extract::FromRequestParts,
    http::{request::Parts, StatusCode},
    response::{IntoResponse, Response},
    Json,
};
use jsonwebtoken::{decode, DecodingKey, Validation, Algorithm};
use serde::{Deserialize, Serialize};
use serde_json::json;
use std::env;

#[derive(Debug, Serialize, Deserialize)]
pub struct Claims {
    pub sub: String, // ID del usuario
    pub exp: usize,  // Expiración
    pub role: String,
}

pub struct JwtMiddleware(pub Claims);

#[async_trait]
impl<S> FromRequestParts<S> for JwtMiddleware
where
    S: Send + Sync,
{
    type Rejection = Response;

    async fn from_request_parts(parts: &mut Parts, _state: &S) -> Result<Self, Self::Rejection> {
        let auth_header = parts
            .headers
            .get("Authorization")
            .and_then(|h| h.to_str().ok())
            .ok_or_else(|| {
                (StatusCode::UNAUTHORIZED, Json(json!({"error": "Missing Authorization header"}))).into_response()
            })?;

        if !auth_header.starts_with("Bearer ") {
            return Err((StatusCode::UNAUTHORIZED, Json(json!({"error": "Invalid token format"}))).into_response());
        }

        let token = &auth_header[7..];
        let secret = env::var("JWT_SECRET").unwrap_or_else(|_| "secret_por_defecto_no_usar_en_produccion".to_string());

        let token_data = decode::<Claims>(
            token,
            &DecodingKey::from_secret(secret.as_bytes()),
            &Validation::new(Algorithm::HS256),
        )
        .map_err(|e| {
            eprintln!("JWT Error: {:?}", e);
            (StatusCode::UNAUTHORIZED, Json(json!({"error": "Invalid or expired token"}))).into_response()
        })?;

        Ok(JwtMiddleware(token_data.claims))
    }
}

pub fn generate_tokens(user_id: &str, role: &str) -> Result<(String, String), jsonwebtoken::errors::Error> {
    let secret = env::var("JWT_SECRET").unwrap_or_else(|_| "secret_por_defecto_no_usar_en_produccion".to_string());
    
    let access_exp = chrono::Utc::now()
        .checked_add_signed(chrono::Duration::hours(1))
        .expect("valid timestamp")
        .timestamp() as usize;

    let refresh_exp = chrono::Utc::now()
        .checked_add_signed(chrono::Duration::days(7))
        .expect("valid timestamp")
        .timestamp() as usize;

    let access_claims = Claims {
        sub: user_id.to_string(),
        exp: access_exp,
        role: role.to_string(),
    };

    let refresh_claims = Claims {
        sub: user_id.to_string(),
        exp: refresh_exp,
        role: role.to_string(),
    };

    let access_token = jsonwebtoken::encode(
        &jsonwebtoken::Header::default(),
        &access_claims,
        &jsonwebtoken::EncodingKey::from_secret(secret.as_bytes()),
    )?;

    let refresh_token = jsonwebtoken::encode(
        &jsonwebtoken::Header::default(),
        &refresh_claims,
        &jsonwebtoken::EncodingKey::from_secret(secret.as_bytes()),
    )?;

    Ok((access_token, refresh_token))
}

pub fn verify_token(token: &str) -> Result<Claims, jsonwebtoken::errors::Error> {
    let secret = env::var("JWT_SECRET").unwrap_or_else(|_| "secret_por_defecto_no_usar_en_produccion".to_string());
    let token_data = decode::<Claims>(
        token,
        &DecodingKey::from_secret(secret.as_bytes()),
        &Validation::new(Algorithm::HS256),
    )?;
    Ok(token_data.claims)
}
