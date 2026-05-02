use api_rest::{
    connect_postgres,
    create_app,
    workers::catalog_seeder,
    workers::pet_sync_worker::PetSyncWorker,
    infrastructure::messaging::rabbitmq::RabbitMQService,
    infrastructure::persistence::mongodb::MongoDBService,
};
use std::net::SocketAddr;
use std::sync::Arc;
use std::time::Duration;
use tracing_subscriber::{layer::SubscriberExt, util::SubscriberInitExt};

#[tokio::main]
async fn main() {
    // Inicializar el sistema de logs (tracing)
    tracing_subscriber::registry()
        .with(
            tracing_subscriber::EnvFilter::try_from_default_env()
                .unwrap_or_else(|_| "api_rest=debug,tower_http=debug".into()),
        )
        .with(tracing_subscriber::fmt::layer())
        .init();

    dotenvy::dotenv().ok();

    // 1. Conectar Postgres (pool compartido para seeder y app)
    let pool = connect_postgres().await;

    // 2. 🌱 Poblar catálogos (idempotente — seguro en cada arranque)
    tracing::info!("🌱 Ejecutando seeder de catálogos...");
    if let Err(e) = catalog_seeder::run_catalog_seed(&pool).await {
        tracing::error!("❌ Error en el seeder de catálogos: {}", e);
    }

    // 3. Conectar RabbitMQ para el worker de sincronización
    let rabbit_uri = std::env::var("RABBITMQ_URI").expect("RABBITMQ_URI must be set");
    let rabbit_service = loop {
        match RabbitMQService::new(&rabbit_uri).await {
            Ok(s) => break Arc::new(s),
            Err(_) => {
                println!("Worker: RabbitMQ no está listo, reintentando...");
                tokio::time::sleep(Duration::from_secs(2)).await;
            }
        }
    };

    // 4. Conectar MongoDB para el worker de sincronización
    let mongo_uri = std::env::var("MONGODB_URI").expect("MONGODB_URI must be set");
    let mongo_db_name = std::env::var("MONGODB_DB_NAME").expect("MONGODB_DB_NAME must be set");
    let mongo_service = loop {
        match MongoDBService::new(&mongo_uri, &mongo_db_name).await {
            Ok(s) => break Arc::new(s),
            Err(_) => {
                println!("Worker: MongoDB no está listo, reintentando...");
                tokio::time::sleep(Duration::from_secs(2)).await;
            }
        }
    };
    tracing::info!("✅ Conexiones para el Worker inicializadas.");

    // 5. Iniciar Worker de Sincronización en segundo plano
    let worker = Arc::new(PetSyncWorker::new(rabbit_service, mongo_service));
    let worker_clone = worker.clone();
    tokio::spawn(async move {
        worker_clone.run().await;
    });
    tracing::info!("👷 PetSyncWorker corriendo en segundo plano.");

    // 6. Construir la app pasando el pool ya existente
    tracing::info!("📡 Inicializando API y rutas...");
    let app = create_app(pool).await;

    // 7. Iniciar Servidor
    let addr = SocketAddr::from(([0, 0, 0, 0], 8080));
    tracing::info!("🚀 ¡Sistema listo! Servidor escuchando en {}", addr);
    tracing::info!("📖 Documentación disponible en: http://localhost/swagger-ui");

    let listener = tokio::net::TcpListener::bind(&addr).await.unwrap();
    axum::serve(listener, app).await.unwrap();
}
