use jsonwebtoken::{decode, decode_header, jwk::JwkSet, Algorithm, DecodingKey, Validation};
use serde::Deserialize;
use std::env;
use std::sync::OnceLock;
use std::time::{Duration, Instant};
use tokio::sync::RwLock;

const GOOGLE_JWKS_URL: &str = "https://www.googleapis.com/oauth2/v3/certs";
const JWKS_TTL_SECS: u64 = 3600;

struct CachedJwks {
    jwks: JwkSet,
    fetched_at: Instant,
}

static GOOGLE_JWKS_CACHE: OnceLock<RwLock<Option<CachedJwks>>> = OnceLock::new();

#[derive(Debug, Deserialize)]
struct GoogleIdTokenClaims {
    pub sub: String,
    pub email: String,
    #[serde(default)]
    pub email_verified: bool,
    pub name: Option<String>,
    pub picture: Option<String>,
    pub iss: String,
    pub nonce: Option<String>,
}

#[derive(Debug, Clone)]
pub struct VerifiedGoogleProfile {
    pub sub: String,
    pub email: String,
    pub name: String,
    pub picture: Option<String>,
}

#[derive(Debug)]
pub enum GoogleAuthError {
    NotConfigured,
    InvalidToken(String),
    UpstreamError(String),
}

async fn fetch_google_jwks() -> Result<JwkSet, GoogleAuthError> {
    let client = reqwest::Client::builder()
        .timeout(Duration::from_secs(10))
        .build()
        .map_err(|e| GoogleAuthError::UpstreamError(format!("HTTP client error: {e}")))?;

    let resp = client
        .get(GOOGLE_JWKS_URL)
        .send()
        .await
        .map_err(|e| GoogleAuthError::UpstreamError(format!("Error obteniendo JWKS de Google: {e}")))?;

    if !resp.status().is_success() {
        return Err(GoogleAuthError::UpstreamError(format!(
            "Google JWKS respondió con estado {}",
            resp.status()
        )));
    }

    resp.json::<JwkSet>()
        .await
        .map_err(|e| GoogleAuthError::UpstreamError(format!("Error parseando JWKS de Google: {e}")))
}

async fn get_google_jwks(force_refresh: bool) -> Result<JwkSet, GoogleAuthError> {
    let cache = GOOGLE_JWKS_CACHE.get_or_init(|| RwLock::new(None));

    if !force_refresh {
        let guard = cache.read().await;
        if let Some(cached) = guard.as_ref() {
            if cached.fetched_at.elapsed() < Duration::from_secs(JWKS_TTL_SECS) {
                return Ok(cached.jwks.clone());
            }
        }
    }

    let fresh = fetch_google_jwks().await?;
    let mut guard = cache.write().await;
    *guard = Some(CachedJwks {
        jwks: fresh.clone(),
        fetched_at: Instant::now(),
    });
    Ok(fresh)
}

/// Verifica criptográficamente un Google ID Token (RS256 contra JWKS de Google),
/// validando `aud`, `iss`, `exp`, `email_verified` y opcionalmente `nonce`.
pub async fn verify_google_id_token(
    id_token: &str,
    expected_nonce: Option<&str>,
) -> Result<VerifiedGoogleProfile, GoogleAuthError> {
    let raw_audiences = env::var("GOOGLE_OAUTH_CLIENT_ID").unwrap_or_default();
    let allowed_audiences: Vec<String> = raw_audiences
        .split(',')
        .map(|s| s.trim().to_string())
        .filter(|s| !s.is_empty())
        .collect();

    if allowed_audiences.is_empty() {
        return Err(GoogleAuthError::NotConfigured);
    }

    let header = decode_header(id_token)
        .map_err(|e| GoogleAuthError::InvalidToken(format!("Encabezado JWT inválido: {e}")))?;

    if header.alg != Algorithm::RS256 {
        return Err(GoogleAuthError::InvalidToken(
            "Algoritmo de firma no permitido (se esperaba RS256)".to_string(),
        ));
    }

    let kid = header
        .kid
        .ok_or_else(|| GoogleAuthError::InvalidToken("Token sin identificador de llave (kid)".to_string()))?;

    let mut jwks = get_google_jwks(false).await?;
    let jwk = match jwks.find(&kid) {
        Some(k) => k.clone(),
        None => {
            // Rotación de llaves de Google: refrescar caché una vez
            jwks = get_google_jwks(true).await?;
            jwks.find(&kid)
                .cloned()
                .ok_or_else(|| GoogleAuthError::InvalidToken("Llave pública (kid) no encontrada en Google JWKS".to_string()))?
        }
    };

    let decoding_key = DecodingKey::from_jwk(&jwk)
        .map_err(|e| GoogleAuthError::InvalidToken(format!("Llave JWK inválida: {e}")))?;

    let mut validation = Validation::new(Algorithm::RS256);
    validation.set_audience(&allowed_audiences);
    validation.set_issuer(&["https://accounts.google.com", "accounts.google.com"]);
    validation.validate_exp = true;

    let token_data = decode::<GoogleIdTokenClaims>(id_token, &decoding_key, &validation)
        .map_err(|e| GoogleAuthError::InvalidToken(format!("Google ID Token inválido o expirado: {e}")))?;

    let claims = token_data.claims;

    if claims.iss != "https://accounts.google.com" && claims.iss != "accounts.google.com" {
        return Err(GoogleAuthError::InvalidToken("Issuer inválido".to_string()));
    }

    if !claims.email_verified {
        return Err(GoogleAuthError::InvalidToken(
            "El correo electrónico de Google no está verificado".to_string(),
        ));
    }

    if let Some(expected) = expected_nonce {
        if claims.nonce.as_deref() != Some(expected) {
            return Err(GoogleAuthError::InvalidToken("Nonce inválido".to_string()));
        }
    }

    let display_name = claims
        .name
        .filter(|n| !n.trim().is_empty())
        .unwrap_or_else(|| claims.email.split('@').next().unwrap_or("Usuario").to_string());

    Ok(VerifiedGoogleProfile {
        sub: claims.sub,
        email: claims.email.to_lowercase(),
        name: display_name,
        picture: claims.picture,
    })
}
