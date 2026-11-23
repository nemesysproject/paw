use reqwest::Client;
use serde::Deserialize;
use serde_json::{json, Value};
use std::env;

use crate::models::ai::{AiChatMessage, AiNavigationAction};

pub struct GeminiService {
    client: Client,
    api_key: Option<String>,
    model: String,
}

#[derive(Debug, Deserialize)]
struct GeminiStructuredOutput {
    reply: String,
    #[serde(default)]
    actions: Vec<AiNavigationAction>,
}

impl GeminiService {
    pub fn from_env() -> Self {
        let api_key = env::var("GEMINI_API_KEY")
            .ok()
            .map(|k| k.trim().to_string())
            .filter(|k| !k.is_empty());
        let model = env::var("GEMINI_MODEL")
            .ok()
            .map(|m| m.trim().to_string())
            .filter(|m| !m.is_empty())
            .unwrap_or_else(|| "gemini-3.8-flash".to_string());

        Self {
            client: Client::new(),
            api_key,
            model,
        }
    }

    pub fn is_configured(&self) -> bool {
        self.api_key.is_some()
    }

    pub fn model_name(&self) -> &str {
        &self.model
    }

    pub async fn chat_with_context(
        &self,
        system_context_json: &Value,
        history: &[AiChatMessage],
        user_message: &str,
    ) -> Result<(String, Vec<AiNavigationAction>), String> {
        let api_key = self
            .api_key
            .as_deref()
            .ok_or_else(|| "GEMINI_API_KEY no configurada".to_string())?;

        let system_instruction = format!(
            r#"Eres PAW Assistant, el copiloto de IA integrado en el panel administrativo web (PawPortal) de la plataforma PAW / Save Puppy.
Tu trabajo es ayudar a los administradores a consultar información real de mascotas, refugios, veterinarias y campañas publicitarias, así como guiarlos rápidamente a la pantalla correcta.

Reglas estrictas:
1. Responde siempre en español, de forma clara, concisa y profesional.
2. Usa ÚNICAMENTE los datos reales proporcionados en el CONTEXTO_ACTUAL abajo. Si un dato no existe, indícalo con honestidad y sugiere en qué sección registrarlo.
3. Nunca inventes IDs, conteos, teléfonos ni métricas.
4. Sugiere entre 0 y 3 acciones de navegación útiles usando únicamente rutas válidas de PawPortal:
   - "/dashboard" (Dashboard principal)
   - "/pets" (Listado de mascotas)
   - "/pets/create" (Registrar nueva mascota)
   - "/pets/<id>/edit" (Editar una mascota específica usando su id real del contexto)
   - "/campaigns" (Publicidad, anunciantes y campañas gubernamentales)
   - "/shelters" (Refugios)
   - "/users" (Usuarios)
   - "/config" (Configuración)
5. Devuelve SIEMPRE un JSON válido con este formato exacto:
{{
  "reply": "Tu respuesta en texto claro (puedes usar viñetas simples)",
  "actions": [
    {{ "label": "Texto corto del botón", "route": "/ruta" }}
  ]
}}

CONTEXTO_ACTUAL (datos en vivo de PostgreSQL):
{}"#,
            serde_json::to_string(system_context_json).unwrap_or_else(|_| "{}".to_string())
        );

        let mut contents: Vec<Value> = history
            .iter()
            .rev()
            .take(8)
            .collect::<Vec<_>>()
            .into_iter()
            .rev()
            .map(|msg| {
                let role = if msg.role == "assistant" || msg.role == "model" {
                    "model"
                } else {
                    "user"
                };
                json!({
                    "role": role,
                    "parts": [{ "text": msg.content }]
                })
            })
            .collect();

        contents.push(json!({
            "role": "user",
            "parts": [{ "text": user_message }]
        }));

        let payload = json!({
            "system_instruction": {
                "parts": [{ "text": system_instruction }]
            },
            "contents": contents,
            "generationConfig": {
                "temperature": 0.2,
                "maxOutputTokens": 900,
                "responseMimeType": "application/json"
            }
        });

        let url = format!(
            "https://generativelanguage.googleapis.com/v1beta/models/{}:generateContent?key={}",
            self.model, api_key
        );

        let response = self
            .client
            .post(&url)
            .json(&payload)
            .send()
            .await
            .map_err(|e| format!("Error de red llamando a Gemini: {}", e))?;

        if !response.status().is_success() {
            let status = response.status();
            let err_body = response.text().await.unwrap_or_default();
            return Err(format!("Gemini API devolvió {}: {}", status, err_body));
        }

        let body: Value = response
            .json()
            .await
            .map_err(|e| format!("Error leyendo JSON de Gemini: {}", e))?;

        let raw_text = body
            .pointer("/candidates/0/content/parts/0/text")
            .and_then(|v| v.as_str())
            .ok_or_else(|| "Gemini no devolvió contenido textual".to_string())?;

        match serde_json::from_str::<GeminiStructuredOutput>(raw_text) {
            Ok(parsed) => Ok((parsed.reply, parsed.actions)),
            Err(_) => Ok((raw_text.to_string(), Vec::new())),
        }
    }
}
