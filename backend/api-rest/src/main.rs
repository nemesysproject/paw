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
use tokio::net::TcpListener;
use std::env; // <--- ESTA ES LA LÍNEA QUE FALTA

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    // Inicializar logs
    tracing_subscriber::fmt()
        .with_env_filter(tracing_subscriber::EnvFilter::from_default_env())
        .init();

    tracing::info!("🚀 Iniciando Save Puppy API (Monolito Rust)...");

    // Intenta cargar el .env y muestra dónde lo encontró
    match dotenvy::dotenv() {
        Ok(path) => println!("✅ Archivo .env cargado desde: {:?}", path),
        Err(e) => println!("⚠️ No se pudo cargar el archivo .env: {}", e),
    }

     // Verifica si la variable DATABASE_URL existe en el entorno (venga de .env o de Docker)
    match std::env::var("DATABASE_URL") {
        Ok(val) => println!("✅ DATABASE_URL detectada."),
        Err(_) => {
            println!("❌ ERROR: DATABASE_URL no está definida en el entorno.");
            // Imprime todas las variables disponibles para ver qué recibió Rust
            for (key, value) in std::env::vars() {
                println!("{}: {}", key, value);
            }
        }
    }

    println!("--- Verificando Variables de Entorno ---");
    
    // Lista de variables clave que definiste en el compose
    let vars = ["DATABASE_URL", "RABBITMQ_URI", "MONGODB_URI", "RUST_LOG"];
    
    for var in vars.iter() {
        match env::var(var) {
            Ok(val) => println!("{}: Configurada correctamente", var),
            Err(_) => println!("{}: NO ENCONTRADA", var),
        }
    }
    println!("---------------------------------------");

    // 1. Conectar Postgres (pool compartido para seeder y app)
    let pool = connect_postgres().await;

    // 1.5. 🚀 Ejecutar migraciones automáticas
    tracing::info!("🚀 Ejecutando migraciones de base de datos...");
    sqlx::migrate!("./migrations")
        .run(&pool)
        .await
        .map_err(|e| {
            tracing::error!("❌ Error ejecutando migraciones: {}", e);
            e
        })?;
    tracing::info!("✅ Migraciones completadas correctamente.");

    // 2. 🌱 Poblar catálogos (idempotente — seguro en cada arranque)
    tracing::info!("🌱 Ejecutando seeder de catálogos...");
    if let Err(e) = catalog_seeder::run_catalog_seed(&pool).await {
        tracing::error!("❌ Error en el seeder de catálogos: {}", e);
    }

    // 3. 🐰 Inicializar Workers de segundo plano (RabbitMQ Consumers)
    tracing::info!("🐰 Iniciando workers de RabbitMQ...");
    let rabbit_uri = std::env::var("RABBITMQ_URI").expect("RABBITMQ_URI must be set");
    let mut retry_count = 0;
    let rabbit_service = loop {
        match RabbitMQService::new(&rabbit_uri).await {
            Ok(s) => break Arc::new(s),
            Err(e) if retry_count < 10 => {
                retry_count += 1;
                tracing::warn!("RabbitMQ no está listo ({}), reintentando ({}/10)...", e, retry_count);
                tokio::time::sleep(std::time::Duration::from_secs(2)).await;
            }
            Err(e) => panic!("No se pudo conectar a RabbitMQ: {}", e),
        }
    };
    tracing::info!("✅ RabbitMQ: Conexión establecida correctamente.");
    
    tracing::info!("🍃 Iniciando conexión a MongoDB...");
    let mongo_uri = std::env::var("MONGODB_URI").expect("MONGODB_URI must be set");
    let mongo_db_name = std::env::var("MONGODB_DB_NAME").expect("MONGODB_DB_NAME must be set");
    let mut retry_count = 0;
    let mongo_service = loop {
        match MongoDBService::new(&mongo_uri, &mongo_db_name).await {
            Ok(s) => break Arc::new(s),
            Err(e) if retry_count < 10 => {
                retry_count += 1;
                tracing::warn!("MongoDB no está listo ({}), reintentando ({}/10)...", e, retry_count);
                tokio::time::sleep(std::time::Duration::from_secs(2)).await;
            }
            Err(e) => panic!("No se pudo conectar a MongoDB: {}", e),
        }
    };
    tracing::info!("✅ MongoDB: Conexión establecida correctamente.");

    let pet_sync_worker = PetSyncWorker::new(rabbit_service.clone(), mongo_service.clone());
    tokio::spawn(async move {
        if let Err(e) = pet_sync_worker.run().await {
            tracing::error!("❌ Error en PetSyncWorker: {}", e);
        }
    });

    // 4. 🌐 Configurar y arrancar servidor Axum
    let app = create_app(pool, rabbit_service, mongo_service).await;

    let addr = SocketAddr::from(([0, 0, 0, 0], 8080));
    let listener = TcpListener::bind(addr).await.map_err(|e| {
        tracing::error!("❌ Error bindeando puerto 8080: {}", e);
        e
    })?;
    
    tracing::info!("✅ Servidor escuchando en http://{}", addr);
    
    axum::serve(listener, app).await.map_err(|e| {
        tracing::error!("❌ Error en el servidor axum: {}", e);
        e
    })?;

    Ok(())
}
