use axum::{routing::get, Router};
use sqlx::PgPool;
use std::sync::Arc;
use std::time::Duration;
use tower_http::trace::TraceLayer;
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
    dotenvy::dotenv().ok();
    let database_url = std::env::var("DATABASE_URL").expect("DATABASE_URL must be set");
    let mut retry_count = 0;
    loop {
        match PgPool::connect(&database_url).await {
            Ok(p) => {
                println!("✅ Postgres: Conexión establecida correctamente.");
                return p;
            }
            Err(_) if retry_count < 5 => {
                retry_count += 1;
                println!("Postgres no está listo, reintentando ({}/5)...", retry_count);
                tokio::time::sleep(Duration::from_secs(2)).await;
            }
            Err(e) => panic!("No se pudo conectar a Postgres: {}", e),
        }
    }
}

/// Construye el Router de la aplicación a partir de un pool ya inicializado.
pub async fn create_app(pool: PgPool) -> Router {
    dotenvy::dotenv().ok();

    // Inicializar RabbitMQ
    let rabbit_uri = std::env::var("RABBITMQ_URI").expect("RABBITMQ_URI must be set");
    let mut retry_count = 0;
    let rabbit_service = loop {
        match RabbitMQService::new(&rabbit_uri).await {
            Ok(s) => break Arc::new(s),
            Err(_) if retry_count < 10 => {
                retry_count += 1;
                println!("RabbitMQ no está listo, reintentando ({}/10)...", retry_count);
                tokio::time::sleep(Duration::from_secs(2)).await;
            }
            Err(e) => panic!("No se pudo conectar a RabbitMQ: {}", e),
        }
    };
    println!("✅ RabbitMQ: Conexión establecida correctamente.");

    // Inicializar MongoDB
    let mongo_uri = std::env::var("MONGODB_URI").expect("MONGODB_URI must be set");
    let mongo_db_name = std::env::var("MONGODB_DB_NAME").expect("MONGODB_DB_NAME must be set");
    let mongo_service = loop {
        match MongoDBService::new(&mongo_uri, &mongo_db_name).await {
            Ok(s) => break Arc::new(s),
            Err(_) => {
                println!("MongoDB no está listo, reintentando...");
                tokio::time::sleep(Duration::from_secs(2)).await;
            }
        }
    };
    println!("✅ MongoDB: Conexión establecida correctamente.");

    let state = AppState {
        pool,
        cloudinary_service: Arc::new(CloudinaryService::new()),
        rabbit_service,
        mongo_service,
    };

    // Construir la aplicación (Rutas y Middleware)
    Router::new()
        .merge(SwaggerUi::new("/swagger-ui").url("/api-docs/openapi.json", ApiDoc::openapi()))
        .route("/health", get(health_check))
        .nest("/api/v1", routes::api_routes())
        .with_state(state)
        .layer(TraceLayer::new_for_http())
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
