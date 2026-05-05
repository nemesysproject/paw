use axum::{routing::get, Router};
use sqlx::PgPool;
use std::sync::Arc;
use std::time::Duration;
use tower_http::trace::TraceLayer;
use tower_http::cors::{Any, CorsLayer};
use axum::http::Method;
use utoipa::OpenApi;
use utoipa_swagger_ui::SwaggerUi;

pub mod db;
pub mod docs;
pub mod handlers;
pub mod infrastructure;
pub mod models;
pub mod repositories;
pub mod routes;
pub mod workers;

use crate::docs::ApiDoc;
use crate::infrastructure::messaging::rabbitmq::RabbitMQService;
use crate::infrastructure::persistence::mongodb::MongoDBService;
use crate::infrastructure::services::cloudinary::CloudinaryService;

#[derive(Clone)]
pub struct AppState {
    pub pool: PgPool,
    pub cloudinary_service: Arc<CloudinaryService>,
    pub rabbit_service: Arc<RabbitMQService>,
    pub mongo_service: Arc<MongoDBService>,
}

/// Crea el pool de conexiones a Postgres con reintentos.
pub async fn connect_postgres() -> PgPool {
    let database_url = std::env::var("DATABASE_URL").unwrap_or_else(|_| {
        eprintln!("❌ ERROR: La variable de entorno DATABASE_URL no está definida.");
        eprintln!("--- Variables de entorno detectadas ---");
        for (key, value) in std::env::vars() {
            eprintln!("{}: {}", key, value);
        }
        eprintln!("---------------------------------------");
        
        // Intentar cargar .env local como último recurso
        dotenvy::dotenv().ok();
        std::env::var("DATABASE_URL").expect("DATABASE_URL no encontrada incluso tras cargar .env")
    });

    let mut retry_count = 0;
    loop {
        match PgPool::connect(&database_url).await {
            Ok(p) => {
                println!("✅ Postgres: Conexión establecida correctamente.");
                return p;
            }
            Err(e) if retry_count < 10 => {
                retry_count += 1;
                println!("Postgres no está listo ({}), reintentando ({}/10)...", e, retry_count);
                tokio::time::sleep(Duration::from_secs(2)).await;
            }
            Err(e) => panic!("No se pudo conectar a Postgres: {}", e),
        }
    }
}

/// Construye el Router de la aplicación a partir de servicios ya inicializados.
pub async fn create_app(
    pool: PgPool,
    rabbit_service: Arc<RabbitMQService>,
    mongo_service: Arc<MongoDBService>,
) -> Router {
    let state = AppState {
        pool,
        cloudinary_service: Arc::new(CloudinaryService::new()),
        rabbit_service,
        mongo_service,
    };

    // Construir la aplicación (Rutas y Middleware)
    let cors = CorsLayer::new()
        .allow_origin(Any)
        .allow_methods([Method::GET, Method::POST, Method::PUT, Method::DELETE])
        .allow_headers(Any);

    Router::new()
        .merge(SwaggerUi::new("/swagger-ui").url("/api-docs/openapi.json", ApiDoc::openapi()))
        .route("/health", get(health_check))
        .nest("/api/v1", routes::api_routes())
        .with_state(state)
        .layer(TraceLayer::new_for_http())
        .layer(cors)
}

/// Verifica que el servicio esté vivo.
#[utoipa::path(
    get,
    path = "/health",
    responses(
        (status = 200, description = "El servicio está operando correctamente", body = String)
    ),
    tag = "Health"
)]
pub async fn health_check() -> &'static str {
    "OK - API Rest (Rust) is running"
}
