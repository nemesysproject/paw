use serde::{Deserialize, Serialize};
use utoipa::ToSchema;

#[derive(Debug, Clone, Serialize, Deserialize, ToSchema)]
pub struct AiChatMessage {
    /// "user" o "assistant"
    pub role: String,
    pub content: String,
}

#[derive(Debug, Deserialize, ToSchema)]
pub struct AiChatRequest {
    /// Pregunta o instrucción en lenguaje natural del administrador
    pub message: String,
    /// Historial reciente de la conversación (máximo 10 mensajes)
    #[serde(default)]
    pub history: Vec<AiChatMessage>,
}

#[derive(Debug, Clone, Serialize, Deserialize, ToSchema)]
pub struct AiNavigationAction {
    /// Texto del botón de navegación (ej. "Ver Mascotas", "Ir a Campañas")
    pub label: String,
    /// Ruta interna de Angular (ej. "/pets", "/campaigns", "/dashboard")
    pub route: String,
}

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct AiChatResponse {
    pub reply: String,
    #[serde(default)]
    pub actions: Vec<AiNavigationAction>,
    /// "gemini" si respondió Gemini API, "local_fallback" si falta GEMINI_API_KEY
    pub provider: String,
    pub model: String,
}
