use serde::{Deserialize, Serialize};
use utoipa::ToSchema;
use validator::Validate;

#[derive(Debug, Deserialize, ToSchema, Validate)]
pub struct RegisterRequest {
    #[validate(email)]
    pub email: String,
    #[validate(length(min = 8, message = "La contraseña debe tener al menos 8 caracteres"))]
    pub password: String,
    #[validate(length(min = 1, max = 100))]
    pub name: String,
}

#[derive(Debug, Deserialize, ToSchema, Validate)]
pub struct LoginRequest {
    #[validate(email)]
    pub email: String,
    #[validate(length(min = 1))]
    pub password: String,
}

#[derive(Debug, Deserialize, ToSchema, Validate)]
pub struct SocialLoginRequest {
    /// Proveedor federado: "GOOGLE" (extensible a "FACEBOOK" y "APPLE")
    #[validate(length(min = 2, max = 32))]
    pub provider: String,
    /// ID Token JWT emitido por el proveedor (Google Credential Manager)
    #[validate(length(min = 20))]
    pub id_token: String,
    /// Nonce opcional para prevenir ataques de repetición
    pub nonce: Option<String>,
}

#[derive(Debug, Serialize, ToSchema)]
pub struct TokenResponse {
    pub access_token: String,
    pub refresh_token: String,
    pub token_type: String,
    pub expires_in: usize,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub email: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub name: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub avatar_url: Option<String>,
}

#[derive(Debug, Deserialize, ToSchema)]
pub struct RefreshRequest {
    pub refresh_token: String,
}
